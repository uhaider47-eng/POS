package com.example.grocerypos.domain.usecase

import com.example.grocerypos.domain.model.Category
import com.example.grocerypos.domain.model.Device
import com.example.grocerypos.domain.model.DeviceStatus
import com.example.grocerypos.domain.model.DeviceType
import com.example.grocerypos.domain.model.Money
import com.example.grocerypos.domain.model.Product
import com.example.grocerypos.domain.model.Shop
import com.example.grocerypos.domain.repository.CategoryRepository
import com.example.grocerypos.domain.repository.DeviceRepository
import com.example.grocerypos.domain.repository.InvoiceSequenceRepository
import com.example.grocerypos.domain.repository.ProductRepository
import com.example.grocerypos.domain.repository.ShopRepository
import com.example.grocerypos.domain.repository.UnitRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class SetupShopUseCase @Inject constructor(
    private val shopRepository: ShopRepository,
    private val deviceRepository: DeviceRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        shopName: String,
        ownerName: String,
        phone: String,
        address: String,
        deviceName: String,
        deviceType: DeviceType = DeviceType.TABLET
    ): Result<Pair<Shop, Device>> = runCatching {
        val now = System.currentTimeMillis()
        val shopId = UUID.randomUUID().toString()

        val shop = Shop(
            shopId = shopId,
            name = shopName.trim(),
            ownerName = ownerName.trim(),
            phone = phone.trim(),
            address = address.trim(),
            currency = "PKR",
            timezone = "Asia/Karachi",
            createdAt = now,
            updatedAt = now
        )
        shopRepository.createShop(shop).getOrThrow()

        // Automatically register current device as the Primary Device (Local Sync Hub)
        val deviceId = UUID.randomUUID().toString()
        val device = Device(
            deviceId = deviceId,
            shopId = shopId,
            deviceName = deviceName.trim().ifEmpty { "Main POS Terminal" },
            deviceType = deviceType,
            isPrimary = true,
            status = DeviceStatus.ACTIVE,
            createdAt = now,
            lastSeenAt = now
        )
        deviceRepository.registerDevice(device).getOrThrow()

        // Seed some initial grocery categories
        val defaultCategories = listOf(
            "Beverages & Tea",
            "Dairy & Eggs",
            "Flour, Rice & Pulses",
            "Cooking Oil & Ghee",
            "Snacks & Biscuits",
            "Spices & Seasoning",
            "Personal Care",
            "Cleaning & Household"
        )
        for (catName in defaultCategories) {
            val cat = Category(
                categoryId = UUID.randomUUID().toString(),
                shopId = shopId,
                name = catName,
                parentCategoryId = null,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
            categoryRepository.createCategory(cat)
        }

        Pair(shop, device)
    }
}

class GetShopUseCase @Inject constructor(
    private val shopRepository: ShopRepository
) {
    operator fun invoke(): Flow<Shop?> = shopRepository.getShopFlow()
}

class GetPrimaryDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(shopId: String): Flow<Device?> = deviceRepository.getPrimaryDeviceFlow(shopId)
}

class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(shopId: String): Flow<List<Product>> = productRepository.getProductsFlow(shopId)
}

class SearchProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(shopId: String, query: String): Flow<List<Product>> = productRepository.searchProductsFlow(shopId, query)
}

class SaveProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(product: Product, changedBy: String? = null): Result<kotlin.Unit> {
        if (product.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Product name cannot be blank."))
        }
        if (!product.sellingPrice.isPositive()) {
            return Result.failure(IllegalArgumentException("Selling price must be greater than zero."))
        }
        if (!product.conversionFactor.isPositive()) {
            return Result.failure(IllegalArgumentException("Conversion factor must be greater than zero."))
        }
        return productRepository.saveProduct(product, changedBy)
    }
}

class ToggleProductStatusUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String, currentActive: Boolean): Result<kotlin.Unit> {
        return productRepository.setProductActiveStatus(productId, !currentActive)
    }
}

/**
 * UseCase for allocating sequential invoice numbers atomically within a database transaction.
 * Ensures the primary device sequence increments safely and provides collision-free invoice numbers.
 */
class AllocateInvoiceNumberUseCase @Inject constructor(
    private val invoiceSequenceRepository: InvoiceSequenceRepository
) {
    suspend operator fun invoke(shopId: String, defaultPrefix: String = "INV-"): Result<String> {
        return invoiceSequenceRepository.allocateNextInvoiceNumber(shopId = shopId, defaultPrefix = defaultPrefix)
    }
}

