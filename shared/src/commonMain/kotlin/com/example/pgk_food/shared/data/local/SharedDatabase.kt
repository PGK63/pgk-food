package com.example.pgk_food.shared.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

internal object SharedDatabase {
    val instance: AppDatabase by lazy {
        createAppDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }
}

expect fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
