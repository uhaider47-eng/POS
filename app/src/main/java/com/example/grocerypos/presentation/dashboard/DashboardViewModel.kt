package com.example.grocerypos.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.Device
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.usecase.GetPrimaryDeviceUseCase
import com.example.grocerypos.domain.usecase.GetShopUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val shop: Shop? = null,
    val primaryDevice: Device? = null,
    val totalProducts: Int = 0,
    val activeProducts: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getShopUseCase: GetShopUseCase,
    getPrimaryDeviceUseCase: GetPrimaryDeviceUseCase,
    productRepository: ProductRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = getShopUseCase().flatMapLatest { shop ->
        if (shop == null) {
            flowOf(DashboardUiState(isLoading = false))
        } else {
            combine(
                getPrimaryDeviceUseCase(shop.shopId),
                productRepository.getTotalProductCountFlow(shop.shopId),
                productRepository.getActiveProductCountFlow(shop.shopId)
            ) { device, total, active ->
                DashboardUiState(
                    shop = shop,
                    primaryDevice = device,
                    totalProducts = total,
                    activeProducts = active,
                    isLoading = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
