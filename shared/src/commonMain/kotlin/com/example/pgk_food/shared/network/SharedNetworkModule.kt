package com.example.pgk_food.shared.network

import com.example.pgk_food.shared.core.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object SharedNetworkModule {
    private const val BASE_URL = "https://food.pgk63.ru"
    private val sessionEventsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
                logger = object : Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }
                level = LogLevel.ALL
            }
        }
    }

    fun getUrl(path: String): String = "$BASE_URL$path"
}
