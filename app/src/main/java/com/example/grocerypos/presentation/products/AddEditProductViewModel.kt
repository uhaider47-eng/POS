package com.example.grocerypos.presentation.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.Barcode
import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Product
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddEditProductUiState(
    val productId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val categoryId: String = "",
    val brand: String = "",
    val sku: String = "",
    val sellingPrice: String = "",
    val minimumStock: String = "0",
    val baseUnitId: String = "",
    val sellingUnitId: String = "",
    val conversionFactor: String = "1.0",
    val barcode: String = "",
    val trackExpiry: Boolean = false,
    val trackBatch: Boolean = false,
    val isActive: Boolean = true,
    val categories: List<Category> = emptyList(),
    val units: List<Unit> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false
)

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val productRepository: ProductRepository,
    private val saveProductUseCase: SaveProductUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productIdArg: String? = savedStateHandle.get<String>("productId")

    private val _uiState = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _uiState.asStateFlow()

    private var currentShopId: String = ""

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val shop = shopRepository.getShop() ?: return@launch
            currentShopId = shop.shopId

            val categories = categoryRepository.getActiveCategories(shop.shopId)
            val units = unitRepository.getAllUnits()

            val defaultCategoryId = categories.firstOrNull()?.categoryId ?: ""
            val defaultUnitId = units.firstOrNull()?.unitId ?: ""

            if (!productIdArg.isNullOrBlank() && productIdArg != "{productId}") {
                // Edit mode
                val product = productRepository.getProductById(productIdArg)
                if (product != null) {
                    _uiState.update {
                        it.copy(
                            productId = product.productId,
                            name = product.name,
                            categoryId = product.categoryId,
                            brand = product.brand,
                            sku = product.sku,
                            sellingPrice = product.sellingPrice.toString(),
                            minimumStock = product.minimumStock.toString(),
                            baseUnitId = product.baseUnitId,
                            sellingUnitId = product.sellingUnitId,
                            conversionFactor = product.conversionFactor.toString(),
                            barcode = product.barcodes.firstOrNull()?.barcode ?: "",
                            trackExpiry = product.trackExpiry,
                            trackBatch = product.trackBatch,
                            isActive = product.isActive,
                            categories = categories,
                            units = units,
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                    return@launch
                }
            }

            // Create mode
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

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(categoryId = value) }
    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }
    fun onSkuChange(value: String) = _uiState.update { it.copy(sku = value) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(sellingPrice = value, errorMessage = null) }
    fun onMinStockChange(value: String) = _uiState.update { it.copy(minimumStock = value) }
    fun onBaseUnitChange(value: String) = _uiState.update { it.copy(baseUnitId = value) }
    fun onSellingUnitChange(value: String) = _uiState.update { it.copy(sellingUnitId = value) }
    fun onConversionFactorChange(value: String) = _uiState.update { it.copy(conversionFactor = value) }
    fun onBarcodeChange(value: String) = _uiState.update { it.copy(barcode = value, errorMessage = null) }
    fun onTrackExpiryChange(value: Boolean) = _uiState.update { it.copy(trackExpiry = value) }
    fun onTrackBatchChange(value: Boolean) = _uiState.update { it.copy(trackBatch = value) }

    fun saveProduct() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Product name is required.") }
            return
        }
        val price = state.sellingPrice.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid selling price greater than 0.") }
            return
        }
        val minStock = state.minimumStock.toDoubleOrNull() ?: 0.0
        val factor = state.conversionFactor.toDoubleOrNull() ?: 1.0
        if (factor <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Conversion factor must be greater than 0.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            // Check barcode uniqueness
            val barcodeVal = state.barcode.trim()
            if (barcodeVal.isNotBlank()) {
                val isTaken = productRepository.isBarcodeTaken(
                    shopId = currentShopId,
                    barcode = barcodeVal,
                    excludeProductId = if (state.isEditMode) state.productId else null
                )
                if (isTaken) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Barcode '$barcodeVal' is already assigned to another product in this shop."
                        )
                    }
                    return@launch
                }
            }

            val barcodes = if (barcodeVal.isNotBlank()) {
                listOf(
                    Barcode(
                        barcodeId = UUID.randomUUID().toString(),
                        productId = state.productId,
                        barcode = barcodeVal,
                        isPrimary = true,
                        shopId = currentShopId
                    )
                )
            } else {
                emptyList()
            }

            val product = Product(
                productId = state.productId,
                shopId = currentShopId,
                name = state.name.trim(),
                categoryId = state.categoryId,
                brand = state.brand.trim(),
                sku = state.sku.trim(),
                baseUnitId = state.baseUnitId,
                sellingUnitId = state.sellingUnitId,
                conversionFactor = factor,
                sellingPrice = price,
                minimumStock = minStock,
                trackExpiry = state.trackExpiry,
                trackBatch = state.trackBatch,
                isActive = state.isActive,
                barcodes = barcodes,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = saveProductUseCase(product)
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, isSavedSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(isSaving = false, errorMessage = err.message ?: "Failed to save product.") }
            }
        }
    }
}
