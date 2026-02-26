package com.example.pgk_food.core.network

import com.example.pgk_food.core.session.SessionManager
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.json.Json

private val errorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

suspend inline fun <reified T> safeApiCall(
    emitSessionEventsOn401: Boolean = true,
    crossinline block: suspend () -> T
): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: ClientRequestException) {
        buildFailureFromResponseException(e, emitSessionEventsOn401)
    } catch (e: ResponseException) {
        buildFailureFromResponseException(e, emitSessionEventsOn401)
    } catch (e: TimeoutCancellationException) {
        ApiResult.Failure(
            ApiError(
                code = "TIMEOUT",
                userMessage = "Превышено время ожидания запроса. Попробуйте снова.",
                technicalMessage = e.message,
                retryable = true
            )
        )
    } catch (e: IOException) {
        ApiResult.Failure(
            ApiError(
                code = "NETWORK_ERROR",
                userMessage = "Нет подключения к сети",
                technicalMessage = e.message,
                retryable = true
            )
        )
    } catch (e: Exception) {
        ApiResult.Failure(
            ApiError(
                code = "UNEXPECTED_ERROR",
                userMessage = "Непредвиденная ошибка. Попробуйте снова.",
                technicalMessage = e.message,
                retryable = false
            )
        )
    }
}

suspend fun <T> ApiResult<T>.requireDataOrThrow(): T {
    return when (this) {
        is ApiResult.Success -> data
        is ApiResult.Failure -> error(throwableMessage())
    }
}

fun ApiResult.Failure<*>.throwableMessage(): String = "${error.code}: ${error.userMessage}"

suspend fun <T> buildFailureFromResponseException(
    e: ResponseException,
    emitSessionEventsOn401: Boolean = true
): ApiResult.Failure<T> {
    val response = e.response
    val status = response.status
    val bodyText = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
    val envelope = runCatching { errorJson.decodeFromString(BackendErrorEnvelope.serializer(), bodyText) }.getOrNull()

    if (emitSessionEventsOn401 && status == HttpStatusCode.Unauthorized) {
        SessionManager.notifySessionExpired()
    }

    val fallbackMessage = when (status) {
        HttpStatusCode.Unauthorized -> "Сессия истекла. Войдите снова."
        HttpStatusCode.Forbidden -> "Нет доступа к этому действию."
        HttpStatusCode.NotFound -> "Запрошенный ресурс не найден."
        else -> "Ошибка запроса к серверу."
    }

    return ApiResult.Failure(
        ApiError(
            code = envelope?.code ?: "HTTP_${status.value}",
            userMessage = envelope?.userMessage ?: fallbackMessage,
            technicalMessage = envelope?.message ?: e.message,
            retryable = envelope?.retryable ?: status.isRetryableHttpStatus(),
            httpStatus = status.value,
            requestId = envelope?.requestId ?: response.headers["X-Request-Id"]
        )
    )
}
