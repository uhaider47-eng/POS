package com.example.grocerypos.domain.usecase

import com.example.grocerypos.data.local.dao.AuditLogDao
import com.example.grocerypos.data.local.dao.CashMovementDao
import com.example.grocerypos.data.local.dao.CustomerLedgerDao
import com.example.grocerypos.data.local.dao.SaleDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.StockMovementDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.entity.AuditLogEntity
import com.example.grocerypos.data.local.entity.CashMovementEntity
import com.example.grocerypos.data.local.entity.CustomerLedgerEntryEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.StockMovementEntity
import com.example.grocerypos.data.local.entity.SyncEventEntity
import com.example.grocerypos.data.local.mapper.toDomain
import com.example.grocerypos.domain.model.AuditAction
import com.example.grocerypos.domain.model.CashMovementType
import com.example.grocerypos.domain.model.CustomerLedgerType
import com.example.grocerypos.domain.model.InvalidSaleStateException
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.MovementType
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.SyncOperation
import com.example.grocerypos.domain.model.SyncStatus
import com.example.grocerypos.domain.transaction.TransactionRunner
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class VoidSaleCommand(
    val saleId: String,
    val cashierId: String,
    val reason: String
)

/**
 * Atomically voids a sale and performs all necessary inventory, financial,
 * and ledger reversals if the sale had previously completed.
 */
@Singleton
class VoidSaleUseCase @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val saleDao: SaleDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val customerLedgerDao: CustomerLedgerDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao,
    private val syncEventDao: SyncEventDao
) {

    suspend operator fun invoke(command: VoidSaleCommand): Result<Sale> = runCatching {
        transactionRunner.runInTransaction {
            val saleDetails = saleDao.getSaleWithDetails(command.saleId)
                ?: throw IllegalArgumentException("Sale '${command.saleId}' not found.")

            val currentSale = saleDetails.sale
            if (currentSale.status == SaleStatus.VOIDED) {
                throw InvalidSaleStateException("Sale '${command.saleId}' is already VOIDED.")
            }

            val now = System.currentTimeMillis()

            // If sale was completed, reverse inventory and financials
            if (currentSale.status == SaleStatus.COMPLETED) {
                // 1. Revert stock
                for (item in saleDetails.items) {
                    val currentStock = stockBalanceDao.getStockBalance(item.productId)
                    val currentQty = currentStock?.quantity ?: Quantity.ZERO
                    val avgCost = currentStock?.averageCost ?: Money.ZERO

                    val restoredQty = currentQty + item.quantity
                    stockBalanceDao.upsertStockBalance(
                        StockBalanceEntity(
                            productId = item.productId,
                            quantity = restoredQty,
                            averageCost = avgCost,
                            updatedAt = now
                        )
                    )

                    stockMovementDao.insertStockMovement(
                        StockMovementEntity(
                            movementId = UUID.randomUUID().toString(),
                            shopId = currentSale.shopId,
                            deviceId = currentSale.deviceId,
                            productId = item.productId,
                            batchId = null,
                            movementType = MovementType.VOID_RETURN,
                            quantity = item.quantity,
                            unitCost = item.costAtSale,
                            referenceType = "VOID_SALE",
                            referenceId = command.saleId,
                            createdBy = command.cashierId,
                            createdAt = now
                        )
                    )
                }

                // 2. Revert Customer Ledger if credit was recorded
                if (currentSale.dueAmount.isPositive() && !currentSale.customerId.isNullOrBlank()) {
                    customerLedgerDao.insertLedgerEntry(
                        CustomerLedgerEntryEntity(
                            entryId = UUID.randomUUID().toString(),
                            customerId = currentSale.customerId,
                            shopId = currentSale.shopId,
                            type = CustomerLedgerType.REFUND_CREDIT,
                            amount = currentSale.dueAmount,
                            referenceType = "VOID_SALE",
                            referenceId = command.saleId,
                            notes = "Voided sale #${currentSale.invoiceNumber ?: command.saleId}: ${command.reason}",
                            createdBy = command.cashierId,
                            createdAt = now
                        )
                    )
                }

                // 3. Revert Cash Drawer if cash was collected
                val cashPaid = saleDetails.payments
                    .filter { it.method == PaymentMethod.CASH }
                    .fold(Money.ZERO) { acc, p -> acc + p.amount }

                if (cashPaid.isPositive()) {
                    val netRefunded = if (cashPaid > currentSale.paidAmount) currentSale.paidAmount else cashPaid
                    cashMovementDao.insertCashMovement(
                        CashMovementEntity(
                            movementId = UUID.randomUUID().toString(),
                            shopId = currentSale.shopId,
                            deviceId = currentSale.deviceId,
                            type = CashMovementType.SALE_VOID_REFUND,
                            amount = netRefunded,
                            referenceType = "VOID_SALE",
                            referenceId = command.saleId,
                            notes = "Voided sale #${currentSale.invoiceNumber ?: command.saleId}: ${command.reason}",
                            createdBy = command.cashierId,
                            createdAt = now
                        )
                    )
                }
            }

            // Update status to VOIDED
            val updatedSaleEntity = currentSale.copy(
                status = SaleStatus.VOIDED,
                notes = if (currentSale.notes.isBlank()) "Void reason: ${command.reason}" else "${currentSale.notes} | Void reason: ${command.reason}",
                updatedAt = now
            )
            saleDao.upsertSale(updatedSaleEntity)

            // Audit log
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    shopId = currentSale.shopId,
                    userId = command.cashierId,
                    action = AuditAction.SALE_VOIDED,
                    entityType = "SALE",
                    entityId = command.saleId,
                    details = "Voided sale #${currentSale.invoiceNumber ?: command.saleId}. Reason: ${command.reason}",
                    timestamp = now
                )
            )

            // Sync event
            syncEventDao.insertSyncEvent(
                SyncEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    shopId = currentSale.shopId,
                    deviceId = currentSale.deviceId,
                    entityType = "SALE",
                    entityId = command.saleId,
                    operation = SyncOperation.UPDATE,
                    syncStatus = SyncStatus.PENDING,
                    timestamp = now
                )
            )

            updatedSaleEntity.toDomain(
                items = saleDetails.items.map { it.toDomain() },
                payments = saleDetails.payments.map { it.toDomain() }
            )
        }
    }
}
