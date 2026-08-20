package com.example.grocerypos.presentation.pos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.HoldSaleCommand
import com.example.grocerypos.domain.model.HoldSaleItem
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import com.example.grocerypos.domain.service.FinancialCalculationService
import com.example.grocerypos.domain.usecase.HoldSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val financialCalculationService: FinancialCalculationService,
    private val holdSaleUseCase: HoldSaleUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItemUi>>(emptyList())
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _showHoldDialog = MutableStateFlow(false)
    private val _showCustomerDialog = MutableStateFlow(false)
    private val _showPaymentDialog = MutableStateFlow(false)

    // Cached shop reference
    private val _currentShop = MutableStateFlow<Shop?>(null)

    init {
        viewModelScope.launch {
            shopRepository.getShopFlow().collect { shop ->
                _currentShop.value = shop
            }
        }
    }

    // Debounced product search flow
    private val searchResultsFlow = _currentShop.flatMapLatest { shop ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            combine(
                _searchQuery.debounce(150L).distinctUntilChanged(),
                categoryRepository.getCategoriesFlow(shop.shopId),
                unitRepository.getAllUnitsFlow()
            ) { query, categories, units ->
                Triple(query, categories, units)
            }.flatMapLatest { (query, categories, units) ->
                val categoryMap = categories.associateBy { it.categoryId }
                val unitMap = units.associateBy { it.unitId }

                val rawProductsFlow = if (query.isBlank()) {
                    productRepository.getProductsFlow(shop.shopId)
                } else {
                    productRepository.searchProductsFlow(shop.shopId, query.trim())
                }

                rawProductsFlow.flatMapLatest { products ->
                    val uiProducts = products.filter { it.isActive }.map { p ->
                        PosProductUi(
                            product = p,
                            categoryName = categoryMap[p.categoryId]?.name ?: "General",
                            sellingUnitSymbol = unitMap[p.sellingUnitId]?.symbol ?: "",
                            primaryBarcode = p.barcodes.firstOrNull()?.barcode ?: ""
                        )
                    }
                    flowOf(uiProducts)
                }
            }
        }
    }

    val uiState: StateFlow<PosUiState> = combine(
        _cartItems,
        _searchQuery,
        searchResultsFlow,
        _userMessage,
        _showHoldDialog,
        _showCustomerDialog,
        _showPaymentDialog
    ) { cartItems, query, searchResults, userMessage, showHold, showCust, showPay ->
        // Calculate subtotal and totals using FinancialCalculationService
        val lineTotals = cartItems.map { it.lineTotal }
        val subtotal = financialCalculationService.calculateSubtotal(lineTotals)
        val grandTotal = financialCalculationService.calculateGrandTotal(subtotal)
        val totalItemCount = cartItems.size
        val totalUnits = cartItems.fold(Quantity.ZERO) { acc, item -> acc + item.quantity }

        PosUiState(
            cartItems = cartItems,
            searchResults = searchResults,
            searchQuery = query,
            subtotal = subtotal,
            grandTotal = grandTotal,
            totalItemCount = totalItemCount,
            totalUnitsCount = totalUnits,
            userMessage = userMessage,
            showHoldDialog = showHold,
            showCustomerDialog = showCust,
            showPaymentDialog = showPay,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Handles barcode submitted from hardware keyboard / barcode scanner (Enter key)
     * or software keyboard search action.
     */
    fun onBarcodeScanned(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return

        val shop = _currentShop.value ?: return

        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(shop.shopId, cleanBarcode)
            if (product != null) {
                if (product.isActive) {
                    addProductToCart(product)
                    _searchQuery.value = "" // clear search input on successful scan
                    _userMessage.value = "Added ${product.name} to cart"
                } else {
                    _userMessage.value = "Product '${product.name}' is currently inactive"
                }
            } else {
                _userMessage.value = "Barcode '$cleanBarcode' not found in catalog"
            }
        }
    }

    /**
     * Adds a product to the cart.
     * If the item already exists in the cart, increases its quantity by 1.
     */
    fun addProductToCart(product: Product) {
        viewModelScope.launch {
            val shop = _currentShop.value ?: return@launch
            val units = unitRepository.getAllUnits()
            val unitMap = units.associateBy { it.unitId }
            val unitSymbol = unitMap[product.sellingUnitId]?.symbol ?: ""

            _cartItems.update { currentList ->
                val existingIndex = currentList.indexOfFirst { it.product.productId == product.productId }
                if (existingIndex >= 0) {
                    val existing = currentList[existingIndex]
                    val newQty = existing.quantity + Quantity.ONE
                    val newLineTotal = financialCalculationService.calculateLineTotal(existing.unitPrice, newQty)
                    val updatedItem = existing.copy(
                        quantity = newQty,
                        lineTotal = newLineTotal
                    )
                    currentList.toMutableList().apply {
                        this[existingIndex] = updatedItem
                    }
                } else {
                    val initialQty = Quantity.ONE
                    val lineTotal = financialCalculationService.calculateLineTotal(product.sellingPrice, initialQty)
                    val newItem = CartItemUi(
                        cartItemId = UUID.randomUUID().toString(),
                        product = product,
                        categoryName = "",
                        unitSymbol = unitSymbol,
                        unitPrice = product.sellingPrice,
                        quantity = initialQty,
                        lineTotal = lineTotal
                    )
                    currentList + newItem
                }
            }
        }
    }

    /**
     * Increases quantity of a cart item by 1 unit.
     */
    fun increaseQuantity(cartItemId: String) {
        _cartItems.update { currentList ->
            currentList.map { item ->
                if (item.cartItemId == cartItemId) {
                    val newQty = item.quantity + Quantity.ONE
                    val newLineTotal = financialCalculationService.calculateLineTotal(item.unitPrice, newQty)
                    item.copy(quantity = newQty, lineTotal = newLineTotal)
                } else {
                    item
                }
            }
        }
    }

    /**
     * Decreases quantity of a cart item by 1 unit.
     * If quantity reaches 0, the item is removed from the cart.
     */
    fun decreaseQuantity(cartItemId: String) {
        _cartItems.update { currentList ->
            val target = currentList.find { it.cartItemId == cartItemId } ?: return@update currentList
            val newQty = target.quantity - Quantity.ONE
            if (newQty <= Quantity.ZERO) {
                currentList.filterNot { it.cartItemId == cartItemId }
            } else {
                currentList.map { item ->
                    if (item.cartItemId == cartItemId) {
                        val newLineTotal = financialCalculationService.calculateLineTotal(item.unitPrice, newQty)
                        item.copy(quantity = newQty, lineTotal = newLineTotal)
                    } else {
                        item
                    }
                }
            }
        }
    }

    /**
     * Explicitly sets the quantity for an item (e.g. from a custom quantity editor/keypad).
     */
    fun setItemQuantity(cartItemId: String, newQuantity: Quantity) {
        _cartItems.update { currentList ->
            if (newQuantity <= Quantity.ZERO) {
                currentList.filterNot { it.cartItemId == cartItemId }
            } else {
                currentList.map { item ->
                    if (item.cartItemId == cartItemId) {
                        val newLineTotal = financialCalculationService.calculateLineTotal(item.unitPrice, newQuantity)
                        item.copy(quantity = newQuantity, lineTotal = newLineTotal)
                    } else {
                        item
                    }
                }
            }
        }
    }

    /**
     * Removes an item completely from the cart.
     */
    fun removeCartItem(cartItemId: String) {
        _cartItems.update { currentList ->
            currentList.filterNot { it.cartItemId == cartItemId }
        }
    }

    /**
     * Clears all items in the current cart.
     */
    fun clearCart() {
        _cartItems.value = emptyList()
        _userMessage.value = "Cart cleared"
    }

    // Dialog state controllers
    fun onHoldClicked() {
        if (_cartItems.value.isEmpty()) {
            _userMessage.value = "Cannot hold an empty cart"
            return
        }
        _showHoldDialog.value = true
    }

    fun dismissHoldDialog() {
        _showHoldDialog.value = false
    }

    fun confirmHoldSale() {
        val currentItems = _cartItems.value
        val shop = _currentShop.value
        if (currentItems.isEmpty() || shop == null) {
            _showHoldDialog.value = false
            return
        }

        viewModelScope.launch {
            val holdCommand = HoldSaleCommand(
                saleId = UUID.randomUUID().toString(),
                shopId = shop.shopId,
                deviceId = "POS_MAIN",
                userId = "CASHIER",
                customerId = null,
                items = currentItems.map { item ->
                    HoldSaleItem(
                        productId = item.product.productId,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice
                    )
                },
                note = "Held from POS terminal"
            )

            val result = holdSaleUseCase(holdCommand)
            if (result.isSuccess) {
                _cartItems.value = emptyList()
                _showHoldDialog.value = false
                _userMessage.value = "Sale placed on hold"
            } else {
                _userMessage.value = "Failed to hold sale: ${result.exceptionOrNull()?.message}"
                _showHoldDialog.value = false
            }
        }
    }

    fun onCustomerClicked() {
        _showCustomerDialog.value = true
    }

    fun dismissCustomerDialog() {
        _showCustomerDialog.value = false
    }

    fun onPaymentClicked() {
        if (_cartItems.value.isEmpty()) {
            _userMessage.value = "Cannot checkout an empty cart"
            return
        }
        _showPaymentDialog.value = true
    }

    fun dismissPaymentDialog() {
        _showPaymentDialog.value = false
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
