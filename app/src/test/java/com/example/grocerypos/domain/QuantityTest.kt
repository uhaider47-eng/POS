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
        assertEquals("3.75", mult.toFormattedString())

        val div = mult / 3L
        assertEquals(1250L, div.amountInScaledUnits)
        assertEquals("1.25", div.toFormattedString())
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
