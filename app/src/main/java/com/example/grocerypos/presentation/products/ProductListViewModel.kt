package com.example.grocerypos.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Unit
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import com.example.grocerypos.domain.usecase.GetProductsUseCase
import com.example.grocerypos.domain.usecase.SearchProductsUseCase
import com.example.grocerypos.domain.usecase.ToggleProductStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductItemUi(
    val product: Product,
    val categoryName: String,
    val baseUnitSymbol: String,
    val sellingUnitSymbol: String
)

data class ProductListUiState(
    val products: List<ProductItemUi> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val unitRepository: UnitRepository,
    private val toggleProductStatusUseCase: ToggleProductStatusUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<ProductListUiState> = shopRepository.getShopFlow().flatMapLatest { shop ->
        if (shop == null) {
            flowOf(ProductListUiState(isLoading = false))
        } else {
            combine(
                _searchQuery,
                categoryRepository.getCategoriesFlow(shop.shopId),
                unitRepository.getAllUnitsFlow()
            ) { query, categories, units ->
                Triple(query, categories, units)
            }.flatMapLatest { (query, categories, units) ->
                val categoryMap = categories.associateBy { it.categoryId }
                val unitMap = units.associateBy { it.unitId }

                val productsFlow = if (query.isBlank()) {
                    productRepository.getProductsFlow(shop.shopId)
                } else {
                    productRepository.searchProductsFlow(shop.shopId, query)
                }

                productsFlow.map { rawProducts ->
                    val uiItems = rawProducts.map { p ->
                        ProductItemUi(
                            product = p,
                            categoryName = categoryMap[p.categoryId]?.name ?: "Uncategorized",
                            baseUnitSymbol = unitMap[p.baseUnitId]?.symbol ?: "",
                            sellingUnitSymbol = unitMap[p.sellingUnitId]?.symbol ?: ""
                        )
                    }
                    ProductListUiState(
                        products = uiItems,
                        searchQuery = query,
                        isLoading = false
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleProductStatus(product: Product) {
        viewModelScope.launch {
            toggleProductStatusUseCase(product.productId, product.isActive)
        }
    }
}
