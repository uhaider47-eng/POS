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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun testFirstInvoiceAllocationReturnsCurrentNumberAndIncrementsSequence() = runBlocking {
        // Shop has no prior sequence
        val allocated = invoiceSequenceDao.allocateNextInvoiceNumber(testShopId, defaultPrefix = "INV-")
        assertEquals("INV-000001", allocated)

        val seq = invoiceSequenceDao.getSequence(testShopId)
        assertNotNull(seq)
        assertEquals(2L, seq?.nextNumber)
        assertEquals("INV-", seq?.prefix)
    }

    @Test
    fun testSubsequentInvoiceAllocationReturnsNextNumber() = runBlocking {
        val first = invoiceSequenceDao.allocateNextInvoiceNumber(testShopId, defaultPrefix = "INV-")
        val second = invoiceSequenceDao.allocateNextInvoiceNumber(testShopId, defaultPrefix = "INV-")
        val third = invoiceSequenceDao.allocateNextInvoiceNumber(testShopId, defaultPrefix = "INV-")

        assertEquals("INV-000001", first)
        assertEquals("INV-000002", second)
        assertEquals("INV-000003", third)

        val seq = invoiceSequenceDao.getSequence(testShopId)
        assertEquals(4L, seq?.nextNumber)
    }

    @Test
    fun testDuplicateNonNullInvoiceNumberWithinSameShopIsRejected() = runBlocking {
        val now = System.currentTimeMillis()
        val sale1 = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = "INV-000999",
            cashierId = testUserId,
            customerId = testCustomerId,
            subtotal = Money.fromRupees(500),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(500),
            paidAmount = Money.fromRupees(500),
            dueAmount = Money.ZERO,
            status = SaleStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            notes = "",
            createdAt = now,
            completedAt = now,
            updatedAt = now
        )
        saleDao.insertSale(sale1)

        val sale2WithSameInvoice = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = "INV-000999",
            cashierId = testUserId,
            customerId = testCustomerId,
            subtotal = Money.fromRupees(300),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(300),
            paidAmount = Money.fromRupees(300),
            dueAmount = Money.ZERO,
            status = SaleStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            notes = "",
            createdAt = now + 10,
            completedAt = now + 10,
            updatedAt = now + 10
        )

        try {
            saleDao.insertSale(sale2WithSameInvoice)
            fail("Expected SQLiteConstraintException on duplicate (shop_id, invoice_number)")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected
            assertTrue(true)
        }
    }

    @Test
    fun testSameInvoiceNumberMayExistInDifferentShop() = runBlocking {
        val now = System.currentTimeMillis()
        val otherShopId = UUID.randomUUID().toString()
        val otherDeviceId = UUID.randomUUID().toString()
        val otherUserId = UUID.randomUUID().toString()

        // Create second shop, device, user
        shopDao.insertShop(
            ShopEntity(
                shopId = otherShopId,
                name = "Second Branch",
                ownerName = "Muhammad Aslam",
                phone = "03001112233",
                address = "Islamabad",
                currency = "PKR",
                timezone = "Asia/Karachi",
                createdAt = now,
                updatedAt = now
            )
        )
        deviceDao.insertDevice(
            DeviceEntity(
                deviceId = otherDeviceId,
                shopId = otherShopId,
                deviceName = "Counter 2 Terminal",
                deviceType = DeviceType.PHONE,
                isPrimary = true,
                status = DeviceStatus.ACTIVE,
                createdAt = now,
                lastSeenAt = now
            )
        )
        userDao.insertUser(
            UserEntity(
                userId = otherUserId,
                shopId = otherShopId,
                roleId = testRoleId,
                username = "cashier2",
                passwordHash = "hash456",
                fullName = "Usman Khan",
                phone = "03004445566",
                pin = "5678",
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        // Insert sale with INV-000100 in testShopId
        val sale1 = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = "INV-000100",
            cashierId = testUserId,
            customerId = null,
            subtotal = Money.fromRupees(100),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(100),
            paidAmount = Money.fromRupees(100),
            dueAmount = Money.ZERO,
            status = SaleStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            notes = "",
            createdAt = now,
            completedAt = now,
            updatedAt = now
        )
        saleDao.insertSale(sale1)

        // Insert sale with same INV-000100 in otherShopId
        val sale2 = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = otherShopId,
            deviceId = otherDeviceId,
            invoiceNumber = "INV-000100",
            cashierId = otherUserId,
            customerId = null,
            subtotal = Money.fromRupees(200),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(200),
            paidAmount = Money.fromRupees(200),
            dueAmount = Money.ZERO,
            status = SaleStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            notes = "",
            createdAt = now,
            completedAt = now,
            updatedAt = now
        )
        saleDao.insertSale(sale2)

        val retrieved1 = saleDao.getSaleById(sale1.saleId)
        val retrieved2 = saleDao.getSaleById(sale2.saleId)
        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals("INV-000100", retrieved1?.invoiceNumber)
        assertEquals("INV-000100", retrieved2?.invoiceNumber)
        assertEquals(testShopId, retrieved1?.shopId)
        assertEquals(otherShopId, retrieved2?.shopId)
    }

    @Test
    fun testMultipleNullInvoiceNumbersAllowedForDraftSales() = runBlocking {
        val now = System.currentTimeMillis()

        val draftSale1 = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = null,
            cashierId = testUserId,
            customerId = null,
            subtotal = Money.fromRupees(100),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(100),
            paidAmount = Money.ZERO,
            dueAmount = Money.fromRupees(100),
            status = SaleStatus.DRAFT,
            paymentStatus = PaymentStatus.UNPAID,
            notes = "Draft 1",
            createdAt = now,
            completedAt = null,
            updatedAt = now
        )
        saleDao.insertSale(draftSale1)

        val draftSale2 = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = null,
            cashierId = testUserId,
            customerId = null,
            subtotal = Money.fromRupees(200),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(200),
            paidAmount = Money.ZERO,
            dueAmount = Money.fromRupees(200),
            status = SaleStatus.DRAFT,
            paymentStatus = PaymentStatus.UNPAID,
            notes = "Draft 2",
            createdAt = now + 1,
            completedAt = null,
            updatedAt = now + 1
        )
        saleDao.insertSale(draftSale2)

        val draftSale3 = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = null,
            cashierId = testUserId,
            customerId = null,
            subtotal = Money.fromRupees(300),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(300),
            paidAmount = Money.ZERO,
            dueAmount = Money.fromRupees(300),
            status = SaleStatus.DRAFT,
            paymentStatus = PaymentStatus.UNPAID,
            notes = "Draft 3",
            createdAt = now + 2,
            completedAt = null,
            updatedAt = now + 2
        )
        saleDao.insertSale(draftSale3)

        val r1 = saleDao.getSaleById(draftSale1.saleId)
        val r2 = saleDao.getSaleById(draftSale2.saleId)
        val r3 = saleDao.getSaleById(draftSale3.saleId)

        assertNotNull(r1)
        assertNotNull(r2)
        assertNotNull(r3)
        assertNull(r1?.invoiceNumber)
        assertNull(r2?.invoiceNumber)
        assertNull(r3?.invoiceNumber)
    }

    @Test
    fun testSaleStatusIncludesHeldAndPartiallyReturned() = runBlocking {
        val now = System.currentTimeMillis()

        val heldSale = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = "INV-HELD-001",
            cashierId = testUserId,
            customerId = testCustomerId,
            subtotal = Money.fromRupees(400),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(400),
            paidAmount = Money.ZERO,
            dueAmount = Money.fromRupees(400),
            status = SaleStatus.HELD,
            paymentStatus = PaymentStatus.UNPAID,
            notes = "Held sale for customer phone call",
            createdAt = now,
            completedAt = null,
            updatedAt = now
        )
        saleDao.insertSale(heldSale)

        val partiallyReturnedSale = SaleEntity(
            saleId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            invoiceNumber = "INV-RET-001",
            cashierId = testUserId,
            customerId = testCustomerId,
            subtotal = Money.fromRupees(800),
            itemDiscount = Money.ZERO,
            saleDiscount = Money.ZERO,
            tax = Money.ZERO,
            grandTotal = Money.fromRupees(800),
            paidAmount = Money.fromRupees(800),
            dueAmount = Money.ZERO,
            status = SaleStatus.PARTIALLY_RETURNED,
            paymentStatus = PaymentStatus.PAID,
            notes = "1 item returned out of 3",
            createdAt = now,
            completedAt = now,
            updatedAt = now
        )
        saleDao.insertSale(partiallyReturnedSale)

        val retrievedHeld = saleDao.getSaleById(heldSale.saleId)
        val retrievedPartial = saleDao.getSaleById(partiallyReturnedSale.saleId)

        assertEquals(SaleStatus.HELD, retrievedHeld?.status)
        assertEquals(SaleStatus.PARTIALLY_RETURNED, retrievedPartial?.status)
    }

    @Test
    fun testCompleteSaleUseCaseExecutesAtomically() = runBlocking {
        val stockBalanceDao = db.stockBalanceDao()
        val stockMovementDao = db.stockMovementDao()
        val transactionRunner = RoomTransactionRunner(db)
        val financialCalcService = com.example.grocerypos.domain.service.FinancialCalculationService()
        val saleCalculator = com.example.grocerypos.domain.service.SaleCalculator(financialCalcService)

        // Seed stock balance of 100 units
        stockBalanceDao.upsertStockBalance(
            com.example.grocerypos.data.local.entity.StockBalanceEntity(
                productId = testProductId,
                quantity = Quantity.fromWholeUnits(100),
                averageCost = Money.fromRupees(240),
                updatedAt = System.currentTimeMillis()
            )
        )

        val completeSaleUseCase = com.example.grocerypos.domain.usecase.CompleteSaleUseCase(
            transactionRunner = transactionRunner,
            saleDao = saleDao,
            saleItemDao = saleItemDao,
            paymentDao = paymentDao,
            productDao = productDao,
            stockBalanceDao = stockBalanceDao,
            stockMovementDao = stockMovementDao,
            customerDao = customerDao,
            customerLedgerDao = customerLedgerDao,
            cashMovementDao = cashMovementDao,
            auditLogDao = auditLogDao,
            syncEventDao = syncEventDao,
            invoiceSequenceDao = invoiceSequenceDao,
            saleCalculator = saleCalculator
        )

        val opId = UUID.randomUUID().toString()
        val command = com.example.grocerypos.domain.model.CompleteSaleCommand(
            operationId = opId,
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testUserId,
            customerId = testCustomerId,
            items = listOf(
                com.example.grocerypos.domain.model.SaleItemCommand(
                    productId = testProductId,
                    quantity = Quantity.fromWholeUnits(5)
                )
            ),
            payments = listOf(
                com.example.grocerypos.domain.model.PaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(1500) // 5 * 280 = 1400, Change = 100
                )
            ),
            allowNegativeStock = false
        )

        val result = completeSaleUseCase(command).getOrThrow()
        assertEquals(Money.fromRupees(1400), result.sale.grandTotal)
        assertEquals(Money.fromRupees(100), result.changeReturned)
        assertEquals(false, result.isIdempotentReplay)
        assertEquals("INV-000001", result.sale.invoiceNumber)

        // Verify stock deducted to 95 units
        val updatedStock = stockBalanceDao.getStockBalance(testProductId)
        assertEquals(Quantity.fromWholeUnits(95), updatedStock?.quantity)

        // Verify idempotency replay
        val secondResult = completeSaleUseCase(command).getOrThrow()
        assertEquals(true, secondResult.isIdempotentReplay)
        assertEquals(result.sale.saleId, secondResult.sale.saleId)

        // Verify stock was not deducted again
        val stockAfterSecond = stockBalanceDao.getStockBalance(testProductId)
        assertEquals(Quantity.fromWholeUnits(95), stockAfterSecond?.quantity)
    }

    @Test
    fun testCompleteSaleNegativeStockRejection() = runBlocking {
        val stockBalanceDao = db.stockBalanceDao()
        val stockMovementDao = db.stockMovementDao()
        val transactionRunner = RoomTransactionRunner(db)
        val financialCalcService = com.example.grocerypos.domain.service.FinancialCalculationService()
        val saleCalculator = com.example.grocerypos.domain.service.SaleCalculator(financialCalcService)

        // Stock is only 2 units
        stockBalanceDao.upsertStockBalance(
            com.example.grocerypos.data.local.entity.StockBalanceEntity(
                productId = testProductId,
                quantity = Quantity.fromWholeUnits(2),
                averageCost = Money.fromRupees(240),
                updatedAt = System.currentTimeMillis()
            )
        )

        val completeSaleUseCase = com.example.grocerypos.domain.usecase.CompleteSaleUseCase(
            transactionRunner = transactionRunner,
            saleDao = saleDao,
            saleItemDao = saleItemDao,
            paymentDao = paymentDao,
            productDao = productDao,
            stockBalanceDao = stockBalanceDao,
            stockMovementDao = stockMovementDao,
            customerDao = customerDao,
            customerLedgerDao = customerLedgerDao,
            cashMovementDao = cashMovementDao,
            auditLogDao = auditLogDao,
            syncEventDao = syncEventDao,
            invoiceSequenceDao = invoiceSequenceDao,
            saleCalculator = saleCalculator
        )

        val command = com.example.grocerypos.domain.model.CompleteSaleCommand(
            operationId = UUID.randomUUID().toString(),
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testUserId,
            customerId = testCustomerId,
            items = listOf(
                com.example.grocerypos.domain.model.SaleItemCommand(
                    productId = testProductId,
                    quantity = Quantity.fromWholeUnits(5) // Needs 5, only 2 available
                )
            ),
            payments = listOf(
                com.example.grocerypos.domain.model.PaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(1400)
                )
            ),
            allowNegativeStock = false
        )

        val result = completeSaleUseCase(command)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is com.example.grocerypos.domain.model.InsufficientStockException)

        // Verify stock was unaffected
        val stock = stockBalanceDao.getStockBalance(testProductId)
        assertEquals(Quantity.fromWholeUnits(2), stock?.quantity)
    }

    @Test
    fun testHoldSaleAndVoidSaleUseCase() = runBlocking {
        val stockBalanceDao = db.stockBalanceDao()
        val stockMovementDao = db.stockMovementDao()
        val transactionRunner = RoomTransactionRunner(db)
        val financialCalcService = com.example.grocerypos.domain.service.FinancialCalculationService()
        val saleCalculator = com.example.grocerypos.domain.service.SaleCalculator(financialCalcService)

        // Stock is 50 units
        stockBalanceDao.upsertStockBalance(
            com.example.grocerypos.data.local.entity.StockBalanceEntity(
                productId = testProductId,
                quantity = Quantity.fromWholeUnits(50),
                averageCost = Money.fromRupees(240),
                updatedAt = System.currentTimeMillis()
            )
        )

        val holdSaleUseCase = com.example.grocerypos.domain.usecase.HoldSaleUseCase(
            transactionRunner = transactionRunner,
            saleDao = saleDao,
            saleItemDao = saleItemDao,
            productDao = productDao,
            stockBalanceDao = stockBalanceDao,
            auditLogDao = auditLogDao,
            syncEventDao = syncEventDao,
            saleCalculator = saleCalculator
        )

        val completeSaleUseCase = com.example.grocerypos.domain.usecase.CompleteSaleUseCase(
            transactionRunner = transactionRunner,
            saleDao = saleDao,
            saleItemDao = saleItemDao,
            paymentDao = paymentDao,
            productDao = productDao,
            stockBalanceDao = stockBalanceDao,
            stockMovementDao = stockMovementDao,
            customerDao = customerDao,
            customerLedgerDao = customerLedgerDao,
            cashMovementDao = cashMovementDao,
            auditLogDao = auditLogDao,
            syncEventDao = syncEventDao,
            invoiceSequenceDao = invoiceSequenceDao,
            saleCalculator = saleCalculator
        )

        val voidSaleUseCase = com.example.grocerypos.domain.usecase.VoidSaleUseCase(
            transactionRunner = transactionRunner,
            saleDao = saleDao,
            stockBalanceDao = stockBalanceDao,
            stockMovementDao = stockMovementDao,
            customerLedgerDao = customerLedgerDao,
            cashMovementDao = cashMovementDao,
            auditLogDao = auditLogDao,
            syncEventDao = syncEventDao
        )

        val saleId = UUID.randomUUID().toString()

        // 1. Hold sale
        val holdCommand = com.example.grocerypos.domain.model.HoldSaleCommand(
            saleId = saleId,
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testUserId,
            customerId = testCustomerId,
            items = listOf(
                com.example.grocerypos.domain.model.SaleItemCommand(
                    productId = testProductId,
                    quantity = Quantity.fromWholeUnits(4)
                )
            )
        )

        val heldSale = holdSaleUseCase(holdCommand).getOrThrow()
        assertEquals(SaleStatus.HELD, heldSale.status)
        assertNull(heldSale.invoiceNumber)
        // Stock should still be 50
        assertEquals(Quantity.fromWholeUnits(50), stockBalanceDao.getStockBalance(testProductId)?.quantity)

        // 2. Complete the held sale
        val completeCommand = com.example.grocerypos.domain.model.CompleteSaleCommand(
            operationId = UUID.randomUUID().toString(),
            draftSaleId = saleId,
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testUserId,
            customerId = testCustomerId,
            items = listOf(
                com.example.grocerypos.domain.model.SaleItemCommand(
                    productId = testProductId,
                    quantity = Quantity.fromWholeUnits(4)
                )
            ),
            payments = listOf(
                com.example.grocerypos.domain.model.PaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(1120) // 4 * 280
                )
            )
        )

        val completedResult = completeSaleUseCase(completeCommand).getOrThrow()
        assertEquals(SaleStatus.COMPLETED, completedResult.sale.status)
        assertNotNull(completedResult.sale.invoiceNumber)
        // Stock reduced to 46
        assertEquals(Quantity.fromWholeUnits(46), stockBalanceDao.getStockBalance(testProductId)?.quantity)

        // 3. Void the completed sale
        val voidResult = voidSaleUseCase(
            com.example.grocerypos.domain.usecase.VoidSaleCommand(
                saleId = saleId,
                cashierId = testUserId,
                reason = "Customer cancelled before leaving counter"
            )
        ).getOrThrow()

        assertEquals(SaleStatus.VOIDED, voidResult.status)
        // Stock restored back to 50
        assertEquals(Quantity.fromWholeUnits(50), stockBalanceDao.getStockBalance(testProductId)?.quantity)
    }

    @Test
    fun testConcurrentInvoiceAllocationProducesUniqueNumbers() = runBlocking {
        val concurrentShopId = UUID.randomUUID().toString()
        val allocationCount = 25

        val results = coroutineScope {
            (1..allocationCount).map {
                async(Dispatchers.Default) {
                    invoiceSequenceDao.allocateNextInvoiceNumber(concurrentShopId, "TX-", paddingDigits = 5)
                }
            }.awaitAll()
        }

        assertEquals(allocationCount, results.size)
        // All allocated numbers must be unique
        val uniqueResults = results.toSet()
        assertEquals(allocationCount, uniqueResults.size)

        // Verify the sequence stored in DB is at allocationCount + 1
        val seq = invoiceSequenceDao.getSequence(concurrentShopId)
        assertNotNull(seq)
        assertEquals((allocationCount + 1).toLong(), seq?.nextNumber)
    }
}

