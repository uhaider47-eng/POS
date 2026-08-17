package com.example.grocerypos.domain

import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Payment
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleItem
import com.example.grocerypos.domain.model.SaleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesDomainTest {

    @Test
    fun testSaleItemCalculationsWithPrecision() {
        val quantity = Quantity.fromScaledUnits(2500) // 2.500 kg
        val unitPrice = Money.fromRupees(120) // Rs. 120 per kg

        // Gross = 2.500 * 120 = 300.00 Rs (30000 minor units)
        val gross = unitPrice.multiply(quantity)
        assertEquals(Money.fromMinorUnits(30000), gross)
        assertEquals("300.00", gross.toFormattedString())

        val discount = Money.fromRupees(15)
        val net = gross.minus(discount)
        assertEquals(Money.fromRupees(285), net)

        val saleItem = SaleItem(
            saleItemId = "item-1",
            saleId = "sale-1",
            productId = "prod-1",
            productName = "Basmati Rice Super",
            soldUnitId = "unit_kg",
            quantity = quantity,
            unitPrice = unitPrice,
            grossAmount = gross,
            discount = discount,
            tax = Money.ZERO,
            netAmount = net,
            costAtSale = Money.fromRupees(95)
        )

        assertEquals(Quantity.fromScaledUnits(2500), saleItem.quantity)
        assertEquals(Money.fromRupees(285), saleItem.netAmount)
    }

    @Test
    fun testSaleFinancialTotalsAndPaymentStatus() {
        val item1 = SaleItem(
            saleItemId = "item-1",
            saleId = "sale-1",
            productId = "prod-1",
            productName = "Cooking Oil 1L",
            soldUnitId = "unit_piece",
            quantity = Quantity.fromWholeUnits(2),
            unitPrice = Money.fromRupees(500),
            grossAmount = Money.fromRupees(1000),
            discount = Money.ZERO,
            tax = Money.ZERO,
            netAmount = Money.fromRupees(1000)
        )

        val item2 = SaleItem(
            saleItemId = "item-2",
            saleId = "sale-1",
            productId = "prod-2",
            productName = "Sugar 1kg",
            soldUnitId = "unit_kg",
            quantity = Quantity.fromWholeUnits(3),
            unitPrice = Money.fromRupees(150),
            grossAmount = Money.fromRupees(450),
            discount = Money.ZERO,
            tax = Money.ZERO,
            netAmount = Money.fromRupees(450)
        )

        val subtotal = item1.netAmount.plus(item2.netAmount) // 1450.00
        val saleDiscount = Money.fromRupees(50)
        val grandTotal = subtotal.minus(saleDiscount) // 1400.00

        val payment1 = Payment(
            paymentId = "pmt-1",
            saleId = "sale-1",
            shopId = "shop-1",
            method = PaymentMethod.CASH,
            amount = Money.fromRupees(1000),
            receivedAt = 1000L,
            receivedBy = "user-1"
        )

        val payment2 = Payment(
            paymentId = "pmt-2",
            saleId = "sale-1",
            shopId = "shop-1",
            method = PaymentMethod.EASYPAISA,
            amount = Money.fromRupees(400),
            receivedAt = 1000L,
            receivedBy = "user-1"
        )

        val totalPaid = payment1.amount.plus(payment2.amount) // 1400.00
        val due = grandTotal.minus(totalPaid) // 0.00

        val sale = Sale(
            saleId = "sale-1",
            shopId = "shop-1",
            deviceId = "dev-1",
            invoiceNumber = "INV-000100",
            cashierId = "user-1",
            subtotal = subtotal,
            saleDiscount = saleDiscount,
            grandTotal = grandTotal,
            paidAmount = totalPaid,
            dueAmount = due,
            status = SaleStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            items = listOf(item1, item2),
            payments = listOf(payment1, payment2),
            createdAt = 1000L,
            completedAt = 1000L,
            updatedAt = 1000L
        )

        assertEquals(Money.fromRupees(1450), sale.subtotal)
        assertEquals(Money.fromRupees(1400), sale.grandTotal)
        assertEquals(Money.fromRupees(1400), sale.paidAmount)
        assertEquals(Money.ZERO, sale.dueAmount)
        assertEquals(PaymentStatus.PAID, sale.paymentStatus)
        assertEquals(SaleStatus.COMPLETED, sale.status)
        assertEquals(2, sale.items.size)
        assertEquals(2, sale.payments.size)
    }

    @Test
    fun testSaleStatusEnumValues() {
        val statuses = SaleStatus.values().map { it.name }.toSet()
        val expectedStatuses = setOf(
            "DRAFT",
            "HELD",
            "COMPLETED",
            "VOIDED",
            "PARTIALLY_RETURNED",
            "RETURNED"
        )
        assertEquals(expectedStatuses, statuses)
    }
}
