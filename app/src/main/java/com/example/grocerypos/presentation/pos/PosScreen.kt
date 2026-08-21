package com.example.grocerypos.presentation.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.grocerypos.R
import com.example.grocerypos.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateBack: () -> Unit,
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Show snackbar feedback when userMessage changes
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.pos_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pos_cart_items_count, uiState.totalItemCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.btn_cancel)
                        )
                    }
                },
                actions = {
                    // Held Sales Action Button with Badge
                    IconButton(onClick = { viewModel.onShowHeldSalesClicked() }) {
                        BadgedBox(
                            badge = {
                                if (uiState.activeHeldSalesCount > 0) {
                                    Badge {
                                        Text("${uiState.activeHeldSalesCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircleOutline,
                                contentDescription = "Held Sales"
                            )
                        }
                    }

                    if (uiState.cartItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Text(
                                text = stringResource(R.string.pos_btn_clear_cart),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTabletLayout = maxWidth >= 600.dp
            val isLargeTablet = maxWidth >= 900.dp

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                if (isTabletLayout) {
                    // Two-Pane Tablet Layout
                    TabletPosLayout(
                        uiState = uiState,
                        isLargeTablet = isLargeTablet,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onBarcodeScanned = {
                            viewModel.onBarcodeScanned(it)
                            focusManager.clearFocus()
                        },
                        onProductSelected = { product ->
                            viewModel.addProductToCart(product)
                        },
                        onIncreaseQty = viewModel::increaseQuantity,
                        onDecreaseQty = viewModel::decreaseQuantity,
                        onRemoveItem = viewModel::removeCartItem,
                        onHoldClicked = viewModel::onHoldClicked,
                        onCustomerClicked = viewModel::onCustomerClicked,
                        onPaymentClicked = viewModel::onPaymentClicked
                    )
                } else {
                    // Single-Pane Phone Layout
                    PhonePosLayout(
                        uiState = uiState,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onBarcodeScanned = {
                            viewModel.onBarcodeScanned(it)
                            focusManager.clearFocus()
                        },
                        onProductSelected = { product ->
                            viewModel.addProductToCart(product)
                        },
                        onIncreaseQty = viewModel::increaseQuantity,
                        onDecreaseQty = viewModel::decreaseQuantity,
                        onRemoveItem = viewModel::removeCartItem,
                        onHoldClicked = viewModel::onHoldClicked,
                        onCustomerClicked = viewModel::onCustomerClicked,
                        onPaymentClicked = viewModel::onPaymentClicked
                    )
                }
            }
        }
    }

    // Hold Sale Confirmation Dialog
    if (uiState.showHoldDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissHoldDialog() },
            title = {
                Text(
                    text = stringResource(R.string.pos_hold_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.pos_hold_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmHoldSale() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.pos_hold_dialog_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissHoldDialog() }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Customer Selection Bottom Sheet
    if (uiState.showCustomerSheet) {
        CustomerSelectionSheet(
            searchQuery = uiState.customerSearchQuery,
            searchResults = uiState.customerSearchResults,
            selectedCustomer = uiState.selectedCustomer,
            onQueryChanged = viewModel::onCustomerSearchQueryChanged,
            onSelectCustomer = viewModel::selectCustomer,
            onClearCustomer = viewModel::clearCustomer,
            onQuickAddClicked = viewModel::onQuickAddCustomerClicked,
            onDismiss = viewModel::dismissCustomerSheet
        )
    }

    // Quick Add Customer Dialog
    if (uiState.showQuickAddCustomerDialog) {
        QuickAddCustomerDialog(
            onDismiss = viewModel::dismissQuickAddCustomerDialog,
            onConfirm = { name, phone, address, limit ->
                viewModel.createCustomer(name, phone, address, limit)
            }
        )
    }

    // Held Sales Bottom Sheet
    if (uiState.showHeldSalesSheet) {
        HeldSalesSheet(
            heldSales = uiState.heldSalesList,
            onResumeSale = viewModel::resumeHeldSale,
            onDiscardSale = viewModel::discardHeldSale,
            onDismiss = viewModel::dismissHeldSalesSheet
        )
    }

    // Payment Checkout Bottom Sheet
    if (uiState.showPaymentSheet) {
        PaymentSheet(
            grandTotal = uiState.grandTotal,
            selectedCustomer = uiState.selectedCustomer,
            selectedCustomerBalance = uiState.selectedCustomerBalance,
            isCompletingSale = uiState.isCompletingSale,
            onCompleteSale = { payments, tendered ->
                viewModel.completeSale(payments, tendered)
            },
            onDismiss = viewModel::dismissPaymentSheet
        )
    }

    // Sale Receipt / Success Dialog
    uiState.saleSuccessResult?.let { successData ->
        SaleSuccessDialog(
            successData = successData,
            onDismiss = viewModel::dismissSuccessResult
        )
    }
}

/**
 * Single-Pane Phone Layout.
 * Vertically arranged: Top Search/Scan Bar -> Customer Chip -> Cart Items / Empty State -> Sticky Bottom Total & Action Panel.
 */
@Composable
private fun PhonePosLayout(
    uiState: PosUiState,
    onSearchQueryChanged: (String) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onProductSelected: (Product) -> Unit,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onHoldClicked: () -> Unit,
    onCustomerClicked: () -> Unit,
    onPaymentClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Barcode input bar
        SearchBarSection(
            query = uiState.searchQuery,
            onQueryChanged = onSearchQueryChanged,
            onBarcodeSubmit = onBarcodeScanned,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Customer Quick Bar
        CustomerQuickBar(
            customer = uiState.selectedCustomer,
            balance = uiState.selectedCustomerBalance,
            onClick = onCustomerClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // If search query is non-empty, display inline search results overlay
        if (uiState.searchQuery.isNotBlank()) {
            SearchResultsList(
                searchResults = uiState.searchResults,
                onProductSelected = { product ->
                    onProductSelected(product)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        } else {
            // Display Current Cart or Empty state
            if (uiState.cartItems.isEmpty()) {
                EmptyCartState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.cartItems,
                        key = { it.cartItemId }
                    ) { item ->
                        CartItemRow(
                            item = item,
                            onIncrease = { onIncreaseQty(item.cartItemId) },
                            onDecrease = { onDecreaseQty(item.cartItemId) },
                            onRemove = { onRemoveItem(item.cartItemId) }
                        )
                    }
                }
            }
        }

        // Bottom Action Panel (Total, Hold, Customer, Payment)
        BottomActionPanel(
            uiState = uiState,
            onHoldClicked = onHoldClicked,
            onCustomerClicked = onCustomerClicked,
            onPaymentClicked = onPaymentClicked,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Two-Pane Tablet Layout.
 * Left Pane: Product Search & Instant Catalog Grid.
 * Right Pane: Active Cart, Customer Bar, Live Totals, and Checkout Actions.
 */
@Composable
private fun TabletPosLayout(
    uiState: PosUiState,
    isLargeTablet: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onProductSelected: (Product) -> Unit,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onHoldClicked: () -> Unit,
    onCustomerClicked: () -> Unit,
    onPaymentClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Pane: Search & Product Catalog (55% or 58% on large screens)
        Card(
            modifier = Modifier
                .weight(if (isLargeTablet) 1.2f else 1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.products_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                SearchBarSection(
                    query = uiState.searchQuery,
                    onQueryChanged = onSearchQueryChanged,
                    onBarcodeSubmit = onBarcodeScanned,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                SearchResultsList(
                    searchResults = uiState.searchResults,
                    onProductSelected = onProductSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
        }

        // Right Pane: Current Cart & Actions (45% or 42% on large screens)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.pos_current_sale),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.totalItemCount} items in cart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CustomerQuickBar(
                        customer = uiState.selectedCustomer,
                        balance = uiState.selectedCustomerBalance,
                        onClick = onCustomerClicked
                    )
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Cart List or Empty State
                if (uiState.cartItems.isEmpty()) {
                    EmptyCartState(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.cartItems,
                            key = { it.cartItemId }
                        ) { item ->
                            CartItemRow(
                                item = item,
                                onIncrease = { onIncreaseQty(item.cartItemId) },
                                onDecrease = { onDecreaseQty(item.cartItemId) },
                                onRemove = { onRemoveItem(item.cartItemId) }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Summary & Action Bar
                BottomActionPanel(
                    uiState = uiState,
                    onHoldClicked = onHoldClicked,
                    onCustomerClicked = onCustomerClicked,
                    onPaymentClicked = onPaymentClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Customer Quick Selector Bar Chip.
 */
@Composable
private fun CustomerQuickBar(
    customer: com.example.grocerypos.domain.model.Customer?,
    balance: com.example.grocerypos.domain.model.Money,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (customer != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (customer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = customer?.name ?: "Walk-in Customer",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (customer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (customer != null && balance.isPositive()) {
                    Text(
                        text = "Khata Due: ${CurrencyFormatter.formatPkr(balance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Reusable Search and Barcode Input Bar with Enter Action for Hardware/Keyboard Scanners.
 */
@Composable
private fun SearchBarSection(
    query: String,
    onQueryChanged: (String) -> Unit,
    onBarcodeSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier,
        placeholder = {
            Text(
                text = stringResource(R.string.pos_search_barcode_hint),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Barcode ready",
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onBarcodeSubmit(query)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}

/**
 * Displays Search & Catalog results in a clean, high-density scrollable list.
 */
@Composable
private fun SearchResultsList(
    searchResults: List<PosProductUi>,
    onProductSelected: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (searchResults.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.pos_no_search_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(
                items = searchResults,
                key = { it.product.productId }
            ) { itemUi ->
                ProductCatalogItemRow(
                    itemUi = itemUi,
                    onClick = { onProductSelected(itemUi.product) }
                )
            }
        }
    }
}

/**
 * Individual Product Row in the Search / Catalog List.
 */
@Composable
private fun ProductCatalogItemRow(
    itemUi: PosProductUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itemUi.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = itemUi.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (itemUi.primaryBarcode.isNotBlank()) {
                        Text(
                            text = "• ${itemUi.primaryBarcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatPkr(itemUi.product.sellingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                if (itemUi.sellingUnitSymbol.isNotBlank()) {
                    Text(
                        text = "per ${itemUi.sellingUnitSymbol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Individual Cart Item Row with Quantity Incrementor/Decrementor & Line Total.
 */
@Composable
private fun CartItemRow(
    item: CartItemUi,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Name and Unit Price
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${CurrencyFormatter.formatPkr(item.unitPrice)} / ${item.unitSymbol.ifBlank { "unit" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quantity Control Buttons (- QTY +)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease quantity",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.quantity.toFormattedString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase quantity",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Line Total & Remove
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = CurrencyFormatter.formatPkr(item.lineTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty Cart State graphic and guidance text.
 */
@Composable
private fun EmptyCartState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.pos_empty_cart_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.pos_empty_cart_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/**
 * Bottom Action Panel: Total Display, Hold, Customer, Payment buttons.
 */
@Composable
private fun BottomActionPanel(
    uiState: PosUiState,
    onHoldClicked: () -> Unit,
    onCustomerClicked: () -> Unit,
    onPaymentClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Total Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.pos_total),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.totalItemCount > 0) {
                        Text(
                            text = "${uiState.totalItemCount} items (${uiState.totalUnitsCount.toFormattedString()} units)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = CurrencyFormatter.formatPkr(uiState.grandTotal),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Buttons: HOLD & CUSTOMER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onHoldClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = uiState.cartItems.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.PauseCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.pos_btn_hold),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCustomerClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.selectedCustomer != null) uiState.selectedCustomer.name.take(10) else stringResource(R.string.pos_btn_customer),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary PAYMENT Button
            Button(
                onClick = onPaymentClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = uiState.cartItems.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.pos_btn_payment)} (${CurrencyFormatter.formatPkr(uiState.grandTotal)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
