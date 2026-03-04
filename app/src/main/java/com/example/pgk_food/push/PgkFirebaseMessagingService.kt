package com.example.pgk_food.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PgkFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        PushNotificationChannels.ensureCreated(applicationContext)
        PushTokenSyncManager.start(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushTokenSyncManager.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val route = message.data["route"]?.trim()
        val type = message.data["type"]?.trim()
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]

        PushNotificationChannels.ensureCreated(applicationContext)
        PushMessageNotifier.show(
            context = applicationContext,
            title = title,
            body = body,
            route = route,
            type = type,
        )
    }
}

