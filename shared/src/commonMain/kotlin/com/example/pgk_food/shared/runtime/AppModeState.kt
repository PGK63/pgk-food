package com.example.pgk_food.shared.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppModeState {
    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    fun setTestMode(enabled: Boolean) {
        _isTestMode.value = enabled
    }

    fun reset() {
        _isTestMode.value = false
    }
}
