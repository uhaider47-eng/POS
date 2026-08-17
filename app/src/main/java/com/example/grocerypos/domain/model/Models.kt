package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

enum class DeviceType {
    PHONE,
    TABLET
}

enum class DeviceStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED
}

enum class RoleName {
    OWNER,
    MANAGER,
    CASHIER,
    STOCK_MANAGER
}

enum class UnitCode {
    PIECE,
    GRAM,
    KILOGRAM,
    MILLILITRE,
    LITRE,
    PACK,
    BOX,
    CARTON,
    DOZEN,
    BAG,
    CUSTOM
}

enum class MovementType {
    PURCHASE,
    SALE,
    SALE_RETURN,
    PURCHASE_RETURN,
    DAMAGE,
    LOSS,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT,
    OPENING_STOCK,
    TRANSFER_IN,
    TRANSFER_OUT
}

@Serializable
data class Shop(
    val shopId: String,
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val currency: String = "PKR",
    val timezone: String = "Asia/Karachi",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Device(
    val deviceId: String,
    val shopId: String,
    val deviceName: String,
    val deviceType: DeviceType = DeviceType.TABLET,
    val isPrimary: Boolean = false,
    val status: DeviceStatus = DeviceStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis()
)

@Serializable
data class Role(
    val roleId: String,
    val name: RoleName,
    val description: String,
    val permissions: Set<AppPermission> = emptySet()
)

@Serializable
data class Category(
    val categoryId: String,
    val shopId: String,
    val name: String,
    val parentCategoryId: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Unit(
    val unitId: String,
    val code: UnitCode,
    val name: String,
    val symbol: String,
    val isCustom: Boolean = false
)

@Serializable
data class Product(
    val productId: String,
    val shopId: String,
    val name: String,
    val categoryId: String,
    val brand: String = "",
    val sku: String = "",
    val baseUnitId: String,
    val sellingUnitId: String,
    val conversionFactor: Quantity = Quantity.ONE,
    val sellingPrice: Money,
    val minimumStock: Quantity = Quantity.ZERO,
    val trackExpiry: Boolean = false,
    val trackBatch: Boolean = false,
    val isActive: Boolean = true,
    val barcodes: List<Barcode> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Barcode(
    val barcodeId: String,
    val productId: String,
    val barcode: String,
    val isPrimary: Boolean = true,
    val shopId: String
)

@Serializable
data class PriceHistory(
    val priceHistoryId: String,
    val productId: String,
    val sellingPrice: Money,
    val effectiveFrom: Long = System.currentTimeMillis(),
    val effectiveTo: Long? = null,
    val changedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class StockBalance(
    val productId: String,
    val quantity: Quantity = Quantity.ZERO,
    val averageCost: Money = Money.ZERO,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class StockMovement(
    val movementId: String,
    val shopId: String,
    val deviceId: String,
    val productId: String,
    val batchId: String? = null,
    val movementType: MovementType,
    val quantity: Quantity,
    val unitCost: Money = Money.ZERO,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Customer(
    val customerId: String,
    val shopId: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val creditLimit: Money = Money.ZERO,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Supplier(
    val supplierId: String,
    val shopId: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SaleStatus {
    DRAFT,
    COMPLETED,
    VOIDED,
    PARTIALLY_RETURNED,
    RETURNED
}

enum class PaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID
}

enum class PaymentMethod {
    CASH,
    BANK_TRANSFER,
    CARD,
    EASYPAISA,
    JAZZCASH,
    OTHER
}

enum class CustomerLedgerType {
    SALE_CREDIT,
    PAYMENT,
    RETURN_CREDIT,
    ADJUSTMENT,
    OPENING_BALANCE
}

enum class CashMovementType {
    SALE_CASH,
    CUSTOMER_PAYMENT,
    REFUND,
    EXPENSE,
    CASH_IN,
    CASH_OUT,
    OPENING_CASH
}

enum class AuditAction {
    SALE_CREATED,
    SALE_COMPLETED,
    SALE_VOIDED,
    PAYMENT_RECORDED,
    STOCK_ADJUSTED,
    SETTING_CHANGED,
    CUSTOMER_CREDIT_RECORDED,
    CASH_MOVED
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}

@Serializable
data class Sale(
    val saleId: String,
    val shopId: String,
    val deviceId: String,
    val invoiceNumber: String? = null,
    val cashierId: String,
    val customerId: String? = null,
    val subtotal: Money,
    val itemDiscount: Money = Money.ZERO,
    val saleDiscount: Money = Money.ZERO,
    val tax: Money = Money.ZERO,
    val grandTotal: Money,
    val paidAmount: Money = Money.ZERO,
    val dueAmount: Money = Money.ZERO,
    val status: SaleStatus = SaleStatus.DRAFT,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val items: List<SaleItem> = emptyList(),
    val payments: List<Payment> = emptyList()
)

@Serializable
data class SaleItem(
    val saleItemId: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val soldUnitId: String,
    val quantity: Quantity,
    val unitPrice: Money,
    val grossAmount: Money,
    val discount: Money = Money.ZERO,
    val tax: Money = Money.ZERO,
    val netAmount: Money,
    val costAtSale: Money = Money.ZERO,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Payment(
    val paymentId: String,
    val saleId: String,
    val shopId: String,
    val method: PaymentMethod,
    val amount: Money,
    val referenceNumber: String? = null,
    val receivedAt: Long = System.currentTimeMillis(),
    val receivedBy: String
)

@Serializable
data class CustomerLedgerEntry(
    val entryId: String,
    val customerId: String,
    val shopId: String,
    val type: CustomerLedgerType,
    val amount: Money,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val notes: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CashMovement(
    val movementId: String,
    val shopId: String,
    val deviceId: String,
    val type: CashMovementType,
    val amount: Money,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val notes: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AuditLog(
    val logId: String,
    val shopId: String,
    val userId: String,
    val action: AuditAction,
    val entityType: String,
    val entityId: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SyncEvent(
    val eventId: String,
    val shopId: String,
    val deviceId: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperation,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class InvoiceSequence(
    val shopId: String,
    val nextNumber: Long = 1L,
    val prefix: String = "INV-",
    val updatedAt: Long = System.currentTimeMillis()
)
