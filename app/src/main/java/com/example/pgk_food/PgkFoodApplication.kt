package com.example.pgk_food

import android.app.Application
import com.example.pgk_food.background.BackgroundKeysSyncScheduler
import com.example.pgk_food.push.PushNotificationChannels
import com.example.pgk_food.push.PushTokenSyncManager
import com.example.pgk_food.shared.data.local.initAndroidDatabaseContext
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.platform.initAndroidPlatformContext

class PgkFoodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAndroidDatabaseContext(this)
        initAndroidPlatformContext(this)
        SessionStore.ensureRestored()
        PushNotificationChannels.ensureCreated(this)
        PushTokenSyncManager.start(this)
        BackgroundKeysSyncScheduler.schedule(this)
    }
}
