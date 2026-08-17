package com.example.grocerypos.presentation.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.Barcode
import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Unit
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import com.example.grocerypos.domain.usecase.SaveProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddEditProductUiState(
    val productId: String? = null,
    val isEditMode: Boolean = false,
    val name: String = "",
    val categoryId: String = "",
    val brand: String = "",
    val sku: String = "",
    val baseUnitId: String = "",
    val sellingUnitId: String = "",
    val conversionFactor: String = "1.0",
    val sellingPrice: String = "",
    val minimumStock: String = "0",
    val barcode: String = "",
    val trackExpiry: Boolean = false,
    val trackBatch: Boolean = false,
    val categories: List<Category> = emptyList(),
    val units: List<Unit> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false
)

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val shopRepository: ShopRepository,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val productRepository: ProductRepository,
    private val saveProductUseCase: SaveProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _uiState.asStateFlow()

    private var currentShopId: String = ""

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val shop = shopRepository.getShopFlow().firstOrNull() ?: shopRepository.getShop()
            if (shop == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Shop configuration not found") }
                return@launch
            }
            currentShopId = shop.shopId

            val categories = categoryRepository.getActiveCategories(currentShopId)
            val units = unitRepository.getAllUnits()

            val defaultCategoryId = categories.firstOrNull()?.categoryId.orEmpty()
            val defaultUnitId = units.firstOrNull()?.unitId.orEmpty()

            val productIdParam: String? = savedStateHandle.get<String>("productId")?.takeIf { it.isNotBlank() && it != "new" }

            if (productIdParam != null) {
                val existingProduct = productRepository.getProductById(productIdParam)
                if (existingProduct != null) {
                    val primaryBarcode = existingProduct.barcodes.firstOrNull { it.isPrimary }?.barcode
                        ?: existingProduct.barcodes.firstOrNull()?.barcode.orEmpty()

                    _uiState.update {
                        it.copy(
                            productId = existingProduct.productId,
                            isEditMode = true,
                            name = existingProduct.name,
                            categoryId = existingProduct.categoryId,
                            brand = existingProduct.brand,
                            sku = existingProduct.sku,
                            baseUnitId = existingProduct.baseUnitId,
                            sellingUnitId = existingProduct.sellingUnitId,
                            conversionFactor = existingProduct.conversionFactor.toFormattedString(),
                            sellingPrice = existingProduct.sellingPrice.toPlainDecimalString(),
                            minimumStock = existingProduct.minimumStock.toFormattedString(),
                            barcode = primaryBarcode,
                            trackExpiry = existingProduct.trackExpiry,
                            trackBatch = existingProduct.trackBatch,
                            categories = categories,
                            units = units,
                            isLoading = false
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    categoryId = defaultCategoryId,
                    baseUnitId = defaultUnitId,
                    sellingUnitId = defaultUnitId,
                    categories = categories,
                    units = units,
                    isLoading = false
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, errorMessage = null) }
    fun onCategoryChange(categoryId: String) = _uiState.update { it.copy(categoryId = categoryId) }
    fun onBrandChange(brand: String) = _uiState.update { it.copy(brand = brand) }
    fun onSkuChange(sku: String) = _uiState.update { it.copy(sku = sku) }
    fun onBaseUnitChange(unitId: String) = _uiState.update { it.copy(baseUnitId = unitId) }
    fun onSellingUnitChange(unitId: String) = _uiState.update { it.copy(sellingUnitId = unitId) }
    fun onConversionFactorChange(factor: String) = _uiState.update { it.copy(conversionFactor = factor) }
    fun onPriceChange(price: String) = _uiState.update { it.copy(sellingPrice = price, errorMessage = null) }
    fun onMinStockChange(minStock: String) = _uiState.update { it.copy(minimumStock = minStock) }
    fun onBarcodeChange(barcode: String) = _uiState.update { it.copy(barcode = barcode) }
    fun onTrackExpiryChange(track: Boolean) = _uiState.update { it.copy(trackExpiry = track) }
    fun onTrackBatchChange(track: Boolean) = _uiState.update { it.copy(trackBatch = track) }

    fun saveProduct() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Product name is required") }
            return
        }

        val price = Money.parseOrNull(state.sellingPrice)
        if (price == null || !price.isPositive()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid price in PKR greater than 0") }
            return
        }

        val minStock = Quantity.parseOrDefault(state.minimumStock, Quantity.ZERO)
        val conversion = Quantity.parseOrNull(state.conversionFactor)
        if (conversion == null || !conversion.isPositive()) {
            _uiState.update { it.copy(errorMessage = "Conversion factor must be greater than 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val now = System.currentTimeMillis()
            val productId = state.productId ?: UUID.randomUUID().toString()

            val barcodes = if (state.barcode.isNotBlank()) {
                listOf(
                    Barcode(
                        barcodeId = UUID.randomUUID().toString(),
                        productId = productId,
                        barcode = state.barcode.trim(),
                        isPrimary = true,
                        shopId = currentShopId
                    )
                )
            } else {
                emptyList()
            }

            val product = Product(
                productId = productId,
                shopId = currentShopId,
                name = state.name.trim(),
                categoryId = state.categoryId,
                brand = state.brand.trim(),
                sku = state.sku.trim(),
                baseUnitId = state.baseUnitId,
                sellingUnitId = state.sellingUnitId,
                conversionFactor = conversion,
                sellingPrice = price,
                minimumStock = minStock,
                trackExpiry = state.trackExpiry,
                trackBatch = state.trackBatch,
                isActive = true,
                barcodes = barcodes,
                createdAt = if (state.isEditMode) 0L else now,
                updatedAt = now
            )

            val result = saveProductUseCase(product)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isSavedSuccess = true) }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to save product"
                _uiState.update { it.copy(isSaving = false, errorMessage = error) }
            }
        }
    }
}
