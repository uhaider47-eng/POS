package com.example.grocerypos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.CustomerEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.RoleEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.StockMovementEntity
import com.example.grocerypos.data.local.entity.SupplierEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShop(shop: ShopEntity)

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Query("SELECT * FROM shops LIMIT 1")
    fun getShopFlow(): Flow<ShopEntity?>

    @Query("SELECT * FROM shops LIMIT 1")
    suspend fun getShop(): ShopEntity?

    @Query("SELECT COUNT(*) FROM shops")
    suspend fun getShopCount(): Int
}

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("SELECT * FROM devices WHERE is_primary = 1 AND shop_id = :shopId LIMIT 1")
    fun getPrimaryDeviceFlow(shopId: String): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE is_primary = 1 AND shop_id = :shopId LIMIT 1")
    suspend fun getPrimaryDevice(shopId: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE device_id = :deviceId")
    suspend fun getDeviceById(deviceId: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE shop_id = :shopId")
    fun getAllDevicesFlow(shopId: String): Flow<List<DeviceEntity>>
}

@Dao
interface RoleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RoleEntity>)

    @Query("SELECT * FROM roles")
    suspend fun getAllRoles(): List<RoleEntity>
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE shop_id = :shopId AND is_active = 1")
    fun getActiveUsersFlow(shopId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE shop_id = :shopId ORDER BY name ASC")
    fun getCategoriesFlow(shopId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE shop_id = :shopId AND is_active = 1 ORDER BY name ASC")
    suspend fun getActiveCategories(shopId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE category_id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?
}

@Dao
interface UnitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>)

    @Query("SELECT * FROM units ORDER BY name ASC")
    fun getAllUnitsFlow(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units ORDER BY name ASC")
    suspend fun getAllUnits(): List<UnitEntity>

    @Query("SELECT * FROM units WHERE unit_id = :unitId")
    suspend fun getUnitById(unitId: String): UnitEntity?
}

data class ProductWithBarcodes(
    val product: ProductEntity,
    val barcodes: List<BarcodeEntity>,
    val categoryName: String,
    val baseUnitSymbol: String,
    val sellingUnitSymbol: String
)

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE shop_id = :shopId ORDER BY name ASC")
    fun getProductsFlow(shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE product_id = :productId")
    suspend fun getProductById(productId: String): ProductEntity?

    @Query("""
        SELECT p.* FROM products p
        LEFT JOIN barcodes b ON p.product_id = b.product_id
        WHERE p.shop_id = :shopId AND (
            p.name LIKE '%' || :query || '%' OR
            p.sku LIKE '%' || :query || '%' OR
            p.brand LIKE '%' || :query || '%' OR
            b.barcode LIKE '%' || :query || '%'
        )
        GROUP BY p.product_id
        ORDER BY p.name ASC
    """)
    fun searchProductsFlow(shopId: String, query: String): Flow<List<ProductEntity>>

    @Query("UPDATE products SET is_active = :isActive, updated_at = :updatedAt WHERE product_id = :productId")
    suspend fun setProductActiveStatus(productId: String, isActive: Boolean, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM products WHERE shop_id = :shopId")
    fun getTotalProductCountFlow(shopId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE shop_id = :shopId AND is_active = 1")
    fun getActiveProductCountFlow(shopId: String): Flow<Int>
}

@Dao
interface BarcodeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBarcode(barcode: BarcodeEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBarcodes(barcodes: List<BarcodeEntity>)

    @Query("DELETE FROM barcodes WHERE product_id = :productId")
    suspend fun deleteBarcodesForProduct(productId: String)

    @Query("SELECT * FROM barcodes WHERE product_id = :productId")
    fun getBarcodesForProductFlow(productId: String): Flow<List<BarcodeEntity>>

    @Query("SELECT * FROM barcodes WHERE product_id = :productId")
    suspend fun getBarcodesForProduct(productId: String): List<BarcodeEntity>

    @Query("SELECT * FROM barcodes WHERE barcode = :barcode AND shop_id = :shopId LIMIT 1")
    suspend fun findBarcodeInShop(shopId: String, barcode: String): BarcodeEntity?
}

@Dao
interface PriceHistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPriceHistory(priceHistory: PriceHistoryEntity)

    @Query("SELECT * FROM price_history WHERE product_id = :productId ORDER BY effective_from DESC")
    fun getPriceHistoryForProductFlow(productId: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE product_id = :productId ORDER BY effective_from DESC")
    suspend fun getPriceHistoryForProduct(productId: String): List<PriceHistoryEntity>

    @Query("UPDATE price_history SET effective_to = :effectiveTo WHERE product_id = :productId AND effective_to IS NULL")
    suspend fun closePreviousPriceHistory(productId: String, effectiveTo: Long)
}

@Dao
interface StockBalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockBalance(balance: StockBalanceEntity)

    @Query("SELECT * FROM stock_balances WHERE product_id = :productId")
    fun getStockBalanceFlow(productId: String): Flow<StockBalanceEntity?>

    @Query("SELECT * FROM stock_balances WHERE product_id = :productId")
    suspend fun getStockBalance(productId: String): StockBalanceEntity?
}

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE product_id = :productId ORDER BY created_at DESC")
    fun getMovementsForProductFlow(productId: String): Flow<List<StockMovementEntity>>
}

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE shop_id = :shopId AND is_active = 1 ORDER BY name ASC")
    fun getActiveCustomersFlow(shopId: String): Flow<List<CustomerEntity>>
}

@Dao
interface SupplierDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE shop_id = :shopId AND is_active = 1 ORDER BY name ASC")
    fun getActiveSuppliersFlow(shopId: String): Flow<List<SupplierEntity>>
}
