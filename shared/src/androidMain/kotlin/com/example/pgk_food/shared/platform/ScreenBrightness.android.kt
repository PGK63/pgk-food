package com.example.pgk_food.shared.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max

@Composable
actual fun PlatformQrBrightnessEffect(enabled: Boolean) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity, enabled) {
        if (!enabled || activity == null) {
            onDispose { }
        } else {
            val window = activity.window
            val originalBrightness = window.attributes.screenBrightness
            val boostedBrightness = if (originalBrightness in 0f..1f) {
                max(originalBrightness, 0.95f)
            } else {
                0.95f
            }

            window.attributes = window.attributes.apply {
                screenBrightness = boostedBrightness
            }

            onDispose {
                window.attributes = window.attributes.apply {
                    screenBrightness = originalBrightness
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
