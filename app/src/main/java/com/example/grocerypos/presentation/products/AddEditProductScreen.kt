package com.example.grocerypos.presentation.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.grocerypos.R

@Composable
fun AddEditProductScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSavedSuccess) {
        if (state.isSavedSuccess) {
            onNavigateBack()
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var baseUnitDropdownExpanded by remember { mutableStateOf(false) }
    var sellingUnitDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCategoryName = state.categories.find { it.categoryId == state.categoryId }?.name ?: "Select Category"
    val selectedBaseUnit = state.units.find { it.unitId == state.baseUnitId }?.let { "${it.name} (${it.symbol})" } ?: "Select Base Unit"
    val selectedSellingUnit = state.units.find { it.unitId == state.sellingUnitId }?.let { "${it.name} (${it.symbol})" } ?: "Select Selling Unit"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isEditMode) stringResource(R.string.edit_product_title) else stringResource(R.string.add_product_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Product Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.product_name_label)) },
                    placeholder = { Text(stringResource(R.string.product_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category_select_label)) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        state.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    viewModel.onCategoryChange(cat.categoryId)
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand and SKU row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.brand,
                        onValueChange = viewModel::onBrandChange,
                        label = { Text(stringResource(R.string.brand_label)) },
                        placeholder = { Text(stringResource(R.string.brand_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.sku,
                        onValueChange = viewModel::onSkuChange,
                        label = { Text(stringResource(R.string.sku_label)) },
                        placeholder = { Text(stringResource(R.string.sku_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selling Price & Minimum Stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.sellingPrice,
                        onValueChange = viewModel::onPriceChange,
                        label = { Text(stringResource(R.string.price_pkr_label)) },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.minimumStock,
                        onValueChange = viewModel::onMinStockChange,
                        label = { Text(stringResource(R.string.min_stock_label)) },
                        placeholder = { Text(stringResource(R.string.min_stock_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Barcode text field (Build 01: basic text field, no camera scanner yet)
                OutlinedTextField(
                    value = state.barcode,
                    onValueChange = viewModel::onBarcodeChange,
                    label = { Text(stringResource(R.string.barcode_label)) },
                    placeholder = { Text(stringResource(R.string.barcode_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Unit Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Base Unit
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedBaseUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.base_unit_label)) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { baseUnitDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = baseUnitDropdownExpanded,
                            onDismissRequest = { baseUnitDropdownExpanded = false }
                        ) {
                            state.units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.symbol})") },
                                    onClick = {
                                        viewModel.onBaseUnitChange(u.unitId)
                                        baseUnitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Selling Unit
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedSellingUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.selling_unit_label)) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sellingUnitDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = sellingUnitDropdownExpanded,
                            onDismissRequest = { sellingUnitDropdownExpanded = false }
                        ) {
                            state.units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.symbol})") },
                                    onClick = {
                                        viewModel.onSellingUnitChange(u.unitId)
                                        sellingUnitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Conversion Factor
                OutlinedTextField(
                    value = state.conversionFactor,
                    onValueChange = viewModel::onConversionFactorChange,
                    label = { Text(stringResource(R.string.conversion_factor_label)) },
                    supportingText = { Text(stringResource(R.string.conversion_factor_help)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Checkboxes for tracking
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.trackExpiry,
                        onCheckedChange = viewModel::onTrackExpiryChange
                    )
                    Text(text = stringResource(R.string.track_expiry_label), style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.trackBatch,
                        onCheckedChange = viewModel::onTrackBatchChange
                    )
                    Text(text = stringResource(R.string.track_batch_label), style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save & Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = viewModel::saveProduct,
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_save_product), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
