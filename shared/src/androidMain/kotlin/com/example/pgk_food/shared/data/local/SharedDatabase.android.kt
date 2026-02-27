package com.example.pgk_food.shared.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private lateinit var appContext: Context

fun initAndroidDatabaseContext(context: Context) {
    appContext = context.applicationContext
}

actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    check(::appContext.isInitialized) { "Android database context is not initialized" }
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = "pgk_food_database"
    ).fallbackToDestructiveMigration(dropAllTables = true)
}
