package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable value object representing monetary amounts in Pakistani Rupee (PKR).
 *
 * Backed internally by [amountInMinorUnits] as a 64-bit signed [Long]
 * with 2 decimal places of precision (Paisas):
 *
 * 1 PKR = 100 minor units (Paisas)
 * Rs. 1.00   = 100
 * Rs. 10.50  = 1050
 * Rs. 100.25 = 10025
 *
 * NO floating-point arithmetic (Double/Float) is used in any calculation.
 */
@Serializable
data class Money(
    val amountInMinorUnits: Long
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        return Money(Math.addExact(amountInMinorUnits, other.amountInMinorUnits))
    }

    operator fun minus(other: Money): Money {
        return Money(Math.subtractExact(amountInMinorUnits, other.amountInMinorUnits))
    }

    operator fun times(factor: Long): Money {
        return Money(Math.multiplyExact(amountInMinorUnits, factor))
    }

    operator fun unaryMinus(): Money {
        return Money(-amountInMinorUnits)
    }

    override fun compareTo(other: Money): Int {
        return amountInMinorUnits.compareTo(other.amountInMinorUnits)
    }

    fun isZero(): Boolean = amountInMinorUnits == 0L
    fun isPositive(): Boolean = amountInMinorUnits > 0L
    fun isNegative(): Boolean = amountInMinorUnits < 0L

    fun abs(): Money = Money(kotlin.math.abs(amountInMinorUnits))

    /**
     * Converts to human-readable PKR display format.
     * E.g. "Rs. 250" or "Rs. 250.50" or "Rs. 100.25"
     */
    fun toFormattedRupees(includePrefix: Boolean = true): String {
        val isNeg = amountInMinorUnits < 0
        val absVal = kotlin.math.abs(amountInMinorUnits)
        val rupees = absVal / 100L
        val paisas = absVal % 100L

        val sign = if (isNeg) "-" else ""
        val prefix = if (includePrefix) "Rs. " else ""

        return if (paisas == 0L) {
            "$prefix$sign$rupees"
        } else {
            val paisasFormatted = if (paisas < 10) "0$paisas" else "$paisas"
            "$prefix$sign$rupees.$paisasFormatted"
        }
    }

    /**
     * Returns exact plain decimal string representation, e.g. "100.25" or "250.00"
     */
    fun toPlainDecimalString(): String {
        val isNeg = amountInMinorUnits < 0
        val absVal = kotlin.math.abs(amountInMinorUnits)
        val rupees = absVal / 100L
        val paisas = absVal % 100L
        val paisasFormatted = if (paisas < 10) "0$paisas" else "$paisas"
        val sign = if (isNeg) "-" else ""
        return "$sign$rupees.$paisasFormatted"
    }

    override fun toString(): String = toFormattedRupees()

    companion object {
        val ZERO = Money(0L)
        val ONE_RUPEE = Money(100L)

        fun fromMinorUnits(minorUnits: Long): Money = Money(minorUnits)

        fun fromRupees(rupees: Long): Money = Money(Math.multiplyExact(rupees, 100L))

        fun fromRupeesAndPaisas(rupees: Long, paisas: Long): Money {
            require(paisas in 0L..99L) { "Paisas must be between 0 and 99, got $paisas" }
            val sign = if (rupees < 0) -1L else 1L
            val total = Math.addExact(Math.multiplyExact(kotlin.math.abs(rupees), 100L), paisas)
            return Money(total * sign)
        }

        /**
         * Safely parses user input string (e.g. "250", "250.5", "100.25")
         * using deterministic string tokenization with NO Double conversion.
         */
        fun parseOrNull(rawInput: String?): Money? {
            if (rawInput.isNullOrBlank()) return null
            val clean = rawInput.trim().replace("Rs.", "", ignoreCase = true).replace("PKR", "", ignoreCase = true).replace(",", "").trim()
            if (clean.isEmpty()) return null

            val isNegative = clean.startsWith("-")
            val unsignedInput = if (isNegative) clean.substring(1) else clean

            val parts = unsignedInput.split(".")
            if (parts.size > 2) return null

            val rupeesPart = parts[0].ifEmpty { "0" }
            val rupees = rupeesPart.toLongOrNull() ?: return null

            val paisas: Long = if (parts.size == 2) {
                val frac = parts[1]
                when {
                    frac.isEmpty() -> 0L
                    frac.length == 1 -> (frac + "0").toLongOrNull() ?: return null
                    frac.length == 2 -> frac.toLongOrNull() ?: return null
                    else -> {
                        // Round 3rd digit to 2 decimals deterministically
                        val twoDigits = frac.substring(0, 2).toLongOrNull() ?: return null
                        val thirdDigit = frac[2].digitToIntOrNull() ?: return null
                        if (thirdDigit >= 5) twoDigits + 1L else twoDigits
                    }
                }
            } else {
                0L
            }

            val totalMinor = Math.addExact(Math.multiplyExact(rupees, 100L), paisas)
            return Money(if (isNegative) -totalMinor else totalMinor)
        }

        fun parseOrDefault(rawInput: String?, default: Money = ZERO): Money {
            return parseOrNull(rawInput) ?: default
        }
    }
}
