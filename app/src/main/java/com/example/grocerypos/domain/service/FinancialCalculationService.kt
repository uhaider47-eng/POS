package com.example.grocerypos.domain.service

import com.example.grocerypos.domain.model.Discount
import com.example.grocerypos.domain.model.DiscountType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.TaxInclusiveMode
import com.example.grocerypos.domain.model.TaxRule
import com.example.grocerypos.domain.model.TaxType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized financial calculation service.
 *
 * ROUNDING POLICY SPECIFICATION:
 * - Pakistani currency (PKR) is denominated in whole paisas (1 PKR = 100 minor units).
 * - All fractional arithmetic (such as basis-point percentages or quantity multipliers)
 *   uses deterministic FIXED-POINT HALF-UP integer rounding:
 *     roundHalfUp(N, D) = if (N >= 0) (N + D / 2) / D else (N - D / 2) / D
 * - No floating-point types (Double or Float) are permitted anywhere in calculations.
 * - This guarantees exact, reproducible financial precision immune to IEEE 754 precision drift.
 */
@Singleton
class FinancialCalculationService @Inject constructor() {

    /**
     * Calculates the percentage amount for a given [baseAmount] using [percentageBasisPoints].
     * 1% = 100 basis points, 10% = 1000 basis points, 100% = 10,000 basis points.
     */
    fun calculatePercentage(baseAmount: Money, percentageBasisPoints: Long): Money {
        if (baseAmount.isZero() || percentageBasisPoints == 0L) {
            return Money.ZERO
        }
        val numerator = Math.multiplyExact(baseAmount.amountInMinorUnits, percentageBasisPoints)
        val denominator = 10000L
        val roundedMinor = roundHalfUp(numerator, denominator)
        return Money(roundedMinor)
    }

    /**
     * Calculates the monetary discount amount for a given [baseAmount].
     * The resulting discount is clamped between [Money.ZERO] and [baseAmount].
     */
    fun calculateDiscount(baseAmount: Money, discount: Discount): Money {
        if (baseAmount.isZero() || baseAmount.isNegative()) {
            return Money.ZERO
        }

        val rawDiscount = when (discount.type) {
            DiscountType.FIXED_AMOUNT -> discount.fixedAmount
            DiscountType.PERCENTAGE -> calculatePercentage(baseAmount, discount.percentageBasisPoints)
        }

        // Clamp discount between 0 and baseAmount
        return when {
            rawDiscount.isNegative() -> Money.ZERO
            rawDiscount > baseAmount -> baseAmount
            else -> rawDiscount
        }
    }

    /**
     * Computes the line total given a [unitPrice] and [quantity] (Scale 3 = 1000),
     * applying any optional line-level [discount].
     */
    fun calculateLineTotal(
        unitPrice: Money,
        quantity: Quantity,
        discount: Discount = Discount.NONE
    ): Money {
        if (unitPrice.isZero() || quantity.isZero()) {
            return Money.ZERO
        }

        // unitPrice (minor units: 100) * quantity (scaled units: 1000) -> scaled by 1000
        val numerator = Math.multiplyExact(unitPrice.amountInMinorUnits, quantity.amountInScaledUnits)
        val denominator = Quantity.SCALE_FACTOR // 1000L
        val grossMinor = roundHalfUp(numerator, denominator)
        val grossTotal = Money(grossMinor)

        val discountAmount = calculateDiscount(grossTotal, discount)
        return grossTotal - discountAmount
    }

    /**
     * Calculates the subtotal of a list of line item totals.
     */
    fun calculateSubtotal(lineTotals: List<Money>): Money {
        var subtotal = Money.ZERO
        for (item in lineTotals) {
            subtotal += item
        }
        return subtotal
    }

    /**
     * Calculates the tax amount for a given [taxableAmount] under the specified [taxRule].
     */
    fun calculateTax(taxableAmount: Money, taxRule: TaxRule): Money {
        if (!taxRule.isActive || taxRule == TaxRule.NONE || taxableAmount.isZero()) {
            return Money.ZERO
        }

        return when (taxRule.type) {
            TaxType.FIXED -> taxRule.fixedAmount
            TaxType.PERCENTAGE -> {
                when (taxRule.inclusiveMode) {
                    TaxInclusiveMode.TAX_EXCLUSIVE -> {
                        calculatePercentage(taxableAmount, taxRule.percentageBasisPoints)
                    }
                    TaxInclusiveMode.TAX_INCLUSIVE -> {
                        // Inclusive tax = (taxableAmount * rate) / (10000 + rate)
                        val rate = taxRule.percentageBasisPoints
                        val numerator = Math.multiplyExact(taxableAmount.amountInMinorUnits, rate)
                        val denominator = 10000L + rate
                        val taxMinor = roundHalfUp(numerator, denominator)
                        Money(taxMinor)
                    }
                }
            }
        }
    }

    /**
     * Calculates the final grand total: Subtotal - TotalDiscount + TotalTax.
     */
    fun calculateGrandTotal(
        subtotal: Money,
        totalDiscount: Money = Money.ZERO,
        totalTax: Money = Money.ZERO
    ): Money {
        val discounted = if (totalDiscount > subtotal) Money.ZERO else subtotal - totalDiscount
        return discounted + totalTax
    }

    /**
     * Centralized Half-Up integer rounding:
     * (N + D / 2) / D for positive numbers.
     */
    private fun roundHalfUp(numerator: Long, denominator: Long): Long {
        require(denominator > 0L) { "Denominator must be positive" }
        return if (numerator >= 0L) {
            (numerator + denominator / 2L) / denominator
        } else {
            (numerator - denominator / 2L) / denominator
        }
    }
}
