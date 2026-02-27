package com.example.pgk_food.shared.runtime

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification

actual fun appForegroundEvents(): Flow<Unit> = callbackFlow {
    val center = NSNotificationCenter.defaultCenter
    val observer = center.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        queue = null,
    ) {
        trySend(Unit)
    }

    awaitClose {
        center.removeObserver(observer)
    }
}
