package com.example.pgk_food.shared.runtime

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.pgk_food.shared.platform.requireAndroidAppContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun appForegroundEvents(): Flow<Unit> = callbackFlow {
    val application = requireAndroidAppContext().applicationContext as Application
    var startedCount = 0
    var inForeground = false

    val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit

        override fun onActivityStarted(activity: Activity) {
            startedCount += 1
            if (!inForeground && startedCount > 0) {
                inForeground = true
                trySend(Unit)
            }
        }

        override fun onActivityStopped(activity: Activity) {
            startedCount = (startedCount - 1).coerceAtLeast(0)
            if (startedCount == 0) {
                inForeground = false
            }
        }
    }

    application.registerActivityLifecycleCallbacks(callbacks)
    awaitClose {
        application.unregisterActivityLifecycleCallbacks(callbacks)
    }
}
