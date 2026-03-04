package com.example.pgk_food.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object PushNotificationChannels {
    const val GENERAL_CHANNEL_ID = "pgk_food_general"
    private const val GENERAL_CHANNEL_NAME = "Уведомления ПГК Питание"
    private const val GENERAL_CHANNEL_DESCRIPTION = "Напоминания и важные действия"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(GENERAL_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            GENERAL_CHANNEL_ID,
            GENERAL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = GENERAL_CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }
}
