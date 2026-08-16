package com.example.grocerypos.di

import com.example.grocerypos.data.repository.CategoryRepositoryImpl
import com.example.grocerypos.data.repository.DeviceRepositoryImpl
import com.example.grocerypos.data.repository.ProductRepositoryImpl
import com.example.grocerypos.data.repository.ShopRepositoryImpl
import com.example.grocerypos.data.repository.UnitRepositoryImpl
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.DeviceRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindShopRepository(impl: ShopRepositoryImpl): ShopRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindUnitRepository(impl: UnitRepositoryImpl): UnitRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
}
