package com.example.pgk_food.shared.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApiModelsTest {
    @Test
    fun toDetailedUserMessage_localizes_network_error_category() {
        val apiError = ApiError(
            code = "NETWORK_ERROR",
            userMessage = "Нет подключения к сети",
        )

        assertEquals(
            "Сетевая ошибка: Нет подключения к сети",
            apiError.toDetailedUserMessage(),
        )
    }

    @Test
    fun toDetailedUserMessage_localizes_auth_and_server_http_categories() {
        val authError = ApiError(
            code = "HTTP_403",
            userMessage = "Нет доступа к этому действию.",
            httpStatus = 403,
        )
        val serverError = ApiError(
            code = "HTTP_503",
            userMessage = "Сервис временно недоступен",
            httpStatus = 503,
            requestId = "abc",
            requestMethod = "get",
            requestPath = "/api/test",
        )

        assertEquals(
            "Ошибка авторизации: Нет доступа к этому действию.",
            authError.toDetailedUserMessage(),
        )
        assertEquals(
            "Ошибка сервера: Сервис временно недоступен",
            serverError.toDetailedUserMessage(),
        )
        assertFalse(serverError.toDetailedUserMessage().contains("requestId"))
        assertFalse(serverError.toDetailedUserMessage().contains("HTTP"))
    }
}
