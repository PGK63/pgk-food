package com.example.pgk_food.shared.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionStore {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    fun save(session: UserSession) {
        _session.value = session
    }

    fun clear() {
        _session.value = null
    }
}
