package com.example.pgk_food.shared.core.network

import com.example.pgk_food.shared.core.session.SessionManager
import com.example.pgk_food.shared.util.UxAnalytics
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

private val errorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class ApiCallException(
    val apiError: ApiError,
) : Exception("${apiError.code}: ${apiError.userMessage}")

suspend inline fun <reified T> safeApiCall(
    emitSessionEventsOn401: Boolean = true,
    crossinline block: suspend () -> T,
): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: ClientRequestException) {
        buildFailureFromResponseException(e, emitSessionEventsOn401)
    } catch (e: ResponseException) {
        buildFailureFromResponseException(e, emitSessionEventsOn401)
    } catch (e: ContentConvertException) {
        buildClientFailure(
            ApiError(
                code = "RESPONSE_DESERIALIZATION_ERROR",
                userMessage = "Ошибка обработки ответа сервера. Попробуйте снова.",
                technicalMessage = e.message,
                retryable = false,
            )
        )
    } catch (e: SerializationException) {
        buildClientFailure(
            ApiError(
                code = "RESPONSE_DESERIALIZATION_ERROR",
                userMessage = "Ошибка обработки ответа сервера. Попробуйте снова.",
                technicalMessage = e.message,
                retryable = false,
            )
        )
    } catch (e: TimeoutCancellationException) {
        buildClientFailure(
            ApiError(
                code = "TIMEOUT",
                userMessage = "Превышено время ожидания запроса. Попробуйте снова.",
                technicalMessage = e.message,
                retryable = true,
            )
        )
    } catch (e: IOException) {
        buildClientFailure(
            ApiError(
                code = "NETWORK_ERROR",
                userMessage = "Нет подключения к сети",
                technicalMessage = e.message,
                retryable = true,
            )
        )
    } catch (e: Exception) {
        buildClientFailure(
            ApiError(
                code = "UNEXPECTED_ERROR",
                userMessage = "Непредвиденная ошибка. Попробуйте снова.",
                technicalMessage = e.message,
                retryable = false,
            )
        )
    }
}

suspend inline fun <reified T> safeResultApiCall(
    emitSessionEventsOn401: Boolean = true,
    crossinline block: suspend () -> T,
): Result<T> {
    return when (val result = safeApiCall(emitSessionEventsOn401 = emitSessionEventsOn401, block = block)) {
        is ApiResult.Success -> Result.success(result.data)
        is ApiResult.Failure -> Result.failure(ApiCallException(result.error))
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
    emitSessionEventsOn401: Boolean = true,
): ApiResult.Failure<T> {
    val response = e.response
    val status = response.status
    val requestMethod = runCatching { response.call.request.method.value }.getOrNull()
    val requestPath = runCatching {
        val url = response.call.request.url
        buildString {
            append(url.encodedPath)
            if (url.encodedQuery.isNotBlank()) {
                append('?')
                append(url.encodedQuery)
            }
        }
    }.getOrNull()
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

    return buildClientFailure(
        ApiError(
            code = envelope?.code ?: "HTTP_${status.value}",
            userMessage = envelope?.userMessage ?: fallbackMessage,
            technicalMessage = envelope?.message ?: e.message,
            retryable = envelope?.retryable ?: status.isRetryableHttpStatus(),
            httpStatus = status.value,
            requestId = envelope?.requestId ?: response.headers["X-Request-Id"],
            requestMethod = requestMethod,
            requestPath = requestPath,
        )
    )
}

@PublishedApi
internal fun <T> buildClientFailure(error: ApiError): ApiResult.Failure<T> {
    UxAnalytics.log(
        event = "network_failure",
        role = "NETWORK",
        screen = "API_CALL",
        code = error.code,
        requestId = error.requestId
    )
    return ApiResult.Failure(error)
}
