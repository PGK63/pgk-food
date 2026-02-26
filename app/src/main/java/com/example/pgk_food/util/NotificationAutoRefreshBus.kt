package com.example.pgk_food.util

import kotlinx.coroutines.flow.MutableSharedFlow

object NotificationAutoRefreshBus {
    val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifyUpdated() {
        events.tryEmit(Unit)
    }
}
