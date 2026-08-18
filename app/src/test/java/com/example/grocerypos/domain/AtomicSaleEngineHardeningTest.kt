package com.example.grocerypos.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.grocerypos.data.local.dao.AuditLogDao
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
import com.example.grocerypos.data.local.dao.SaleOperationDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.StockMovementDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.dao.UserDao
import com.example.grocerypos.data.local.database.GroceryPosDatabase
import com.example.grocerypos.data.local.database.RoomTransactionRunner
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.CustomerEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.InvoiceSequenceEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.RoleEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.data.local.entity.UserEntity
import com.example.grocerypos.domain.model.CompleteSaleCommand
import com.example.grocerypos.domain.model.CompleteSaleItemCommand
import com.example.grocerypos.domain.model.CompleteSalePaymentCommand
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.InsufficientStockException
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.UnitCode
import com.example.grocerypos.domain.service.FinancialCalculationService
import com.example.grocerypos.domain.service.SaleCalculator
import com.example.grocerypos.domain.usecase.CompleteSaleUseCase
import com.example.grocerypos.domain.usecase.SaleExecutionStage
import com.example.grocerypos.domain.usecase.SaleFailureHook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

/**
 * Hardening test suite for BUILD 02B.1:
 * 1. Durable idempotency via sale_operations table with unique constraint.
 * 2. Concurrency-safe conditional stock deductions preventing negative inventory.
 * 3. Multi-stage failure injection rollback verification across all pipeline steps.
 * 4. Invoice sequence rollback & retry verification.
 */
@RunWith(AndroidJUnit4::class)
class AtomicSaleEngineHardeningTest {

    private lateinit var db: GroceryPosDatabase
    private lateinit var transactionRunner: RoomTransactionRunner
    private lateinit var shopDao: ShopDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var roleDao: RoleDao
    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var unitDao: UnitDao
    private lateinit var productDao: ProductDao
    private lateinit var stockBalanceDao: StockBalanceDao
    private lateinit var stockMovementDao: StockMovementDao
    private lateinit var customerDao: CustomerDao
    private lateinit var customerLedgerDao: CustomerLedgerDao
    private lateinit var cashMovementDao: CashMovementDao
    private lateinit var saleDao: SaleDao
    private lateinit var saleItemDao: SaleItemDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var auditLogDao: AuditLogDao
    private lateinit var syncEventDao: SyncEventDao
    private lateinit var invoiceSequenceDao: InvoiceSequenceDao
    private lateinit var saleOperationDao: SaleOperationDao
    private lateinit var saleCalculator: SaleCalculator

    private val testShopId = "shop_test_1"
    private val testDeviceId = "dev_test_1"
    private val testCashierId = "user_test_1"
    private val testCustomerId = "cust_test_1"
    private val testProductId1 = "prod_rice_1"
    private val testProductId2 = "prod_oil_1"

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GroceryPosDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        transactionRunner = RoomTransactionRunner(db)
        shopDao = db.shopDao()
        deviceDao = db.deviceDao()
        roleDao = db.roleDao()
        userDao = db.userDao()
        categoryDao = db.categoryDao()
        unitDao = db.unitDao()
        productDao = db.productDao()
        stockBalanceDao = db.stockBalanceDao()
        stockMovementDao = db.stockMovementDao()
        customerDao = db.customerDao()
        customerLedgerDao = db.customerLedgerDao()
        cashMovementDao = db.cashMovementDao()
        saleDao = db.saleDao()
        saleItemDao = db.saleItemDao()
        paymentDao = db.paymentDao()
        auditLogDao = db.auditLogDao()
        syncEventDao = db.syncEventDao()
        invoiceSequenceDao = db.invoiceSequenceDao()
        saleOperationDao = db.saleOperationDao()

        saleCalculator = SaleCalculator(FinancialCalculationService())

