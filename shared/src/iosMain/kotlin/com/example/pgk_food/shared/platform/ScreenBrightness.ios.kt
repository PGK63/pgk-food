package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlin.math.max
import platform.UIKit.UIScreen

@Composable
actual fun PlatformQrBrightnessEffect(enabled: Boolean) {
    DisposableEffect(enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val screen = UIScreen.mainScreen
            val originalBrightness = screen.brightness
            screen.brightness = max(originalBrightness, 0.95)

            onDispose {
                screen.brightness = originalBrightness
            }
        }
    }
}
