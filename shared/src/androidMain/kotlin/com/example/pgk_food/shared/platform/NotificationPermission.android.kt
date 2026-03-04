package com.example.pgk_food.shared.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private const val PREFS_NAME = "notification_permission_state"
private const val KEY_NOTIFICATIONS_REQUESTED = "post_notifications_requested"

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    val context = LocalContext.current.applicationContext
    var status by remember(context) { mutableStateOf(resolveNotificationPermissionStatus(context)) }
    var resultCallback by remember { mutableStateOf<((NotificationPermissionStatus) -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            markNotificationsPermissionRequested(context)
            val resolved = resolveNotificationPermissionStatus(context)
            status = resolved
            resultCallback?.invoke(resolved)
            resultCallback = null
        },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = resolveNotificationPermissionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(context) {
        status = resolveNotificationPermissionStatus(context)
    }

    val requestPermission = remember(context, permissionLauncher) {
        { onResult: (NotificationPermissionStatus) -> Unit ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onResult(NotificationPermissionStatus.UNSUPPORTED)
            } else {
                resultCallback = onResult
                markNotificationsPermissionRequested(context)
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    return remember(status, requestPermission) {
        NotificationPermissionController(
            status = status,
            canRequestPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            requestPermission = requestPermission,
        )
    }
}

private fun resolveNotificationPermissionStatus(context: Context): NotificationPermissionStatus {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return NotificationPermissionStatus.UNSUPPORTED
    }
    val hasRuntimePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    if (hasRuntimePermission && notificationsEnabled) {
        return NotificationPermissionStatus.GRANTED
    }
    if (!wasNotificationsPermissionRequested(context)) {
        return NotificationPermissionStatus.NOT_REQUESTED
    }
    return NotificationPermissionStatus.DENIED
}

private fun wasNotificationsPermissionRequested(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_NOTIFICATIONS_REQUESTED, false)
}

private fun markNotificationsPermissionRequested(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_NOTIFICATIONS_REQUESTED, true)
        .apply()
}