        // Seed basic fixtures
        val now = System.currentTimeMillis()
        shopDao.insertShop(
            ShopEntity(
                shopId = testShopId,
                name = "Al-Madina Grocers",
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
                deviceName = "Counter 1 Main",
                deviceType = DeviceType.TABLET,
                isPrimary = true,
                status = DeviceStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        )

        roleDao.insertRole(
            RoleEntity(
                roleId = "role_cashier",
                name = RoleName.CASHIER,
                description = "Cashier",
                permissionsJson = "[\"perform_sale\"]",
                createdAt = now,
                updatedAt = now
            )
        )

        userDao.insertUser(
            UserEntity(
                userId = testCashierId,
                shopId = testShopId,
                roleId = "role_cashier",
                username = "cashier1",
                passwordHash = "hash123",
                pinHash = "pin123",
                fullName = "Ahmed Ali",
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        categoryDao.insertCategory(
            CategoryEntity(
                categoryId = "cat_staples",
                shopId = testShopId,
                name = "Staples & Grains",
                description = "Basic Food Items",
                createdAt = now,
                updatedAt = now
            )
        )

        unitDao.insertUnit(
            UnitEntity(
                unitId = "unit_kg",
                code = UnitCode.KILOGRAM,
                name = "Kilogram",
                symbol = "kg",
                isCustom = false,
                createdAt = now,
                updatedAt = now
            )
        )

        unitDao.insertUnit(
            UnitEntity(
                unitId = "unit_piece",
                code = UnitCode.PIECE,
                name = "Piece",
                symbol = "pc",
                isCustom = false,
                createdAt = now,
                updatedAt = now
            )
        )

        productDao.insertProduct(
            ProductEntity(
                productId = testProductId1,
                shopId = testShopId,
                name = "Super Kernel Basmati Rice",
                categoryId = "cat_staples",
                brand = "Guard",
                sku = "RICE-BASMATI-1KG",
                baseUnitId = "unit_kg",
                sellingUnitId = "unit_kg",
                conversionFactor = Quantity.WHOLE_UNIT,
                sellingPrice = Money.fromRupees(350), // Rs. 350.00 / kg
                minimumStock = Quantity.fromWholeUnits(10),
                trackExpiry = false,
                trackBatch = false,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        productDao.insertProduct(
            ProductEntity(
                productId = testProductId2,
                shopId = testShopId,
                name = "Dalda Cooking Oil 1L",
                categoryId = "cat_staples",
                brand = "Dalda",
                sku = "OIL-DALDA-1L",
                baseUnitId = "unit_piece",
                sellingUnitId = "unit_piece",
                conversionFactor = Quantity.WHOLE_UNIT,
                sellingPrice = Money.fromRupees(520), // Rs. 520.00 / piece
                minimumStock = Quantity.fromWholeUnits(5),
                trackExpiry = false,
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
                phone = "03219876543",
                address = "Model Town, Lahore",
                creditLimit = Money.fromRupees(50000),
                notes = "Trusted Regular Customer",
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )

        // Seed initial stock
        stockBalanceDao.upsertStockBalance(
            StockBalanceEntity(
                productId = testProductId1,
                quantity = Quantity.fromWholeUnits(50), // 50 kg in stock
                averageCost = Money.fromRupees(280),
                updatedAt = now
            )
        )

        stockBalanceDao.upsertStockBalance(
            StockBalanceEntity(
                productId = testProductId2,
                quantity = Quantity.fromWholeUnits(20), // 20 pieces in stock
                averageCost = Money.fromRupees(460),
                updatedAt = now
            )
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun createUseCase(failureHook: SaleFailureHook = SaleFailureHook { }): CompleteSaleUseCase {
        return CompleteSaleUseCase(
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
            saleOperationDao = saleOperationDao,
            saleCalculator = saleCalculator,
            failureHook = failureHook
        )
    }

    // =========================================================================
    // 1. DURABLE IDEMPOTENCY TESTS
    // =========================================================================

    @Test
    fun testDurableIdempotencyReturnsReplayWithoutDuplicateSideEffects() = runBlocking {
        val useCase = createUseCase()
        val opId = "op_idempotent_001"

        val command = CompleteSaleCommand(
            operationId = opId,
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testCashierId,
            customerId = testCustomerId,
            items = listOf(
                CompleteSaleItemCommand(
                    productId = testProductId1,
                    quantity = Quantity.fromWholeUnits(5), // 5 kg
                    unitPrice = Money.fromRupees(350)
                )
            ),
            payments = listOf(
                CompleteSalePaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(1750)
                )
            )
        )

        // First execution
        val result1 = useCase(command).getOrThrow()
        assertFalse(result1.isIdempotentReplay)
        assertEquals(SaleStatus.COMPLETED, result1.sale.status)
        assertEquals(PaymentStatus.PAID, result1.sale.paymentStatus)
        assertEquals(Money.fromRupees(1750), result1.sale.grandTotal)

        val stockAfterFirst = stockBalanceDao.getStockBalance(testProductId1)!!.quantity
        assertEquals(Quantity.fromWholeUnits(45), stockAfterFirst)

        // Verify sale_operations record exists in DB
        val opRecord = saleOperationDao.getOperation(opId)
        assertNotNull(opRecord)
        assertEquals(opId, opRecord!!.operationId)
        assertEquals(result1.sale.saleId, opRecord.saleId)

        // Second execution (Idempotent replay)
        val result2 = useCase(command).getOrThrow()
        assertTrue(result2.isIdempotentReplay)
        assertEquals(result1.sale.saleId, result2.sale.saleId)
        assertEquals(result1.sale.invoiceNumber, result2.sale.invoiceNumber)
        assertEquals(result1.sale.grandTotal, result2.sale.grandTotal)

        // Verify stock was NOT deducted again
        val stockAfterSecond = stockBalanceDao.getStockBalance(testProductId1)!!.quantity
        assertEquals(Quantity.fromWholeUnits(45), stockAfterSecond)

        // Verify only 1 sale exists in database
        val allSales = saleDao.getSaleWithDetails(result1.sale.saleId)
        assertNotNull(allSales)
        assertEquals(1, allSales!!.items.size)
        assertEquals(1, allSales.payments.size)
    }

    @Test
    fun testConcurrentIdempotentRequestsCreateSingleSale() = runBlocking {
        val useCase = createUseCase()
        val opId = "op_concurrent_idempotent_100"

        val command = CompleteSaleCommand(
            operationId = opId,
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testCashierId,
            customerId = testCustomerId,
            items = listOf(
                CompleteSaleItemCommand(
                    productId = testProductId2,
                    quantity = Quantity.fromWholeUnits(2), // 2 units
                    unitPrice = Money.fromRupees(520)
                )
            ),
            payments = listOf(
                CompleteSalePaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(1040)
                )
            )
        )

        // Run 5 concurrent invocations with the same operationId
        val results = coroutineScope {
            (1..5).map {
                async(Dispatchers.IO) {
                    useCase(command)
                }
            }.awaitAll()
        }

        // All should succeed
        assertTrue(results.all { it.isSuccess })
        val completedResults = results.map { it.getOrThrow() }

        // Exactly one should be the primary execution, remainder are idempotent replays
        val primaryExecutions = completedResults.count { !it.isIdempotentReplay }
        val replayExecutions = completedResults.count { it.isIdempotentReplay }
        assertEquals(1, primaryExecutions)
        assertEquals(4, replayExecutions)

        // All results point to the same sale ID and invoice number
        val firstSaleId = completedResults.first().sale.saleId
        val firstInvoice = completedResults.first().sale.invoiceNumber
        assertTrue(completedResults.all { it.sale.saleId == firstSaleId })
        assertTrue(completedResults.all { it.sale.invoiceNumber == firstInvoice })

        // Stock must have been deducted exactly once (20 - 2 = 18)
        val finalStock = stockBalanceDao.getStockBalance(testProductId2)!!.quantity
        assertEquals(Quantity.fromWholeUnits(18), finalStock)
    }

    // =========================================================================
    // 2. CONCURRENCY-SAFE CONDITIONAL STOCK DEDUCTION TESTS
    // =========================================================================

    @Test
    fun testConditionalStockDecrementPreventsNegativeStock() = runBlocking {
        // Product 2 has 20 units in stock.
        // We attempt a sale of 25 units when allowNegativeStock = false.
        val useCase = createUseCase()

        val command = CompleteSaleCommand(
            operationId = "op_insufficient_stock_01",
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testCashierId,
            customerId = testCustomerId,
            allowNegativeStock = false,
            items = listOf(
                CompleteSaleItemCommand(
                    productId = testProductId2,
                    quantity = Quantity.fromWholeUnits(25), // 25 > 20
                    unitPrice = Money.fromRupees(520)
                )
            ),
            payments = listOf(
                CompleteSalePaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(13000)
                )
            )
        )

        val result = useCase(command)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InsufficientStockException)

        // Verify stock is untouched at 20 units
        val stock = stockBalanceDao.getStockBalance(testProductId2)!!.quantity
        assertEquals(Quantity.fromWholeUnits(20), stock)

        // Verify no sale or operation record exists
        assertNull(saleOperationDao.getOperation("op_insufficient_stock_01"))
    }

    @Test
    fun testConditionalStockDecrementAllowsNegativeStockWhenFlagEnabled() = runBlocking {
        val useCase = createUseCase()

        val command = CompleteSaleCommand(
            operationId = "op_allow_negative_stock_01",
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testCashierId,
            customerId = testCustomerId,
            allowNegativeStock = true,
            items = listOf(
                CompleteSaleItemCommand(
                    productId = testProductId2,
                    quantity = Quantity.fromWholeUnits(25), // 25 > 20
                    unitPrice = Money.fromRupees(520)
                )
            ),
            payments = listOf(
                CompleteSalePaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(13000)
                )
            )
        )

        val result = useCase(command).getOrThrow()
        assertEquals(SaleStatus.COMPLETED, result.sale.status)

        // Stock should be 20 - 25 = -5
        val stock = stockBalanceDao.getStockBalance(testProductId2)!!.quantity
        assertEquals(Quantity.fromWholeUnits(-5), stock)
    }

    @Test
    fun testConcurrentStockRacesDoNotOverdrawStock() = runBlocking {
        // Product 2 has 20 units in stock.
        // We launch 4 concurrent sales of 10 units each with allowNegativeStock = false.
        // Exactly 2 sales should succeed (consuming 20 units), and 2 should fail with InsufficientStockException.
        val useCase = createUseCase()

        val commands = (1..4).map { i ->
            CompleteSaleCommand(
                operationId = "op_stock_race_$i",
                shopId = testShopId,
                deviceId = testDeviceId,
                cashierId = testCashierId,
                customerId = testCustomerId,
                allowNegativeStock = false,
                items = listOf(
                    CompleteSaleItemCommand(
                        productId = testProductId2,
                        quantity = Quantity.fromWholeUnits(10), // 10 units each
                        unitPrice = Money.fromRupees(520)
                    )
                ),
                payments = listOf(
                    CompleteSalePaymentCommand(
                        method = PaymentMethod.CASH,
                        amount = Money.fromRupees(5200)
                    )
                )
            )
        }

        val results = coroutineScope {
            commands.map { cmd ->
                async(Dispatchers.IO) {
                    useCase(cmd)
                }
            }.awaitAll()
        }

        val successes = results.count { it.isSuccess }
        val failures = results.count { it.isFailure }

        assertEquals(2, successes)
        assertEquals(2, failures)

        // Remaining stock must be exactly 0
        val finalStock = stockBalanceDao.getStockBalance(testProductId2)!!.quantity
        assertEquals(Quantity.ZERO, finalStock)
    }

    // =========================================================================
    // 3. FAILURE-INJECTION ROLLBACK VERIFICATION TESTS
    // =========================================================================

    private fun testFailureInjectionAtStage(stage: SaleExecutionStage) = runBlocking {
        val opId = "op_failure_${stage.name}"
        val initialStock1 = stockBalanceDao.getStockBalance(testProductId1)!!.quantity
        val initialStock2 = stockBalanceDao.getStockBalance(testProductId2)!!.quantity

        val useCaseWithFailure = createUseCase { currentStage ->
            if (currentStage == stage) {
                throw RuntimeException("Simulated injected failure at stage: $stage")
            }
        }

        val command = CompleteSaleCommand(
            operationId = opId,
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testCashierId,
            customerId = testCustomerId,
            items = listOf(
                CompleteSaleItemCommand(
                    productId = testProductId1,
                    quantity = Quantity.fromWholeUnits(4), // 4 kg (Rs. 1400)
                    unitPrice = Money.fromRupees(350)
                ),
                CompleteSaleItemCommand(
                    productId = testProductId2,
                    quantity = Quantity.fromWholeUnits(1), // 1 unit (Rs. 520)
                    unitPrice = Money.fromRupees(520)
                )
            ),
            payments = listOf(
                CompleteSalePaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(1500)
                )
            ) // Due = 1920 - 1500 = 420.00
        )

        val result = useCaseWithFailure(command)
        assertTrue("Stage $stage should have failed", result.isFailure)

        // Verify full database rollback
        assertNull("Sale record must be rolled back", saleDao.getSaleById(opId))
        assertTrue("Sale items must be empty", saleItemDao.getItemsForSale(opId).isEmpty())
        assertTrue("Payments must be empty", paymentDao.getPaymentsForSale(opId).isEmpty())
        assertNull("Sale operation must be rolled back", saleOperationDao.getOperation(opId))

        // Stock balances must remain identical to before
        val stock1After = stockBalanceDao.getStockBalance(testProductId1)!!.quantity
        val stock2After = stockBalanceDao.getStockBalance(testProductId2)!!.quantity
        assertEquals("Stock 1 must not be modified", initialStock1, stock1After)
        assertEquals("Stock 2 must not be modified", initialStock2, stock2After)
    }

    @Test
    fun testRollback_FailureDuringStockDeduction() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_STOCK_DEDUCTION)
    }

    @Test
    fun testRollback_FailureAfterStockDeduction() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_STOCK_DEDUCTION)
    }

    @Test
    fun testRollback_FailureDuringStockMovementInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_STOCK_MOVEMENT)
    }

    @Test
    fun testRollback_FailureDuringInvoiceAllocation() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_INVOICE_ALLOCATION)
    }

    @Test
    fun testRollback_FailureAfterInvoiceAllocation() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_INVOICE_ALLOCATION)
    }

    @Test
    fun testRollback_FailureDuringSaleInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_SALE_INSERTION)
    }

    @Test
    fun testRollback_FailureAfterSaleInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_SALE_INSERTION)
    }

    @Test
    fun testRollback_FailureDuringSaleItemsInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_SALE_ITEMS_INSERTION)
    }

    @Test
    fun testRollback_FailureAfterSaleItemsInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_SALE_ITEMS_INSERTION)
    }

    @Test
    fun testRollback_FailureDuringPaymentsInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_PAYMENTS_INSERTION)
    }

    @Test
    fun testRollback_FailureAfterPaymentsInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_PAYMENTS_INSERTION)
    }

    @Test
    fun testRollback_FailureDuringCustomerLedgerInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_LEDGER_INSERTION)
    }

    @Test
    fun testRollback_FailureAfterCustomerLedgerInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_LEDGER_INSERTION)
    }

    @Test
    fun testRollback_FailureDuringCashMovementInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_CASH_MOVEMENT_INSERTION)
    }

    @Test
    fun testRollback_FailureAfterCashMovementInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_CASH_MOVEMENT_INSERTION)
    }

    @Test
    fun testRollback_FailureDuringAuditLogInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_AUDIT_LOG_INSERTION)
    }

    @Test
    fun testRollback_FailureAfterAuditLogInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_AUDIT_LOG_INSERTION)
    }

    @Test
    fun testRollback_FailureDuringSyncEventInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_SYNC_EVENT_INSERTION)
    }

    @Test
    fun testRollback_FailureAfterSyncEventInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.AFTER_SYNC_EVENT_INSERTION)
    }

    @Test
    fun testRollback_FailureDuringOperationRecordInsertion() {
        testFailureInjectionAtStage(SaleExecutionStage.BEFORE_OPERATION_RECORD_INSERTION)
    }

    // =========================================================================
    // 4. INVOICE SEQUENCE ROLLBACK AND RETRY GUARANTEE TESTS
    // =========================================================================

    @Test
    fun testInvoiceSequenceRollbackAndSuccessfulRetry() = runBlocking {
        // Pre-seed invoice sequence with next_number = 1005
        val now = System.currentTimeMillis()
        invoiceSequenceDao.upsertSequence(
            InvoiceSequenceEntity(
                shopId = testShopId,
                nextNumber = 1005L,
                prefix = "INV-",
                updatedAt = now
            )
        )

        // Attempt 1: Sale injected to fail AFTER invoice allocation (e.g. at payments insertion)
        val opId = "op_invoice_retry_test"
        val failingUseCase = createUseCase { stage ->
            if (stage == SaleExecutionStage.BEFORE_PAYMENTS_INSERTION) {
                throw RuntimeException("Network / hardware crash during payment step")
            }
        }

        val command = CompleteSaleCommand(
            operationId = opId,
            shopId = testShopId,
            deviceId = testDeviceId,
            cashierId = testCashierId,
            customerId = testCustomerId,
            items = listOf(
                CompleteSaleItemCommand(
                    productId = testProductId1,
                    quantity = Quantity.fromWholeUnits(2), // 2 kg
                    unitPrice = Money.fromRupees(350)
                )
            ),
            payments = listOf(
                CompleteSalePaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(700)
                )
            )
        )

        val failedResult = failingUseCase(command)
        assertTrue(failedResult.isFailure)

        // Verify sequence rolled back to 1005 (NOT 1006)
        val sequenceAfterFailure = invoiceSequenceDao.getSequence(testShopId)
        assertNotNull(sequenceAfterFailure)
        assertEquals(1005L, sequenceAfterFailure!!.nextNumber)

        // Attempt 2: Clean retry with same command
        val normalUseCase = createUseCase()
        val successResult = normalUseCase(command).getOrThrow()

        // Verify allocated invoice number is exactly INV-001005!
        assertEquals("INV-001005", successResult.sale.invoiceNumber)

        // Next sequence in DB is now incremented to 1006
        val sequenceAfterSuccess = invoiceSequenceDao.getSequence(testShopId)
        assertEquals(1006L, sequenceAfterSuccess!!.nextNumber)
    }
}
