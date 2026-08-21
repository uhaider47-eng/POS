package com.example.grocerypos.domain.repository

import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.CustomerLedgerEntry
import com.example.grocerypos.domain.model.Money
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getActiveCustomersFlow(shopId: String): Flow<List<Customer>>
    suspend fun getCustomerById(customerId: String): Customer?
    suspend fun createCustomer(customer: Customer): Result<Unit>
    suspend fun updateCustomer(customer: Customer): Result<Unit>
    fun getCustomerLedgerEntriesFlow(customerId: String): Flow<List<CustomerLedgerEntry>>
    suspend fun getCustomerBalance(customerId: String): Money
}
