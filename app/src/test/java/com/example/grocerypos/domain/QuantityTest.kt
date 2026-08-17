package com.example.grocerypos.domain

import com.example.grocerypos.domain.model.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityTest {

    @Test
    fun `scale 3 internal representation is exact`() {
        val oneUnit = Quantity.fromWholeUnits(1)
        assertEquals(1000L, oneUnit.amountInScaledUnits)

        val oneAndHalf = Quantity.parseOrNull("1.5")
        assertEquals(1500L, oneAndHalf?.amountInScaledUnits)

        val quarterKg = Quantity.parseOrNull("0.25")
        assertEquals(250L, quarterKg?.amountInScaledUnits)

        val preciseThreeDecimals = Quantity.parseOrNull("2.750")
        assertEquals(2750L, preciseThreeDecimals?.amountInScaledUnits)
    }

    @Test
    fun `quantity addition and subtraction`() {
        val q1 = Quantity.parseOrDefault("1.5")
        val q2 = Quantity.parseOrDefault("0.75")

        val sum = q1 + q2
        assertEquals(2250L, sum.amountInScaledUnits)
        assertEquals("2.25", sum.toFormattedString())

        val diff = q1 - q2
        assertEquals(750L, diff.amountInScaledUnits)
        assertEquals("0.75", diff.toFormattedString())
    }

    @Test
    fun `quantity scalar multiplication and division`() {
        val q = Quantity.parseOrDefault("1.25")
        val mult = q * 3L
        assertEquals(3750L, mult.amountInScaledUnits)
        assertEquals("1.25", (mult / 3L).toFormattedString())
    }

    @Test
    fun `quantity division exact divisions`() {
        val sixUnits = Quantity.fromWholeUnits(6)
        val div2 = sixUnits / 2L
        assertEquals(3000L, div2.amountInScaledUnits)
        assertEquals("3", div2.toFormattedString())

        val tenUnits = Quantity.fromWholeUnits(10)
        val div4 = tenUnits / 4L
        assertEquals(2500L, div4.amountInScaledUnits)
        assertEquals("2.5", div4.toFormattedString())

        val fractional = Quantity.parseOrDefault("1.250")
        val div5 = fractional / 5L
        assertEquals(250L, div5.amountInScaledUnits)
        assertEquals("0.25", div5.toFormattedString())
    }

    @Test
    fun `quantity division repeating fractions with deterministic HALF_UP rounding`() {
        // 1.000 / 3 = 0.33333... -> 333 scaled units (0.333)
        val oneThird = Quantity.ONE / 3L
        assertEquals(333L, oneThird.amountInScaledUnits)
        assertEquals("0.333", oneThird.toFormattedString())

        // 2.000 / 3 = 0.66666... -> 667 scaled units (0.667)
        val twoThirds = Quantity.fromWholeUnits(2) / 3L
        assertEquals(667L, twoThirds.amountInScaledUnits)
        assertEquals("0.667", twoThirds.toFormattedString())

        // 10.000 / 3 = 3.33333... -> 3333 scaled units (3.333)
        val tenThirds = Quantity.fromWholeUnits(10) / 3L
        assertEquals(3333L, tenThirds.amountInScaledUnits)
        assertEquals("3.333", tenThirds.toFormattedString())

        // 5.000 / 3 = 1.66666... -> 1667 scaled units (1.667)
        val fiveThirds = Quantity.fromWholeUnits(5) / 3L
        assertEquals(1667L, fiveThirds.amountInScaledUnits)
        assertEquals("1.667", fiveThirds.toFormattedString())

        // 1.000 / 6 = 0.16666... -> 167 scaled units (0.167)
        val oneSixth = Quantity.ONE / 6L
        assertEquals(167L, oneSixth.amountInScaledUnits)
        assertEquals("0.167", oneSixth.toFormattedString())
    }

    @Test
    fun `quantity division below supported precision`() {
        // 0.001 (1 scaled unit) / 2 = 0.5 scaled units -> HALF_UP rounds to 1 scaled unit (0.001)
        val minUnit = Quantity.fromScaledUnits(1)
        val halfMin = minUnit / 2L
        assertEquals(1L, halfMin.amountInScaledUnits)
        assertEquals("0.001", halfMin.toFormattedString())

        // 0.001 (1 scaled unit) / 3 = 0.333 scaled units -> rounds down to 0 scaled units (0.000)
        val thirdMin = minUnit / 3L
        assertEquals(0L, thirdMin.amountInScaledUnits)
        assertEquals("0", thirdMin.toFormattedString())

        // 0.002 (2 scaled units) / 3 = 0.667 scaled units -> rounds up to 1 scaled unit (0.001)
        val twoThirdsMin = Quantity.fromScaledUnits(2) / 3L
        assertEquals(1L, twoThirdsMin.amountInScaledUnits)
        assertEquals("0.001", twoThirdsMin.toFormattedString())
    }

    @Test(expected = ArithmeticException::class)
    fun `quantity division by zero throws ArithmeticException`() {
        Quantity.ONE / 0L
    }

    @Test(expected = ArithmeticException::class)
    fun `quantity division by Int zero throws ArithmeticException`() {
        Quantity.ONE / 0
    }

    @Test
    fun `quantity division with negative values maintains symmetric HALF_UP rounding`() {
        // -1.000 / 3 = -0.333
        val negOneThird = -Quantity.ONE / 3L
        assertEquals(-333L, negOneThird.amountInScaledUnits)
        assertEquals("-0.333", negOneThird.toFormattedString())

        // -2.000 / 3 = -0.667
        val negTwoThirds = -Quantity.fromWholeUnits(2) / 3L
        assertEquals(-667L, negTwoThirds.amountInScaledUnits)
        assertEquals("-0.667", negTwoThirds.toFormattedString())

        // 1.000 / -3 = -0.333
        val posOverNeg = Quantity.ONE / -3L
        assertEquals(-333L, posOverNeg.amountInScaledUnits)
        assertEquals("-0.333", posOverNeg.toFormattedString())

        // -1.000 / -3 = 0.333
        val negOverNeg = -Quantity.ONE / -3L
        assertEquals(333L, negOverNeg.amountInScaledUnits)
        assertEquals("0.333", negOverNeg.toFormattedString())
    }

    @Test
    fun `fixed-point unit conversion factor arithmetic without floating point`() {
        // Conversion factor: 1 Carton = 24 Pieces (conversionFactor = 24.000 = 24000 scaled units)
        val conversionFactor = Quantity.fromWholeUnits(24)
        val cartonsToSell = Quantity.fromWholeUnits(5) // 5 cartons

        // Selling unit quantity -> Base unit quantity: (5 * 24000) / 1000 = 120 pieces
        val baseQuantityScaled = Math.multiplyExact(cartonsToSell.amountInScaledUnits, conversionFactor.amountInScaledUnits) / Quantity.SCALE_FACTOR
        val baseQuantity = Quantity.fromScaledUnits(baseQuantityScaled)
        assertEquals(Quantity.fromWholeUnits(120), baseQuantity)
        assertEquals("120", baseQuantity.toFormattedString())

        // Base unit quantity -> Selling unit quantity: (120 * 1000 + 24000/2) / 24000 = 5 cartons
        val numerator = Math.multiplyExact(baseQuantity.amountInScaledUnits, Quantity.SCALE_FACTOR)
        val denominator = conversionFactor.amountInScaledUnits
        val sellingQuantityScaled = (numerator + denominator / 2L) / denominator
        val sellingQuantity = Quantity.fromScaledUnits(sellingQuantityScaled)
        assertEquals(Quantity.fromWholeUnits(5), sellingQuantity)
        assertEquals("5", sellingQuantity.toFormattedString())
    }

    @Test
    fun `quantity parsing and formatting`() {
        assertEquals("1", Quantity.fromWholeUnits(1).toFormattedString())
        assertEquals("1.5", Quantity.fromScaledUnits(1500).toFormattedString())
        assertEquals("0.25", Quantity.fromScaledUnits(250).toFormattedString())
        assertEquals("0.125", Quantity.fromScaledUnits(125).toFormattedString())
        assertEquals("0", Quantity.ZERO.toFormattedString())

        assertNull(Quantity.parseOrNull("invalid"))
        assertNull(Quantity.parseOrNull(""))
    }

    @Test
    fun `comparisons work properly`() {
        val low = Quantity.parseOrDefault("0.5")
        val high = Quantity.parseOrDefault("1.0")

        assertTrue(low < high)
        assertTrue(high > low)
        assertFalse(low.isZero())
        assertTrue(Quantity.ZERO.isZero())
    }
}
