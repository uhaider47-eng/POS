package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

enum class TaxType {
    PERCENTAGE,
    FIXED
}

enum class TaxInclusiveMode {
    TAX_INCLUSIVE,
    TAX_EXCLUSIVE
}

/**
 * Tax-ready configuration. Tax is optional in Grocery POS.
 */
@Serializable
data class TaxRule(
    val taxRuleId: String,
    val name: String,
    val type: TaxType = TaxType.PERCENTAGE,
    val percentageBasisPoints: Long = 0L, // 17% GST = 1700 basis points
    val fixedAmount: Money = Money.ZERO,
    val inclusiveMode: TaxInclusiveMode = TaxInclusiveMode.TAX_EXCLUSIVE,
    val isActive: Boolean = true
) {
    companion object {
        val NONE = TaxRule(
            taxRuleId = "none",
            name = "No Tax",
            type = TaxType.PERCENTAGE,
            percentageBasisPoints = 0L,
            inclusiveMode = TaxInclusiveMode.TAX_EXCLUSIVE,
            isActive = false
        )
    }
}
