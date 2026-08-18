package com.example.grocerypos.domain.usecase

import com.example.grocerypos.data.local.dao.AuditLogDao
import com.example.grocerypos.data.local.dao.CashMovementDao
import com.example.grocerypos.data.local.dao.CustomerDao
import com.example.grocerypos.data.local.dao.CustomerLedgerDao
import com.example.grocerypos.data.local.dao.InvoiceSequenceDao
import com.example.grocerypos.data.local.dao.PaymentDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.SaleDao
import com.example.grocerypos.data.local.dao.SaleItemDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.StockMovementDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.entity.AuditLogEntity
import com.example.grocerypos.data.local.entity.CashMovementEntity
import com.example.grocerypos.data.local.entity.CustomerLedgerEntryEntity
import com.example.grocerypos.data.local.entity.PaymentEntity
import com.example.grocerypos.data.local.entity.SaleEntity
import com.example.grocerypos.data.local.entity.SaleItemEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.StockMovementEntity
import com.example.grocerypos.data.local.entity.SyncEventEntity
import com.example.grocerypos.data.local.mapper.toDomain
import com.example.grocerypos.domain.model.AuditAction
import com.example.grocerypos.domain.model.CashMovementType
import com.example.grocerypos.domain.model.CompleteSaleCommand
import com.example.grocerypos.domain.model.CompleteSaleResult
import com.example.grocerypos.domain.model.CustomerLedgerType
import com.example.grocerypos.domain.model.EmptySaleException
import com.example.grocerypos.domain.model.InsufficientStockException
import com.example.grocerypos.domain.model.InvalidSaleStateException
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.MovementType
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.ProductUnavailableException
import com.example.grocerypos.domain.model.Quantity
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
 * Domain engine responsible for executing grocery sale completion atomically.
 *
 * Atomically orchestrates:
 * 1. Idempotency check via operationId/draftSaleId
 * 2. Product and customer validation
 * 3. Exact fixed-point financial totals calculation
 * 4. Deducting stock balances and recording stock movements
 * 5. Allocating sequential invoice numbers
 * 6. Persisting Sale, Sale Items, and Payments
 * 7. Recording Customer Ledger entry for credit sales
 * 8. Recording Cash Movement entry for cash payments
 * 9. Writing Audit Log
 * 10. Writing Local Sync Event
 */
