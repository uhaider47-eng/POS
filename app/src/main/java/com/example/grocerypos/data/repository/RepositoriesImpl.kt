package com.example.grocerypos.data.repository

import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.InvoiceSequenceDao
import com.example.grocerypos.data.local.dao.PriceHistoryDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.RoleDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.dao.UserDao
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.InvoiceSequenceEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.data.local.entity.UserEntity
import com.example.grocerypos.domain.model.AppPermission
import com.example.grocerypos.domain.model.Barcode
import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Device
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.InvoiceSequence
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PriceHistory
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.model.StockBalance
import com.example.grocerypos.domain.model.Unit
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.DeviceRepository
import com.example.grocerypos.domain.repository.InvoiceSequenceRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import com.example.grocerypos.domain.transaction.TransactionRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepositoryImpl @Inject constructor(
    private val shopDao: ShopDao,
    private val deviceDao: DeviceDao,
    private val userDao: UserDao,
    private val roleDao: RoleDao,
    private val transactionRunner: TransactionRunner
) : ShopRepository {

    override fun getShopFlow(): Flow<Shop?> {
        return shopDao.getShop().map { it?.toDomain() }
    }

    override suspend fun getShop(): Shop? {
        return shopDao.getShopSync()?.toDomain()
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

    override suspend fun initializeShopAndPrimaryDevice(
        shop: Shop,
        primaryDevice: Device,
        ownerPin: String
    ): Result<kotlin.Unit> = runCatching {
        transactionRunner.runInTransaction {
            shopDao.insertShop(shop.toEntity())
            deviceDao.insertDevice(primaryDevice.copy(isPrimary = true).toEntity())

            val ownerRole = roleDao.getRoleByName("OWNER")
                ?: throw IllegalStateException("Default OWNER role not initialized")

            val ownerUser = UserEntity(
                userId = UUID.randomUUID().toString(),
                shopId = shop.shopId,
                roleId = ownerRole.roleId,
                name = shop.ownerName,
                phone = shop.phone,
                pinHash = hashPin(ownerPin),
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.insertUser(ownerUser)
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun ShopEntity.toDomain() = Shop(
        shopId = shopId,
        name = name,
        ownerName = ownerName,
        phone = phone,
        address = address,
        currency = currency,
        timezone = timezone,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Shop.toEntity() = ShopEntity(
        shopId = shopId,
        name = name,
        ownerName = ownerName,
        phone = phone,
        address = address,
        currency = currency,
        timezone = timezone,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Device.toEntity() = DeviceEntity(
        deviceId = deviceId,
        shopId = shopId,
        deviceName = deviceName,
        deviceType = deviceType,
        isPrimary = isPrimary,
        status = status,
        createdAt = createdAt,
        lastSeenAt = lastSeenAt
    )
}

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override fun getPrimaryDeviceFlow(shopId: String): Flow<Device?> {
        return deviceDao.getPrimaryDevice(shopId).map { it?.toDomain() }
    }

    override suspend fun getPrimaryDevice(shopId: String): Device? {
        return deviceDao.getPrimaryDeviceSync(shopId)?.toDomain()
    }

    override suspend fun registerDevice(device: Device): Result<kotlin.Unit> = runCatching {
        deviceDao.insertDevice(device.toEntity())
    }

    override fun getAllDevicesFlow(shopId: String): Flow<List<Device>> {
        return deviceDao.getDevicesForShop(shopId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateDeviceStatus(deviceId: String, status: DeviceStatus): Result<kotlin.Unit> = runCatching {
        deviceDao.updateDeviceStatus(deviceId, status)
    }

    private fun DeviceEntity.toDomain() = Device(
        deviceId = deviceId,
        shopId = shopId,
        deviceName = deviceName,
        deviceType = deviceType,
        isPrimary = isPrimary,
        status = status,
        createdAt = createdAt,
        lastSeenAt = lastSeenAt
    )

    private fun Device.toEntity() = DeviceEntity(
        deviceId = deviceId,
        shopId = shopId,
        deviceName = deviceName,
        deviceType = deviceType,
        isPrimary = isPrimary,
        status = status,
        createdAt = createdAt,
        lastSeenAt = lastSeenAt
    )
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategoriesFlow(shopId: String): Flow<List<Category>> {
        return categoryDao.getCategoriesForShop(shopId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getActiveCategories(shopId: String): List<Category> {
        return categoryDao.getActiveCategoriesSync(shopId).map { it.toDomain() }
    }

    override suspend fun createCategory(category: Category): Result<kotlin.Unit> = runCatching {
        categoryDao.insertCategory(category.toEntity())
    }

    private fun CategoryEntity.toDomain() = Category(
        categoryId = categoryId,
        shopId = shopId,
        name = name,
        parentCategoryId = parentCategoryId,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Category.toEntity() = CategoryEntity(
        categoryId = categoryId,
        shopId = shopId,
        name = name,
        parentCategoryId = parentCategoryId,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Singleton
class UnitRepositoryImpl @Inject constructor(
    private val unitDao: UnitDao
) : UnitRepository {

    override fun getAllUnitsFlow(): Flow<List<Unit>> {
        return unitDao.getAllUnits().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllUnits(): List<Unit> {
        return unitDao.getAllUnitsSync().map { it.toDomain() }
    }

    override suspend fun saveCustomUnit(unit: Unit): Result<kotlin.Unit> = runCatching {
        unitDao.insertUnit(unit.toEntity())
    }

    private fun UnitEntity.toDomain() = Unit(
        unitId = unitId,
        code = code,
        name = name,
        symbol = symbol,
        isCustom = isCustom
    )

    private fun Unit.toEntity() = UnitEntity(
        unitId = unitId,
        code = code,
        name = name,
        symbol = symbol,
        isCustom = isCustom
    )
}

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val barcodeDao: BarcodeDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val stockBalanceDao: StockBalanceDao,
    private val transactionRunner: TransactionRunner
) : ProductRepository {

    override fun getProductsFlow(shopId: String): Flow<List<Product>> {
        return productDao.getProductsForShop(shopId).combine(
            barcodeDao.getAllBarcodesForShop(shopId)
        ) { products, allBarcodes ->
            val barcodeMap = allBarcodes.groupBy { it.productId }
            products.map { p ->
                val barcodes = barcodeMap[p.productId]?.map { it.toDomain() } ?: emptyList()
                p.toDomain(barcodes)
            }
        }
    }

    override fun searchProductsFlow(shopId: String, query: String): Flow<List<Product>> {
        return productDao.searchProducts(shopId, query).combine(
            barcodeDao.getAllBarcodesForShop(shopId)
        ) { products, allBarcodes ->
            val barcodeMap = allBarcodes.groupBy { it.productId }
            products.map { p ->
                val barcodes = barcodeMap[p.productId]?.map { it.toDomain() } ?: emptyList()
                p.toDomain(barcodes)
            }
        }
    }

    override suspend fun getProductById(productId: String): Product? {
        val entity = productDao.getProductById(productId) ?: return null
        val barcodes = barcodeDao.getBarcodesForProduct(productId).map { it.toDomain() }
        return entity.toDomain(barcodes)
    }

    override suspend fun getProductByBarcode(shopId: String, barcode: String): Product? {
        val barcodeEntity = barcodeDao.findBarcode(shopId, barcode) ?: return null
        return getProductById(barcodeEntity.productId)
    }

    override suspend fun isBarcodeTaken(shopId: String, barcode: String, excludeProductId: String?): Boolean {
        val existing = barcodeDao.findBarcode(shopId, barcode) ?: return false
        return excludeProductId == null || existing.productId != excludeProductId
    }

    override suspend fun saveProduct(product: Product, priceChangedBy: String?): Result<kotlin.Unit> = runCatching {
        transactionRunner.runInTransaction {
            val now = System.currentTimeMillis()

            // 1. Barcode uniqueness check
            for (bc in product.barcodes) {
                val existing = barcodeDao.findBarcode(product.shopId, bc.barcode)
                if (existing != null && existing.productId != product.productId) {
                    throw IllegalStateException("Barcode '${bc.barcode}' is already assigned to another product")
                }
            }

            // 2. Insert/Update Product
            val productEntity = ProductEntity(
                productId = product.productId,
                shopId = product.shopId,
                name = product.name,
                categoryId = product.categoryId,
                brand = product.brand,
                sku = product.sku,
                baseUnitId = product.baseUnitId,
                sellingUnitId = product.sellingUnitId,
                conversionFactor = product.conversionFactor,
                sellingPrice = product.sellingPrice,
                minimumStock = product.minimumStock,
                trackExpiry = product.trackExpiry,
                trackBatch = product.trackBatch,
                isActive = product.isActive,
                createdAt = product.createdAt,
                updatedAt = now
            )
            productDao.insertProduct(productEntity)

            // 3. Atomically replace barcodes
            barcodeDao.deleteBarcodesForProduct(product.productId)
            for (bc in product.barcodes) {
                barcodeDao.insertBarcode(
                    BarcodeEntity(
                        barcodeId = bc.barcodeId.ifEmpty { UUID.randomUUID().toString() },
                        productId = product.productId,
                        barcode = bc.barcode,
                        isPrimary = bc.isPrimary,
                        shopId = product.shopId
                    )
                )
            }

            // 4. Update Price History if price changed or first time
            val currentPriceHistory = priceHistoryDao.getCurrentPrice(product.productId)
            if (currentPriceHistory == null || currentPriceHistory.sellingPrice != product.sellingPrice) {
                if (currentPriceHistory != null) {
                    priceHistoryDao.closePriceHistory(currentPriceHistory.priceHistoryId, now)
                }
                priceHistoryDao.insertPriceHistory(
                    PriceHistoryEntity(
                        priceHistoryId = UUID.randomUUID().toString(),
                        productId = product.productId,
                        sellingPrice = product.sellingPrice,
                        effectiveFrom = now,
                        effectiveTo = null,
                        changedBy = priceChangedBy,
                        createdAt = now
                    )
                )
            }

            // 5. Ensure Stock Balance record exists
            val currentStock = stockBalanceDao.getStockBalance(product.productId)
            if (currentStock == null) {
                stockBalanceDao.insertOrUpdateStockBalance(
                    StockBalanceEntity(
                        productId = product.productId,
                        quantity = Quantity.ZERO,
                        averageCost = Money.ZERO,
                        updatedAt = now
                    )
                )
            }
        }
    }

    override suspend fun deleteProduct(productId: String): Result<kotlin.Unit> = runCatching {
        transactionRunner.runInTransaction {
            barcodeDao.deleteBarcodesForProduct(productId)
            productDao.deleteProduct(productId)
        }
    }

    override suspend fun setProductActiveStatus(productId: String, isActive: Boolean): Result<kotlin.Unit> = runCatching {
        productDao.setProductActiveStatus(productId, isActive, System.currentTimeMillis())
    }

    override fun getPriceHistoryFlow(productId: String): Flow<List<PriceHistory>> {
        return priceHistoryDao.getPriceHistoryForProduct(productId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getStockBalanceFlow(productId: String): Flow<StockBalance?> {
        return stockBalanceDao.getStockBalanceFlow(productId).map { it?.toDomain() }
    }

    override fun getTotalProductCountFlow(shopId: String): Flow<Int> {
        return productDao.getTotalProductCount(shopId)
    }

    override fun getActiveProductCountFlow(shopId: String): Flow<Int> {
        return productDao.getActiveProductCount(shopId)
    }

    private fun ProductEntity.toDomain(barcodes: List<Barcode>) = Product(
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

    private fun BarcodeEntity.toDomain() = Barcode(
        barcodeId = barcodeId,
        productId = productId,
        barcode = barcode,
        isPrimary = isPrimary,
        shopId = shopId
    )

    private fun PriceHistoryEntity.toDomain() = PriceHistory(
        priceHistoryId = priceHistoryId,
        productId = productId,
        sellingPrice = sellingPrice,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        changedBy = changedBy,
        createdAt = createdAt
    )

    private fun StockBalanceEntity.toDomain() = StockBalance(
        productId = productId,
        quantity = quantity,
        averageCost = averageCost,
        updatedAt = updatedAt
    )
}

@Singleton
class InvoiceSequenceRepositoryImpl @Inject constructor(
    private val invoiceSequenceDao: InvoiceSequenceDao,
    private val transactionRunner: TransactionRunner
) : InvoiceSequenceRepository {

    override suspend fun allocateNextInvoiceNumber(shopId: String, defaultPrefix: String): Result<String> = runCatching {
        transactionRunner.runInTransaction {
            invoiceSequenceDao.allocateNextInvoiceNumber(shopId = shopId, defaultPrefix = defaultPrefix)
        }
    }

    override suspend fun getSequence(shopId: String): InvoiceSequence? {
        return invoiceSequenceDao.getSequence(shopId)?.let {
            InvoiceSequence(
                shopId = it.shopId,
                nextNumber = it.nextNumber,
                prefix = it.prefix,
                updatedAt = it.updatedAt
            )
        }
    }

    override suspend fun initializeSequence(shopId: String, startNumber: Long, prefix: String): Result<kotlin.Unit> = runCatching {
        invoiceSequenceDao.upsertSequence(
            InvoiceSequenceEntity(
                shopId = shopId,
                nextNumber = startNumber,
                prefix = prefix,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val paymentDao: PaymentDao
) : SaleRepository {

    override suspend fun getSaleById(saleId: String): Sale? {
        return saleDao.getSaleWithDetails(saleId)?.toDomain()
    }

    override fun getSalesFlow(shopId: String): Flow<List<Sale>> {
        return saleDao.getSalesForShopFlow(shopId).map { sales ->
            sales.map { it.toDomain() }
        }
    }

    override fun getSalesByStatusFlow(shopId: String, status: SaleStatus): Flow<List<Sale>> {
        return saleDao.getSalesByStatusFlow(shopId, status).map { sales ->
            sales.map { it.toDomain() }
        }
    }

    override fun getSalesWithDetailsByStatusFlow(shopId: String, status: SaleStatus): Flow<List<Sale>> {
        return saleDao.getSalesWithDetailsByStatusFlow(shopId, status).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun findSaleByInvoiceNumber(shopId: String, invoiceNumber: String): Sale? {
        val entity = saleDao.findSaleByInvoiceNumber(shopId, invoiceNumber) ?: return null
        val items = saleItemDao.getItemsForSale(entity.saleId).map { it.toDomain() }
        val payments = paymentDao.getPaymentsForSale(entity.saleId).map { it.toDomain() }
        return entity.toDomain(items = items, payments = payments)
    }
}


