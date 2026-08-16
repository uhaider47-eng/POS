package com.example.grocerypos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.PriceHistoryDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.database.GroceryPosDatabase
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.UnitCode
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

        // Seed initial unit
        unitDao.insertUnits(
            listOf(
                UnitEntity(testBaseUnitId, UnitCode.PIECE, "Piece", "pc", false)
            )
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

        val retrieved = shopDao.getShop()
        assertNotNull(retrieved)
        assertEquals("Al-Madina Super Store", retrieved?.name)
        assertEquals("PKR", retrieved?.currency)
        assertEquals("Asia/Karachi", retrieved?.timezone)
    }

    // 2. Device creation
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

        val primaryDevice = deviceDao.getPrimaryDevice(testShopId)
        assertNotNull(primaryDevice)
        assertEquals(true, primaryDevice?.isPrimary)
        assertEquals(DeviceType.TABLET, primaryDevice?.deviceType)
        assertEquals("Counter 1 Main Terminal", primaryDevice?.deviceName)
    }

    // 3. Product creation
    @Test
    fun test3_productCreation() = runBlocking {
        test1_shopCreation()

        // Insert category
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
            conversionFactor = 1.0,
            sellingPrice = 650.0,
            minimumStock = 5.0,
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
        assertEquals(650.0, retrieved?.sellingPrice ?: 0.0, 0.001)
    }

    // 4. Product retrieval
    @Test
    fun test4_productRetrieval() = runBlocking {
        test3_productCreation()
        val shop = shopDao.getShop()
        assertNotNull(shop)
    }

    // 5. Product update
    @Test
    fun test5_productUpdate() = runBlocking {
        test1_shopCreation()
        val cat = CategoryEntity(testCategoryId, testShopId, "Dairy", null, true, 0L, 0L)
        categoryDao.insertCategory(cat)

        val prodId = UUID.randomUUID().toString()
        val prod = ProductEntity(prodId, testShopId, "Milk Pack 1L", testCategoryId, "Nestle", "NES-1L", testBaseUnitId, testBaseUnitId, 1.0, 270.0, 10.0, true, true, true, 0L, 0L)
        productDao.insertProduct(prod)

        val updated = prod.copy(sellingPrice = 290.0, name = "Milk Pack 1L (Updated)")
        productDao.updateProduct(updated)

        val result = productDao.getProductById(prodId)
        assertEquals(290.0, result?.sellingPrice ?: 0.0, 0.001)
        assertEquals("Milk Pack 1L (Updated)", result?.name)
    }

    // 6. Product deactivation (Soft delete)
    @Test
    fun test6_productDeactivation() = runBlocking {
        test1_shopCreation()
        val cat = CategoryEntity(testCategoryId, testShopId, "Spices", null, true, 0L, 0L)
        categoryDao.insertCategory(cat)

        val prodId = UUID.randomUUID().toString()
        val prod = ProductEntity(prodId, testShopId, "Shan Biryani Masala", testCategoryId, "Shan", "SHN-BIR", testBaseUnitId, testBaseUnitId, 1.0, 120.0, 2.0, false, false, true, 0L, 0L)
        productDao.insertProduct(prod)

        productDao.setProductActiveStatus(prodId, false, System.currentTimeMillis())

        val retrieved = productDao.getProductById(prodId)
        assertNotNull(retrieved)
        assertFalse(retrieved!!.isActive)
    }

    // 7. Barcode uniqueness within shop
    @Test
    fun test7_barcodeUniqueness() = runBlocking {
        test1_shopCreation()
        val cat = CategoryEntity(testCategoryId, testShopId, "Snacks", null, true, 0L, 0L)
        categoryDao.insertCategory(cat)

        val p1 = ProductEntity(UUID.randomUUID().toString(), testShopId, "Lays Classic 40g", testCategoryId, "Lays", "LAY-CLS", testBaseUnitId, testBaseUnitId, 1.0, 60.0, 5.0, false, false, true, 0L, 0L)
        val p2 = ProductEntity(UUID.randomUUID().toString(), testShopId, "Kurkure Chutney", testCategoryId, "Kurkure", "KUR-CHT", testBaseUnitId, testBaseUnitId, 1.0, 60.0, 5.0, false, false, true, 0L, 0L)
        productDao.insertProduct(p1)
        productDao.insertProduct(p2)

        val barcodeStr = "8964000998877"
        val b1 = BarcodeEntity(UUID.randomUUID().toString(), p1.productId, barcodeStr, true, testShopId)
        barcodeDao.insertBarcode(b1)

        val existing = barcodeDao.findBarcodeInShop(testShopId, barcodeStr)
        assertNotNull(existing)
        assertEquals(p1.productId, existing?.productId)

        // Attempting to insert same barcode in same shop for product 2 triggers SQLiteConstraintException
        try {
            val b2 = BarcodeEntity(UUID.randomUUID().toString(), p2.productId, barcodeStr, true, testShopId)
            barcodeDao.insertBarcode(b2)
            fail("Expected SQLiteConstraintException on duplicate barcode within the same shop")
        } catch (e: Exception) {
            // Expected
            assertTrue(e.message?.contains("UNIQUE") == true || e.message?.contains("constraint") == true || e is android.database.sqlite.SQLiteConstraintException)
        }
    }

    // 8. Category relationship & Parent/Child support
    @Test
    fun test8_categoryRelationship() = runBlocking {
        test1_shopCreation()

        val parentCatId = UUID.randomUUID().toString()
        val parentCat = CategoryEntity(parentCatId, testShopId, "Food & Grocery", null, true, 0L, 0L)
        categoryDao.insertCategory(parentCat)

        val childCatId = UUID.randomUUID().toString()
        val childCat = CategoryEntity(childCatId, testShopId, "Cooking Oil", parentCatId, true, 0L, 0L)
        categoryDao.insertCategory(childCat)

        val retrievedChild = categoryDao.getCategoryById(childCatId)
        assertNotNull(retrievedChild)
        assertEquals(parentCatId, retrievedChild?.parentCategoryId)
    }

    // 9. Price history creation (Immutable audit)
    @Test
    fun test9_priceHistoryCreation() = runBlocking {
        test1_shopCreation()
        val cat = CategoryEntity(testCategoryId, testShopId, "Grains", null, true, 0L, 0L)
        categoryDao.insertCategory(cat)

        val prodId = UUID.randomUUID().toString()
        val prod = ProductEntity(prodId, testShopId, "Basmati Rice 5kg", testCategoryId, "Guard", "GRD-RIC-5", testBaseUnitId, testBaseUnitId, 1.0, 1800.0, 2.0, false, false, true, 0L, 0L)
        productDao.insertProduct(prod)

        val now = System.currentTimeMillis()
        val ph1 = PriceHistoryEntity(UUID.randomUUID().toString(), prodId, 1800.0, now - 100000, now, "user_1", now - 100000)
        val ph2 = PriceHistoryEntity(UUID.randomUUID().toString(), prodId, 1950.0, now, null, "user_1", now)
        priceHistoryDao.insertPriceHistory(ph1)
        priceHistoryDao.insertPriceHistory(ph2)

        val history = priceHistoryDao.getPriceHistoryForProduct(prodId)
        assertEquals(2, history.size)
        assertEquals(1950.0, history[0].sellingPrice, 0.001)
        assertNull(history[0].effectiveTo)
        assertEquals(1800.0, history[1].sellingPrice, 0.001)
        assertNotNull(history[1].effectiveTo)
    }

    // 10. Database persistence after reopening
    @Test
    fun test10_databasePersistenceAfterReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_persistence.db")
        dbFile.delete()

        // Create initial database
        var persistentDb = Room.databaseBuilder(context, GroceryPosDatabase::class.java, "test_persistence.db")
            .allowMainThreadQueries()
            .build()

        val shop = ShopEntity(testShopId, "Persistent Shop", "Imran Khan", "03009876543", "Islamabad", "PKR", "Asia/Karachi", 0L, 0L)
        persistentDb.shopDao().insertShop(shop)
        persistentDb.close()

        // Reopen database
        persistentDb = Room.databaseBuilder(context, GroceryPosDatabase::class.java, "test_persistence.db")
            .allowMainThreadQueries()
            .build()

        val reloadedShop = persistentDb.shopDao().getShop()
        assertNotNull(reloadedShop)
        assertEquals("Persistent Shop", reloadedShop?.name)
        assertEquals("Islamabad", reloadedShop?.address)

        persistentDb.close()
        dbFile.delete()
    }
}
