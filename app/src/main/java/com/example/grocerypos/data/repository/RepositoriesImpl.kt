package com.example.grocerypos.data.repository

import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.PriceHistoryDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.domain.model.Barcode
import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Device
import com.example.grocerypos.domain.model.PriceHistory
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.model.StockBalance
import com.example.grocerypos.domain.model.Unit
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.DeviceRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepositoryImpl @Inject constructor(
    private val shopDao: ShopDao
) : ShopRepository {

    override fun getShopFlow(): Flow<Shop?> {
        return shopDao.getShopFlow().map { it?.toDomain() }
    }

    override suspend fun getShop(): Shop? {
        return shopDao.getShop()?.toDomain()
    }

    override suspend fun createShop(shop: Shop): Result<kotlin.Unit> = runCatching {
        shopDao.insertShop(shop.toEntity())
    }

    override suspend fun updateShop(shop: Shop): Result<kotlin.Unit> = runCatching {
        shopDao.updateShop(shop.toEntity())
    }

    override suspend fun isShopConfigured(): Boolean {
        return shopDao.getShopCount() > 0
    }
}

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override fun getPrimaryDeviceFlow(shopId: String): Flow<Device?> {
        return deviceDao.getPrimaryDeviceFlow(shopId).map { it?.toDomain() }
    }

    override suspend fun getPrimaryDevice(shopId: String): Device? {
        return deviceDao.getPrimaryDevice(shopId)?.toDomain()
    }

    override suspend fun registerDevice(device: Device): Result<kotlin.Unit> = runCatching {
        deviceDao.insertDevice(device.toEntity())
    }

    override fun getAllDevicesFlow(shopId: String): Flow<List<Device>> {
        return deviceDao.getAllDevicesFlow(shopId).map { list -> list.map { it.toDomain() } }
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategoriesFlow(shopId: String): Flow<List<Category>> {
        return categoryDao.getCategoriesFlow(shopId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveCategories(shopId: String): List<Category> {
        return categoryDao.getActiveCategories(shopId).map { it.toDomain() }
    }

    override suspend fun createCategory(category: Category): Result<kotlin.Unit> = runCatching {
        categoryDao.insertCategory(category.toEntity())
    }
}

@Singleton
class UnitRepositoryImpl @Inject constructor(
    private val unitDao: UnitDao
) : UnitRepository {

    override fun getAllUnitsFlow(): Flow<List<Unit>> {
        return unitDao.getAllUnitsFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllUnits(): List<Unit> {
        return unitDao.getAllUnits().map { it.toDomain() }
    }
}

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val barcodeDao: BarcodeDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val stockBalanceDao: StockBalanceDao
) : ProductRepository {

    override fun getProductsFlow(shopId: String): Flow<List<Product>> {
        return productDao.getProductsFlow(shopId).map { productEntities ->
            productEntities.map { entity ->
                val barcodes = barcodeDao.getBarcodesForProduct(entity.productId).map { it.toDomain() }
                entity.toDomain(barcodes)
            }
        }
    }

    override fun searchProductsFlow(shopId: String, query: String): Flow<List<Product>> {
        if (query.isBlank()) {
            return getProductsFlow(shopId)
        }
        return productDao.searchProductsFlow(shopId, query.trim()).map { productEntities ->
            productEntities.map { entity ->
                val barcodes = barcodeDao.getBarcodesForProduct(entity.productId).map { it.toDomain() }
                entity.toDomain(barcodes)
            }
        }
    }

    override suspend fun getProductById(productId: String): Product? {
        val entity = productDao.getProductById(productId) ?: return null
        val barcodes = barcodeDao.getBarcodesForProduct(productId).map { it.toDomain() }
        return entity.toDomain(barcodes)
    }

    override suspend fun saveProduct(product: Product, priceChangedBy: String?): Result<kotlin.Unit> = runCatching {
        val existing = productDao.getProductById(product.productId)
        val now = System.currentTimeMillis()

        // Validate barcode uniqueness
        for (b in product.barcodes) {
            val duplicate = barcodeDao.findBarcodeInShop(product.shopId, b.barcode)
            if (duplicate != null && duplicate.productId != product.productId) {
                throw IllegalArgumentException("Barcode '${b.barcode}' is already assigned to another product in this shop.")
            }
        }

        if (existing == null) {
            // New Product
            productDao.insertProduct(product.toEntity())

            // Insert barcodes
            if (product.barcodes.isNotEmpty()) {
                barcodeDao.insertBarcodes(product.barcodes.map { it.toEntity() })
            }

            // Create initial PriceHistory record
            val priceHistory = PriceHistoryEntity(
                priceHistoryId = UUID.randomUUID().toString(),
                productId = product.productId,
                sellingPrice = product.sellingPrice,
                effectiveFrom = now,
                effectiveTo = null,
                changedBy = priceChangedBy,
                createdAt = now
            )
            priceHistoryDao.insertPriceHistory(priceHistory)

            // Initialize StockBalance cache
            stockBalanceDao.upsertStockBalance(
                StockBalanceEntity(
                    productId = product.productId,
                    quantity = 0.0,
                    averageCost = 0.0,
                    updatedAt = now
                )
            )
        } else {
            // Update existing Product
            productDao.updateProduct(product.toEntity())

            // Update barcodes
            barcodeDao.deleteBarcodesForProduct(product.productId)
            if (product.barcodes.isNotEmpty()) {
                barcodeDao.insertBarcodes(product.barcodes.map { it.toEntity() })
            }

            // Check if selling price changed
            if (existing.sellingPrice != product.sellingPrice) {
                // Close previous open price history
                priceHistoryDao.closePreviousPriceHistory(product.productId, now)
                // Insert new price history record
                val newPriceHistory = PriceHistoryEntity(
                    priceHistoryId = UUID.randomUUID().toString(),
                    productId = product.productId,
                    sellingPrice = product.sellingPrice,
                    effectiveFrom = now,
                    effectiveTo = null,
                    changedBy = priceChangedBy,
                    createdAt = now
                )
                priceHistoryDao.insertPriceHistory(newPriceHistory)
            }
        }
    }

    override suspend fun setProductActiveStatus(productId: String, isActive: Boolean): Result<kotlin.Unit> = runCatching {
        productDao.setProductActiveStatus(productId, isActive, System.currentTimeMillis())
    }

    override suspend fun isBarcodeTaken(shopId: String, barcode: String, excludeProductId: String?): Boolean {
        val existing = barcodeDao.findBarcodeInShop(shopId, barcode.trim()) ?: return false
        return if (excludeProductId != null) {
            existing.productId != excludeProductId
        } else {
            true
        }
    }

    override fun getPriceHistoryFlow(productId: String): Flow<List<PriceHistory>> {
        return priceHistoryDao.getPriceHistoryForProductFlow(productId).map { list -> list.map { it.toDomain() } }
    }

    override fun getStockBalanceFlow(productId: String): Flow<StockBalance?> {
        return stockBalanceDao.getStockBalanceFlow(productId).map { it?.toDomain() }
    }

    override fun getTotalProductCountFlow(shopId: String): Flow<Int> {
        return productDao.getTotalProductCountFlow(shopId)
    }

    override fun getActiveProductCountFlow(shopId: String): Flow<Int> {
        return productDao.getActiveProductCountFlow(shopId)
    }
}

// Extension mappers
private fun ShopEntity.toDomain() = Shop(shopId, name, ownerName, phone, address, currency, timezone, createdAt, updatedAt)
private fun Shop.toEntity() = ShopEntity(shopId, name, ownerName, phone, address, currency, timezone, createdAt, updatedAt)

private fun DeviceEntity.toDomain() = Device(deviceId, shopId, deviceName, deviceType, isPrimary, status, createdAt, lastSeenAt)
private fun Device.toEntity() = DeviceEntity(deviceId, shopId, deviceName, deviceType, isPrimary, status, createdAt, lastSeenAt)

private fun CategoryEntity.toDomain() = Category(categoryId, shopId, name, parentCategoryId, isActive, createdAt, updatedAt)
private fun Category.toEntity() = CategoryEntity(categoryId, shopId, name, parentCategoryId, isActive, createdAt, updatedAt)

private fun UnitEntity.toDomain() = Unit(unitId, code, name, symbol, isCustom)
private fun Unit.toEntity() = UnitEntity(unitId, code, name, symbol, isCustom)

private fun ProductEntity.toDomain(barcodes: List<Barcode>) = Product(
    productId, shopId, name, categoryId, brand, sku, baseUnitId, sellingUnitId, conversionFactor, sellingPrice, minimumStock, trackExpiry, trackBatch, isActive, barcodes, createdAt, updatedAt
)
private fun Product.toEntity() = ProductEntity(
    productId, shopId, name, categoryId, brand, sku, baseUnitId, sellingUnitId, conversionFactor, sellingPrice, minimumStock, trackExpiry, trackBatch, isActive, createdAt, updatedAt
)

private fun BarcodeEntity.toDomain() = Barcode(barcodeId, productId, barcode, isPrimary, shopId)
private fun Barcode.toEntity() = BarcodeEntity(barcodeId, productId, barcode, isPrimary, shopId)

private fun PriceHistoryEntity.toDomain() = PriceHistory(priceHistoryId, productId, sellingPrice, effectiveFrom, effectiveTo, changedBy, createdAt)
private fun StockBalanceEntity.toDomain() = StockBalance(productId, quantity, averageCost, updatedAt)
