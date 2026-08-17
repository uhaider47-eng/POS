package com.example.grocerypos.data.local.database

import androidx.room.withTransaction
import com.example.grocerypos.domain.transaction.TransactionRunner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [TransactionRunner].
 * Executes the given suspension block inside an atomic Room database transaction.
 */
@Singleton
class RoomTransactionRunner @Inject constructor(
    private val database: GroceryPosDatabase
) : TransactionRunner {

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction {
            block()
        }
    }
}
