package com.example.pgk_food.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pgk_food.R

object PushMessageNotifier {
    fun show(
        context: Context,
        title: String?,
        body: String?,
        route: String?,
        type: String?,
    ) {
        if (!canPostNotifications(context)) return

        val safeTitle = title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name)
        val safeBody = body?.takeIf { it.isNotBlank() } ?: "Откройте приложение для деталей."
        val pendingIntent = PushIntentRouter.createContentIntent(context, route, type)

        val notification = NotificationCompat.Builder(context, PushNotificationChannels.GENERAL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(safeTitle)
            .setContentText(safeBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(safeBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(generateNotificationId(), notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun generateNotificationId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }
}

