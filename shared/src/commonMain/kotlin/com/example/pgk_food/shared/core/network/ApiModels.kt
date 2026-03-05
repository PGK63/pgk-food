package com.example.pgk_food.shared.core.network

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class BackendErrorEnvelope(
    val code: String? = null,
    val message: String? = null,
    val userMessage: String? = null,
    val retryable: Boolean? = null,
    val requestId: String? = null,
)

@Serializable
data class ApiError(
    val code: String = "UNKNOWN_ERROR",
    val userMessage: String = "Не удалось выполнить запрос",
    val technicalMessage: String? = null,
    val retryable: Boolean = false,
    val httpStatus: Int? = null,
    val requestId: String? = null,
    val requestMethod: String? = null,
    val requestPath: String? = null,
)

sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val meta: Map<String, String> = emptyMap(),
    ) : ApiResult<T>

    data class Failure<T>(
        val error: ApiError,
    ) : ApiResult<T>

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun onSuccess(block: (@UnsafeVariance T) -> Unit): ApiResult<T> {
        if (this is Success) block(data)
        return this
    }

    fun onFailure(block: (ApiError) -> Unit): ApiResult<T> {
        if (this is Failure) block(error)
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> default
    }

    fun errorOrNull(): ApiError? = when (this) {
        is Success -> null
        is Failure -> error
    }
}

fun HttpStatusCode.isRetryableHttpStatus(): Boolean {
    return value == 408 || value == 429 || value in 500..599
}

fun ApiError.toDetailedUserMessage(fallback: String? = null): String {
    val base = userMessage.ifBlank { fallback ?: "Не удалось выполнить запрос" }
    return "${errorCategoryRu()}: $base"
}

private fun ApiError.errorCategoryRu(): String {
    val status = httpStatus ?: parseHttpStatusFromCode(code)
    if (status != null) {
        return when {
            status == 401 || status == 403 -> "Ошибка авторизации"
            status in 500..599 -> "Ошибка сервера"
            status == 408 -> "Тайм-аут"
            status in 400..499 -> "Ошибка запроса"
            else -> "Ошибка операции"
        }
    }

    return when {
        code == "NETWORK_ERROR" -> "Сетевая ошибка"
        code == "TIMEOUT" -> "Тайм-аут"
        code == "RESPONSE_DESERIALIZATION_ERROR" -> "Ошибка данных"
        code == "UNEXPECTED_ERROR" -> "Неизвестная ошибка"
        code.contains("AUTH", ignoreCase = true) -> "Ошибка авторизации"
        code.contains("NETWORK", ignoreCase = true) -> "Сетевая ошибка"
        retryable -> "Временная ошибка"
        else -> "Ошибка операции"
    }
}

private fun parseHttpStatusFromCode(code: String): Int? {
    if (!code.startsWith("HTTP_")) return null
    return code.removePrefix("HTTP_").toIntOrNull()
}
