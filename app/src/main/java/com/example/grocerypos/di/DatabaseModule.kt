package com.example.grocerypos.di

import android.content.Context
import com.example.grocerypos.data.local.dao.BarcodeDao
import com.example.grocerypos.data.local.dao.CategoryDao
import com.example.grocerypos.data.local.dao.CustomerDao
import com.example.grocerypos.data.local.dao.DeviceDao
import com.example.grocerypos.data.local.dao.PriceHistoryDao
import com.example.grocerypos.data.local.dao.ProductDao
import com.example.grocerypos.data.local.dao.RoleDao
import com.example.grocerypos.data.local.dao.ShopDao
import com.example.grocerypos.data.local.dao.StockBalanceDao
import com.example.grocerypos.data.local.dao.StockMovementDao
import com.example.grocerypos.data.local.dao.SupplierDao
import com.example.grocerypos.data.local.dao.UnitDao
import com.example.grocerypos.data.local.dao.UserDao
import com.example.grocerypos.data.local.database.GroceryPosDatabase
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
}
