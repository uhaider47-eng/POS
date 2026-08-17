package com.example.grocerypos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.MovementType
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.UnitCode

@Entity(
    tableName = "shops"
)
data class ShopEntity(
    @PrimaryKey
    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "owner_name")
    val ownerName: String,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "currency", defaultValue = "PKR")
    val currency: String = "PKR",

    @ColumnInfo(name = "timezone", defaultValue = "Asia/Karachi")
    val timezone: String = "Asia/Karachi",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "devices",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["shop_id", "is_primary"])
    ]
)
data class DeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "device_name")
    val deviceName: String,

    @ColumnInfo(name = "device_type")
    val deviceType: DeviceType,

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,

    @ColumnInfo(name = "status")
    val status: DeviceStatus,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long
)

@Entity(
    tableName = "roles"
)
data class RoleEntity(
    @PrimaryKey
    @ColumnInfo(name = "role_id")
    val roleId: String,

    @ColumnInfo(name = "name")
    val name: RoleName,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "permissions_json")
    val permissionsJson: String
)

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["role_id"],
            childColumns = ["role_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["role_id"])
    ]
)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "role_id")
    val roleId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "pin_hash")
    val pinHash: String, // Secure hash, never stored as plaintext

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["parent_category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["parent_category_id"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "parent_category_id")
    val parentCategoryId: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "units"
)
data class UnitEntity(
    @PrimaryKey
    @ColumnInfo(name = "unit_id")
    val unitId: String,

    @ColumnInfo(name = "code")
    val code: UnitCode,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "symbol")
    val symbol: String,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["unit_id"],
            childColumns = ["base_unit_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["unit_id"],
            childColumns = ["selling_unit_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["name"]),
        Index(value = ["sku"]),
        Index(value = ["category_id"]),
        Index(value = ["shop_id", "name"]),
        Index(value = ["shop_id", "sku"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "brand")
    val brand: String = "",

    @ColumnInfo(name = "sku")
    val sku: String = "",

    @ColumnInfo(name = "base_unit_id")
    val baseUnitId: String,

    @ColumnInfo(name = "selling_unit_id")
    val sellingUnitId: String,

    @ColumnInfo(name = "conversion_factor")
    val conversionFactor: Double = 1.0,

    @ColumnInfo(name = "selling_price")
    val sellingPrice: Money,

    @ColumnInfo(name = "minimum_stock")
    val minimumStock: Quantity = Quantity.ZERO,

    @ColumnInfo(name = "track_expiry")
    val trackExpiry: Boolean = false,

    @ColumnInfo(name = "track_batch")
    val trackBatch: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "barcodes",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["barcode"]),
        Index(value = ["shop_id", "barcode"], unique = true) // Barcode unique within the shop
    ]
)
data class BarcodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "barcode_id")
    val barcodeId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "barcode")
    val barcode: String,

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = true,

    @ColumnInfo(name = "shop_id")
    val shopId: String
)

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["product_id", "effective_from"])
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "price_history_id")
    val priceHistoryId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "selling_price")
    val sellingPrice: Money,

    @ColumnInfo(name = "effective_from")
    val effectiveFrom: Long,

    @ColumnInfo(name = "effective_to")
    val effectiveTo: Long? = null,

    @ColumnInfo(name = "changed_by")
    val changedBy: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "stock_balances",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class StockBalanceEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "quantity")
    val quantity: Quantity = Quantity.ZERO,

    @ColumnInfo(name = "average_cost")
    val averageCost: Money = Money.ZERO,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["device_id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["shop_id"]),
        Index(value = ["created_at"]),
        Index(value = ["shop_id", "created_at"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey
    @ColumnInfo(name = "movement_id")
    val movementId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "batch_id")
    val batchId: String? = null,

    @ColumnInfo(name = "movement_type")
    val movementType: MovementType,

    @ColumnInfo(name = "quantity")
    val quantity: Quantity,

    @ColumnInfo(name = "unit_cost")
    val unitCost: Money = Money.ZERO,

    @ColumnInfo(name = "reference_type")
    val referenceType: String? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["name"]),
        Index(value = ["phone"]),
        Index(value = ["shop_id", "phone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    @ColumnInfo(name = "customer_id")
    val customerId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "credit_limit")
    val creditLimit: Money = Money.ZERO,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "suppliers",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["name"]),
        Index(value = ["shop_id", "name"])
    ]
)
data class SupplierEntity(
    @PrimaryKey
    @ColumnInfo(name = "supplier_id")
    val supplierId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
