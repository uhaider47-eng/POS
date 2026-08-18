package com.example.grocerypos.domain.model

sealed class SaleDomainException(message: String) : RuntimeException(message)

class EmptySaleException(message: String = "Sale must contain at least one item.") : SaleDomainException(message)

class InvalidQuantityException(message: String) : SaleDomainException(message)

class InvalidPriceException(message: String) : SaleDomainException(message)

class InvalidDiscountException(message: String) : SaleDomainException(message)

class InvalidPaymentAmountException(message: String) : SaleDomainException(message)

class DuplicatePaymentException(message: String) : SaleDomainException(message)

class CustomerRequiredForCreditException(
    message: String = "Credit sale with due balance requires a registered customer."
) : SaleDomainException(message)

class InvalidOverpaymentException(
    message: String = "Overpayment is only permitted via CASH payments."
) : SaleDomainException(message)

class InsufficientStockException(
    val productId: String,
    val productName: String,
    val requiredQuantity: Quantity,
    val availableQuantity: Quantity
) : SaleDomainException(
    "Insufficient stock for product '$productName' (ID: $productId). Required: ${requiredQuantity.toFormattedString()}, Available: ${availableQuantity.toFormattedString()}."
)

class ProductUnavailableException(message: String) : SaleDomainException(message)

class InvalidSaleStateException(message: String) : SaleDomainException(message)
