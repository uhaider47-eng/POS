package com.example.grocerypos.di

import android.content.Context
import com.example.grocerypos.data.local.dao.AuditLogDao
import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CashMovementDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.CustomerDao
import com.example.grocerypos.data.local.dao.CustomerLedgerDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.InvoiceSequenceDao
import com.example.grocerypos.data.local.dao.PaymentDao
import com.example.grocerypos.data.local.dao.PriceHistoryDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.RoleDao
import com.example.grocerypos.data.local.dao.SaleDao
import com.example.grocerypos.data.local.dao.SaleItemDao
import com.example.grocerypos.data.local.dao.SaleOperationDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.StockMovementDao
import com.example.grocerypos.data.local.dao.SupplierDao
import com.example.grocerypos.data.local.dao.SyncEventDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.dao.UserDao
import com.example.grocerypos.data.local.database.GroceryPosDatabase
import com.example.grocerypos.data.local.database.RoomTransactionRunner
import com.example.grocerypos.domain.transaction.TransactionRunner
import com.example.grocerypos.domain.usecase.SaleFailureHook
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideGroceryPosDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): GroceryPosDatabase {
        return GroceryPosDatabase.buildDatabase(context, scope)
    }

    @Provides
    @Singleton
    fun provideTransactionRunner(db: GroceryPosDatabase): TransactionRunner {
        return RoomTransactionRunner(db)
    }

    @Provides
    fun provideShopDao(db: GroceryPosDatabase): ShopDao = db.shopDao()

    @Provides
    fun provideDeviceDao(db: GroceryPosDatabase): DeviceDao = db.deviceDao()

    @Provides
    fun provideRoleDao(db: GroceryPosDatabase): RoleDao = db.roleDao()

    @Provides
    fun provideUserDao(db: GroceryPosDatabase): UserDao = db.userDao()

    @Provides
    fun provideCategoryDao(db: GroceryPosDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideUnitDao(db: GroceryPosDatabase): UnitDao = db.unitDao()

    @Provides
    fun provideProductDao(db: GroceryPosDatabase): ProductDao = db.productDao()

    @Provides
    fun provideBarcodeDao(db: GroceryPosDatabase): BarcodeDao = db.barcodeDao()

    @Provides
    fun providePriceHistoryDao(db: GroceryPosDatabase): PriceHistoryDao = db.priceHistoryDao()

    @Provides
    fun provideStockBalanceDao(db: GroceryPosDatabase): StockBalanceDao = db.stockBalanceDao()

    @Provides
    fun provideStockMovementDao(db: GroceryPosDatabase): StockMovementDao = db.stockMovementDao()

    @Provides
    fun provideCustomerDao(db: GroceryPosDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideSupplierDao(db: GroceryPosDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun provideSaleDao(db: GroceryPosDatabase): SaleDao = db.saleDao()

    @Provides
    fun provideSaleItemDao(db: GroceryPosDatabase): SaleItemDao = db.saleItemDao()

    @Provides
    fun providePaymentDao(db: GroceryPosDatabase): PaymentDao = db.paymentDao()

    @Provides
    fun provideCustomerLedgerDao(db: GroceryPosDatabase): CustomerLedgerDao = db.customerLedgerDao()

    @Provides
    fun provideCashMovementDao(db: GroceryPosDatabase): CashMovementDao = db.cashMovementDao()

    @Provides
    fun provideAuditLogDao(db: GroceryPosDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideSyncEventDao(db: GroceryPosDatabase): SyncEventDao = db.syncEventDao()

    @Provides
    fun provideInvoiceSequenceDao(db: GroceryPosDatabase): InvoiceSequenceDao = db.invoiceSequenceDao()

    @Provides
    fun provideSaleOperationDao(db: GroceryPosDatabase): SaleOperationDao = db.saleOperationDao()

    @Provides
    @Singleton
    fun provideSaleFailureHook(): SaleFailureHook = SaleFailureHook { }
}
