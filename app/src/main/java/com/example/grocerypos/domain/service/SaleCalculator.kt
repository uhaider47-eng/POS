package com.example.grocerypos.domain.service

import com.example.grocerypos.domain.model.Discount
import com.example.grocerypos.domain.model.DiscountType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentCommand
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.SaleItemCommand
import com.example.grocerypos.domain.model.TaxRule
import javax.inject.Inject
import javax.inject.Singleton

data class CalculatedSaleItem(
    val productId: String,
    val productName: String,
    val soldUnitId: String,
    val baseUnitId: String,
    val quantity: Quantity,
    val conversionFactor: Quantity,
    val deductBaseQuantity: Quantity,
    val unitPrice: Money,
    val grossAmount: Money,
    val discount: Money,
    val tax: Money,
    val netAmount: Money,
    val costAtSale: Money
)

data class CalculatedSaleTotals(
    val items: List<CalculatedSaleItem>,
    val subtotal: Money,
    val itemDiscount: Money,
    val saleDiscount: Money,
    val tax: Money,
    val grandTotal: Money,
    val payments: List<PaymentCommand>,
    val totalCashPaid: Money,
    val totalNonCashPaid: Money,
    val totalPaid: Money,
    val appliedPaidAmount: Money,
    val changeReturned: Money,
    val dueAmount: Money,
    val paymentStatus: PaymentStatus
)

/**
 * Pure domain calculation engine for POS sales.
 * Separates financial and quantitative computations from database persistence.
 */
