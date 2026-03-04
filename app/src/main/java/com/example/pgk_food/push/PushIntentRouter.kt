package com.example.pgk_food.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.pgk_food.MainActivity

object PushIntentRouter {
    const val EXTRA_PUSH_ROUTE = "push_route"
    const val EXTRA_PUSH_TYPE = "push_type"
    private const val EXTRA_FCM_ROUTE = "route"
    private const val EXTRA_FCM_TYPE = "type"

    fun extractRoute(intent: Intent?): String? {
        return extractValue(intent, EXTRA_PUSH_ROUTE, EXTRA_FCM_ROUTE)
    }

    fun extractType(intent: Intent?): String? {
        return extractValue(intent, EXTRA_PUSH_TYPE, EXTRA_FCM_TYPE)
    }

    fun createContentIntent(
        context: Context,
        route: String?,
        type: String?,
    ): PendingIntent {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!route.isNullOrBlank()) putExtra(EXTRA_PUSH_ROUTE, route)
            if (!type.isNullOrBlank()) putExtra(EXTRA_PUSH_TYPE, type)
        }
        val requestCode = "${route.orEmpty()}:${type.orEmpty()}:${System.currentTimeMillis()}".hashCode()
        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun consumeHandledExtras(intent: Intent?) {
        intent?.removeExtra(EXTRA_PUSH_ROUTE)
        intent?.removeExtra(EXTRA_PUSH_TYPE)
        intent?.extras?.remove(EXTRA_FCM_ROUTE)
        intent?.extras?.remove(EXTRA_FCM_TYPE)
    }

    private fun extractValue(intent: Intent?, primaryKey: String, fallbackKey: String): String? {
        val direct = intent?.getStringExtra(primaryKey)?.trim()?.takeIf { it.isNotEmpty() }
        if (direct != null) return direct
        return intent?.extras?.getString(fallbackKey)?.trim()?.takeIf { it.isNotEmpty() }
    }
}

