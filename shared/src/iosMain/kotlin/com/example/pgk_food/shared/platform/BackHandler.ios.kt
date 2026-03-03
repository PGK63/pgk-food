package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS hardware/system back is not used in this app shell.
}
