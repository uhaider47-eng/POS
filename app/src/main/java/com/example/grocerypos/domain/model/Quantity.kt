package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable value object representing business quantities in grocery operations
 * (e.g. 1 piece, 1.5 kg, 0.250 kg, 2.750 litres).
 *
 * Backed internally by [amountInScaledUnits] as a 64-bit signed [Long]
 * with a fixed scale of 3 decimal places ([SCALE_FACTOR] = 1000):
 *
 * 1.000 whole unit = 1000 scaled units
 * 1.500 kg         = 1500 scaled units
 * 0.250 kg         = 250 scaled units
 * 2.750 litres     = 2750 scaled units
 *
 * NO floating-point arithmetic (Double/Float) is used in internal calculations.
 */
@Serializable
data class Quantity(
    val amountInScaledUnits: Long
) : Comparable<Quantity> {

    operator fun plus(other: Quantity): Quantity {
        return Quantity(Math.addExact(amountInScaledUnits, other.amountInScaledUnits))
    }

    operator fun minus(other: Quantity): Quantity {
        return Quantity(Math.subtractExact(amountInScaledUnits, other.amountInScaledUnits))
    }

    operator fun times(scalar: Long): Quantity {
        return Quantity(Math.multiplyExact(amountInScaledUnits, scalar))
    }

    operator fun div(divisor: Long): Quantity {
        require(divisor != 0L) { "Cannot divide Quantity by zero" }
        return Quantity(amountInScaledUnits / divisor)
    }

    operator fun unaryMinus(): Quantity {
        return Quantity(-amountInScaledUnits)
    }

    override fun compareTo(other: Quantity): Int {
        return amountInScaledUnits.compareTo(other.amountInScaledUnits)
    }

    fun isZero(): Boolean = amountInScaledUnits == 0L
    fun isPositive(): Boolean = amountInScaledUnits > 0L
    fun isNegative(): Boolean = amountInScaledUnits < 0L

    fun abs(): Quantity = Quantity(kotlin.math.abs(amountInScaledUnits))

    /**
     * Formats to clean human-readable representation:
     * 1000 -> "1"
     * 1500 -> "1.5"
     * 250  -> "0.25"
     * 2750 -> "2.75"
     */
    fun toFormattedString(): String {
        val isNeg = amountInScaledUnits < 0
        val absVal = kotlin.math.abs(amountInScaledUnits)
        val whole = absVal / SCALE_FACTOR
        val frac = absVal % SCALE_FACTOR

        val sign = if (isNeg) "-" else ""

        if (frac == 0L) {
            return "$sign$whole"
        }

        // Format fractional part without trailing zeros
        val fracStr = when {
            frac % 100L == 0L -> "${frac / 100L}"
            frac % 10L == 0L -> String.format("%02d", frac / 10L)
            else -> String.format("%03d", frac)
        }

        return "$sign$whole.$fracStr"
    }

    fun toPlainDecimalString(): String {
        val isNeg = amountInScaledUnits < 0
        val absVal = kotlin.math.abs(amountInScaledUnits)
        val whole = absVal / SCALE_FACTOR
        val frac = absVal % SCALE_FACTOR
        val sign = if (isNeg) "-" else ""
        return "$sign$whole.${String.format("%03d", frac)}"
    }

    override fun toString(): String = toFormattedString()

    companion object {
        const val SCALE = 3
        const val SCALE_FACTOR = 1000L

        val ZERO = Quantity(0L)
        val ONE = Quantity(1000L)

        fun fromScaledUnits(scaledUnits: Long): Quantity = Quantity(scaledUnits)

        fun fromWholeUnits(wholeUnits: Long): Quantity = Quantity(Math.multiplyExact(wholeUnits, SCALE_FACTOR))

        /**
         * Safely parses decimal string representation (e.g. "1.5", "0.25", "3")
         * into fixed-scale [Quantity] without using floating-point types.
         */
        fun parseOrNull(rawInput: String?): Quantity? {
            if (rawInput.isNullOrBlank()) return null
            val clean = rawInput.trim().replace(",", "").trim()
            if (clean.isEmpty()) return null

            val isNegative = clean.startsWith("-")
            val unsignedInput = if (isNegative) clean.substring(1) else clean

            val parts = unsignedInput.split(".")
            if (parts.size > 2) return null

            val wholePart = parts[0].ifEmpty { "0" }
            val whole = wholePart.toLongOrNull() ?: return null

            val fracPart: Long = if (parts.size == 2) {
                val frac = parts[1]
                when {
                    frac.isEmpty() -> 0L
                    frac.length == 1 -> (frac + "00").toLongOrNull() ?: return null
                    frac.length == 2 -> (frac + "0").toLongOrNull() ?: return null
                    frac.length == 3 -> frac.toLongOrNull() ?: return null
                    else -> {
                        // Round 4th decimal digit
                        val threeDigits = frac.substring(0, 3).toLongOrNull() ?: return null
                        val fourthDigit = frac[3].digitToIntOrNull() ?: return null
                        if (fourthDigit >= 5) threeDigits + 1L else threeDigits
                    }
                }
            } else {
                0L
            }

            val totalScaled = Math.addExact(Math.multiplyExact(whole, SCALE_FACTOR), fracPart)
            return Quantity(if (isNegative) -totalScaled else totalScaled)
        }

        fun parseOrDefault(rawInput: String?, default: Quantity = ZERO): Quantity {
            return parseOrNull(rawInput) ?: default
        }
    }
}
