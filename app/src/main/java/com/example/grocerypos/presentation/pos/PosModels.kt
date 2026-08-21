package com.example.grocerypos.presentation.pos

import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Sale
import java.text.NumberFormat
import java.util.Locale

/**
 * Presentation model representing an individual item in the active POS cart.
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
 * Presentation model for a payment entry in the payment dialog.
 */
data class PaymentLineUi(
    val id: String,
    val method: PaymentMethod,
    val amount: Money
)

/**
 * Presentation model for customer display with live balance and credit limit.
 */
data class CustomerUi(
    val customer: Customer,
    val currentBalance: Money = Money.ZERO
)

/**
 * Presentation model for displaying a held/parked sale in the Held Sales list.
 */
data class HeldSaleUi(
    val sale: Sale,
    val formattedTime: String,
    val customerName: String,
    val itemCount: Int,
    val totalAmount: Money
)

/**
 * Presentation model for completed sale receipt/success display.
 */
data class SaleSuccessUi(
    val invoiceNumber: String,
    val grandTotal: Money,
    val paidAmount: Money,
    val dueAmount: Money,
    val changeReturned: Money,
    val customerName: String?,
    val completedAt: Long,
    val paymentBreakdown: List<PaymentLineUi>
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
    val selectedCustomer: Customer? = null,
    val selectedCustomerBalance: Money = Money.ZERO,
    val currentDraftSaleId: String? = null,
    val activeHeldSalesCount: Int = 0,
    val heldSalesList: List<HeldSaleUi> = emptyList(),
    val customerSearchResults: List<CustomerUi> = emptyList(),
    val customerSearchQuery: String = "",
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isCompletingSale: Boolean = false,
    val userMessage: String? = null,
    // Dialogs & Sheets
    val showHoldDialog: Boolean = false,
    val showHeldSalesSheet: Boolean = false,
    val showCustomerSheet: Boolean = false,
    val showQuickAddCustomerDialog: Boolean = false,
    val showPaymentSheet: Boolean = false,
    val saleSuccessResult: SaleSuccessUi? = null
)

/**
 * Centralized formatting utility for currency and display presentation.
 */
object CurrencyFormatter {
    private val locale = Locale("en", "PK")

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
