package com.example.grocerypos.domain

import com.example.grocerypos.domain.model.CustomerRequiredForCreditException
import com.example.grocerypos.domain.model.Discount
import com.example.grocerypos.domain.model.DiscountType
import com.example.grocerypos.domain.model.EmptySaleException
import com.example.grocerypos.domain.model.InvalidOverpaymentException
import com.example.grocerypos.domain.model.InvalidPaymentAmountException
import com.example.grocerypos.domain.model.InvalidPriceException
import com.example.grocerypos.domain.model.InvalidQuantityException
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.PaymentCommand
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.ProductUnavailableException
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.SaleItemCommand
import com.example.grocerypos.domain.model.TaxRule
import com.example.grocerypos.domain.model.TaxType
import com.example.grocerypos.domain.service.FinancialCalculationService
import com.example.grocerypos.domain.service.SaleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaleCalculatorTest {

    private lateinit var calculationService: FinancialCalculationService
    private lateinit var saleCalculator: SaleCalculator

    private val riceProduct = Product(
        productId = "prod-rice",
        shopId = "shop-1",
        name = "Super Kernel Basmati Rice",
        categoryId = "cat-grains",
        brand = "Guard",
        sku = "SKU-RICE-01",
        baseUnitId = "unit-kg",
        sellingUnitId = "unit-kg",
        conversionFactor = Quantity.fromWholeUnits(1),
        sellingPrice = Money.fromRupees(300), // Rs. 300.00 / kg
        minimumStock = Quantity.fromWholeUnits(10),
        isActive = true
    )

    private val oilCartonProduct = Product(
        productId = "prod-oil",
        shopId = "shop-1",
        name = "Habib Cooking Oil Carton",
        categoryId = "cat-oil",
        brand = "Habib",
        sku = "SKU-OIL-12L",
        baseUnitId = "unit-liter",
        sellingUnitId = "unit-carton",
        conversionFactor = Quantity.fromWholeUnits(12), // 1 carton = 12 liters
        sellingPrice = Money.fromRupees(6000), // Rs. 6000.00 per carton
        minimumStock = Quantity.fromWholeUnits(2),
        isActive = true
    )

    private val productsMap = mapOf(
        riceProduct.productId to riceProduct,
        oilCartonProduct.productId to oilCartonProduct
    )

    @Before
    fun setup() {
        calculationService = FinancialCalculationService()
        saleCalculator = SaleCalculator(calculationService)
    }

    @Test
    fun testBasicSaleCalculationWithoutDiscountsOrTaxes() {
        val items = listOf(
            SaleItemCommand(
                productId = riceProduct.productId,
                quantity = Quantity.fromScaledUnits(2500) // 2.5 kg
            )
        )

        // 2.5 * 300 = 750.00 Rs
        val totals = saleCalculator.calculateSale(
            items = items,
            productsMap = productsMap,
            payments = listOf(
                PaymentCommand(
                    method = PaymentMethod.CASH,
                    amount = Money.fromRupees(1000)
                )
            )
        )

        assertEquals(Money.fromRupees(750), totals.subtotal)
        assertEquals(Money.ZERO, totals.itemDiscount)
        assertEquals(Money.ZERO, totals.saleDiscount)
        assertEquals(Money.ZERO, totals.tax)
        assertEquals(Money.fromRupees(750), totals.grandTotal)
        assertEquals(Money.fromRupees(750), totals.appliedPaidAmount)
        assertEquals(Money.fromRupees(250), totals.changeReturned)
        assertEquals(Money.ZERO, totals.dueAmount)
        assertEquals(PaymentStatus.PAID, totals.paymentStatus)
        assertEquals(Quantity.fromScaledUnits(2500), totals.items[0].deductBaseQuantity)
    }

    @Test
    fun testCartonUnitConversionStockDeduction() {
        val items = listOf(
            SaleItemCommand(
                productId = oilCartonProduct.productId,
                soldUnitId = "unit-carton",
                quantity = Quantity.fromWholeUnits(2) // 2 cartons
            )
        )

        // 2 cartons * 12 liters/carton = 24 base liters deducted
        val totals = saleCalculator.calculateSale(
            items = items,
            productsMap = productsMap,
            payments = listOf(
                PaymentCommand(
                    method = PaymentMethod.BANK_TRANSFER,
                    amount = Money.fromRupees(12000)
                )
            )
        )

        assertEquals(Money.fromRupees(12000), totals.grandTotal)
        assertEquals(Quantity.fromWholeUnits(24), totals.items[0].deductBaseQuantity)
        assertEquals(PaymentStatus.PAID, totals.paymentStatus)
    }

    @Test
    fun testItemAndSaleLevelDiscountsAndTaxes() {
        val items = listOf(
            SaleItemCommand(
                productId = riceProduct.productId,
                quantity = Quantity.fromWholeUnits(10), // 10 kg * 300 = 3000.00 Rs
                discount = Discount(DiscountType.PERCENTAGE, 1000) // 10.00% discount = 300.00 Rs -> taxable 2700.00
            )
        )

        val totals = saleCalculator.calculateSale(
            items = items,
            productsMap = productsMap,
            saleDiscount = Discount(DiscountType.FLAT, 20000), // Rs. 200.00 flat -> taxable 2500.00
            saleTaxRule = TaxRule(TaxType.PERCENTAGE, 500, "GST 5%"), // 5% of 2500 = 125.00 Rs
            payments = listOf(
                PaymentCommand(PaymentMethod.CASH, Money.fromMinorUnits(262500)) // 2625.00 Rs
            )
        )

        assertEquals(Money.fromRupees(3000), totals.subtotal)
        assertEquals(Money.fromRupees(300), totals.itemDiscount)
        assertEquals(Money.fromRupees(200), totals.saleDiscount)
        assertEquals(Money.fromMinorUnits(12500), totals.tax) // 125.00 Rs
        assertEquals(Money.fromMinorUnits(262500), totals.grandTotal) // 2500 + 125 = 2625.00 Rs
        assertEquals(PaymentStatus.PAID, totals.paymentStatus)
    }

    @Test
    fun testSplitPaymentsAcrossMultipleMethods() {
        val items = listOf(
            SaleItemCommand(
                productId = riceProduct.productId,
                quantity = Quantity.fromWholeUnits(10) // 3000.00 Rs
            )
        )

        val totals = saleCalculator.calculateSale(
            items = items,
            productsMap = productsMap,
            payments = listOf(
                PaymentCommand(PaymentMethod.CASH, Money.fromRupees(1000)),
                PaymentCommand(PaymentMethod.JAZZCASH, Money.fromRupees(1000)),
                PaymentCommand(PaymentMethod.EASYPAISA, Money.fromRupees(1000))
            )
        )

        assertEquals(Money.fromRupees(3000), totals.grandTotal)
        assertEquals(Money.fromRupees(3000), totals.totalPaid)
        assertEquals(Money.ZERO, totals.dueAmount)
        assertEquals(PaymentStatus.PAID, totals.paymentStatus)
    }

    @Test(expected = InvalidOverpaymentException::class)
    fun testNonCashOverpaymentThrowsException() {
        val items = listOf(
            SaleItemCommand(
                productId = riceProduct.productId,
                quantity = Quantity.fromWholeUnits(1) // 300.00 Rs
            )
        )

        saleCalculator.calculateSale(
            items = items,
            productsMap = productsMap,
            payments = listOf(
                PaymentCommand(PaymentMethod.CREDIT_CARD, Money.fromRupees(500)) // Non-cash overpayment forbidden
            )
        )
    }

    @Test(expected = CustomerRequiredForCreditException::class)
    fun testCreditSaleWithoutCustomerThrowsException() {
        val items = listOf(
            SaleItemCommand(
                productId = riceProduct.productId,
                quantity = Quantity.fromWholeUnits(1) // 300.00 Rs
            )
        )

        saleCalculator.calculateSale(
            items = items,
            productsMap = productsMap,
            payments = listOf(
                PaymentCommand(PaymentMethod.CASH, Money.fromRupees(100)) // 200.00 Rs due
            ),
            customerId = null // Missing customer for credit sale!
        )
    }

    @Test
    fun testCreditSaleWithCustomerSucceeds() {
        val items = listOf(
            SaleItemCommand(
                productId = riceProduct.productId,
                quantity = Quantity.fromWholeUnits(1) // 300.00 Rs
            )
        )

        val totals = saleCalculator.calculateSale(
            items = items,
            productsMap = productsMap,
            payments = listOf(
                PaymentCommand(PaymentMethod.CASH, Money.fromRupees(100))
            ),
            customerId = "cust-123"
        )

        assertEquals(Money.fromRupees(300), totals.grandTotal)
        assertEquals(Money.fromRupees(100), totals.appliedPaidAmount)
        assertEquals(Money.fromRupees(200), totals.dueAmount)
        assertEquals(PaymentStatus.PARTIALLY_PAID, totals.paymentStatus)
    }

    @Test(expected = EmptySaleException::class)
    fun testEmptySaleThrowsException() {
        saleCalculator.calculateSale(
            items = emptyList(),
            productsMap = productsMap
        )
    }

    @Test(expected = ProductUnavailableException::class)
    fun testUnknownProductThrowsException() {
        saleCalculator.calculateSale(
            items = listOf(SaleItemCommand(productId = "unknown-id", quantity = Quantity.fromWholeUnits(1))),
            productsMap = productsMap
        )
    }
}