@Singleton
class SaleCalculator @Inject constructor(
    private val financialCalculationService: FinancialCalculationService
) {

    /**
     * Calculates line items, discounts, taxes, totals, and payment balances.
     *
     * @param items Requested sale items
     * @param productsMap Product lookup map for validation and pricing
     * @param productCostsMap Current average cost snapshot for each product
     * @param saleDiscount Sale-level discount
     * @param saleTaxRule Sale-level tax rule
     * @param payments Payments provided by cashier
     * @param customerId Optional customer ID (required for credit sales)
     */
    fun calculateSale(
        items: List<SaleItemCommand>,
        productsMap: Map<String, Product>,
        productCostsMap: Map<String, Money> = emptyMap(),
        saleDiscount: Discount = Discount.NONE,
        saleTaxRule: TaxRule = TaxRule.NONE,
        payments: List<PaymentCommand> = emptyList(),
        customerId: String? = null
    ): CalculatedSaleTotals {
        require(items.isNotEmpty()) { "Sale must contain at least one item." }

        var runningSubtotal = Money.ZERO
        var runningItemDiscount = Money.ZERO
        var runningItemTax = Money.ZERO

        val calculatedItems = items.map { itemCmd ->
            val product = productsMap[itemCmd.productId]
                ?: throw IllegalArgumentException("Product '${itemCmd.productId}' not found.")

            require(product.isActive) { "Product '${product.name}' is inactive." }
            require(itemCmd.quantity.isPositive()) { "Item quantity must be positive, got ${itemCmd.quantity}" }

            val unitPrice = itemCmd.unitPriceOverride ?: product.sellingPrice
            require(!unitPrice.isNegative()) { "Unit price cannot be negative, got $unitPrice" }

            val soldUnitId = itemCmd.soldUnitId ?: product.sellingUnitId

            // Calculate base deduction quantity
            val deductBaseQuantity = if (soldUnitId == product.sellingUnitId && !product.conversionFactor.isZero()) {
                itemCmd.quantity.multiply(product.conversionFactor)
            } else {
                itemCmd.quantity
            }

            // Line gross = unitPrice * quantity
            val grossAmount = unitPrice.multiply(itemCmd.quantity)

            // Line discount
            val lineDiscountAmount = financialCalculationService.calculateDiscount(grossAmount, itemCmd.discount)

            // Taxable basis = gross - discount
            val taxableAmount = if (lineDiscountAmount > grossAmount) Money.ZERO else grossAmount - lineDiscountAmount

            // Line tax
            val lineTaxAmount = financialCalculationService.calculateTax(taxableAmount, itemCmd.taxRule)

            // Net line amount = gross - discount + tax
            val netAmount = taxableAmount + lineTaxAmount

            val costAtSale = productCostsMap[itemCmd.productId] ?: Money.ZERO

            runningSubtotal += grossAmount
            runningItemDiscount += lineDiscountAmount
            runningItemTax += lineTaxAmount

            CalculatedSaleItem(
                productId = product.productId,
                productName = product.name,
                soldUnitId = soldUnitId,
                baseUnitId = product.baseUnitId,
                quantity = itemCmd.quantity,
                conversionFactor = product.conversionFactor,
                deductBaseQuantity = deductBaseQuantity,
                unitPrice = unitPrice,
                grossAmount = grossAmount,
                discount = lineDiscountAmount,
                tax = lineTaxAmount,
                netAmount = netAmount,
                costAtSale = costAtSale
            )
        }

        // Subtotal post item-level discounts
        val subtotalPostItemDiscount = if (runningItemDiscount > runningSubtotal) Money.ZERO else runningSubtotal - runningItemDiscount

        // Sale-level discount
        val saleDiscountAmount = financialCalculationService.calculateDiscount(subtotalPostItemDiscount, saleDiscount)

        // Taxable amount after all discounts
        val taxableAfterSaleDiscount = if (saleDiscountAmount > subtotalPostItemDiscount) Money.ZERO else subtotalPostItemDiscount - saleDiscountAmount

        // Sale-level tax
        val saleLevelTaxAmount = financialCalculationService.calculateTax(taxableAfterSaleDiscount, saleTaxRule)
        val totalTax = runningItemTax + saleLevelTaxAmount

        // Grand total
        val grandTotal = financialCalculationService.calculateGrandTotal(
            subtotal = runningSubtotal,
            totalDiscount = runningItemDiscount + saleDiscountAmount,
            totalTax = totalTax
        )

        // Process payments
        var totalCash = Money.ZERO
        var totalNonCash = Money.ZERO

        for (pmt in payments) {
            require(!pmt.amount.isNegative()) { "Payment amount cannot be negative, got ${pmt.amount}" }
            if (pmt.method == PaymentMethod.CASH) {
                totalCash += pmt.amount
            } else {
                totalNonCash += pmt.amount
            }
        }

        val totalPaid = totalCash + totalNonCash

        // Non-cash overpayment rule: Non-cash payments cannot exceed grand total
        if (totalNonCash > grandTotal) {
            throw IllegalArgumentException(
                "Non-cash payments (${totalNonCash.toFormattedRupees()}) exceed grand total (${grandTotal.toFormattedRupees()}). Overpayment is allowed only via CASH."
            )
        }

        val changeReturned: Money
        val appliedPaidAmount: Money
        val dueAmount: Money
        val paymentStatus: PaymentStatus

        if (totalPaid > grandTotal) {
            // Overpayment via cash
            changeReturned = totalPaid - grandTotal
            appliedPaidAmount = grandTotal
            dueAmount = Money.ZERO
            paymentStatus = PaymentStatus.PAID
        } else if (totalPaid == grandTotal) {
            changeReturned = Money.ZERO
            appliedPaidAmount = grandTotal
            dueAmount = Money.ZERO
            paymentStatus = PaymentStatus.PAID
        } else {
            // Underpayment / Partial / Unpaid
            changeReturned = Money.ZERO
            appliedPaidAmount = totalPaid
            dueAmount = grandTotal - totalPaid
            paymentStatus = if (totalPaid.isZero()) PaymentStatus.UNPAID else PaymentStatus.PARTIALLY_PAID

            // Credit rule: credit sale requires customerId
            if (customerId.isNullOrBlank()) {
                throw IllegalArgumentException(
                    "Credit sale with remaining due balance (${dueAmount.toFormattedRupees()}) requires a registered customer. Anonymous customer debt is not allowed."
                )
            }
        }

        return CalculatedSaleTotals(
            items = calculatedItems,
            subtotal = runningSubtotal,
            itemDiscount = runningItemDiscount,
            saleDiscount = saleDiscountAmount,
            tax = totalTax,
            grandTotal = grandTotal,
            payments = payments,
            totalCashPaid = totalCash,
            totalNonCashPaid = totalNonCash,
            totalPaid = totalPaid,
            appliedPaidAmount = appliedPaidAmount,
            changeReturned = changeReturned,
            dueAmount = dueAmount,
            paymentStatus = paymentStatus
        )
    }
}
