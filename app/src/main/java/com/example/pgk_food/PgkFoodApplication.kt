package com.example.pgk_food

import android.app.Application
import com.example.pgk_food.shared.data.local.initAndroidDatabaseContext
import com.example.pgk_food.shared.platform.initAndroidPlatformContext

class PgkFoodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAndroidDatabaseContext(this)
        initAndroidPlatformContext(this)
    }
}
