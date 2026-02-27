package com.example.pgk_food.shared.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.example.pgk_food.shared.data.local.dao.ScannedQrDao
import com.example.pgk_food.shared.data.local.dao.TransactionDao
import com.example.pgk_food.shared.data.local.dao.UserSessionDao
import com.example.pgk_food.shared.data.local.dao.MenuDao
import com.example.pgk_food.shared.data.local.dao.StudentKeyDao
import com.example.pgk_food.shared.data.local.dao.PermissionCacheDao
import com.example.pgk_food.shared.data.local.dao.OfflineCouponDao
import com.example.pgk_food.shared.data.local.entity.ScannedQrEntity
import com.example.pgk_food.shared.data.local.entity.UserSessionEntity
import com.example.pgk_food.shared.data.local.entity.MenuItemEntity
import com.example.pgk_food.shared.data.local.entity.OfflineTransactionEntity
import com.example.pgk_food.shared.data.local.entity.StudentKeyEntity
import com.example.pgk_food.shared.data.local.entity.PermissionCacheEntity
import com.example.pgk_food.shared.data.local.entity.OfflineCouponEntity

@Database(
    entities = [
        UserSessionEntity::class,
        ScannedQrEntity::class,
        OfflineTransactionEntity::class,
        MenuItemEntity::class,
        StudentKeyEntity::class,
        PermissionCacheEntity::class,
        OfflineCouponEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun scannedQrDao(): ScannedQrDao
    abstract fun transactionDao(): TransactionDao
    abstract fun menuDao(): MenuDao
    abstract fun studentKeyDao(): StudentKeyDao
    abstract fun permissionCacheDao(): PermissionCacheDao
    abstract fun offlineCouponDao(): OfflineCouponDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
