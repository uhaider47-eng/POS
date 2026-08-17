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
