package com.example.grocerypos.domain.usecase

/**
 * Execution stages of the sale completion pipeline where controlled failure injection
 * can be tested to verify complete transaction rollback and idempotency guarantees.
 */
enum class SaleExecutionStage {
    BEFORE_STOCK_DEDUCTION,
    AFTER_STOCK_DEDUCTION,
    BEFORE_STOCK_MOVEMENT,
    AFTER_STOCK_MOVEMENT,
    BEFORE_INVOICE_ALLOCATION,
    AFTER_INVOICE_ALLOCATION,
    BEFORE_SALE_INSERTION,
    AFTER_SALE_INSERTION,
    BEFORE_SALE_ITEMS_INSERTION,
    AFTER_SALE_ITEMS_INSERTION,
    BEFORE_PAYMENTS_INSERTION,
    AFTER_PAYMENTS_INSERTION,
    BEFORE_LEDGER_INSERTION,
    AFTER_LEDGER_INSERTION,
    BEFORE_CASH_MOVEMENT_INSERTION,
    AFTER_CASH_MOVEMENT_INSERTION,
    BEFORE_AUDIT_LOG_INSERTION,
    AFTER_AUDIT_LOG_INSERTION,
    BEFORE_SYNC_EVENT_INSERTION,
    AFTER_SYNC_EVENT_INSERTION,
    BEFORE_OPERATION_RECORD_INSERTION,
    AFTER_OPERATION_RECORD_INSERTION
}

/**
 * Testable hook interface allowing tests to inject failures at exact pipeline stages.
 * Defaults to a no-op in production.
 */
fun interface SaleFailureHook {
    fun onStage(stage: SaleExecutionStage)
}
