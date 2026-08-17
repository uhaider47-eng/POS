package com.example.grocerypos.domain.transaction

/**
 * Interface defining the execution boundary for atomic multi-table business transactions.
 *
 * All multi-entity modifications (such as saving a product with barcodes, price history,
 * and stock balance, or processing future sales/purchases/ledgers) must be executed
 * within a [runInTransaction] block to guarantee ACID semantics (complete success or full rollback).
 */
interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}
