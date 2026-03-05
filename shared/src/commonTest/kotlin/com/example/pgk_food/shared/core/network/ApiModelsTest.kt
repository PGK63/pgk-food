package com.example.pgk_food.shared.core.network

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiModelsTest {
    @Test
    fun detailedMessage_mapsNetworkErrorCategory() {
        val error = ApiError(
            code = "NETWORK_ERROR",
            userMessage = "Нет соединения",
        )

        assertEquals("Сетевая ошибка: Нет соединения", error.toDetailedUserMessage())
    }

    @Test
    fun detailedMessage_mapsTimeoutCategory() {
        val timeoutByCode = ApiError(
            code = "TIMEOUT",
            userMessage = "Сервер не ответил",
        )
        val timeoutByHttp = ApiError(
            code = "HTTP_408",
            userMessage = "Превышено время ожидания",
            httpStatus = 408,
        )

        assertEquals("Тайм-аут: Сервер не ответил", timeoutByCode.toDetailedUserMessage())
        assertEquals("Тайм-аут: Превышено время ожидания", timeoutByHttp.toDetailedUserMessage())
    }

    @Test
    fun detailedMessage_mapsAuthorizationAndServerCategories() {
        val authError = ApiError(
            code = "HTTP_401",
            userMessage = "Требуется повторный вход",
            httpStatus = 401,
        )
        val serverError = ApiError(
            code = "HTTP_500",
            userMessage = "Внутренняя ошибка",
            httpStatus = 500,
        )

        assertEquals("Ошибка авторизации: Требуется повторный вход", authError.toDetailedUserMessage())
        assertEquals("Ошибка сервера: Внутренняя ошибка", serverError.toDetailedUserMessage())
    }
}
