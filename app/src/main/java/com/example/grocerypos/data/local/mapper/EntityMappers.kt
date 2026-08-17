package com.example.grocerypos.data.local.mapper

import com.example.grocerypos.data.local.dao.SaleWithItemsAndPayments
import com.example.grocerypos.data.local.entity.AuditLogEntity
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CashMovementEntity
import com.example.grocerypos.data.local.entity.CustomerEntity
import com.example.grocerypos.data.local.entity.CustomerLedgerEntryEntity
import com.example.grocerypos.data.local.entity.InvoiceSequenceEntity
import com.example.grocerypos.data.local.entity.PaymentEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.SaleEntity
import com.example.grocerypos.data.local.entity.SaleItemEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.StockMovementEntity
import com.example.grocerypos.data.local.entity.SyncEventEntity
import com.example.grocerypos.domain.model.AuditLog
import com.example.grocerypos.domain.model.Barcode
import com.example.grocerypos.domain.model.CashMovement
import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.CustomerLedgerEntry
import com.example.grocerypos.domain.model.InvoiceSequence
import com.example.grocerypos.domain.model.Payment
import com.example.grocerypos.domain.model.PriceHistory
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleItem
import com.example.grocerypos.domain.model.StockBalance
import com.example.grocerypos.domain.model.StockMovement
import com.example.grocerypos.domain.model.SyncEvent

fun SaleEntity.toDomain(
    items: List<SaleItem> = emptyList(),
    payments: List<Payment> = emptyList()
): Sale = Sale(
    saleId = saleId,
    shopId = shopId,
    deviceId = deviceId,
    invoiceNumber = invoiceNumber,
    cashierId = cashierId,
    customerId = customerId,
    subtotal = subtotal,
    itemDiscount = itemDiscount,
    saleDiscount = saleDiscount,
    tax = tax,
    grandTotal = grandTotal,
    paidAmount = paidAmount,
    dueAmount = dueAmount,
    status = status,
    paymentStatus = paymentStatus,
    notes = notes,
    createdAt = createdAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    items = items,
    payments = payments
)

fun SaleItemEntity.toDomain(): SaleItem = SaleItem(
    saleItemId = saleItemId,
    saleId = saleId,
    productId = productId,
    productName = productName,
    soldUnitId = soldUnitId,
    quantity = quantity,
    unitPrice = unitPrice,
    grossAmount = grossAmount,
    discount = discount,
    tax = tax,
    netAmount = netAmount,
    costAtSale = costAtSale,
    createdAt = createdAt
)

fun PaymentEntity.toDomain(): Payment = Payment(
    paymentId = paymentId,
    saleId = saleId,
    shopId = shopId,
    method = method,
    amount = amount,
    referenceNumber = referenceNumber,
    receivedAt = receivedAt,
    receivedBy = receivedBy
)

fun SaleWithItemsAndPayments.toDomain(): Sale = sale.toDomain(
    items = items.map { it.toDomain() },
    payments = payments.map { it.toDomain() }
)

fun ProductEntity.toDomain(barcodes: List<Barcode> = emptyList()): Product = Product(
    productId = productId,
    shopId = shopId,
    name = name,
    categoryId = categoryId,
    brand = brand,
    sku = sku,
    baseUnitId = baseUnitId,
    sellingUnitId = sellingUnitId,
    conversionFactor = conversionFactor,
    sellingPrice = sellingPrice,
    minimumStock = minimumStock,
    trackExpiry = trackExpiry,
    trackBatch = trackBatch,
    isActive = isActive,
    barcodes = barcodes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BarcodeEntity.toDomain(): Barcode = Barcode(
    barcodeId = barcodeId,
    productId = productId,
    barcode = barcode,
    isPrimary = isPrimary,
    shopId = shopId
)

fun StockBalanceEntity.toDomain(): StockBalance = StockBalance(
    productId = productId,
    quantity = quantity,
    averageCost = averageCost,
    updatedAt = updatedAt
)

fun StockMovementEntity.toDomain(): StockMovement = StockMovement(
    movementId = movementId,
    shopId = shopId,
    deviceId = deviceId,
    productId = productId,
    batchId = batchId,
    movementType = movementType,
    quantity = quantity,
    unitCost = unitCost,
    referenceType = referenceType,
    referenceId = referenceId,
    createdBy = createdBy,
    createdAt = createdAt
)

fun CustomerEntity.toDomain(): Customer = Customer(
    customerId = customerId,
    shopId = shopId,
    name = name,
    phone = phone,
    address = address,
    creditLimit = creditLimit,
    notes = notes,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerLedgerEntryEntity.toDomain(): CustomerLedgerEntry = CustomerLedgerEntry(
    entryId = entryId,
    customerId = customerId,
    shopId = shopId,
    type = type,
    amount = amount,
    referenceType = referenceType,
    referenceId = referenceId,
    notes = notes,
    createdBy = createdBy,
    createdAt = createdAt
)

fun CashMovementEntity.toDomain(): CashMovement = CashMovement(
    movementId = movementId,
    shopId = shopId,
    deviceId = deviceId,
    type = type,
    amount = amount,
    referenceType = referenceType,
    referenceId = referenceId,
    notes = notes,
    createdBy = createdBy,
    createdAt = createdAt
)

fun AuditLogEntity.toDomain(): AuditLog = AuditLog(
    logId = logId,
    shopId = shopId,
    userId = userId,
    action = action,
    entityType = entityType,
    entityId = entityId,
    details = details,
    timestamp = timestamp
)

fun SyncEventEntity.toDomain(): SyncEvent = SyncEvent(
    eventId = eventId,
    shopId = shopId,
    deviceId = deviceId,
    entityType = entityType,
    entityId = entityId,
    operation = operation,
    syncStatus = syncStatus,
    timestamp = timestamp
)

fun InvoiceSequenceEntity.toDomain(): InvoiceSequence = InvoiceSequence(
    shopId = shopId,
    nextNumber = nextNumber,
    prefix = prefix,
    updatedAt = updatedAt
)
