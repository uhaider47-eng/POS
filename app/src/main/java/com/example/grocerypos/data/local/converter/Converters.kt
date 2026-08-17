package com.example.grocerypos.data.local.converter

import androidx.room.TypeConverter
import com.example.grocerypos.domain.model.AuditAction
import com.example.grocerypos.domain.model.CashMovementType
import com.example.grocerypos.domain.model.CustomerLedgerType
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.MovementType
import com.example.grocerypos.domain.model.PaymentMethod
import com.example.grocerypos.domain.model.PaymentStatus
import com.example.grocerypos.domain.model.Quantity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.SaleStatus
import com.example.grocerypos.domain.model.SyncOperation
import com.example.grocerypos.domain.model.SyncStatus
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

    @TypeConverter
    fun fromSaleStatus(value: SaleStatus): String = value.name

    @TypeConverter
    fun toSaleStatus(value: String): SaleStatus = runCatching {
        SaleStatus.valueOf(value)
    }.getOrDefault(SaleStatus.DRAFT)

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = runCatching {
        PaymentStatus.valueOf(value)
    }.getOrDefault(PaymentStatus.UNPAID)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = runCatching {
        PaymentMethod.valueOf(value)
    }.getOrDefault(PaymentMethod.CASH)

    @TypeConverter
    fun fromCustomerLedgerType(value: CustomerLedgerType): String = value.name

    @TypeConverter
    fun toCustomerLedgerType(value: String): CustomerLedgerType = runCatching {
        CustomerLedgerType.valueOf(value)
    }.getOrDefault(CustomerLedgerType.SALE_CREDIT)

    @TypeConverter
    fun fromCashMovementType(value: CashMovementType): String = value.name

    @TypeConverter
    fun toCashMovementType(value: String): CashMovementType = runCatching {
        CashMovementType.valueOf(value)
    }.getOrDefault(CashMovementType.SALE_CASH)

    @TypeConverter
    fun fromAuditAction(value: AuditAction): String = value.name

    @TypeConverter
    fun toAuditAction(value: String): AuditAction = runCatching {
        AuditAction.valueOf(value)
    }.getOrDefault(AuditAction.SALE_CREATED)

    @TypeConverter
    fun fromSyncOperation(value: SyncOperation): String = value.name

    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation = runCatching {
        SyncOperation.valueOf(value)
    }.getOrDefault(SyncOperation.CREATE)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = runCatching {
        SyncStatus.valueOf(value)
    }.getOrDefault(SyncStatus.PENDING)
}
