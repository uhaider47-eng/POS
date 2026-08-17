package com.example.grocerypos.data.local.converter

import androidx.room.TypeConverter
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.MovementType
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.UnitCode

class Converters {

    // --- Money Converter (Stores as minor units: 100 minor units = Rs. 1.00) ---
    @TypeConverter
    fun fromMoney(money: Money?): Long? = money?.amountInMinorUnits

    @TypeConverter
    fun toMoney(value: Long?): Money? = value?.let { Money(it) }

    // --- Quantity Converter (Stores as scaled units: 1000 scaled units = 1.000 whole unit) ---
    @TypeConverter
    fun fromQuantity(quantity: Quantity?): Long? = quantity?.amountInScaledUnits

    @TypeConverter
    fun toQuantity(value: Long?): Quantity? = value?.let { Quantity(it) }

    // --- Enum Converters ---
    @TypeConverter
    fun fromDeviceType(value: DeviceType): String = value.name

    @TypeConverter
    fun toDeviceType(value: String): DeviceType = runCatching {
        DeviceType.valueOf(value)
    }.getOrDefault(DeviceType.TABLET)

    @TypeConverter
    fun fromDeviceStatus(value: DeviceStatus): String = value.name

    @TypeConverter
    fun toDeviceStatus(value: String): DeviceStatus = runCatching {
        DeviceStatus.valueOf(value)
    }.getOrDefault(DeviceStatus.ACTIVE)

    @TypeConverter
    fun fromRoleName(value: RoleName): String = value.name

    @TypeConverter
    fun toRoleName(value: String): RoleName = runCatching {
        RoleName.valueOf(value)
    }.getOrDefault(RoleName.CASHIER)

    @TypeConverter
    fun fromUnitCode(value: UnitCode): String = value.name

    @TypeConverter
    fun toUnitCode(value: String): UnitCode = runCatching {
        UnitCode.valueOf(value)
    }.getOrDefault(UnitCode.PIECE)

    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = runCatching {
        MovementType.valueOf(value)
    }.getOrDefault(MovementType.SALE)
}
