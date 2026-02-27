package com.example.pgk_food.shared.ui.viewmodels

import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.core.feedback.FeedbackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class KmpViewModelScopeOwner {
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

internal data class ErrorInfo(
    val message: String,
    val code: String = "UNEXPECTED_ERROR",
    val retryable: Boolean = false,
)

internal fun Throwable.toErrorInfo(defaultMessage: String): ErrorInfo {
    val apiError = (this as? ApiCallException)?.apiError
    if (apiError != null) {
        return ErrorInfo(
            message = apiError.userMessage.ifBlank { message ?: defaultMessage },
            code = apiError.code,
            retryable = apiError.retryable,
        )
    }
    val messageText = message ?: defaultMessage
    return ErrorInfo(message = messageText, code = "UNEXPECTED_ERROR", retryable = false)
}

internal fun notifySuccess(text: String) {
    FeedbackController.success(text)
}

internal fun notifyError(text: String) {
    FeedbackController.error(text)
}
