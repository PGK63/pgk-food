package com.example.pgk_food.shared.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PushRouteCoordinator {
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    fun publish(route: String?) {
        val normalized = route?.trim().orEmpty()
        if (normalized.isBlank()) return
        _pendingRoute.value = normalized
    }

    fun consume(route: String) {
        if (_pendingRoute.value == route) {
            _pendingRoute.value = null
        }
    }
}

