package com.example.grocerypos.domain.usecase

import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetActiveCustomersUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {
    operator fun invoke(shopId: String): Flow<List<Customer>> =
        customerRepository.getActiveCustomersFlow(shopId)
}

class GetCustomerBalanceUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(customerId: String): Money =
        customerRepository.getCustomerBalance(customerId)
}

class CreateCustomerUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(
        shopId: String,
        name: String,
        phone: String,
        address: String = "",
        creditLimit: Money = Money.ZERO,
        notes: String = ""
    ): Result<Customer> = runCatching {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            throw IllegalArgumentException("Customer name cannot be blank.")
        }
        val now = System.currentTimeMillis()
        val customer = Customer(
            customerId = UUID.randomUUID().toString(),
            shopId = shopId,
            name = trimmedName,
            phone = phone.trim(),
            address = address.trim(),
            creditLimit = creditLimit,
            notes = notes.trim(),
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        customerRepository.createCustomer(customer).getOrThrow()
        customer
    }
}
