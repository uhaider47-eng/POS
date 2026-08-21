package com.example.grocerypos.domain.usecase

import com.example.grocerypos.domain.model.Sale
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.repository.SaleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes all held (parked) sales for a shop, including their items.
 */
@Singleton
class GetHeldSalesUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    operator fun invoke(shopId: String): Flow<List<Sale>> =
        saleRepository.getSalesWithDetailsByStatusFlow(shopId, SaleStatus.HELD)
}

/**
 * Retrieves a held sale by ID with its complete line items for resuming into the active POS cart.
 */
@Singleton
class ResumeHeldSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: String): Result<Sale> = runCatching {
        val sale = saleRepository.getSaleById(saleId)
            ?: throw IllegalArgumentException("Held sale '$saleId' not found.")
        if (sale.status != SaleStatus.HELD) {
            throw IllegalStateException("Sale '$saleId' is not in HELD status (current: ${sale.status}).")
        }
        sale
    }
}

/**
 * Auditably discards a held sale by setting its status to VOIDED via [VoidSaleUseCase].
 * Preserves audit logs and sync records with zero financial or stock reversal side effects.
 */
@Singleton
class DiscardHeldSaleUseCase @Inject constructor(
    private val voidSaleUseCase: VoidSaleUseCase
) {
    suspend operator fun invoke(
        saleId: String,
        cashierId: String,
        reason: String = "Discarded held sale from POS terminal"
    ): Result<Sale> {
        return voidSaleUseCase(
            VoidSaleCommand(
                saleId = saleId,
                cashierId = cashierId,
                reason = reason
            )
        )
    }
}
