package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

enum class DiscountType {
    FIXED_AMOUNT,
    PERCENTAGE
}

/**
 * Immutable discount representation.
 * Supports:
 * - FIXED_AMOUNT: specified directly as [fixedAmount] (e.g. Rs. 50.00 = 5000 minor units).
 * - PERCENTAGE: specified as basis points (e.g. 10.00% = 1000 basis points, 5.50% = 550 basis points).
 */
@Serializable
data class Discount(
    val type: DiscountType,
    val fixedAmount: Money = Money.ZERO,
    val percentageBasisPoints: Long = 0L // 1% = 100 bps, 10% = 1000 bps, 100% = 10000 bps
) {
    companion object {
        val NONE = Discount(DiscountType.FIXED_AMOUNT, Money.ZERO, 0L)

        fun fixed(amount: Money): Discount {
            require(!amount.isNegative()) { "Discount amount cannot be negative" }
            return Discount(DiscountType.FIXED_AMOUNT, fixedAmount = amount)
        }

        fun percentage(percent: Long): Discount {
            require(percent in 0..100) { "Percentage must be between 0 and 100" }
            return Discount(DiscountType.PERCENTAGE, percentageBasisPoints = percent * 100L)
        }

        fun percentageWithBasisPoints(bps: Long): Discount {
            require(bps in 0..10000) { "Basis points must be between 0 and 10000" }
            return Discount(DiscountType.PERCENTAGE, percentageBasisPoints = bps)
        }
    }
}
