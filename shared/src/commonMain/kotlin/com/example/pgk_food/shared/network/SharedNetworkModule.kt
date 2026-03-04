package com.example.pgk_food.shared.network

import com.example.pgk_food.shared.core.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object SharedNetworkModule {
    private const val DEFAULT_BASE_URL = "https://food.pgk.apis.alspio.com"
    private val sessionEventsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val baseUrl: String by lazy {
        normalizeBaseUrl(platformApiBaseUrlOverride())
    }

    val client: HttpClient by lazy {
        HttpClient(platformHttpClientEngineFactory()) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 10_000
            }
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, _ ->
                    val responseException = cause as? ResponseException ?: return@handleResponseExceptionWithRequest
                    if (responseException.response.status == HttpStatusCode.Unauthorized) {
                        sessionEventsScope.launch {
                            SessionManager.notifySessionExpired()
                        }
                    }
                }
            }
            install(Logging) {
                val enabled = isDebugHttpLoggingEnabled()
                logger = object : Logger {
                    override fun log(message: String) {
                        if (enabled) {
                            println(message)
                        }
                    }
                }
                level = if (enabled) LogLevel.HEADERS else LogLevel.NONE
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }
    }

    fun getUrl(path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "$baseUrl$normalizedPath"
    }

    private fun normalizeBaseUrl(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return DEFAULT_BASE_URL

        val withoutTrailingSlash = trimmed.trimEnd('/')
        val hasSupportedScheme =
            withoutTrailingSlash.startsWith("http://") ||
                withoutTrailingSlash.startsWith("https://")

        return if (hasSupportedScheme) {
            withoutTrailingSlash
        } else {
            DEFAULT_BASE_URL
        }
    }
}
