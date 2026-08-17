package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

/**
 * Command representing a line item in a sale operation.
 */
@Serializable
data class SaleItemCommand(
    val productId: String,
    val soldUnitId: String? = null,
    val quantity: Quantity,
    val unitPriceOverride: Money? = null,
    val discount: Discount = Discount.NONE,
    val taxRule: TaxRule = TaxRule.NONE
)

/**
 * Command representing a payment attempt in a sale transaction.
 */
@Serializable
data class PaymentCommand(
    val method: PaymentMethod,
    val amount: Money,
    val referenceNumber: String? = null
)

/**
 * Command representing an atomic sale completion request.
 */
@Serializable
data class CompleteSaleCommand(
    val operationId: String,
    val draftSaleId: String? = null,
    val shopId: String,
    val deviceId: String,
    val cashierId: String,
    val customerId: String? = null,
    val items: List<SaleItemCommand>,
    val saleDiscount: Discount = Discount.NONE,
    val taxRule: TaxRule = TaxRule.NONE,
    val payments: List<PaymentCommand> = emptyList(),
    val notes: String = "",
    val invoicePrefix: String = "INV-"
)

/**
 * Result returned upon atomic sale completion.
 */
@Serializable
data class CompleteSaleResult(
    val sale: Sale,
    val changeReturned: Money = Money.ZERO,
    val isIdempotentReplay: Boolean = false
)

/**
 * Command representing a request to hold (park) a sale.
 */
@Serializable
data class HoldSaleCommand(
    val saleId: String,
    val shopId: String,
    val deviceId: String,
    val cashierId: String,
    val customerId: String? = null,
    val items: List<SaleItemCommand>,
    val saleDiscount: Discount = Discount.NONE,
    val taxRule: TaxRule = TaxRule.NONE,
    val notes: String = ""
)
