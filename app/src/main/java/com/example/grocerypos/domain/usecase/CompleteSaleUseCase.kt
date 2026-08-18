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
import com.example.grocerypos.data.local.dao.SaleOperationDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.StockMovementDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.entity.AuditLogEntity
import com.example.grocerypos.data.local.entity.CashMovementEntity
import com.example.grocerypos.data.local.entity.CustomerLedgerEntryEntity
import com.example.grocerypos.data.local.entity.PaymentEntity
import com.example.grocerypos.data.local.entity.SaleEntity
import com.example.grocerypos.data.local.entity.SaleItemEntity
import com.example.grocerypos.data.local.entity.SaleOperationEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.StockMovementEntity
import com.example.grocerypos.data.local.entity.SyncEventEntity
import com.example.grocerypos.data.local.mapper.toDomain
import com.example.grocerypos.domain.model.AuditAction
import com.example.grocerypos.domain.model.CashMovementType
import com.example.grocerypos.domain.model.CompleteSaleCommand
import com.example.grocerypos.domain.model.CompleteSaleResult
import com.example.grocerypos.domain.model.CustomerLedgerType
import com.example.grocerypos.domain.model.CustomerNotFoundException
import com.example.grocerypos.domain.model.EmptySaleException
import com.example.grocerypos.domain.model.InactiveCustomerException
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
 * Domain engine responsible for executing grocery sale completion atomically with
 * hardened durable idempotency, concurrency-safe conditional stock deductions,
 * and strict multi-stage failure rollback guarantees.
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
    private val saleOperationDao: SaleOperationDao,
    private val saleCalculator: SaleCalculator,
    private val failureHook: SaleFailureHook = SaleFailureHook { }
) {

    suspend operator fun invoke(command: CompleteSaleCommand): Result<CompleteSaleResult> = runCatching {
        try {
            transactionRunner.runInTransaction {
                // 1. Durable Idempotency Check via sale_operations table
                val existingOp = saleOperationDao.getOperation(command.operationId)
                if (existingOp != null) {
                    val existingSale = saleDao.getSaleWithDetails(existingOp.saleId)
                    if (existingSale != null && existingSale.sale.status == SaleStatus.COMPLETED) {
                        return@runInTransaction CompleteSaleResult(
                            sale = existingSale.toDomain(),
                            changeReturned = Money.ZERO,
                            isIdempotentReplay = true
                        )
                    }
                }

                val targetSaleId = command.draftSaleId ?: command.operationId

                // Check draft sale status if resuming or completing a held/draft sale
                if (command.draftSaleId != null) {
                    val existingDraft = saleDao.getSaleWithDetails(command.draftSaleId)
                    if (existingDraft != null) {
                        if (existingDraft.sale.status == SaleStatus.COMPLETED) {
                            return@runInTransaction CompleteSaleResult(
                                sale = existingDraft.toDomain(),
                                changeReturned = Money.ZERO,
                                isIdempotentReplay = true
                            )
                        }
                        if (existingDraft.sale.status == SaleStatus.VOIDED) {
                            throw InvalidSaleStateException("Cannot complete a VOIDED sale (${command.draftSaleId}).")
                        }
                    }
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
                        ?: throw CustomerNotFoundException("Customer '${command.customerId}' not found.")
                    if (!customer.isActive) {
                        throw InactiveCustomerException("Customer '${customer.name}' is inactive.")
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

                // 5. Concurrency-Safe Stock Validations & Deductions
                failureHook.onStage(SaleExecutionStage.BEFORE_STOCK_DEDUCTION)

                for (item in totals.items) {
                    // Ensure stock balance record exists
                    stockBalanceDao.insertInitialStockBalanceIfNotExists(
                        StockBalanceEntity(
                            productId = item.productId,
                            quantity = Quantity.ZERO,
                            averageCost = Money.ZERO,
                            updatedAt = now
                        )
                    )

                    val updatedRows = stockBalanceDao.decrementStock(
                        productId = item.productId,
                        deductQuantity = item.deductBaseQuantity,
                        allowNegativeStock = command.allowNegativeStock,
                        updatedAt = now
                    )

                    if (updatedRows == 0) {
                        val currentStock = stockBalanceDao.getStockBalance(item.productId)?.quantity ?: Quantity.ZERO
                        throw InsufficientStockException(
                            productId = item.productId,
                            productName = item.productName,
                            requiredQuantity = item.deductBaseQuantity,
                            availableQuantity = currentStock
                        )
                    }
                }

                failureHook.onStage(SaleExecutionStage.AFTER_STOCK_DEDUCTION)

                // 6. Stock Movements
                failureHook.onStage(SaleExecutionStage.BEFORE_STOCK_MOVEMENT)

                for (item in totals.items) {
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

                failureHook.onStage(SaleExecutionStage.AFTER_STOCK_MOVEMENT)

                // 7. Allocate Next Sequential Invoice Number
                failureHook.onStage(SaleExecutionStage.BEFORE_INVOICE_ALLOCATION)

                val invoiceNumber = invoiceSequenceDao.allocateNextInvoiceNumber(
                    shopId = command.shopId,
                    defaultPrefix = command.invoicePrefix
                )

                failureHook.onStage(SaleExecutionStage.AFTER_INVOICE_ALLOCATION)

                // 8. Clean existing draft items/payments if resuming/completing a draft/held sale
                if (command.draftSaleId != null) {
                    saleItemDao.deleteItemsForSale(targetSaleId)
                    paymentDao.deletePaymentsForSale(targetSaleId)
                }

                // 9. Persist Sale
                failureHook.onStage(SaleExecutionStage.BEFORE_SALE_INSERTION)

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
                    createdAt = now,
                    completedAt = now,
                    updatedAt = now
                )
                saleDao.upsertSale(saleEntity)

                failureHook.onStage(SaleExecutionStage.AFTER_SALE_INSERTION)

                // 10. Persist Sale Items
                failureHook.onStage(SaleExecutionStage.BEFORE_SALE_ITEMS_INSERTION)

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

                failureHook.onStage(SaleExecutionStage.AFTER_SALE_ITEMS_INSERTION)

                // 11. Persist Payments
                failureHook.onStage(SaleExecutionStage.BEFORE_PAYMENTS_INSERTION)

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

                failureHook.onStage(SaleExecutionStage.AFTER_PAYMENTS_INSERTION)

                // 12. Record Customer Ledger Entry (Credit Sale)
                failureHook.onStage(SaleExecutionStage.BEFORE_LEDGER_INSERTION)

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

                failureHook.onStage(SaleExecutionStage.AFTER_LEDGER_INSERTION)

                // 13. Record Cash Movement
                failureHook.onStage(SaleExecutionStage.BEFORE_CASH_MOVEMENT_INSERTION)

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

                failureHook.onStage(SaleExecutionStage.AFTER_CASH_MOVEMENT_INSERTION)

                // 14. Audit Log
                failureHook.onStage(SaleExecutionStage.BEFORE_AUDIT_LOG_INSERTION)

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

                failureHook.onStage(SaleExecutionStage.AFTER_AUDIT_LOG_INSERTION)

                // 15. Sync Event
                failureHook.onStage(SaleExecutionStage.BEFORE_SYNC_EVENT_INSERTION)

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

                failureHook.onStage(SaleExecutionStage.AFTER_SYNC_EVENT_INSERTION)

                // 16. Record Durable Idempotency Operation
                failureHook.onStage(SaleExecutionStage.BEFORE_OPERATION_RECORD_INSERTION)

                saleOperationDao.insertOperation(
                    SaleOperationEntity(
                        operationId = command.operationId,
                        saleId = targetSaleId,
                        shopId = command.shopId,
                        status = "COMPLETED",
                        createdAt = now
                    )
                )

                failureHook.onStage(SaleExecutionStage.AFTER_OPERATION_RECORD_INSERTION)

                // 17. Assemble Completed Sale
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
                    createdAt = now,
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
        } catch (e: Exception) {
            // Concurrent race recovery: check if a racing transaction with the same operationId already committed
            val existingOp = saleOperationDao.getOperation(command.operationId)
            if (existingOp != null) {
                val existingSale = saleDao.getSaleWithDetails(existingOp.saleId)
                if (existingSale != null && existingSale.sale.status == SaleStatus.COMPLETED) {
                    return@runCatching CompleteSaleResult(
                        sale = existingSale.toDomain(),
                        changeReturned = Money.ZERO,
                        isIdempotentReplay = true
                    )
                }
            }
            throw e
        }
    }
}
