package com.example.pgk_food.shared.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiCallerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `safeApiCall maps serialization exception to RESPONSE_DESERIALIZATION_ERROR`() = runBlocking {
        val result = safeApiCall<List<String>> {
            throw SerializationException("broken payload")
        }

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error
        assertEquals("RESPONSE_DESERIALIZATION_ERROR", error.code)
        assertEquals("Ошибка обработки ответа сервера. Попробуйте снова.", error.userMessage)
        assertFalse(error.retryable)
    }

    @Test
    fun `consumption dto parsing tolerates missing nullable fields`() {
        val payload = """
            [
              {
                "date": "2026-03-04",
                "groupId": 11,
                "groupName": "ИСП-21",
                "studentId": "b3f2a6fb-5d56-4c6e-ae3e-b7a194db1b15",
                "studentName": "Иван Петров",
                "breakfastUsed": true,
                "lunchUsed": false
              }
            ]
        """.trimIndent()

        val rows = json.decodeFromString(
            ListSerializer(com.example.pgk_food.shared.data.remote.dto.ConsumptionReportRowDto.serializer()),
            payload
        )

        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals("2026-03-04", row.date)
        assertEquals(11, row.groupId)
        assertEquals("ИСП-21", row.groupName)
        assertEquals("Иван Петров", row.studentName)
        assertEquals(null, row.category)
        assertEquals(null, row.assignedByRole)
        assertEquals(null, row.assignedByName)
        assertEquals(true, row.breakfastUsed)
        assertEquals(false, row.lunchUsed)
        assertEquals(null, row.breakfastTransactionId)
        assertEquals(null, row.breakfastScannedByName)
        assertEquals(null, row.lunchTransactionId)
        assertEquals(null, row.lunchScannedByName)
    }
}
