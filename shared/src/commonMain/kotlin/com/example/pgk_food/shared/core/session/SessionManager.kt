package com.example.pgk_food.shared.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SessionManager {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    suspend fun notifySessionExpired() {
        _events.emit(SessionEvent.LogoutRequired("SESSION_EXPIRED"))
    }
}

sealed interface SessionEvent {
    data class LogoutRequired(val reasonCode: String) : SessionEvent
}
