package com.example.pgk_food.shared.data.local

import androidx.room.RoomDatabaseConstructor

actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    actual override fun initialize(): AppDatabase {
        return createAppDatabaseBuilder().build()
    }
}
