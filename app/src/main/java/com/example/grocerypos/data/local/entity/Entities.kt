package com.example.grocerypos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.grocerypos.domain.model.AuditAction
import com.example.grocerypos.domain.model.CashMovementType
import com.example.grocerypos.domain.model.CustomerLedgerType
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.MovementType
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.SyncOperation
import com.example.grocerypos.domain.model.SyncStatus
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
    val conversionFactor: Quantity = Quantity.ONE,

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

@Entity(
    tableName = "sales",
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
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["cashier_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["customer_id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["device_id"]),
        Index(value = ["invoice_number"]),
        Index(value = ["cashier_id"]),
        Index(value = ["customer_id"]),
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["shop_id", "invoice_number"], unique = true)
    ]
)
data class SaleEntity(
    @PrimaryKey
    @ColumnInfo(name = "sale_id")
    val saleId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String? = null,

    @ColumnInfo(name = "cashier_id")
    val cashierId: String,

    @ColumnInfo(name = "customer_id")
    val customerId: String? = null,

    @ColumnInfo(name = "subtotal")
    val subtotal: Money,

    @ColumnInfo(name = "item_discount")
    val itemDiscount: Money = Money.ZERO,

    @ColumnInfo(name = "sale_discount")
    val saleDiscount: Money = Money.ZERO,

    @ColumnInfo(name = "tax")
    val tax: Money = Money.ZERO,

    @ColumnInfo(name = "grand_total")
    val grandTotal: Money,

    @ColumnInfo(name = "paid_amount")
    val paidAmount: Money = Money.ZERO,

    @ColumnInfo(name = "due_amount")
    val dueAmount: Money = Money.ZERO,

    @ColumnInfo(name = "status")
    val status: SaleStatus = SaleStatus.DRAFT,

    @ColumnInfo(name = "payment_status")
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["sale_id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["unit_id"],
            childColumns = ["sold_unit_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sale_id"]),
        Index(value = ["product_id"]),
        Index(value = ["sold_unit_id"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "sale_item_id")
    val saleItemId: String,

    @ColumnInfo(name = "sale_id")
    val saleId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "product_name")
    val productName: String,

    @ColumnInfo(name = "sold_unit_id")
    val soldUnitId: String,

    @ColumnInfo(name = "quantity")
    val quantity: Quantity,

    @ColumnInfo(name = "unit_price")
    val unitPrice: Money,

    @ColumnInfo(name = "gross_amount")
    val grossAmount: Money,

    @ColumnInfo(name = "discount")
    val discount: Money = Money.ZERO,

    @ColumnInfo(name = "tax")
    val tax: Money = Money.ZERO,

    @ColumnInfo(name = "net_amount")
    val netAmount: Money,

    @ColumnInfo(name = "cost_at_sale")
    val costAtSale: Money = Money.ZERO,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["sale_id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["received_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sale_id"]),
        Index(value = ["shop_id"]),
        Index(value = ["received_at"]),
        Index(value = ["received_by"])
    ]
)
data class PaymentEntity(
    @PrimaryKey
    @ColumnInfo(name = "payment_id")
    val paymentId: String,

    @ColumnInfo(name = "sale_id")
    val saleId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "method")
    val method: PaymentMethod,

    @ColumnInfo(name = "amount")
    val amount: Money,

    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null,

    @ColumnInfo(name = "received_at")
    val receivedAt: Long,

    @ColumnInfo(name = "received_by")
    val receivedBy: String
)

@Entity(
    tableName = "customer_ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["customer_id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["shop_id"]),
        Index(value = ["created_at"]),
        Index(value = ["reference_id"]),
        Index(value = ["created_by"])
    ]
)
data class CustomerLedgerEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    val entryId: String,

    @ColumnInfo(name = "customer_id")
    val customerId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "type")
    val type: CustomerLedgerType,

    @ColumnInfo(name = "amount")
    val amount: Money,

    @ColumnInfo(name = "reference_type")
    val referenceType: String? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "cash_movements",
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
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["device_id"]),
        Index(value = ["created_at"]),
        Index(value = ["reference_id"]),
        Index(value = ["created_by"])
    ]
)
data class CashMovementEntity(
    @PrimaryKey
    @ColumnInfo(name = "movement_id")
    val movementId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "type")
    val type: CashMovementType,

    @ColumnInfo(name = "amount")
    val amount: Money,

    @ColumnInfo(name = "reference_type")
    val referenceType: String? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["user_id"]),
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "log_id")
    val logId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "action")
    val action: AuditAction,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "details")
    val details: String = "",

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)

@Entity(
    tableName = "sync_events",
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
        )
    ],
    indices = [
        Index(value = ["shop_id"]),
        Index(value = ["device_id"]),
        Index(value = ["sync_status"]),
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["timestamp"])
    ]
)
data class SyncEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "operation")
    val operation: SyncOperation,

    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)

@Entity(
    tableName = "invoice_sequences",
    foreignKeys = [
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shop_id"])
    ]
)
data class InvoiceSequenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "next_number")
    val nextNumber: Long = 1L,

    @ColumnInfo(name = "prefix")
    val prefix: String = "INV-",

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "sale_operations",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["sale_id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ShopEntity::class,
            parentColumns = ["shop_id"],
            childColumns = ["shop_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["operation_id"], unique = true),
        Index(value = ["sale_id"]),
        Index(value = ["shop_id"])
    ]
)
data class SaleOperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "operation_id")
    val operationId: String,

    @ColumnInfo(name = "sale_id")
    val saleId: String,

    @ColumnInfo(name = "shop_id")
    val shopId: String,

    @ColumnInfo(name = "status")
    val status: String = "COMPLETED",

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

