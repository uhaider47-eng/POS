package com.example.grocerypos.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.usecase.SetupShopUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val deviceName: String = "Counter 1 Main Terminal",
    val deviceType: DeviceType = DeviceType.TABLET,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ShopSetupViewModel @Inject constructor(
    private val setupShopUseCase: SetupShopUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onShopNameChange(value: String) = _uiState.update { it.copy(shopName = value, errorMessage = null) }
    fun onOwnerNameChange(value: String) = _uiState.update { it.copy(ownerName = value, errorMessage = null) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, errorMessage = null) }
    fun onAddressChange(value: String) = _uiState.update { it.copy(address = value, errorMessage = null) }
    fun onDeviceNameChange(value: String) = _uiState.update { it.copy(deviceName = value) }
    fun onDeviceTypeChange(value: DeviceType) = _uiState.update { it.copy(deviceType = value) }

    fun submitSetup() {
        val state = _uiState.value
        if (state.shopName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your shop name.") }
            return
        }
        if (state.ownerName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter the owner's name.") }
            return
        }
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a contact phone number.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = setupShopUseCase(
                shopName = state.shopName,
                ownerName = state.ownerName,
                phone = state.phone,
                address = state.address,
                deviceName = state.deviceName,
                deviceType = state.deviceType
            )
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Failed to initialize shop.") }
            }
        }
    }
}
