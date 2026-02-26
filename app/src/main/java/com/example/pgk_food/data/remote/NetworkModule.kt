package com.example.pgk_food.data.remote

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object NetworkModule {
    private const val BASE_URL = "https://food.pgk.apis.alspio.com"

    val client = HttpClient(Android) {
        expectSuccess = true

        defaultRequest {
            url(BASE_URL)
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    fun getUrl(path: String): String = if (path.startsWith("http")) path else "$BASE_URL$path"
}
