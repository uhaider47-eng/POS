package com.example.grocerypos

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.PriceHistoryDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.database.GroceryPosDatabase
import com.example.grocerypos.data.local.database.RoomTransactionRunner
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.data.repository.ProductRepositoryImpl
import com.example.grocerypos.domain.model.Barcode
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.UnitCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: GroceryPosDatabase
    private lateinit var shopDao: ShopDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var unitDao: UnitDao
    private lateinit var productDao: ProductDao
    private lateinit var barcodeDao: BarcodeDao
    private lateinit var priceHistoryDao: PriceHistoryDao
    private lateinit var stockBalanceDao: StockBalanceDao
    private lateinit var productRepository: ProductRepositoryImpl

    private val testShopId = UUID.randomUUID().toString()
    private val testBaseUnitId = "unit_piece"
    private val testCategoryId = UUID.randomUUID().toString()

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GroceryPosDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        shopDao = db.shopDao()
        deviceDao = db.deviceDao()
        categoryDao = db.categoryDao()
        unitDao = db.unitDao()
        productDao = db.productDao()
        barcodeDao = db.barcodeDao()
        priceHistoryDao = db.priceHistoryDao()
        stockBalanceDao = db.stockBalanceDao()

        val runner = RoomTransactionRunner(db)
        productRepository = ProductRepositoryImpl(
            productDao = productDao,
            barcodeDao = barcodeDao,
            priceHistoryDao = priceHistoryDao,
            stockBalanceDao = stockBalanceDao,
            transactionRunner = runner
        )

        // Seed initial unit
        unitDao.insertUnit(
            UnitEntity(testBaseUnitId, UnitCode.PIECE, "Piece", "pc", false)
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // 1. Shop creation
    @Test
    fun test1_shopCreation() = runBlocking {
        val shop = ShopEntity(
            shopId = testShopId,
            name = "Al-Madina Super Store",
            ownerName = "Muhammad Aslam",
            phone = "03001234567",
            address = "Lahore, Pakistan",
            currency = "PKR",
            timezone = "Asia/Karachi",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        shopDao.insertShop(shop)

        val retrieved = shopDao.getShopSync()
        assertNotNull(retrieved)
        assertEquals("Al-Madina Super Store", retrieved?.name)
        assertEquals("PKR", retrieved?.currency)
        assertEquals("Asia/Karachi", retrieved?.timezone)
    }

    // 2. Device creation & primary device single-hub
    @Test
    fun test2_deviceCreation() = runBlocking {
        test1_shopCreation()

        val deviceId = UUID.randomUUID().toString()
        val device = DeviceEntity(
            deviceId = deviceId,
            shopId = testShopId,
            deviceName = "Counter 1 Main Terminal",
            deviceType = DeviceType.TABLET,
            isPrimary = true,
            status = DeviceStatus.ACTIVE,
            createdAt = System.currentTimeMillis(),
            lastSeenAt = System.currentTimeMillis()
        )
        deviceDao.insertDevice(device)

        val primaryDevice = deviceDao.getPrimaryDeviceSync(testShopId)
        assertNotNull(primaryDevice)
        assertEquals(true, primaryDevice?.isPrimary)
        assertEquals(DeviceType.TABLET, primaryDevice?.deviceType)
        assertEquals("Counter 1 Main Terminal", primaryDevice?.deviceName)
    }

    // 3. Product creation with Money & Quantity precision
    @Test
    fun test3_productCreationWithMoneyAndQuantity() = runBlocking {
        test1_shopCreation()

        val category = CategoryEntity(
            categoryId = testCategoryId,
            shopId = testShopId,
            name = "Beverages",
            parentCategoryId = null,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insertCategory(category)

        val productId = UUID.randomUUID().toString()
        val product = ProductEntity(
            productId = productId,
            shopId = testShopId,
            name = "Tapal Danedar Tea 430g",
            categoryId = testCategoryId,
            brand = "Tapal",
            sku = "TAP-430",
            baseUnitId = testBaseUnitId,
            sellingUnitId = testBaseUnitId,
            conversionFactor = Quantity.ONE,
            sellingPrice = Money.parseOrDefault("650.50"), // 65050 minor units
            minimumStock = Quantity.fromWholeUnits(5),    // 5000 scaled units
            trackExpiry = true,
            trackBatch = false,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        productDao.insertProduct(product)

        val retrieved = productDao.getProductById(productId)
        assertNotNull(retrieved)
        assertEquals("Tapal Danedar Tea 430g", retrieved?.name)
        assertEquals(Money.fromMinorUnits(65050), retrieved?.sellingPrice)
        assertEquals(Quantity.fromScaledUnits(5000), retrieved?.minimumStock)
    }

    // 4. Atomic Product Save Transaction
    @Test
    fun test4_atomicProductSaveWithBarcodesAndPriceHistory() = runBlocking {
        test1_shopCreation()

        val category = CategoryEntity(
            categoryId = testCategoryId,
            shopId = testShopId,
            name = "Beverages",
            parentCategoryId = null,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insertCategory(category)

        val productId = UUID.randomUUID().toString()
        val domainProduct = Product(
            productId = productId,
            shopId = testShopId,
            name = "Rooh Afza 800ml",
            categoryId = testCategoryId,
            brand = "Hamdard",
            sku = "HAM-RA-800",
            baseUnitId = testBaseUnitId,
            sellingUnitId = testBaseUnitId,
            conversionFactor = Quantity.ONE,
            sellingPrice = Money.parseOrDefault("450.00"),
            minimumStock = Quantity.fromWholeUnits(10),
            trackExpiry = true,
            trackBatch = false,
            isActive = true,
            barcodes = listOf(
                Barcode(UUID.randomUUID().toString(), productId, "8964000112233", true, testShopId)
            )
        )

        // Save atomically via ProductRepository
        val result = productRepository.saveProduct(domainProduct)
        assertTrue(result.isSuccess)

        // Verify all 4 database tables were populated in one atomic transaction:
        // 1. Product table
        val savedProduct = productDao.getProductById(productId)
        assertNotNull(savedProduct)
        assertEquals(Money.fromRupees(450), savedProduct?.sellingPrice)
        assertEquals(Quantity.ONE, savedProduct?.conversionFactor)

        // 2. Barcode table
        val barcodes = barcodeDao.getBarcodesForProduct(productId)
        assertEquals(1, barcodes.size)
        assertEquals("8964000112233", barcodes[0].barcode)

        // 3. Price History table
        val currentPrice = priceHistoryDao.getCurrentPrice(productId)
        assertNotNull(currentPrice)
        assertEquals(Money.fromRupees(450), currentPrice?.sellingPrice)

        // 4. Stock Balance table initialized
        val balance = stockBalanceDao.getStockBalance(productId)
        assertNotNull(balance)
        assertEquals(Quantity.ZERO, balance?.quantity)
        assertEquals(Money.ZERO, balance?.averageCost)
    }

    // 5. Price update maintains immutable audit trail
    @Test
    fun test5_priceUpdateCreatesAuditTrail() = runBlocking {
        test4_atomicProductSaveWithBarcodesAndPriceHistory()

        val products = productDao.getProductsForShopSync(testShopId)
        val product = products.first()

        // Update price from Rs. 450 to Rs. 490
        val updatedProduct = product.copy(
            sellingPrice = Money.parseOrDefault("490.00")
        )

        val domainProduct = Product(
            productId = updatedProduct.productId,
            shopId = updatedProduct.shopId,
            name = updatedProduct.name,
            categoryId = updatedProduct.categoryId,
            brand = updatedProduct.brand,
            sku = updatedProduct.sku,
            baseUnitId = updatedProduct.baseUnitId,
            sellingUnitId = updatedProduct.sellingUnitId,
            conversionFactor = updatedProduct.conversionFactor,
            sellingPrice = Money.parseOrDefault("490.00"),
            minimumStock = updatedProduct.minimumStock,
            trackExpiry = updatedProduct.trackExpiry,
            trackBatch = updatedProduct.trackBatch,
            isActive = updatedProduct.isActive,
            barcodes = listOf(Barcode(UUID.randomUUID().toString(), updatedProduct.productId, "8964000112233", true, testShopId))
        )

        productRepository.saveProduct(domainProduct, priceChangedBy = "admin_user")

        val history = priceHistoryDao.getPriceHistoryForProduct(product.productId).first()
        assertEquals(2, history.size)

        // Most recent price is Rs. 490 with active effectiveTo = null
        assertEquals(Money.parseOrDefault("490.00"), history[0].sellingPrice)
        assertNull(history[0].effectiveTo)

        // Previous price is Rs. 450 with closed effectiveTo
        assertEquals(Money.parseOrDefault("450.00"), history[1].sellingPrice)
        assertNotNull(history[1].effectiveTo)
    }

    // 6. Atomic Transaction Rollback on Failure
    @Test
    fun test6_atomicRollbackOnFailure() = runBlocking {
        test1_shopCreation()

        val category = CategoryEntity(
            categoryId = testCategoryId,
            shopId = testShopId,
            name = "Beverages",
            parentCategoryId = null,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insertCategory(category)

        // Pre-existing product with a barcode
        val prod1 = Product(
            productId = UUID.randomUUID().toString(),
            shopId = testShopId,
            name = "Existing Drink",
            categoryId = testCategoryId,
            sellingPrice = Money.fromRupees(100),
            barcodes = listOf(Barcode(UUID.randomUUID().toString(), "", "111222333", true, testShopId)),
            baseUnitId = testBaseUnitId,
            sellingUnitId = testBaseUnitId
        )
        productRepository.saveProduct(prod1)

        // Second product attempting to use the EXACT SAME duplicate barcode in the same shop
        val prod2Id = UUID.randomUUID().toString()
        val prod2 = Product(
            productId = prod2Id,
            shopId = testShopId,
            name = "Conflicting Drink",
            categoryId = testCategoryId,
            sellingPrice = Money.fromRupees(200),
            barcodes = listOf(Barcode(UUID.randomUUID().toString(), prod2Id, "111222333", true, testShopId)),
            baseUnitId = testBaseUnitId,
            sellingUnitId = testBaseUnitId
        )

        val result = productRepository.saveProduct(prod2)
        assertTrue(result.isFailure)

        // Ensure prod2 was completely rolled back: no product entity and no price history created
        val rolledBackProduct = productDao.getProductById(prod2Id)
        assertNull(rolledBackProduct)

        val rolledBackPrice = priceHistoryDao.getCurrentPrice(prod2Id)
        assertNull(rolledBackPrice)
    }

    // 7. Direct TransactionRunner rollback on unexpected runtime exception
    @Test
    fun test7_directTransactionRollbackOnException() = runBlocking {
        test1_shopCreation()

        val tempCategoryId = UUID.randomUUID().toString()
        val tempProductId = UUID.randomUUID().toString()

        var caughtException = false
        try {
            transactionRunner.runInTransaction {
                // Step 1: Insert category
                categoryDao.insertCategory(
                    CategoryEntity(
                        categoryId = tempCategoryId,
                        shopId = testShopId,
                        name = "Temporary Category",
                        parentCategoryId = null,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Step 2: Insert product
                productDao.insertProduct(
                    ProductEntity(
                        productId = tempProductId,
                        shopId = testShopId,
                        name = "Temporary Product",
                        categoryId = tempCategoryId,
                        brand = "Brand",
                        sku = "TEMP-1",
                        baseUnitId = testBaseUnitId,
                        sellingUnitId = testBaseUnitId,
                        conversionFactor = Quantity.fromWholeUnits(12),
                        sellingPrice = Money.fromRupees(150),
                        minimumStock = Quantity.ZERO,
                        trackExpiry = false,
                        trackBatch = false,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Step 3: Simulate catastrophic failure midway through transaction
                throw IllegalStateException("Simulated mid-transaction failure")
            }
        } catch (e: IllegalStateException) {
            caughtException = true
        }

        assertTrue("Expected transaction exception was not thrown", caughtException)

        // Verify ACID Atomicity: neither the category nor the product should exist in the database
        assertNull("Category must be rolled back", categoryDao.getCategoryById(tempCategoryId))
        assertNull("Product must be rolled back", productDao.getProductById(tempProductId))
    }

    // 8. Deterministic fixed-point conversion factor persistence
    @Test
    fun test8_fixedPointConversionFactorPersistence() = runBlocking {
        test1_shopCreation()

        val category = CategoryEntity(
            categoryId = testCategoryId,
            shopId = testShopId,
            name = "Packaged Goods",
            parentCategoryId = null,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insertCategory(category)

        val cartonProductId = UUID.randomUUID().toString()
        // 1 Carton = 24 Pieces (conversion factor 24.000 -> 24000 scaled units)
        val cartonProduct = ProductEntity(
            productId = cartonProductId,
            shopId = testShopId,
            name = "Biscuits Box 24-Pack",
            categoryId = testCategoryId,
            brand = "LU",
            sku = "LU-BOX-24",
            baseUnitId = testBaseUnitId,
            sellingUnitId = testBaseUnitId,
            conversionFactor = Quantity.fromWholeUnits(24),
            sellingPrice = Money.fromRupees(1200),
            minimumStock = Quantity.fromWholeUnits(2),
            trackExpiry = true,
            trackBatch = false,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        productDao.insertProduct(cartonProduct)

        val retrieved = productDao.getProductById(cartonProductId)
        assertNotNull(retrieved)
        assertEquals(24000L, retrieved?.conversionFactor?.amountInScaledUnits)
        assertEquals(Quantity.fromWholeUnits(24), retrieved?.conversionFactor)
        assertEquals("24", retrieved?.conversionFactor?.toFormattedString())

        // Fractional conversion factor: 1 Half-Kg pack = 0.500 kg (500 scaled units)
        val halfKgProductId = UUID.randomUUID().toString()
        val halfKgProduct = ProductEntity(
            productId = halfKgProductId,
            shopId = testShopId,
            name = "Sugar 500g Pack",
            categoryId = testCategoryId,
            brand = "SugarMills",
            sku = "SUG-500G",
            baseUnitId = testBaseUnitId,
            sellingUnitId = testBaseUnitId,
            conversionFactor = Quantity.fromScaledUnits(500),
            sellingPrice = Money.fromRupees(75),
            minimumStock = Quantity.fromWholeUnits(10),
            trackExpiry = false,
            trackBatch = false,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        productDao.insertProduct(halfKgProduct)

        val retrievedHalfKg = productDao.getProductById(halfKgProductId)
        assertNotNull(retrievedHalfKg)
        assertEquals(500L, retrievedHalfKg?.conversionFactor?.amountInScaledUnits)
        assertEquals(Quantity.fromScaledUnits(500), retrievedHalfKg?.conversionFactor)
        assertEquals("0.5", retrievedHalfKg?.conversionFactor?.toFormattedString())
    }
}
