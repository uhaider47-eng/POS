package com.example.grocerypos.domain.model

import kotlinx.serialization.Serializable

/**
 * Unit-aware quantity measurement.
 *
 * Ensures that quantities are coupled to their physical unit of measurement
 * so that e.g. 1.500 kg and 1.500 pieces are never accidentally conflated.
 */
@Serializable
data class Measurement(
    val quantity: Quantity,
    val unitCode: UnitCode
) {
    fun format(): String {
        val unitLabel = when (unitCode) {
            UnitCode.PIECE -> "pc"
            UnitCode.GRAM -> "g"
            UnitCode.KILOGRAM -> "kg"
            UnitCode.MILLILITRE -> "ml"
            UnitCode.LITRE -> "L"
            UnitCode.PACK -> "pk"
            UnitCode.BOX -> "bx"
            UnitCode.CARTON -> "ctn"
            UnitCode.DOZEN -> "dz"
            UnitCode.BAG -> "bag"
            UnitCode.CUSTOM -> ""
        }
        return "${quantity.toFormattedString()} $unitLabel".trim()
    }

    companion object {
        fun of(quantity: Quantity, unitCode: UnitCode): Measurement = Measurement(quantity, unitCode)
    }
}
