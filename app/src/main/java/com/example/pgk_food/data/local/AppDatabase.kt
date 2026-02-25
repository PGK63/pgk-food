package com.example.pgk_food.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pgk_food.data.local.dao.*
import com.example.pgk_food.data.local.entity.*

@Database(
    entities = [
        UserSessionEntity::class, 
        ScannedQrEntity::class, 
        OfflineTransactionEntity::class, 
        MenuItemEntity::class,
        StudentKeyEntity::class,
        PermissionCacheEntity::class
    ], 
    version = 6
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun scannedQrDao(): ScannedQrDao
    abstract fun transactionDao(): TransactionDao
    abstract fun menuDao(): MenuDao
    abstract fun studentKeyDao(): StudentKeyDao
    abstract fun permissionCacheDao(): PermissionCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pgk_food_database"
                )
                    .fallbackToDestructiveMigration() // Simplified for now
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
