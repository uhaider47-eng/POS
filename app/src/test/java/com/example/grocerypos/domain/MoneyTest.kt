package com.example.grocerypos.domain

import com.example.grocerypos.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `minor unit representation is exact`() {
        val oneRupee = Money.fromRupees(1)
        assertEquals(100L, oneRupee.amountInMinorUnits)

        val price = Money.parseOrNull("100.25")
        assertEquals(10025L, price?.amountInMinorUnits)

        val paisas = Money.fromRupeesAndPaisas(10, 50)
        assertEquals(1050L, paisas.amountInMinorUnits)
    }

    @Test
    fun `money addition is exact and avoids floating point drift`() {
        // In IEEE 754 floating point, 100.25 + 50.50 can produce precision artifacts.
        // Money backed by Long minor units guarantees absolute precision.
        val a = Money.parseOrDefault("100.25")
        val b = Money.parseOrDefault("50.50")
        val sum = a + b

        assertEquals(15075L, sum.amountInMinorUnits)
        assertEquals("Rs. 150.75", sum.toFormattedRupees())
    }

    @Test
    fun `money subtraction calculates exact difference`() {
        val a = Money.parseOrDefault("100.25")
        val b = Money.parseOrDefault("50.50")
        val diff = a - b

        assertEquals(4975L, diff.amountInMinorUnits)
        assertEquals("Rs. 49.75", diff.toFormattedRupees())
    }

    @Test
    fun `money multiplication by integer scalar`() {
        val unitPrice = Money.parseOrDefault("25.50")
        val total = unitPrice * 4L

        assertEquals(10200L, total.amountInMinorUnits)
        assertEquals("Rs. 102", total.toFormattedRupees())
    }

    @Test
    fun `comparisons work correctly`() {
        val low = Money.parseOrDefault("10.00")
        val high = Money.parseOrDefault("20.00")
        val equalLow = Money.fromRupees(10)

        assertTrue(low < high)
        assertTrue(high > low)
        assertEquals(low, equalLow)
        assertTrue(low <= equalLow)
        assertTrue(low >= equalLow)
    }

    @Test
    fun `zero positive negative state predicates`() {
        val zero = Money.ZERO
        val positive = Money.fromMinorUnits(500)
        val negative = Money.fromMinorUnits(-500)

        assertTrue(zero.isZero())
        assertFalse(zero.isPositive())
        assertFalse(zero.isNegative())

        assertTrue(positive.isPositive())
        assertFalse(positive.isZero())
        assertFalse(positive.isNegative())

        assertTrue(negative.isNegative())
        assertFalse(negative.isPositive())
        assertFalse(negative.isZero())
    }

    @Test
    fun `safe string parsing handles various inputs`() {
        assertEquals(Money.fromMinorUnits(25000), Money.parseOrNull("250"))
        assertEquals(Money.fromMinorUnits(25050), Money.parseOrNull("250.5"))
        assertEquals(Money.fromMinorUnits(25050), Money.parseOrNull("250.50"))
        assertEquals(Money.fromMinorUnits(10025), Money.parseOrNull("Rs. 100.25"))
        assertEquals(Money.fromMinorUnits(10025), Money.parseOrNull("100.25 PKR"))
        assertEquals(Money.fromMinorUnits(-5000), Money.parseOrNull("-50.00"))

        assertNull(Money.parseOrNull("abc"))
        assertNull(Money.parseOrNull(""))
        assertNull(Money.parseOrNull(null))
        assertNull(Money.parseOrNull("100.25.50"))
    }

    @Test
    fun `formatting displays clean Pakistani Rupee notation`() {
        assertEquals("Rs. 250", Money.fromRupees(250).toFormattedRupees())
        assertEquals("Rs. 250.50", Money.parseOrDefault("250.50").toFormattedRupees())
        assertEquals("Rs. 0", Money.ZERO.toFormattedRupees())
        assertEquals("250.50", Money.parseOrDefault("250.50").toPlainDecimalString())
    }
}
