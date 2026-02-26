package com.example.pgk_food.ui.state

data class ActionId(val value: String)

sealed interface UiActionState {
    data object Idle : UiActionState
    data object Loading : UiActionState
    data class Success(val message: String) : UiActionState
    data class Error(
        val userMessage: String,
        val code: String,
        val retryable: Boolean
    ) : UiActionState
}

sealed interface UiEvent {
    data class ShowMessage(
        val text: String,
        val actionLabel: String? = null,
        val actionId: ActionId? = null
    ) : UiEvent

    data class Navigate(val route: String) : UiEvent
    data class Haptic(val kind: HapticKind) : UiEvent
    data class LogoutRequired(val reasonCode: String) : UiEvent
}

enum class HapticKind {
    Success,
    Error,
    CriticalTap
}

