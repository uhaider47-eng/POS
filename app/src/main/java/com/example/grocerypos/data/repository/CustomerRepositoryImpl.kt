package com.example.grocerypos.data.repository

import com.example.grocerypos.data.local.dao.CustomerDao
import com.example.grocerypos.data.local.dao.CustomerLedgerDao
import com.example.grocerypos.data.local.entity.CustomerEntity
import com.example.grocerypos.data.local.mapper.toDomain
import com.example.grocerypos.domain.model.Customer
import com.example.grocerypos.domain.model.CustomerLedgerEntry
import com.example.grocerypos.domain.model.CustomerLedgerType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val customerLedgerDao: CustomerLedgerDao
) : CustomerRepository {

    override fun getActiveCustomersFlow(shopId: String): Flow<List<Customer>> {
        return customerDao.getActiveCustomersFlow(shopId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCustomerById(customerId: String): Customer? {
        return customerDao.getCustomerById(customerId)?.toDomain()
    }

    override suspend fun createCustomer(customer: Customer): Result<Unit> = runCatching {
        customerDao.insertCustomer(
            CustomerEntity(
                customerId = customer.customerId,
                shopId = customer.shopId,
                name = customer.name,
                phone = customer.phone,
                address = customer.address,
                creditLimit = customer.creditLimit,
                notes = customer.notes,
                isActive = customer.isActive,
                createdAt = customer.createdAt,
                updatedAt = customer.updatedAt
            )
        )
    }

    override suspend fun updateCustomer(customer: Customer): Result<Unit> = runCatching {
        customerDao.updateCustomer(
            CustomerEntity(
                customerId = customer.customerId,
                shopId = customer.shopId,
                name = customer.name,
                phone = customer.phone,
                address = customer.address,
                creditLimit = customer.creditLimit,
                notes = customer.notes,
                isActive = customer.isActive,
                createdAt = customer.createdAt,
                updatedAt = customer.updatedAt
            )
        )
    }

    override fun getCustomerLedgerEntriesFlow(customerId: String): Flow<List<CustomerLedgerEntry>> {
        return customerLedgerDao.getEntriesForCustomerFlow(customerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Calculates the customer's current outstanding balance from ledger entries.
     * SALE_CREDIT and OPENING_BALANCE increase debt, while PAYMENT and RETURN_CREDIT decrease debt.
     */
    override suspend fun getCustomerBalance(customerId: String): Money {
        val entries = customerLedgerDao.getEntriesForCustomer(customerId)
        var balance = Money.ZERO
        for (entry in entries) {
            when (entry.type) {
                CustomerLedgerType.SALE_CREDIT,
                CustomerLedgerType.OPENING_BALANCE,
                CustomerLedgerType.ADJUSTMENT -> balance += entry.amount

                CustomerLedgerType.PAYMENT,
                CustomerLedgerType.RETURN_CREDIT,
                CustomerLedgerType.REFUND_CREDIT -> balance -= entry.amount
            }
        }
        return balance
    }
}
