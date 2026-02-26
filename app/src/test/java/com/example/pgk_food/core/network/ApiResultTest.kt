package com.example.pgk_food.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResultTest {

    @Test
    fun success_result_exposes_data_and_flags() {
        val result: ApiResult<String> = ApiResult.Success("ok")

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("ok", result.getOrNull())
        assertEquals("ok", result.getOrDefault("fallback"))
    }

    @Test
    fun failure_result_exposes_error_and_default() {
        val error = ApiError(
            code = "NETWORK_ERROR",
            userMessage = "No network",
            retryable = true
        )
        val result: ApiResult<String> = ApiResult.Failure(error)

        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
        assertEquals(null, result.getOrNull())
        assertEquals("fallback", result.getOrDefault("fallback"))
        assertNotNull(result.errorOrNull())
        assertEquals("NETWORK_ERROR", result.errorOrNull()?.code)
    }
}

