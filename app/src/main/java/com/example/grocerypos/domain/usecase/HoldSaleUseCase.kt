package com.example.grocerypos.domain.usecase

import com.example.grocerypos.data.local.dao.AuditLogDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.SaleDao
import com.example.grocerypos.data.local.dao.SaleItemDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.entity.AuditLogEntity
import com.example.grocerypos.data.local.entity.SaleEntity
import com.example.grocerypos.data.local.entity.SaleItemEntity
import com.example.grocerypos.data.local.entity.SyncEventEntity
import com.example.grocerypos.data.local.mapper.toDomain
import com.example.grocerypos.domain.model.AuditAction
import com.example.grocerypos.domain.model.EmptySaleException
import com.example.grocerypos.domain.model.HoldSaleCommand
import com.example.grocerypos.domain.model.InvalidSaleStateException
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.ProductUnavailableException
import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.SyncOperation
import com.example.grocerypos.domain.model.SyncStatus
import com.example.grocerypos.domain.service.SaleCalculator
import com.example.grocerypos.domain.transaction.TransactionRunner
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to park / hold an in-progress POS sale.
 *
 * Held sales retain calculated line items without deducting inventory
 * or allocating a finalized invoice number.
 */
@Singleton
class HoldSaleUseCase @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val productDao: ProductDao,
    private val stockBalanceDao: StockBalanceDao,
    private val auditLogDao: AuditLogDao,
    private val syncEventDao: SyncEventDao,
    private val saleCalculator: SaleCalculator
) {

    suspend operator fun invoke(command: HoldSaleCommand): Result<Sale> = runCatching {
        transactionRunner.runInTransaction {
            val existing = saleDao.getSaleById(command.saleId)
            if (existing != null && (existing.status == SaleStatus.COMPLETED || existing.status == SaleStatus.VOIDED)) {
                throw InvalidSaleStateException("Cannot hold a sale that is already ${existing.status}.")
            }

            if (command.items.isEmpty()) {
                throw EmptySaleException("Cannot hold an empty sale.")
            }

            val productsMap = mutableMapOf<String, Product>()
            val productCostsMap = mutableMapOf<String, Money>()

            for (item in command.items) {
                val productEntity = productDao.getProductById(item.productId)
                    ?: throw ProductUnavailableException("Product '${item.productId}' not found.")
                productsMap[item.productId] = productEntity.toDomain()
                val stockBalance = stockBalanceDao.getStockBalance(item.productId)
                productCostsMap[item.productId] = stockBalance?.averageCost ?: Money.ZERO
            }

            val totals = saleCalculator.calculateSale(
                items = command.items,
                productsMap = productsMap,
                productCostsMap = productCostsMap,
                saleDiscount = command.saleDiscount,
                saleTaxRule = command.taxRule,
                payments = emptyList(),
                customerId = command.customerId
            )

            val now = System.currentTimeMillis()

            // Delete old items for this saleId if updating
            saleItemDao.deleteItemsForSale(command.saleId)

            val saleEntity = SaleEntity(
                saleId = command.saleId,
                shopId = command.shopId,
                deviceId = command.deviceId,
                invoiceNumber = null,
                cashierId = command.cashierId,
                customerId = command.customerId,
                subtotal = totals.subtotal,
                itemDiscount = totals.itemDiscount,
                saleDiscount = totals.saleDiscount,
                tax = totals.tax,
                grandTotal = totals.grandTotal,
                paidAmount = Money.ZERO,
                dueAmount = totals.grandTotal,
                status = SaleStatus.HELD,
                paymentStatus = PaymentStatus.UNPAID,
                notes = command.notes,
                createdAt = existing?.createdAt ?: now,
                completedAt = null,
                updatedAt = now
            )
            saleDao.upsertSale(saleEntity)

            val itemEntities = totals.items.map { item ->
                SaleItemEntity(
                    saleItemId = UUID.randomUUID().toString(),
                    saleId = command.saleId,
                    productId = item.productId,
                    productName = item.productName,
                    soldUnitId = item.soldUnitId,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    grossAmount = item.grossAmount,
                    discount = item.discount,
                    tax = item.tax,
                    netAmount = item.netAmount,
                    costAtSale = item.costAtSale,
                    createdAt = now
                )
            }
            saleItemDao.insertSaleItems(itemEntities)

            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    shopId = command.shopId,
                    userId = command.cashierId,
                    action = AuditAction.SALE_HELD,
                    entityType = "SALE",
                    entityId = command.saleId,
                    details = "Sale held with total ${totals.grandTotal.toPlainDecimalString()}",
                    timestamp = now
                )
            )

            syncEventDao.insertSyncEvent(
                SyncEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    shopId = command.shopId,
                    deviceId = command.deviceId,
                    entityType = "SALE",
                    entityId = command.saleId,
                    operation = SyncOperation.CREATE,
                    syncStatus = SyncStatus.PENDING,
                    timestamp = now
                )
            )

            Sale(
                saleId = command.saleId,
                shopId = command.shopId,
                deviceId = command.deviceId,
                invoiceNumber = null,
                cashierId = command.cashierId,
                customerId = command.customerId,
                subtotal = totals.subtotal,
                itemDiscount = totals.itemDiscount,
                saleDiscount = totals.saleDiscount,
                tax = totals.tax,
                grandTotal = totals.grandTotal,
                paidAmount = Money.ZERO,
                dueAmount = totals.grandTotal,
                status = SaleStatus.HELD,
                paymentStatus = PaymentStatus.UNPAID,
                notes = command.notes,
                createdAt = existing?.createdAt ?: now,
                completedAt = null,
                updatedAt = now,
                items = itemEntities.map { it.toDomain() },
                payments = emptyList()
            )
        }
    }
}
