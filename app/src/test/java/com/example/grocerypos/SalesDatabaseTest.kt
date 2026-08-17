package com.example.grocerypos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.grocerypos.data.local.dao.AuditLogDao
import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CashMovementDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.CustomerDao
import com.example.grocerypos.data.local.dao.CustomerLedgerDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.InvoiceSequenceDao
import com.example.grocerypos.data.local.dao.PaymentDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.RoleDao
import com.example.grocerypos.data.local.dao.SaleDao
import com.example.grocerypos.data.local.dao.SaleItemDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.dao.UserDao
import com.example.grocerypos.data.local.database.GroceryPosDatabase
import com.example.grocerypos.data.local.entity.AuditLogEntity
import com.example.grocerypos.data.local.entity.CashMovementEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.CustomerEntity
import com.example.grocerypos.data.local.entity.CustomerLedgerEntryEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.InvoiceSequenceEntity
import com.example.grocerypos.data.local.entity.PaymentEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.RoleEntity
import com.example.grocerypos.data.local.entity.SaleEntity
import com.example.grocerypos.data.local.entity.SaleItemEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.SyncEventEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.data.local.entity.UserEntity
import com.example.grocerypos.domain.model.AuditAction
import com.example.grocerypos.domain.model.CashMovementType
import com.example.grocerypos.domain.model.CustomerLedgerType
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.SyncOperation
import com.example.grocerypos.domain.model.SyncStatus
import com.example.grocerypos.domain.model.UnitCode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SalesDatabaseTest {

    private lateinit var db: GroceryPosDatabase
    private lateinit var shopDao: ShopDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var roleDao: RoleDao
    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var unitDao: UnitDao
    private lateinit var productDao: ProductDao
    private lateinit var customerDao: CustomerDao
    private lateinit var saleDao: SaleDao
    private lateinit var saleItemDao: SaleItemDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var customerLedgerDao: CustomerLedgerDao
    private lateinit var cashMovementDao: CashMovementDao
    private lateinit var auditLogDao: AuditLogDao
    private lateinit var syncEventDao: SyncEventDao
    private lateinit var invoiceSequenceDao: InvoiceSequenceDao

    private val testShopId = UUID.randomUUID().toString()
    private val testDeviceId = UUID.randomUUID().toString()
    private val testUserId = UUID.randomUUID().toString()
    private val testRoleId = "role_cashier"
    private val testCategoryId = UUID.randomUUID().toString()
    private val testUnitId = "unit_piece"
    private val testCustomerId = UUID.randomUUID().toString()
    private val testProductId = UUID.randomUUID().toString()

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GroceryPosDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        shopDao = db.shopDao()
        deviceDao = db.deviceDao()
        roleDao = db.roleDao()
        userDao = db.userDao()
        categoryDao = db.categoryDao()
        unitDao = db.unitDao()
        productDao = db.productDao()
        customerDao = db.customerDao()
        saleDao = db.saleDao()
        saleItemDao = db.saleItemDao()
        paymentDao = db.paymentDao()
        customerLedgerDao = db.customerLedgerDao()
        cashMovementDao = db.cashMovementDao()
        auditLogDao = db.auditLogDao()
        syncEventDao = db.syncEventDao()
        invoiceSequenceDao = db.invoiceSequenceDao()

        // Seed basic fixtures
        val now = System.currentTimeMillis()

        shopDao.insertShop(
            ShopEntity(
                shopId = testShopId,
                name = "Al-Madina Super Store",
                ownerName = "Muhammad Aslam",
                phone = "03001234567",
                address = "Lahore, Pakistan",
                currency = "PKR",
                timezone = "Asia/Karachi",
                createdAt = now,
                updatedAt = now
            )
        )

        deviceDao.insertDevice(
            DeviceEntity(
                deviceId = testDeviceId,
                shopId = testShopId,
                deviceName = "Counter 1 Main Terminal",
                deviceType = DeviceType.TABLET,
                isPrimary = true,
                status = DeviceStatus.ACTIVE,
                createdAt = now,
                lastSeenAt = now
            )
        )

        roleDao.insertRole(
            RoleEntity(
                roleId = testRoleId,
                name = RoleName.CASHIER,
                description = "Cashier role",
                permissionsJson = "[]"
            )
        )

        userDao.insertUser(
            UserEntity(
                userId = testUserId,
                shopId = testShopId,
                roleId = testRoleId,
                username = "cashier1",
                passwordHash = "hash123",
                fullName = "Ali Raza",
                phone = "03009876543",
                pin = "1234",
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        unitDao.insertUnit(
            UnitEntity(
                unitId = testUnitId,
                code = UnitCode.PIECE,
                name = "Piece",
                symbol = "pc",
                isCustom = false
            )
        )

        categoryDao.insertCategory(
            CategoryEntity(
                categoryId = testCategoryId,
                shopId = testShopId,
                name = "Dairy",
                parentCategoryId = null,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        productDao.insertProduct(
            ProductEntity(
                productId = testProductId,
                shopId = testShopId,
                name = "Milk Pack 1 Litre",
                categoryId = testCategoryId,
                brand = "Nestle",
                sku = "NES-MILK-1L",
                baseUnitId = testUnitId,
                sellingUnitId = testUnitId,
                conversionFactor = Quantity.ONE,
                sellingPrice = Money.fromRupees(280),
                minimumStock = Quantity.fromWholeUnits(10),
                trackExpiry = true,
                trackBatch = false,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        customerDao.insertCustomer(
            CustomerEntity(
                customerId = testCustomerId,
                shopId = testShopId,
                name = "Tariq Mahmood",
                phone = "03123456789",
                address = "Model Town, Lahore",
                creditLimit = Money.fromRupees(5000),
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testSaleInsertionAndRetrievalWithItemsAndPayments() = runBlocking {
        val now = System.currentTimeMillis()
        val saleId = UUID.randomUUID().toString()

        val sale = SaleEntity(
            saleId = saleId,
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = "INV-000001",
            cashierId = testUserId,
            customerId = testCustomerId,
            subtotal = Money.fromRupees(560), // 2 packs @ 280
            itemDiscount = Money.ZERO,
            saleDiscount = Money.fromRupees(60),
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(500),
            paidAmount = Money.fromRupees(500),
            dueAmount = Money.ZERO,
            status = SaleStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            notes = "Counter sale",
            createdAt = now,
            completedAt = now,
            updatedAt = now
        )
        saleDao.insertSale(sale)

        val saleItemId = UUID.randomUUID().toString()
        val saleItem = SaleItemEntity(
            saleItemId = saleItemId,
            saleId = saleId,
            productId = testProductId,
            productName = "Milk Pack 1 Litre",
            soldUnitId = testUnitId,
            quantity = Quantity.fromWholeUnits(2),
            unitPrice = Money.fromRupees(280),
            grossAmount = Money.fromRupees(560),
            discount = Money.ZERO,
            tax = Money.ZERO,
            netAmount = Money.fromRupees(560),
            costAtSale = Money.fromRupees(250),
            createdAt = now
        )
        saleItemDao.insertSaleItem(saleItem)

        val paymentId = UUID.randomUUID().toString()
        val payment = PaymentEntity(
            paymentId = paymentId,
            saleId = saleId,
            shopId = testShopId,
            method = PaymentMethod.CASH,
            amount = Money.fromRupees(500),
            referenceNumber = null,
            receivedAt = now,
            receivedBy = testUserId
        )
        paymentDao.insertPayment(payment)

        // Retrieve sale with details relation
        val details = saleDao.getSaleWithDetails(saleId)
        assertNotNull(details)
        assertEquals("INV-000001", details?.sale?.invoiceNumber)
        assertEquals(Money.fromRupees(500), details?.sale?.grandTotal)
        assertEquals(SaleStatus.COMPLETED, details?.sale?.status)
        assertEquals(PaymentStatus.PAID, details?.sale?.paymentStatus)

        // Verify items relation
        assertEquals(1, details?.items?.size)
        val item = details?.items?.first()
        assertEquals(testProductId, item?.productId)
        assertEquals(Quantity.fromWholeUnits(2), item?.quantity)
        assertEquals(Money.fromRupees(560), item?.grossAmount)

        // Verify payments relation
        assertEquals(1, details?.payments?.size)
        val pmt = details?.payments?.first()
        assertEquals(PaymentMethod.CASH, pmt?.method)
        assertEquals(Money.fromRupees(500), pmt?.amount)
    }

    @Test
    fun testCustomerLedgerAndCashMovementEntities() = runBlocking {
        val now = System.currentTimeMillis()

        // Test Customer Ledger Entry
        val entryId = UUID.randomUUID().toString()
        val ledgerEntry = CustomerLedgerEntryEntity(
            entryId = entryId,
            customerId = testCustomerId,
            shopId = testShopId,
            type = CustomerLedgerType.CREDIT_SALE,
            amount = Money.fromRupees(1500),
            referenceType = "SALE",
            referenceId = "sale-123",
            notes = "Credit purchase on invoice INV-000002",
            createdBy = testUserId,
            createdAt = now
        )
        customerLedgerDao.insertLedgerEntry(ledgerEntry)

        val entries = customerLedgerDao.getEntriesForCustomer(testCustomerId)
        assertEquals(1, entries.size)
        assertEquals(CustomerLedgerType.CREDIT_SALE, entries.first().type)
        assertEquals(Money.fromRupees(1500), entries.first().amount)

        // Test Cash Movement
        val movementId = UUID.randomUUID().toString()
        val cashMovement = CashMovementEntity(
            movementId = movementId,
            shopId = testShopId,
            deviceId = testDeviceId,
            type = CashMovementType.SALE_INFLOW,
            amount = Money.fromRupees(1500),
            referenceType = "SALE",
            referenceId = "sale-123",
            notes = "Counter collection",
            createdBy = testUserId,
            createdAt = now
        )
        cashMovementDao.insertCashMovement(cashMovement)

        assertNotNull(cashMovement)
    }

    @Test
    fun testAuditLogAndSyncEventDaos() = runBlocking {
        val now = System.currentTimeMillis()

        // Test Audit Log
        val logId = UUID.randomUUID().toString()
        val auditLog = AuditLogEntity(
            logId = logId,
            shopId = testShopId,
            userId = testUserId,
            action = AuditAction.COMPLETE_SALE,
            entityType = "Sale",
            entityId = "sale-001",
            details = "Completed sale with invoice INV-000001",
            timestamp = now
        )
        auditLogDao.insertAuditLog(auditLog)

        // Test Sync Event
        val eventId = UUID.randomUUID().toString()
        val syncEvent = SyncEventEntity(
            eventId = eventId,
            shopId = testShopId,
            deviceId = testDeviceId,
            entityType = "Sale",
            entityId = "sale-001",
            operation = SyncOperation.INSERT,
            syncStatus = SyncStatus.PENDING,
            timestamp = now
        )
        syncEventDao.insertSyncEvent(syncEvent)

        val pending = syncEventDao.getPendingSyncEvents(testShopId)
        assertEquals(1, pending.size)
        assertEquals(SyncStatus.PENDING, pending.first().syncStatus)

        syncEventDao.updateSyncStatus(eventId, SyncStatus.SYNCED)
        val pendingAfter = syncEventDao.getPendingSyncEvents(testShopId)
        assertEquals(0, pendingAfter.size)
    }

    @Test
    fun testInvoiceSequenceUpsertAndIncrement() = runBlocking {
        val now = System.currentTimeMillis()

        val seq = InvoiceSequenceEntity(
            shopId = testShopId,
            nextNumber = 1L,
            prefix = "INV-",
            updatedAt = now
        )
        invoiceSequenceDao.upsertSequence(seq)

        val retrieved = invoiceSequenceDao.getSequence(testShopId)
        assertNotNull(retrieved)
        assertEquals(1L, retrieved?.nextNumber)
        assertEquals("INV-", retrieved?.prefix)

        // Increment sequence
        invoiceSequenceDao.incrementNextNumber(testShopId, now + 100)
        val updated = invoiceSequenceDao.getSequence(testShopId)
        assertEquals(2L, updated?.nextNumber)
    }
}