@Singleton
class CompleteSaleUseCase @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val paymentDao: PaymentDao,
    private val productDao: ProductDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val customerDao: CustomerDao,
    private val customerLedgerDao: CustomerLedgerDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao,
    private val syncEventDao: SyncEventDao,
    private val invoiceSequenceDao: InvoiceSequenceDao,
    private val saleCalculator: SaleCalculator
) {

    suspend operator fun invoke(command: CompleteSaleCommand): Result<CompleteSaleResult> = runCatching {
        transactionRunner.runInTransaction {
            val targetSaleId = command.draftSaleId ?: command.operationId

            // 1. Idempotency Check
            val existingSale = saleDao.getSaleWithDetails(targetSaleId)
                ?: if (targetSaleId != command.operationId) saleDao.getSaleWithDetails(command.operationId) else null

            if (existingSale != null && existingSale.sale.status == SaleStatus.COMPLETED) {
                return@runInTransaction CompleteSaleResult(
                    sale = existingSale.toDomain(),
                    changeReturned = Money.ZERO,
                    isIdempotentReplay = true
                )
            }

            if (existingSale != null && existingSale.sale.status == SaleStatus.VOIDED) {
                throw InvalidSaleStateException("Cannot complete a VOIDED sale ($targetSaleId).")
            }

            // 2. Validate Items & Fetch Products
            if (command.items.isEmpty()) {
                throw EmptySaleException("Sale must contain at least one item.")
            }

            val productsMap = mutableMapOf<String, Product>()
            val productCostsMap = mutableMapOf<String, Money>()

            for (item in command.items) {
                val productEntity = productDao.getProductById(item.productId)
                    ?: throw ProductUnavailableException("Product '${item.productId}' not found.")
                val product = productEntity.toDomain()
                productsMap[item.productId] = product

                val stockBalance = stockBalanceDao.getStockBalance(item.productId)
                productCostsMap[item.productId] = stockBalance?.averageCost ?: Money.ZERO
            }

            // 3. Validate Customer
            if (!command.customerId.isNullOrBlank()) {
                val customer = customerDao.getCustomerById(command.customerId)
                    ?: throw IllegalArgumentException("Customer '${command.customerId}' not found.")
                if (!customer.isActive) {
                    throw IllegalStateException("Customer '${customer.name}' is inactive.")
                }
            }

            // 4. Financial Calculations
            val totals = saleCalculator.calculateSale(
                items = command.items,
                productsMap = productsMap,
                productCostsMap = productCostsMap,
                saleDiscount = command.saleDiscount,
                saleTaxRule = command.taxRule,
                payments = command.payments,
                customerId = command.customerId
            )

            val now = System.currentTimeMillis()

            // 5. Stock Validations, Deductions and Movements
            for (item in totals.items) {
                val currentStock = stockBalanceDao.getStockBalance(item.productId)
                val currentQuantity = currentStock?.quantity ?: Quantity.ZERO
                val avgCost = currentStock?.averageCost ?: Money.ZERO

                // Stock validation inside transaction
                if (!command.allowNegativeStock && currentQuantity < item.deductBaseQuantity) {
                    throw InsufficientStockException(
                        productId = item.productId,
                        productName = item.productName,
                        requiredQuantity = item.deductBaseQuantity,
                        availableQuantity = currentQuantity
                    )
                }

                val newStockQuantity = currentQuantity - item.deductBaseQuantity
                stockBalanceDao.upsertStockBalance(
                    StockBalanceEntity(
                        productId = item.productId,
                        quantity = newStockQuantity,
                        averageCost = avgCost,
                        updatedAt = now
                    )
                )

                stockMovementDao.insertStockMovement(
                    StockMovementEntity(
                        movementId = UUID.randomUUID().toString(),
                        shopId = command.shopId,
                        deviceId = command.deviceId,
                        productId = item.productId,
                        batchId = null,
                        movementType = MovementType.SALE,
                        quantity = item.deductBaseQuantity,
                        unitCost = item.costAtSale,
                        referenceType = "SALE",
                        referenceId = targetSaleId,
                        createdBy = command.cashierId,
                        createdAt = now
                    )
                )
            }

            // 6. Allocate Next Sequential Invoice Number
            val invoiceNumber = invoiceSequenceDao.allocateNextInvoiceNumber(
                shopId = command.shopId,
                defaultPrefix = command.invoicePrefix
            )

            // 7. Clean existing draft items/payments if resuming/completing a draft/held sale
            if (command.draftSaleId != null) {
                saleItemDao.deleteItemsForSale(targetSaleId)
                paymentDao.deletePaymentsForSale(targetSaleId)
            }

            // 8. Persist Sale
            val saleEntity = SaleEntity(
                saleId = targetSaleId,
                shopId = command.shopId,
                deviceId = command.deviceId,
                invoiceNumber = invoiceNumber,
                cashierId = command.cashierId,
                customerId = command.customerId,
                subtotal = totals.subtotal,
                itemDiscount = totals.itemDiscount,
                saleDiscount = totals.saleDiscount,
                tax = totals.tax,
                grandTotal = totals.grandTotal,
                paidAmount = totals.appliedPaidAmount,
                dueAmount = totals.dueAmount,
                status = SaleStatus.COMPLETED,
                paymentStatus = totals.paymentStatus,
                notes = command.notes,
                createdAt = existingSale?.sale?.createdAt ?: now,
                completedAt = now,
                updatedAt = now
            )
            saleDao.upsertSale(saleEntity)

            // 9. Persist Sale Items
            val saleItemEntities = totals.items.map { item ->
                SaleItemEntity(
                    saleItemId = UUID.randomUUID().toString(),
                    saleId = targetSaleId,
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
            saleItemDao.insertSaleItems(saleItemEntities)

            // 10. Persist Payments
            val paymentEntities = totals.payments.map { pmt ->
                PaymentEntity(
                    paymentId = UUID.randomUUID().toString(),
                    saleId = targetSaleId,
                    shopId = command.shopId,
                    method = pmt.method,
                    amount = pmt.amount,
                    referenceNumber = pmt.referenceNumber,
                    receivedAt = now,
                    receivedBy = command.cashierId
                )
            }
            if (paymentEntities.isNotEmpty()) {
                paymentDao.insertPayments(paymentEntities)
            }

            // 11. Record Customer Ledger Entry (Credit Sale)
            if (totals.dueAmount.isPositive() && !command.customerId.isNullOrBlank()) {
                customerLedgerDao.insertLedgerEntry(
                    CustomerLedgerEntryEntity(
                        entryId = UUID.randomUUID().toString(),
                        customerId = command.customerId,
                        shopId = command.shopId,
                        type = CustomerLedgerType.SALE_CREDIT,
                        amount = totals.dueAmount,
                        referenceType = "SALE",
                        referenceId = targetSaleId,
                        notes = "Invoice #$invoiceNumber - Credit Sale Due",
                        createdBy = command.cashierId,
                        createdAt = now
                    )
                )
            }

            // 12. Record Cash Movement
            if (totals.totalCashPaid.isPositive()) {
                val netCashDrawerIn = totals.totalCashPaid - totals.changeReturned
                cashMovementDao.insertCashMovement(
                    CashMovementEntity(
                        movementId = UUID.randomUUID().toString(),
                        shopId = command.shopId,
                        deviceId = command.deviceId,
                        type = CashMovementType.SALE_CASH,
                        amount = netCashDrawerIn,
                        referenceType = "SALE",
                        referenceId = targetSaleId,
                        notes = "Invoice #$invoiceNumber (Cash: ${totals.totalCashPaid.toPlainDecimalString()}, Change: ${totals.changeReturned.toPlainDecimalString()})",
                        createdBy = command.cashierId,
                        createdAt = now
                    )
                )
            }

            // 13. Audit Log
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    logId = UUID.randomUUID().toString(),
                    shopId = command.shopId,
                    userId = command.cashierId,
                    action = AuditAction.SALE_COMPLETED,
                    entityType = "SALE",
                    entityId = targetSaleId,
                    details = "Completed sale #$invoiceNumber, Total: ${totals.grandTotal.toPlainDecimalString()}, Paid: ${totals.appliedPaidAmount.toPlainDecimalString()}, Due: ${totals.dueAmount.toPlainDecimalString()}",
                    timestamp = now
                )
            )

            // 14. Sync Event
            syncEventDao.insertSyncEvent(
                SyncEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    shopId = command.shopId,
                    deviceId = command.deviceId,
                    entityType = "SALE",
                    entityId = targetSaleId,
                    operation = SyncOperation.CREATE,
                    syncStatus = SyncStatus.PENDING,
                    timestamp = now
                )
            )

            // 15. Assemble Completed Sale
            val domainSale = Sale(
                saleId = targetSaleId,
                shopId = command.shopId,
                deviceId = command.deviceId,
                invoiceNumber = invoiceNumber,
                cashierId = command.cashierId,
                customerId = command.customerId,
                subtotal = totals.subtotal,
                itemDiscount = totals.itemDiscount,
                saleDiscount = totals.saleDiscount,
                tax = totals.tax,
                grandTotal = totals.grandTotal,
                paidAmount = totals.appliedPaidAmount,
                dueAmount = totals.dueAmount,
                status = SaleStatus.COMPLETED,
                paymentStatus = totals.paymentStatus,
                notes = command.notes,
                createdAt = existingSale?.sale?.createdAt ?: now,
                completedAt = now,
                updatedAt = now,
                items = saleItemEntities.map { it.toDomain() },
                payments = paymentEntities.map { it.toDomain() }
            )

            CompleteSaleResult(
                sale = domainSale,
                changeReturned = totals.changeReturned,
                isIdempotentReplay = false
            )
        }
    }
}
