package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    return remember {
        NotificationPermissionController(
            status = NotificationPermissionStatus.UNSUPPORTED,
            canRequestPermission = false,
            requestPermission = { onResult ->
                onResult(NotificationPermissionStatus.UNSUPPORTED)
            },
        )
    }
}
