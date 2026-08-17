package com.example.grocerypos.domain

import com.example.grocerypos.domain.model.Discount
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.TaxInclusiveMode
import com.example.grocerypos.domain.model.TaxRule
import com.example.grocerypos.domain.model.TaxType
import com.example.grocerypos.domain.service.FinancialCalculationService
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FinancialCalculationServiceTest {

    private lateinit var calculator: FinancialCalculationService

    @Before
    fun setUp() {
        calculator = FinancialCalculationService()
    }

    @Test
    fun `percentage calculation with half-up rounding`() {
        val base = Money.parseOrDefault("100.00")
        // 10% (1000 basis points)
        val tenPercent = calculator.calculatePercentage(base, 1000L)
        assertEquals(Money.parseOrDefault("10.00"), tenPercent)

        // 17% GST (1700 basis points) of Rs. 250.00
        val base250 = Money.parseOrDefault("250.00")
        val gst = calculator.calculatePercentage(base250, 1700L)
        assertEquals(Money.parseOrDefault("42.50"), gst)

        // Half-up rounding on fractional paisas: Rs. 10.35 * 15% (1500 bps) = 155.25 paisas -> 155 paisas
        val fractional = calculator.calculatePercentage(Money.parseOrDefault("10.35"), 1500L)
        assertEquals(Money.fromMinorUnits(155), fractional)
    }

    @Test
    fun `line total calculation for whole and fractional quantities`() {
        // 3 pieces @ Rs. 150.00 each = Rs. 450.00
        val line1 = calculator.calculateLineTotal(
            unitPrice = Money.parseOrDefault("150.00"),
            quantity = Quantity.fromWholeUnits(3)
        )
        assertEquals(Money.parseOrDefault("450.00"), line1)

        // 1.500 kg @ Rs. 250.50 per kg = Rs. 375.75
        val line2 = calculator.calculateLineTotal(
            unitPrice = Money.parseOrDefault("250.50"),
            quantity = Quantity.parseOrDefault("1.5")
        )
        assertEquals(Money.parseOrDefault("375.75"), line2)

        // 0.250 kg @ Rs. 800.00 per kg = Rs. 200.00
        val line3 = calculator.calculateLineTotal(
            unitPrice = Money.parseOrDefault("800.00"),
            quantity = Quantity.parseOrDefault("0.25")
        )
        assertEquals(Money.parseOrDefault("200.00"), line3)
    }

    @Test
    fun `line total with fixed and percentage discounts`() {
        // Line total Rs. 500 with fixed discount Rs. 50 = Rs. 450
        val lineWithFixedDiscount = calculator.calculateLineTotal(
            unitPrice = Money.parseOrDefault("500.00"),
            quantity = Quantity.ONE,
            discount = Discount.fixed(Money.parseOrDefault("50.00"))
        )
        assertEquals(Money.parseOrDefault("450.00"), lineWithFixedDiscount)

        // Line total Rs. 200 with 10% discount = Rs. 180
        val lineWithPercentDiscount = calculator.calculateLineTotal(
            unitPrice = Money.parseOrDefault("200.00"),
            quantity = Quantity.ONE,
            discount = Discount.percentage(10)
        )
        assertEquals(Money.parseOrDefault("180.00"), lineWithPercentDiscount)
    }

    @Test
    fun `discount cannot exceed base amount`() {
        val base = Money.parseOrDefault("100.00")
        val hugeDiscount = Discount.fixed(Money.parseOrDefault("150.00"))
        val calculated = calculator.calculateDiscount(base, hugeDiscount)
        assertEquals(base, calculated)
    }

    @Test
    fun `subtotal and grand total calculation`() {
        val lineItems = listOf(
            Money.parseOrDefault("150.00"),
            Money.parseOrDefault("375.75"),
            Money.parseOrDefault("200.00")
        )
        val subtotal = calculator.calculateSubtotal(lineItems)
        assertEquals(Money.parseOrDefault("725.75"), subtotal)

        val totalDiscount = Money.parseOrDefault("25.75")
        val tax = Money.parseOrDefault("50.00")

        val grandTotal = calculator.calculateGrandTotal(subtotal, totalDiscount, tax)
        assertEquals(Money.parseOrDefault("750.00"), grandTotal)
    }

    @Test
    fun `tax calculations inclusive vs exclusive`() {
        val amount = Money.parseOrDefault("1000.00")

        // 17% Exclusive GST: tax = 170.00
        val exclusiveTax = calculator.calculateTax(
            taxableAmount = amount,
            taxRule = TaxRule(
                taxRuleId = "gst17",
                name = "GST 17%",
                type = TaxType.PERCENTAGE,
                percentageBasisPoints = 1700L,
                inclusiveMode = TaxInclusiveMode.TAX_EXCLUSIVE,
                isActive = true
            )
        )
        assertEquals(Money.parseOrDefault("170.00"), exclusiveTax)

        // 17% Inclusive GST: tax = (1000 * 1700) / (10000 + 1700) = 1700000 / 11700 = 145.299 -> 145.30
        val inclusiveTax = calculator.calculateTax(
            taxableAmount = amount,
            taxRule = TaxRule(
                taxRuleId = "gst17inc",
                name = "GST 17% Inc",
                type = TaxType.PERCENTAGE,
                percentageBasisPoints = 1700L,
                inclusiveMode = TaxInclusiveMode.TAX_INCLUSIVE,
                isActive = true
            )
        )
        assertEquals(Money.parseOrDefault("145.30"), inclusiveTax)
    }
}
