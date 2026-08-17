package com.example.grocerypos.domain.repository

import com.example.grocerypos.domain.model.Barcode
import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Device
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.PriceHistory
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.model.StockBalance
import com.example.grocerypos.domain.model.Unit
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    fun getShopFlow(): Flow<Shop?>
    suspend fun getShop(): Shop?
    suspend fun createShop(shop: Shop): Result<Unit>
    suspend fun updateShop(shop: Shop): Result<Unit>
    suspend fun isShopConfigured(): Boolean
    suspend fun initializeShopAndPrimaryDevice(shop: Shop, primaryDevice: Device, ownerPin: String): Result<Unit>
}

interface DeviceRepository {
    fun getPrimaryDeviceFlow(shopId: String): Flow<Device?>
    suspend fun getPrimaryDevice(shopId: String): Device?
    suspend fun registerDevice(device: Device): Result<Unit>
    fun getAllDevicesFlow(shopId: String): Flow<List<Device>>
    suspend fun updateDeviceStatus(deviceId: String, status: DeviceStatus): Result<Unit>
}

interface CategoryRepository {
    fun getCategoriesFlow(shopId: String): Flow<List<Category>>
    suspend fun getActiveCategories(shopId: String): List<Category>
    suspend fun createCategory(category: Category): Result<Unit>
}

interface UnitRepository {
    fun getAllUnitsFlow(): Flow<List<Unit>>
    suspend fun getAllUnits(): List<Unit>
    suspend fun saveCustomUnit(unit: Unit): Result<Unit>
}

interface ProductRepository {
    fun getProductsFlow(shopId: String): Flow<List<Product>>
    fun searchProductsFlow(shopId: String, query: String): Flow<List<Product>>
    suspend fun getProductById(productId: String): Product?
    suspend fun getProductByBarcode(shopId: String, barcode: String): Product?
    suspend fun saveProduct(product: Product, priceChangedBy: String? = null): Result<Unit>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun setProductActiveStatus(productId: String, isActive: Boolean): Result<Unit>
    suspend fun isBarcodeTaken(shopId: String, barcode: String, excludeProductId: String? = null): Boolean
    fun getPriceHistoryFlow(productId: String): Flow<List<PriceHistory>>
    fun getStockBalanceFlow(productId: String): Flow<StockBalance?>
    fun getTotalProductCountFlow(shopId: String): Flow<Int>
    fun getActiveProductCountFlow(shopId: String): Flow<Int>
}
