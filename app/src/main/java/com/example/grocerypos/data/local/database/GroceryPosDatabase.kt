package com.example.grocerypos.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.grocerypos.data.local.converter.Converters
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
import com.example.grocerypos.data.local.entity.BarcodeEntity
import com.example.grocerypos.data.local.entity.CategoryEntity
import com.example.grocerypos.data.local.entity.CustomerEntity
import com.example.grocerypos.data.local.entity.DeviceEntity
import com.example.grocerypos.data.local.entity.PriceHistoryEntity
import com.example.grocerypos.data.local.entity.ProductEntity
import com.example.grocerypos.data.local.entity.RoleEntity
import com.example.grocerypos.data.local.entity.ShopEntity
import com.example.grocerypos.data.local.entity.StockBalanceEntity
import com.example.grocerypos.data.local.entity.StockMovementEntity
import com.example.grocerypos.data.local.entity.SupplierEntity
import com.example.grocerypos.data.local.entity.UnitEntity
import com.example.grocerypos.data.local.entity.UserEntity
import com.example.grocerypos.domain.model.RoleName
import com.example.grocerypos.domain.model.UnitCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        ShopEntity::class,
        DeviceEntity::class,
        RoleEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        UnitEntity::class,
        ProductEntity::class,
        BarcodeEntity::class,
        PriceHistoryEntity::class,
        StockBalanceEntity::class,
        StockMovementEntity::class,
        CustomerEntity::class,
        SupplierEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GroceryPosDatabase : RoomDatabase() {

    abstract fun shopDao(): ShopDao
    abstract fun deviceDao(): DeviceDao
    abstract fun roleDao(): RoleDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun unitDao(): UnitDao
    abstract fun productDao(): ProductDao
    abstract fun barcodeDao(): BarcodeDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun stockBalanceDao(): StockBalanceDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao

    companion object {
        const val DATABASE_NAME = "grocery_pos_db"

        fun buildDatabase(context: Context, scope: CoroutineScope): GroceryPosDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GroceryPosDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(DatabaseCallback(scope))
                .build()
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial standard Units and Roles upon first creation
                scope.launch(Dispatchers.IO) {
                    seedInitialSystemData(db)
                }
            }

            private fun seedInitialSystemData(db: SupportSQLiteDatabase) {
                // Initial Units
                val standardUnits = listOf(
                    Triple(UnitCode.PIECE.name, "Piece", "pc"),
                    Triple(UnitCode.GRAM.name, "Gram", "g"),
                    Triple(UnitCode.KILOGRAM.name, "Kilogram", "kg"),
                    Triple(UnitCode.MILLILITRE.name, "Millilitre", "ml"),
                    Triple(UnitCode.LITRE.name, "Litre", "L"),
                    Triple(UnitCode.PACK.name, "Pack", "pk"),
                    Triple(UnitCode.BOX.name, "Box", "bx"),
                    Triple(UnitCode.CARTON.name, "Carton", "ctn"),
                    Triple(UnitCode.DOZEN.name, "Dozen", "dz"),
                    Triple(UnitCode.BAG.name, "Bag", "bag")
                )

                for ((code, name, symbol) in standardUnits) {
                    val id = "unit_" + code.lowercase()
                    db.execSQL(
                        "INSERT OR IGNORE INTO units (unit_id, code, name, symbol, is_custom) VALUES ('$id', '$code', '$name', '$symbol', 0)"
                    )
                }

                // Initial Roles
                val standardRoles = listOf(
                    Triple(RoleName.OWNER.name, "Store Owner", "[\"all\"]"),
                    Triple(RoleName.MANAGER.name, "Store Manager", "[\"inventory\",\"sales\",\"reports\"]"),
                    Triple(RoleName.CASHIER.name, "Cashier", "[\"sales\"]"),
                    Triple(RoleName.STOCK_MANAGER.name, "Stock Manager", "[\"inventory\"]")
                )

                for ((name, desc, perms) in standardRoles) {
                    val id = "role_" + name.lowercase()
                    db.execSQL(
                        "INSERT OR IGNORE INTO roles (role_id, name, description, permissions_json) VALUES ('$id', '$name', '$desc', '$perms')"
                    )
                }
            }
        }
    }
}
