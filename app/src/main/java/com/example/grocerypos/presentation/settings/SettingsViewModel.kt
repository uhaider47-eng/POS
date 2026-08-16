package com.example.grocerypos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.Device
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.usecase.GetPrimaryDeviceUseCase
import com.example.grocerypos.domain.usecase.GetShopUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SettingsUiState(
    val shop: Shop? = null,
    val primaryDevice: Device? = null,
    val databaseVersion: Int = 1,
    val databaseName: String = "grocery_pos_db",
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getShopUseCase: GetShopUseCase,
    getPrimaryDeviceUseCase: GetPrimaryDeviceUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = getShopUseCase().flatMapLatest { shop ->
        if (shop == null) {
            flowOf(SettingsUiState(isLoading = false))
        } else {
            getPrimaryDeviceUseCase(shop.shopId).flatMapLatest { device ->
                flowOf(
                    SettingsUiState(
                        shop = shop,
                        primaryDevice = device,
                        isLoading = false
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )
}
