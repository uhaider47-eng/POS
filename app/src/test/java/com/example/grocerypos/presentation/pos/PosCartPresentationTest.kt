package com.example.grocerypos.presentation.pos

import androidx.lifecycle.SavedStateHandle
import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleItem
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.model.Unit
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.CustomerRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import com.example.grocerypos.domain.service.FinancialCalculationService
import com.example.grocerypos.domain.usecase.CompleteSaleUseCase
import com.example.grocerypos.domain.usecase.CreateCustomerUseCase
import com.example.grocerypos.domain.usecase.DiscardHeldSaleUseCase
import com.example.grocerypos.domain.usecase.GetActiveCustomersUseCase
import com.example.grocerypos.domain.usecase.GetCustomerBalanceUseCase
import com.example.grocerypos.domain.usecase.GetHeldSalesUseCase
import com.example.grocerypos.domain.usecase.HoldSaleUseCase
import com.example.grocerypos.domain.usecase.ResumeHeldSaleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PosCartPresentationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val shopRepository: ShopRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val categoryRepository: CategoryRepository = mock()
    private val unitRepository: UnitRepository = mock()
    private val customerRepository: CustomerRepository = mock()
    private val completeSaleUseCase: CompleteSaleUseCase = mock()
    private val holdSaleUseCase: HoldSaleUseCase = mock()
    private val getHeldSalesUseCase: GetHeldSalesUseCase = mock()
    private val resumeHeldSaleUseCase: ResumeHeldSaleUseCase = mock()
    private val discardHeldSaleUseCase: DiscardHeldSaleUseCase = mock()
    private val getActiveCustomersUseCase: GetActiveCustomersUseCase = mock()
    private val getCustomerBalanceUseCase: GetCustomerBalanceUseCase = mock()
    private val createCustomerUseCase: CreateCustomerUseCase = mock()
    private val financialCalculationService = FinancialCalculationService()

    private val sampleShop = Shop(
        shopId = "shop-test-1",
        name = "Al-Madina Super Store",
        ownerName = "Muhammad Aslam",
        phone = "0300-1234567",
        address = "Main Market"
    )

    private val sampleUnit = Unit(
        unitId = "unit-pcs",
        code = "PIECE",
        name = "Piece",
        symbol = "pcs",
        isCustom = false
    )

    private val milkProduct = Product(
        productId = "prod-milk-1",
        shopId = "shop-test-1",
        name = "Milk 1L",
        categoryId = "cat-dairy",
        brand = "Olpers",
        sku = "SKU-MILK-1",
        baseUnitId = "unit-pcs",
        sellingUnitId = "unit-pcs",
        conversionFactor = Quantity.ONE,
        sellingPrice = Money.fromRupees(220),
        minimumStock = Quantity.fromWholeUnits(5),
        isActive = true
    )

    private val breadProduct = Product(
        productId = "prod-bread-1",
        shopId = "shop-test-1",
        name = "Bread",
        categoryId = "cat-bakery",
        brand = "Dawn",
        sku = "SKU-BREAD-1",
        baseUnitId = "unit-pcs",
        sellingUnitId = "unit-pcs",
        conversionFactor = Quantity.ONE,
        sellingPrice = Money.fromRupees(120),
        minimumStock = Quantity.fromWholeUnits(2),
        isActive = true
    )

    private val sugarProduct = Product(
        productId = "prod-sugar-1",
        shopId = "shop-test-1",
        name = "Sugar 1kg",
        categoryId = "cat-grocery",
        brand = "Local",
        sku = "SKU-SUGAR-1",
        baseUnitId = "unit-pcs",
        sellingUnitId = "unit-pcs",
        conversionFactor = Quantity.ONE,
        sellingPrice = Money.fromRupees(180),
        minimumStock = Quantity.fromWholeUnits(10),
        isActive = true
    )

    private val sampleCustomer = Customer(
        customerId = "cust-1",
        shopId = "shop-test-1",
        name = "Tariq Mahmood",
        phone = "0300-9876543",
        creditLimit = Money.fromRupees(10000),
        isActive = true
    )

    private lateinit var viewModel: PosViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(shopRepository.getShopFlow()).thenReturn(flowOf(sampleShop))
        whenever(categoryRepository.getCategoriesFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(unitRepository.getAllUnitsFlow()).thenReturn(flowOf(listOf(sampleUnit)))
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        whenever(productRepository.getProductsFlow(any())).thenReturn(flowOf(listOf(milkProduct, breadProduct, sugarProduct)))
        whenever(getHeldSalesUseCase(any())).thenReturn(flowOf(emptyList()))
        whenever(getActiveCustomersUseCase(any())).thenReturn(flowOf(listOf(sampleCustomer)))

        viewModel = PosViewModel(
            shopRepository = shopRepository,
            productRepository = productRepository,
            categoryRepository = categoryRepository,
            unitRepository = unitRepository,
            customerRepository = customerRepository,
            financialCalculationService = financialCalculationService,
            completeSaleUseCase = completeSaleUseCase,
            holdSaleUseCase = holdSaleUseCase,
            getHeldSalesUseCase = getHeldSalesUseCase,
            resumeHeldSaleUseCase = resumeHeldSaleUseCase,
            discardHeldSaleUseCase = discardHeldSaleUseCase,
            getActiveCustomersUseCase = getActiveCustomersUseCase,
            getCustomerBalanceUseCase = getCustomerBalanceUseCase,
            createCustomerUseCase = createCustomerUseCase,
            savedStateHandle = SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEmptyCartInitialState() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(0, state.cartItems.size)
        assertEquals(Money.ZERO, state.subtotal)
        assertEquals(Money.ZERO, state.grandTotal)
        assertEquals(0, state.totalItemCount)
        assertEquals(Quantity.ZERO, state.totalUnitsCount)
        assertNull(state.selectedCustomer)
    }

    @Test
    fun testAddProductIncrementsQuantityAndCalculatesLineTotal() = runTest {
        advanceUntilIdle()

        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        assertEquals(Quantity.ONE, state.cartItems[0].quantity)
        assertEquals(Money.fromRupees(220), state.cartItems[0].lineTotal)
        assertEquals(Money.fromRupees(220), state.subtotal)
        assertEquals(Money.fromRupees(220), state.grandTotal)

        // Add same product again -> quantity should become 2
        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        assertEquals(Quantity.fromWholeUnits(2), state.cartItems[0].quantity)
        assertEquals(Money.fromRupees(440), state.cartItems[0].lineTotal)
        assertEquals(Money.fromRupees(440), state.subtotal)
        assertEquals(Money.fromRupees(440), state.grandTotal)
    }

    @Test
    fun testAddMultipleProductsAndTotals() = runTest {
        advanceUntilIdle()

        viewModel.addProductToCart(milkProduct) // 220
        viewModel.addProductToCart(breadProduct) // 120
        viewModel.addProductToCart(sugarProduct) // 180
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.cartItems.size)
        assertEquals(Money.fromRupees(520), state.subtotal)
        assertEquals(Money.fromRupees(520), state.grandTotal)
        assertEquals(3, state.totalItemCount)
        assertEquals(Quantity.fromWholeUnits(3), state.totalUnitsCount)
    }

    @Test
    fun testCustomerSelectionAndCreditBalance() = runTest {
        advanceUntilIdle()

        whenever(getCustomerBalanceUseCase("cust-1")).thenReturn(Money.fromRupees(1500))

        viewModel.selectCustomer(sampleCustomer)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Tariq Mahmood", state.selectedCustomer?.name)
        assertEquals(Money.fromRupees(1500), state.selectedCustomerBalance)

        // Clear customer to walk-in
        viewModel.clearCustomer()
        advanceUntilIdle()

        val stateAfterClear = viewModel.uiState.value
        assertNull(stateAfterClear.selectedCustomer)
        assertEquals(Money.ZERO, stateAfterClear.selectedCustomerBalance)
    }

    @Test
    fun testHoldSaleFlow() = runTest {
        advanceUntilIdle()

        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        val mockSale = Sale(
            saleId = "sale-123",
            shopId = "shop-test-1",
            deviceId = "POS_MAIN",
            cashierId = "CASHIER",
            status = SaleStatus.HELD,
            subtotal = Money.fromRupees(220),
            discountAmount = Money.ZERO,
            taxAmount = Money.ZERO,
            grandTotal = Money.fromRupees(220),
            paidAmount = Money.ZERO,
            dueAmount = Money.fromRupees(220),
            createdAt = System.currentTimeMillis()
        )
        whenever(holdSaleUseCase(any())).thenReturn(Result.success(mockSale))

        viewModel.onHoldClicked()
        assertTrue(viewModel.uiState.value.showHoldDialog)

        viewModel.confirmHoldSale()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showHoldDialog)
        assertTrue(state.cartItems.isEmpty())
        assertEquals(Money.ZERO, state.grandTotal)
    }

    @Test
    fun testCompleteSaleFlow() = runTest {
        advanceUntilIdle()

        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        val mockCompletedSale = Sale(
            saleId = "sale-comp-1",
            shopId = "shop-test-1",
            deviceId = "POS_MAIN",
            cashierId = "CASHIER",
            status = SaleStatus.COMPLETED,
            subtotal = Money.fromRupees(220),
            discountAmount = Money.ZERO,
            taxAmount = Money.ZERO,
            grandTotal = Money.fromRupees(220),
            paidAmount = Money.fromRupees(220),
            dueAmount = Money.ZERO,
            invoiceNumber = "INV-000001",
            completedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        whenever(completeSaleUseCase(any())).thenReturn(Result.success(mockCompletedSale))

        val payments = listOf(
            PaymentLineUi(
                id = "pay-1",
                method = PaymentMethod.CASH,
                amount = Money.fromRupees(220)
            )
        )

        viewModel.completeSale(payments, Money.fromRupees(500))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.saleSuccessResult)
        assertEquals("INV-000001", state.saleSuccessResult?.invoiceNumber)
        assertEquals(Money.fromRupees(280), state.saleSuccessResult?.changeReturned)
        assertTrue(state.cartItems.isEmpty())
    }
}
