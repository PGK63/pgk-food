package com.example.pgk_food.shared.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSDocumentDirectory

actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentsPath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String
        ?: error("Documents directory is unavailable")
    val dbPath = "$documentsPath/pgk_food_database.db"
    return Room.databaseBuilder<AppDatabase>(name = dbPath)
        .fallbackToDestructiveMigration(dropAllTables = true)
}
