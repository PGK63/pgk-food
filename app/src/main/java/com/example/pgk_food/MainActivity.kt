package com.example.pgk_food

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pgk_food.push.PushIntentRouter
import com.example.pgk_food.push.PushNotificationChannels
import com.example.pgk_food.push.PushTokenSyncManager
import com.example.pgk_food.shared.PgkSharedApp
import com.example.pgk_food.shared.util.PushRouteCoordinator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PushNotificationChannels.ensureCreated(this)
        PushTokenSyncManager.start(applicationContext)
        handlePushIntent(intent)
        enableEdgeToEdge()
        setContent {
            PgkSharedApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        val route = PushIntentRouter.extractRoute(intent)
        if (!route.isNullOrBlank()) {
            PushRouteCoordinator.publish(route)
        }
        PushIntentRouter.consumeHandledExtras(intent)
    }
}
