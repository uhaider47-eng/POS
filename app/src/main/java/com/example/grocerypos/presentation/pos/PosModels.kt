package com.example.grocerypos.presentation.pos

import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import java.text.NumberFormat
import java.util.Locale

/**
 * Presentation model representing an individual item in the active POS cart.
 *
 * All financial amounts use immutable [Money] and [Quantity] objects with
 * zero Double/Float conversions.
 */
data class CartItemUi(
    val cartItemId: String,
    val product: Product,
    val categoryName: String,
    val unitSymbol: String,
    val unitPrice: Money,
    val quantity: Quantity,
    val lineTotal: Money
)

/**
 * Presentation model for displaying searchable products in the POS catalog view.
 */
data class PosProductUi(
    val product: Product,
    val categoryName: String,
    val sellingUnitSymbol: String,
    val primaryBarcode: String
)

/**
 * UI State for the POS Cashier Terminal Screen.
 */
data class PosUiState(
    val cartItems: List<CartItemUi> = emptyList(),
    val searchResults: List<PosProductUi> = emptyList(),
    val searchQuery: String = "",
    val subtotal: Money = Money.ZERO,
    val grandTotal: Money = Money.ZERO,
    val totalItemCount: Int = 0,
    val totalUnitsCount: Quantity = Quantity.ZERO,
    val selectedCustomerName: String = "Walk-in Customer",
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val userMessage: String? = null,
    val showHoldDialog: Boolean = false,
    val showCustomerDialog: Boolean = false,
    val showPaymentDialog: Boolean = false
)

/**
 * Centralized formatting utility for currency and display presentation.
 * Prepared for future Urdu/locale localization.
 */
object CurrencyFormatter {
    private val locale = Locale("en", "PK")
    private val numberFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    /**
     * Formats [Money] to display format: e.g. "Rs. 220", "Rs. 1,250", "Rs. 12,500.50"
     */
    fun formatPkr(money: Money, includePrefix: Boolean = true): String {
        val isNeg = money.amountInMinorUnits < 0
        val absVal = kotlin.math.abs(money.amountInMinorUnits)
        val rupees = absVal / 100L
        val paisas = absVal % 100L

        val sign = if (isNeg) "-" else ""
        val prefix = if (includePrefix) "Rs. " else ""

        val formattedRupees = NumberFormat.getNumberInstance(locale).format(rupees)

        return if (paisas == 0L) {
            "$prefix$sign$formattedRupees"
        } else {
            val paisasFormatted = if (paisas < 10) "0$paisas" else "$paisas"
            "$prefix$sign$formattedRupees.$paisasFormatted"
        }
    }
}
