package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable

enum class NotificationPermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUESTED,
    UNSUPPORTED,
}

class NotificationPermissionController(
    val status: NotificationPermissionStatus,
    val canRequestPermission: Boolean,
    val requestPermission: (onResult: (NotificationPermissionStatus) -> Unit) -> Unit,
)

@Composable
expect fun rememberNotificationPermissionController(): NotificationPermissionController
