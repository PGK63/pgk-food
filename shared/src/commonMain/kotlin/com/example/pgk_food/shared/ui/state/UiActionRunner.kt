package com.example.pgk_food.shared.ui.state

import androidx.compose.runtime.MutableState
import com.example.pgk_food.shared.core.feedback.FeedbackController
import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.core.network.ApiError
import com.example.pgk_food.shared.core.network.ApiResult
import kotlinx.coroutines.CancellationException

val UiActionState.isLoading: Boolean
    get() = this is UiActionState.Loading

fun MutableState<UiActionState>.resetUiAction() {
    value = UiActionState.Idle
}

suspend fun runUiAction(
    actionState: MutableState<UiActionState>,
    successMessage: String? = null,
    fallbackErrorMessage: String = "Операция не выполнена",
    emitSuccessFeedback: Boolean = true,
    block: suspend () -> Result<*>,
): Boolean {
    actionState.value = UiActionState.Loading
    return try {
        val result = block()
        if (result.isSuccess) {
            val message = successMessage.orEmpty()
            actionState.value = if (message.isNotBlank()) UiActionState.Success(message) else UiActionState.Idle
            if (emitSuccessFeedback && message.isNotBlank()) {
                FeedbackController.success(message)
            }
            true
        } else {
            val error = result.exceptionOrNull().toUiActionError(fallbackErrorMessage)
            actionState.value = error
            FeedbackController.error(error.userMessage)
            false
        }
    } catch (ce: CancellationException) {
        actionState.value = UiActionState.Idle
        throw ce
    } catch (t: Throwable) {
        val error = t.toUiActionError(fallbackErrorMessage)
        actionState.value = error
        FeedbackController.error(error.userMessage)
        false
    }
}

suspend fun runUiApiAction(
    actionState: MutableState<UiActionState>,
    successMessage: String? = null,
    fallbackErrorMessage: String = "Операция не выполнена",
    emitSuccessFeedback: Boolean = true,
    block: suspend () -> ApiResult<*>,
): Boolean {
    actionState.value = UiActionState.Loading
    return try {
        when (val result = block()) {
            is ApiResult.Success -> {
                val message = successMessage.orEmpty()
                actionState.value = if (message.isNotBlank()) UiActionState.Success(message) else UiActionState.Idle
                if (emitSuccessFeedback && message.isNotBlank()) {
                    FeedbackController.success(message)
                }
                true
            }

            is ApiResult.Failure -> {
                val error = result.error.toUiActionError(fallbackErrorMessage)
                actionState.value = error
                FeedbackController.error(error.userMessage)
                false
            }
        }
    } catch (ce: CancellationException) {
        actionState.value = UiActionState.Idle
        throw ce
    } catch (t: Throwable) {
        val error = t.toUiActionError(fallbackErrorMessage)
        actionState.value = error
        FeedbackController.error(error.userMessage)
        false
    }
}

private fun Throwable?.toUiActionError(fallback: String): UiActionState.Error {
    val apiError = (this as? ApiCallException)?.apiError
    if (apiError != null) {
        return UiActionState.Error(
            userMessage = apiError.userMessage.ifBlank { fallback },
            code = apiError.code,
            retryable = apiError.retryable,
        )
    }
    val message = this?.message?.takeIf { it.isNotBlank() } ?: fallback
    return UiActionState.Error(
        userMessage = message,
        code = "UNEXPECTED_ERROR",
        retryable = false,
    )
}

private fun ApiError.toUiActionError(fallback: String): UiActionState.Error {
    return UiActionState.Error(
        userMessage = userMessage.ifBlank { fallback },
        code = code,
        retryable = retryable,
    )
}
