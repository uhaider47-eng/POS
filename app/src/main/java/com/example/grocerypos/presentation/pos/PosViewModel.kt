package com.example.grocerypos.presentation.pos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.CompleteSaleCommand
import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.HoldSaleCommand
import com.example.grocerypos.domain.model.HoldSaleItem
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentCommand
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleItemCommand
import com.example.grocerypos.domain.model.Shop
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val customerRepository: CustomerRepository,
    private val financialCalculationService: FinancialCalculationService,
    private val completeSaleUseCase: CompleteSaleUseCase,
    private val holdSaleUseCase: HoldSaleUseCase,
    private val getHeldSalesUseCase: GetHeldSalesUseCase,
    private val resumeHeldSaleUseCase: ResumeHeldSaleUseCase,
    private val discardHeldSaleUseCase: DiscardHeldSaleUseCase,
    private val getActiveCustomersUseCase: GetActiveCustomersUseCase,
    private val getCustomerBalanceUseCase: GetCustomerBalanceUseCase,
    private val createCustomerUseCase: CreateCustomerUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItemUi>>(emptyList())
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    private val _selectedCustomerBalance = MutableStateFlow(Money.ZERO)
    private val _currentDraftSaleId = MutableStateFlow<String?>(null)

    private val _customerSearchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isCompletingSale = MutableStateFlow(false)

    // Dialog & Sheet states
    private val _showHoldDialog = MutableStateFlow(false)
    private val _showHeldSalesSheet = MutableStateFlow(false)
    private val _showCustomerSheet = MutableStateFlow(false)
    private val _showQuickAddCustomerDialog = MutableStateFlow(false)
    private val _showPaymentSheet = MutableStateFlow(false)
    private val _saleSuccessResult = MutableStateFlow<SaleSuccessUi?>(null)

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

    // Held sales flow
    private val heldSalesFlow = _currentShop.flatMapLatest { shop ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            getHeldSalesUseCase(shop.shopId).flatMapLatest { sales ->
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                // Fetch customer details if needed
                val uiList = sales.map { sale ->
                    HeldSaleUi(
                        sale = sale,
                        formattedTime = timeFormat.format(Date(sale.createdAt)),
                        customerName = "Walk-in Customer",
                        itemCount = sale.items.size,
                        totalAmount = sale.grandTotal
                    )
                }
                flowOf(uiList)
            }
        }
    }

    // Customer search flow
    private val customerListFlow = _currentShop.flatMapLatest { shop ->
        if (shop == null) {
            flowOf(emptyList())
        } else {
            combine(
                _customerSearchQuery.debounce(100L).distinctUntilChanged(),
                getActiveCustomersUseCase(shop.shopId)
            ) { query, customers ->
                val filtered = if (query.isBlank()) {
                    customers
                } else {
                    customers.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.phone.contains(query, ignoreCase = true)
                    }
                }
                filtered.map { c ->
                    CustomerUi(customer = c, currentBalance = Money.ZERO)
                }
            }
        }
    }

    val uiState: StateFlow<PosUiState> = combine(
        _cartItems,
        _searchQuery,
        searchResultsFlow,
        _selectedCustomer,
        _selectedCustomerBalance,
        _currentDraftSaleId,
        heldSalesFlow,
        customerListFlow,
        _customerSearchQuery
    ) { cartItems, query, searchResults, customer, custBalance, draftId, heldSales, customers, custQuery ->
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
            selectedCustomer = customer,
            selectedCustomerBalance = custBalance,
            currentDraftSaleId = draftId,
            activeHeldSalesCount = heldSales.size,
            heldSalesList = heldSales,
            customerSearchResults = customers,
            customerSearchQuery = custQuery,
            isLoading = false
        )
    }.combine(_userMessage) { state, msg ->
        state.copy(userMessage = msg)
    }.combine(_isCompletingSale) { state, completing ->
        state.copy(isCompletingSale = completing)
    }.combine(_showHoldDialog) { state, showHold ->
        state.copy(showHoldDialog = showHold)
    }.combine(_showHeldSalesSheet) { state, showHeldList ->
        state.copy(showHeldSalesSheet = showHeldList)
    }.combine(_showCustomerSheet) { state, showCust ->
        state.copy(showCustomerSheet = showCust)
    }.combine(_showQuickAddCustomerDialog) { state, showAddCust ->
        state.copy(showQuickAddCustomerDialog = showAddCust)
    }.combine(_showPaymentSheet) { state, showPay ->
        state.copy(showPaymentSheet = showPay)
    }.combine(_saleSuccessResult) { state, successResult ->
        state.copy(saleSuccessResult = successResult)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCustomerSearchQueryChanged(query: String) {
        _customerSearchQuery.value = query
    }

    /**
     * Handles barcode submitted from hardware keyboard / scanner or software keyboard.
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
                    _searchQuery.value = ""
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
     * Adds a product to the cart. If already exists, increases quantity by 1.
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

    fun removeCartItem(cartItemId: String) {
        _cartItems.update { currentList ->
            currentList.filterNot { it.cartItemId == cartItemId }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _currentDraftSaleId.value = null
        _userMessage.value = "Cart cleared"
    }

    // Customer Selection and Management
    fun onCustomerClicked() {
        _showCustomerSheet.value = true
    }

    fun dismissCustomerSheet() {
        _showCustomerSheet.value = false
    }

    fun selectCustomer(customer: Customer) {
        viewModelScope.launch {
            val balance = getCustomerBalanceUseCase(customer.customerId)
            _selectedCustomer.value = customer
            _selectedCustomerBalance.value = balance
            _showCustomerSheet.value = false
            _userMessage.value = "Selected customer: ${customer.name}"
        }
    }

    fun clearCustomer() {
        _selectedCustomer.value = null
        _selectedCustomerBalance.value = Money.ZERO
        _showCustomerSheet.value = false
        _userMessage.value = "Customer set to Walk-in"
    }

    fun onQuickAddCustomerClicked() {
        _showQuickAddCustomerDialog.value = true
    }

    fun dismissQuickAddCustomerDialog() {
        _showQuickAddCustomerDialog.value = false
    }

    fun createCustomer(name: String, phone: String, address: String, creditLimit: Money) {
        val shop = _currentShop.value ?: return
        viewModelScope.launch {
            val result = createCustomerUseCase(
                shopId = shop.shopId,
                name = name,
                phone = phone,
                address = address,
                creditLimit = creditLimit
            )
            result.onSuccess { newCustomer ->
                _showQuickAddCustomerDialog.value = false
                selectCustomer(newCustomer)
                _userMessage.value = "Created customer ${newCustomer.name}"
            }.onFailure { error ->
                _userMessage.value = "Failed to create customer: ${error.message}"
            }
        }
    }

    // Held Sales Management
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

    fun onShowHeldSalesClicked() {
        _showHeldSalesSheet.value = true
    }

    fun dismissHeldSalesSheet() {
        _showHeldSalesSheet.value = false
    }

    fun confirmHoldSale() {
        val currentItems = _cartItems.value
        val shop = _currentShop.value
        if (currentItems.isEmpty() || shop == null) {
            _showHoldDialog.value = false
            return
        }

        viewModelScope.launch {
            val holdSaleId = _currentDraftSaleId.value ?: UUID.randomUUID().toString()
            val holdCommand = HoldSaleCommand(
                saleId = holdSaleId,
                shopId = shop.shopId,
                deviceId = "POS_MAIN",
                cashierId = "CASHIER",
                customerId = _selectedCustomer.value?.customerId,
                items = currentItems.map { item ->
                    SaleItemCommand(
                        productId = item.product.productId,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice
                    )
                },
                notes = "Held from POS terminal"
            )

            val result = holdSaleUseCase(holdCommand)
            if (result.isSuccess) {
                _cartItems.value = emptyList()
                _currentDraftSaleId.value = null
                _selectedCustomer.value = null
                _selectedCustomerBalance.value = Money.ZERO
                _showHoldDialog.value = false
                _userMessage.value = "Sale placed on hold"
            } else {
                _userMessage.value = "Failed to hold sale: ${result.exceptionOrNull()?.message}"
                _showHoldDialog.value = false
            }
        }
    }

    fun resumeHeldSale(heldSaleUi: HeldSaleUi) {
        viewModelScope.launch {
            val result = resumeHeldSaleUseCase(heldSaleUi.sale.saleId)
            result.onSuccess { sale ->
                val units = unitRepository.getAllUnits().associateBy { it.unitId }
                val loadedCartItems = mutableListOf<CartItemUi>()

                for (item in sale.items) {
                    val product = productRepository.getProductById(item.productId)
                    if (product != null) {
                        val unitSymbol = units[item.soldUnitId]?.symbol ?: ""
                        loadedCartItems.add(
                            CartItemUi(
                                cartItemId = UUID.randomUUID().toString(),
                                product = product,
                                categoryName = "",
                                unitSymbol = unitSymbol,
                                unitPrice = item.unitPrice,
                                quantity = item.quantity,
                                lineTotal = item.netAmount
                            )
                        )
                    }
                }

                _cartItems.value = loadedCartItems
                _currentDraftSaleId.value = sale.saleId

                // Restore customer if assigned
                if (!sale.customerId.isNullOrBlank()) {
                    val customer = customerRepository.getCustomerById(sale.customerId)
                    if (customer != null) {
                        val balance = getCustomerBalanceUseCase(customer.customerId)
                        _selectedCustomer.value = customer
                        _selectedCustomerBalance.value = balance
                    } else {
                        _selectedCustomer.value = null
                        _selectedCustomerBalance.value = Money.ZERO
                    }
                } else {
                    _selectedCustomer.value = null
                    _selectedCustomerBalance.value = Money.ZERO
                }

                _showHeldSalesSheet.value = false
                _userMessage.value = "Resumed held sale (${loadedCartItems.size} items)"
            }.onFailure { error ->
                _userMessage.value = "Failed to resume sale: ${error.message}"
            }
        }
    }

    fun discardHeldSale(heldSaleUi: HeldSaleUi) {
        viewModelScope.launch {
            val result = discardHeldSaleUseCase(
                saleId = heldSaleUi.sale.saleId,
                cashierId = "CASHIER",
                reason = "Discarded by cashier"
            )
            result.onSuccess {
                if (_currentDraftSaleId.value == heldSaleUi.sale.saleId) {
                    _currentDraftSaleId.value = null
                }
                _userMessage.value = "Held sale discarded"
            }.onFailure { error ->
                _userMessage.value = "Failed to discard sale: ${error.message}"
            }
        }
    }

    // Payment and Sale Completion
    fun onPaymentClicked() {
        if (_cartItems.value.isEmpty()) {
            _userMessage.value = "Cannot checkout an empty cart"
            return
        }
        _showPaymentSheet.value = true
    }

    fun dismissPaymentSheet() {
        _showPaymentSheet.value = false
    }

    fun completeSale(payments: List<PaymentLineUi>, tenderedAmount: Money = Money.ZERO) {
        val currentItems = _cartItems.value
        val shop = _currentShop.value
        if (currentItems.isEmpty() || shop == null) {
            _userMessage.value = "Cannot complete empty sale"
            return
        }

        viewModelScope.launch {
            _isCompletingSale.value = true

            val saleId = UUID.randomUUID().toString()
            val operationId = UUID.randomUUID().toString()

            val completeCommand = CompleteSaleCommand(
                operationId = operationId,
                saleId = saleId,
                shopId = shop.shopId,
                deviceId = "POS_MAIN",
                cashierId = "CASHIER",
                customerId = _selectedCustomer.value?.customerId,
                items = currentItems.map { item ->
                    SaleItemCommand(
                        productId = item.product.productId,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice
                    )
                },
                payments = payments.map { p ->
                    PaymentCommand(
                        method = p.method,
                        amount = p.amount,
                        referenceNumber = null
                    )
                },
                draftSaleId = _currentDraftSaleId.value,
                notes = ""
            )

            val result = completeSaleUseCase(completeCommand)
            _isCompletingSale.value = false

            result.onSuccess { completedSale ->
                val change = if (tenderedAmount > completedSale.paidAmount) {
                    tenderedAmount - completedSale.paidAmount
                } else {
                    Money.ZERO
                }

                _saleSuccessResult.value = SaleSuccessUi(
                    invoiceNumber = completedSale.invoiceNumber ?: "N/A",
                    grandTotal = completedSale.grandTotal,
                    paidAmount = completedSale.paidAmount,
                    dueAmount = completedSale.dueAmount,
                    changeReturned = change,
                    customerName = _selectedCustomer.value?.name,
                    completedAt = completedSale.completedAt ?: System.currentTimeMillis(),
                    paymentBreakdown = payments
                )

                // Clear cart & state
                _cartItems.value = emptyList()
                _currentDraftSaleId.value = null
                _selectedCustomer.value = null
                _selectedCustomerBalance.value = Money.ZERO
                _showPaymentSheet.value = false
            }.onFailure { error ->
                _userMessage.value = "Sale completion failed: ${error.message}"
            }
        }
    }

    fun dismissSuccessResult() {
        _saleSuccessResult.value = null
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
