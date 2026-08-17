package com.example.grocerypos.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.grocerypos.data.local.converter.Converters
import com.example.grocerypos.data.local.dao.AuditLogDao
import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CashMovementDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.CustomerDao
import com.example.grocerypos.data.local.dao.CustomerLedgerDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.InvoiceSequenceDao
import com.example.grocerypos.data.local.dao.PaymentDao
import com.example.grocerypos.data.local.dao.PriceHistoryDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.RoleDao
import com.example.grocerypos.data.local.dao.SaleDao
import com.example.grocerypos.data.local.dao.SaleItemDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.StockMovementDao
import com.example.grocerypos.data.local.dao.SupplierDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.dao.UserDao
import com.example.grocerypos.data.local.entity.AuditLogEntity
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CashMovementEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.CustomerEntity
import com.example.grocerypos.data.local.entity.CustomerLedgerEntryEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.InvoiceSequenceEntity
import com.example.grocerypos.data.local.entity.PaymentEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.RoleEntity
import com.example.grocerypos.data.local.entity.SaleEntity
import com.example.grocerypos.data.local.entity.SaleItemEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.StockMovementEntity
import com.example.grocerypos.data.local.entity.SupplierEntity
import com.example.grocerypos.data.local.entity.SyncEventEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.data.local.entity.UserEntity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.UnitCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ShopEntity::class,
        DeviceEntity::class,
        RoleEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        UnitEntity::class,
        ProductEntity::class,
        BarcodeEntity::class,
        PriceHistoryEntity::class,
        StockBalanceEntity::class,
        StockMovementEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        CustomerLedgerEntryEntity::class,
        CashMovementEntity::class,
        AuditLogEntity::class,
        SyncEventEntity::class,
        InvoiceSequenceEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GroceryPosDatabase : RoomDatabase() {

    abstract fun shopDao(): ShopDao
    abstract fun deviceDao(): DeviceDao
    abstract fun roleDao(): RoleDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun unitDao(): UnitDao
    abstract fun productDao(): ProductDao
    abstract fun barcodeDao(): BarcodeDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun stockBalanceDao(): StockBalanceDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun customerLedgerDao(): CustomerLedgerDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun syncEventDao(): SyncEventDao
    abstract fun invoiceSequenceDao(): InvoiceSequenceDao

    companion object {
        const val DATABASE_NAME = "grocery_pos_db"
        const val DATABASE_VERSION = 4

        /**
         * MIGRATION_1_2:
         * Converts monetary fields (selling_price, average_cost, unit_cost, credit_limit)
         * from REAL to INTEGER (storing minor units / paisas, multiplied by 100).
         * Converts quantity fields from REAL to INTEGER (scale 3, multiplied by 1000).
         * Completely preserves all existing user and catalog records.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Migrate products
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `products_new` (
                        `product_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category_id` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `base_unit_id` TEXT NOT NULL,
                        `selling_unit_id` TEXT NOT NULL,
                        `conversion_factor` REAL NOT NULL,
                        `selling_price` INTEGER NOT NULL,
                        `minimum_stock` INTEGER NOT NULL,
                        `track_expiry` INTEGER NOT NULL,
                        `track_batch` INTEGER NOT NULL,
                        `is_active` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`category_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`base_unit_id`) REFERENCES `units`(`unit_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`selling_unit_id`) REFERENCES `units`(`unit_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `products_new` SELECT
                        `product_id`, `shop_id`, `name`, `category_id`, `brand`, `sku`, `base_unit_id`, `selling_unit_id`,
                        `conversion_factor`, CAST(ROUND(`selling_price` * 100) AS INTEGER),
                        CAST(ROUND(`minimum_stock` * 1000) AS INTEGER),
                        `track_expiry`, `track_batch`, `is_active`, `created_at`, `updated_at`
                    FROM `products`
                """.trimIndent())
                db.execSQL("DROP TABLE `products`")
                db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_shop_id` ON `products` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_sku` ON `products` (`sku`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category_id` ON `products` (`category_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_shop_id_name` ON `products` (`shop_id`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_shop_id_sku` ON `products` (`shop_id`, `sku`)")

                // 2. Migrate price_history
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `price_history_new` (
                        `price_history_id` TEXT NOT NULL PRIMARY KEY,
                        `product_id` TEXT NOT NULL,
                        `selling_price` INTEGER NOT NULL,
                        `effective_from` INTEGER NOT NULL,
                        `effective_to` INTEGER,
                        `changed_by` TEXT,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `price_history_new` SELECT
                        `price_history_id`, `product_id`, CAST(ROUND(`selling_price` * 100) AS INTEGER),
                        `effective_from`, `effective_to`, `changed_by`, `created_at`
                    FROM `price_history`
                """.trimIndent())
                db.execSQL("DROP TABLE `price_history`")
                db.execSQL("ALTER TABLE `price_history_new` RENAME TO `price_history`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_product_id` ON `price_history` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_product_id_effective_from` ON `price_history` (`product_id`, `effective_from`)")

                // 3. Migrate stock_balances
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_balances_new` (
                        `product_id` TEXT NOT NULL PRIMARY KEY,
                        `quantity` INTEGER NOT NULL,
                        `average_cost` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `stock_balances_new` SELECT
                        `product_id`, CAST(ROUND(`quantity` * 1000) AS INTEGER),
                        CAST(ROUND(`average_cost` * 100) AS INTEGER), `updated_at`
                    FROM `stock_balances`
                """.trimIndent())
                db.execSQL("DROP TABLE `stock_balances`")
                db.execSQL("ALTER TABLE `stock_balances_new` RENAME TO `stock_balances`")

                // 4. Migrate stock_movements
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_movements_new` (
                        `movement_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `device_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `batch_id` TEXT,
                        `movement_type` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unit_cost` INTEGER NOT NULL,
                        `reference_type` TEXT,
                        `reference_id` TEXT,
                        `created_by` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`device_id`) REFERENCES `devices`(`device_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `stock_movements_new` SELECT
                        `movement_id`, `shop_id`, `device_id`, `product_id`, `batch_id`, `movement_type`,
                        CAST(ROUND(`quantity` * 1000) AS INTEGER), CAST(ROUND(`unit_cost` * 100) AS INTEGER),
                        `reference_type`, `reference_id`, `created_by`, `created_at`
                    FROM `stock_movements`
                """.trimIndent())
                db.execSQL("DROP TABLE `stock_movements`")
                db.execSQL("ALTER TABLE `stock_movements_new` RENAME TO `stock_movements`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_product_id` ON `stock_movements` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_shop_id` ON `stock_movements` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_created_at` ON `stock_movements` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_shop_id_created_at` ON `stock_movements` (`shop_id`, `created_at`)")

                // 5. Migrate customers
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `customers_new` (
                        `customer_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `credit_limit` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `customers_new` SELECT
                        `customer_id`, `shop_id`, `name`, `phone`, `address`,
                        CAST(ROUND(`credit_limit` * 100) AS INTEGER), `notes`, `is_active`, `created_at`, `updated_at`
                    FROM `customers`
                """.trimIndent())
                db.execSQL("DROP TABLE `customers`")
                db.execSQL("ALTER TABLE `customers_new` RENAME TO `customers`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_shop_id` ON `customers` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_name` ON `customers` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_shop_id_phone` ON `customers` (`shop_id`, `phone`)")
            }
        }

        /**
         * MIGRATION_2_3:
         * Converts Product.conversion_factor from SQLite REAL to INTEGER (scale 3 fixed-point Quantity, multiplied by 1000).
         * Eliminates floating point types from all database columns.
         * Preserves all existing product catalog records without data loss.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `products_new` (
                        `product_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category_id` TEXT NOT NULL,
                        `brand` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `base_unit_id` TEXT NOT NULL,
                        `selling_unit_id` TEXT NOT NULL,
                        `conversion_factor` INTEGER NOT NULL,
                        `selling_price` INTEGER NOT NULL,
                        `minimum_stock` INTEGER NOT NULL,
                        `track_expiry` INTEGER NOT NULL,
                        `track_batch` INTEGER NOT NULL,
                        `is_active` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`category_id`) REFERENCES `categories`(`category_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`base_unit_id`) REFERENCES `units`(`unit_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`selling_unit_id`) REFERENCES `units`(`unit_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `products_new` SELECT
                        `product_id`, `shop_id`, `name`, `category_id`, `brand`, `sku`, `base_unit_id`, `selling_unit_id`,
                        CAST(ROUND(`conversion_factor` * 1000) AS INTEGER),
                        `selling_price`, `minimum_stock`, `track_expiry`, `track_batch`, `is_active`, `created_at`, `updated_at`
                    FROM `products`
                """.trimIndent())
                db.execSQL("DROP TABLE `products`")
                db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_shop_id` ON `products` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_sku` ON `products` (`sku`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category_id` ON `products` (`category_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_shop_id_name` ON `products` (`shop_id`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_shop_id_sku` ON `products` (`shop_id`, `sku`)")
            }
        }

        /**
         * MIGRATION_3_4:
         * Non-destructive migration creating the Sales domain tables:
         * - sales
         * - sale_items
         * - payments
         * - customer_ledger_entries
         * - cash_movements
         * - audit_logs
         * - sync_events
         * - invoice_sequences
         * All existing product, category, stock, user, and customer records remain completely intact.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create sales table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales` (
                        `sale_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `device_id` TEXT NOT NULL,
                        `invoice_number` TEXT,
                        `cashier_id` TEXT NOT NULL,
                        `customer_id` TEXT,
                        `subtotal` INTEGER NOT NULL,
                        `item_discount` INTEGER NOT NULL,
                        `sale_discount` INTEGER NOT NULL,
                        `tax` INTEGER NOT NULL,
                        `grand_total` INTEGER NOT NULL,
                        `paid_amount` INTEGER NOT NULL,
                        `due_amount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `payment_status` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `completed_at` INTEGER,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`device_id`) REFERENCES `devices`(`device_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`cashier_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`customer_id`) REFERENCES `customers`(`customer_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_shop_id` ON `sales` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_device_id` ON `sales` (`device_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoice_number` ON `sales` (`invoice_number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_cashier_id` ON `sales` (`cashier_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customer_id` ON `sales` (`customer_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_status` ON `sales` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_created_at` ON `sales` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_shop_id_invoice_number` ON `sales` (`shop_id`, `invoice_number`)")

                // 2. Create sale_items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sale_items` (
                        `sale_item_id` TEXT NOT NULL PRIMARY KEY,
                        `sale_id` TEXT NOT NULL,
                        `product_id` TEXT NOT NULL,
                        `product_name` TEXT NOT NULL,
                        `sold_unit_id` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unit_price` INTEGER NOT NULL,
                        `gross_amount` INTEGER NOT NULL,
                        `discount` INTEGER NOT NULL,
                        `tax` INTEGER NOT NULL,
                        `net_amount` INTEGER NOT NULL,
                        `cost_at_sale` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`sale_id`) REFERENCES `sales`(`sale_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`product_id`) REFERENCES `products`(`product_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`sold_unit_id`) REFERENCES `units`(`unit_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_sale_id` ON `sale_items` (`sale_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_product_id` ON `sale_items` (`product_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_sold_unit_id` ON `sale_items` (`sold_unit_id`)")

                // 3. Create payments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payments` (
                        `payment_id` TEXT NOT NULL PRIMARY KEY,
                        `sale_id` TEXT NOT NULL,
                        `shop_id` TEXT NOT NULL,
                        `method` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `reference_number` TEXT,
                        `received_at` INTEGER NOT NULL,
                        `received_by` TEXT NOT NULL,
                        FOREIGN KEY(`sale_id`) REFERENCES `sales`(`sale_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`received_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_sale_id` ON `payments` (`sale_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_shop_id` ON `payments` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_received_at` ON `payments` (`received_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_received_by` ON `payments` (`received_by`)")

                // 4. Create customer_ledger_entries table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `customer_ledger_entries` (
                        `entry_id` TEXT NOT NULL PRIMARY KEY,
                        `customer_id` TEXT NOT NULL,
                        `shop_id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `reference_type` TEXT,
                        `reference_id` TEXT,
                        `notes` TEXT NOT NULL,
                        `created_by` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`customer_id`) REFERENCES `customers`(`customer_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`created_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_entries_customer_id` ON `customer_ledger_entries` (`customer_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_entries_shop_id` ON `customer_ledger_entries` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_entries_created_at` ON `customer_ledger_entries` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_entries_reference_id` ON `customer_ledger_entries` (`reference_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_entries_created_by` ON `customer_ledger_entries` (`created_by`)")

                // 5. Create cash_movements table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cash_movements` (
                        `movement_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `device_id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `reference_type` TEXT,
                        `reference_id` TEXT,
                        `notes` TEXT NOT NULL,
                        `created_by` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`device_id`) REFERENCES `devices`(`device_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`created_by`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_shop_id` ON `cash_movements` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_device_id` ON `cash_movements` (`device_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_created_at` ON `cash_movements` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_reference_id` ON `cash_movements` (`reference_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_created_by` ON `cash_movements` (`created_by`)")

                // 6. Create audit_logs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `audit_logs` (
                        `log_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `action` TEXT NOT NULL,
                        `entity_type` TEXT NOT NULL,
                        `entity_id` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_shop_id` ON `audit_logs` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_user_id` ON `audit_logs` (`user_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_entity_type_entity_id` ON `audit_logs` (`entity_type`, `entity_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_logs_timestamp` ON `audit_logs` (`timestamp`)")

                // 7. Create sync_events table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_events` (
                        `event_id` TEXT NOT NULL PRIMARY KEY,
                        `shop_id` TEXT NOT NULL,
                        `device_id` TEXT NOT NULL,
                        `entity_type` TEXT NOT NULL,
                        `entity_id` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `sync_status` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`device_id`) REFERENCES `devices`(`device_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_events_shop_id` ON `sync_events` (`shop_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_events_device_id` ON `sync_events` (`device_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_events_sync_status` ON `sync_events` (`sync_status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_events_entity_type_entity_id` ON `sync_events` (`entity_type`, `entity_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_events_timestamp` ON `sync_events` (`timestamp`)")

                // 8. Create invoice_sequences table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_sequences` (
                        `shop_id` TEXT NOT NULL PRIMARY KEY,
                        `next_number` INTEGER NOT NULL,
                        `prefix` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`shop_id`) REFERENCES `shops`(`shop_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_sequences_shop_id` ON `invoice_sequences` (`shop_id`)")
            }
        }

        fun buildDatabase(context: Context, scope: CoroutineScope): GroceryPosDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GroceryPosDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(DatabaseCallback(scope))
                .build()
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial standard Units and Roles upon first creation
                scope.launch(Dispatchers.IO) {
                    seedInitialSystemData(db)
                }
            }

            private fun seedInitialSystemData(db: SupportSQLiteDatabase) {
                // Initial Units
                val standardUnits = listOf(
                    Triple(UnitCode.PIECE.name, "Piece", "pc"),
                    Triple(UnitCode.GRAM.name, "Gram", "g"),
                    Triple(UnitCode.KILOGRAM.name, "Kilogram", "kg"),
                    Triple(UnitCode.MILLILITRE.name, "Millilitre", "ml"),
                    Triple(UnitCode.LITRE.name, "Litre", "L"),
                    Triple(UnitCode.PACK.name, "Pack", "pk"),
                    Triple(UnitCode.BOX.name, "Box", "bx"),
                    Triple(UnitCode.CARTON.name, "Carton", "ctn"),
                    Triple(UnitCode.DOZEN.name, "Dozen", "dz"),
                    Triple(UnitCode.BAG.name, "Bag", "bag")
                )

                for ((code, name, symbol) in standardUnits) {
                    val id = "unit_" + code.lowercase()
                    db.execSQL(
                        "INSERT OR IGNORE INTO units (unit_id, code, name, symbol, is_custom) VALUES ('$id', '$code', '$name', '$symbol', 0)"
                    )
                }

                // Initial Roles
                val standardRoles = listOf(
                    Triple(RoleName.OWNER.name, "Store Owner", "[\"all\"]"),
                    Triple(RoleName.MANAGER.name, "Store Manager", "[\"view_products\",\"manage_products\",\"manage_pricing\",\"view_stock\",\"adjust_stock\",\"record_purchase\",\"perform_sale\",\"apply_discount\",\"process_return\",\"view_reports\",\"manage_customers\",\"manage_suppliers\"]"),
                    Triple(RoleName.CASHIER.name, "Cashier", "[\"view_products\",\"perform_sale\",\"process_return\",\"manage_customers\"]"),
                    Triple(RoleName.STOCK_MANAGER.name, "Stock Manager", "[\"view_products\",\"manage_products\",\"view_stock\",\"adjust_stock\",\"record_purchase\",\"manage_suppliers\"]")
                )

                for ((name, desc, perms) in standardRoles) {
                    val id = "role_" + name.lowercase()
                    db.execSQL(
                        "INSERT OR IGNORE INTO roles (role_id, name, description, permissions_json) VALUES ('$id', '$name', '$desc', '$perms')"
                    )
                }
            }
        }
    }
}
