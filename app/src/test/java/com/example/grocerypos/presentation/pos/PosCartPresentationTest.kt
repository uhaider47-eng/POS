package com.example.grocerypos.presentation.pos

import androidx.lifecycle.SavedStateHandle
import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.model.Unit
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import com.example.grocerypos.domain.service.FinancialCalculationService
import com.example.grocerypos.domain.usecase.HoldSaleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val holdSaleUseCase: HoldSaleUseCase = mock()
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

    private lateinit var viewModel: PosViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(shopRepository.getShopFlow()).thenReturn(flowOf(sampleShop))
        whenever(categoryRepository.getCategoriesFlow(any())).thenReturn(flowOf(emptyList()))
        whenever(unitRepository.getAllUnitsFlow()).thenReturn(flowOf(listOf(sampleUnit)))
        whenever(productRepository.getProductsFlow(any())).thenReturn(flowOf(listOf(milkProduct, breadProduct, sugarProduct)))

        viewModel = PosViewModel(
            shopRepository = shopRepository,
            productRepository = productRepository,
            categoryRepository = categoryRepository,
            unitRepository = unitRepository,
            financialCalculationService = financialCalculationService,
            holdSaleUseCase = holdSaleUseCase,
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
    }

    @Test
    fun testAddSingleProductToCart() = runTest {
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        val item = state.cartItems[0]
        assertEquals("Milk 1L", item.product.name)
        assertEquals(Quantity.ONE, item.quantity)
        assertEquals(Money.fromRupees(220), item.lineTotal)
        assertEquals(Money.fromRupees(220), state.subtotal)
        assertEquals(Money.fromRupees(220), state.grandTotal)
    }

    @Test
    fun testAddSameProductTwiceIncreasesQuantity() = runTest {
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        val item = state.cartItems[0]
        assertEquals(Quantity.fromWholeUnits(2), item.quantity)
        assertEquals(Money.fromRupees(440), item.lineTotal)
        assertEquals(Money.fromRupees(440), state.grandTotal)
    }

    @Test
    fun testMultipleProductsCartSubtotal() = runTest {
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        // Milk 1L x 1 = Rs.220
        viewModel.addProductToCart(milkProduct)
        // Bread x 2 = Rs.240
        viewModel.addProductToCart(breadProduct)
        viewModel.addProductToCart(breadProduct)
        // Sugar x 1 = Rs.180
        viewModel.addProductToCart(sugarProduct)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.cartItems.size)
        // 220 + 240 + 180 = 640
        assertEquals(Money.fromRupees(640), state.subtotal)
        assertEquals(Money.fromRupees(640), state.grandTotal)
        assertEquals(3, state.totalItemCount)
        assertEquals(Quantity.fromWholeUnits(4), state.totalUnitsCount)
    }

    @Test
    fun testIncreaseAndDecreaseQuantity() = runTest {
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        viewModel.addProductToCart(milkProduct)
        advanceUntilIdle()

        val itemId = viewModel.uiState.value.cartItems[0].cartItemId

        // Increase quantity
        viewModel.increaseQuantity(itemId)
        advanceUntilIdle()
        assertEquals(Quantity.fromWholeUnits(2), viewModel.uiState.value.cartItems[0].quantity)
        assertEquals(Money.fromRupees(440), viewModel.uiState.value.grandTotal)

        // Decrease quantity back to 1
        viewModel.decreaseQuantity(itemId)
        advanceUntilIdle()
        assertEquals(Quantity.ONE, viewModel.uiState.value.cartItems[0].quantity)
        assertEquals(Money.fromRupees(220), viewModel.uiState.value.grandTotal)

        // Decrease quantity to 0 -> should remove item from cart
        viewModel.decreaseQuantity(itemId)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.cartItems.isEmpty())
        assertEquals(Money.ZERO, viewModel.uiState.value.grandTotal)
    }

    @Test
    fun testSetItemQuantityDirectly() = runTest {
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        viewModel.addProductToCart(sugarProduct)
        advanceUntilIdle()

        val itemId = viewModel.uiState.value.cartItems[0].cartItemId

        // Set to 2.5 kg (2500 scaled units)
        val customQty = Quantity.fromScaledUnits(2500)
        viewModel.setItemQuantity(itemId, customQty)
        advanceUntilIdle()

        val item = viewModel.uiState.value.cartItems[0]
        assertEquals(customQty, item.quantity)
        // Rs. 180 * 2.5 = Rs. 450
        assertEquals(Money.fromRupees(450), item.lineTotal)
        assertEquals(Money.fromRupees(450), viewModel.uiState.value.grandTotal)
    }

    @Test
    fun testRemoveItemAndClearCart() = runTest {
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        viewModel.addProductToCart(milkProduct)
        viewModel.addProductToCart(breadProduct)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.cartItems.size)
        val milkItemId = viewModel.uiState.value.cartItems.first { it.product.productId == milkProduct.productId }.cartItemId

        viewModel.removeCartItem(milkItemId)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.cartItems.size)
        assertEquals(breadProduct.name, viewModel.uiState.value.cartItems[0].product.name)

        // Clear remaining cart
        viewModel.clearCart()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.cartItems.isEmpty())
        assertEquals(Money.ZERO, viewModel.uiState.value.grandTotal)
    }

    @Test
    fun testBarcodeScannerLookupAddsProduct() = runTest {
        whenever(unitRepository.getAllUnits()).thenReturn(listOf(sampleUnit))
        whenever(productRepository.getProductByBarcode("shop-test-1", "8964000123456")).thenReturn(milkProduct)

        viewModel.onBarcodeScanned("8964000123456")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.cartItems.size)
        assertEquals("Milk 1L", state.cartItems[0].product.name)
        assertEquals(Money.fromRupees(220), state.grandTotal)
    }

    @Test
    fun testCurrencyFormatterFormatting() {
        assertEquals("Rs. 220", CurrencyFormatter.formatPkr(Money.fromRupees(220)))
        assertEquals("Rs. 1,250", CurrencyFormatter.formatPkr(Money.fromRupees(1250)))
        assertEquals("Rs. 12,500", CurrencyFormatter.formatPkr(Money.fromRupees(12500)))
        assertEquals("Rs. 640.50", CurrencyFormatter.formatPkr(Money.fromRupeesAndPaisas(640, 50)))
        assertEquals("Rs. 0", CurrencyFormatter.formatPkr(Money.ZERO))
    }
}
