package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.core.network.ApiError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudentRepositoryFallbackPolicyTest {

    @Test
    fun `network and timeout errors allow offline fallback`() {
        val networkError = ApiCallException(
            ApiError(code = "NETWORK_ERROR", retryable = true)
        )
        val timeoutError = ApiCallException(
            ApiError(code = "TIMEOUT", retryable = true)
        )

        assertTrue(networkError.shouldAllowOfflineMealsFallback())
        assertTrue(timeoutError.shouldAllowOfflineMealsFallback())
    }

    @Test
    fun `auth failures do not allow offline fallback`() {
        val unauthorized = ApiCallException(
            ApiError(code = "HTTP_401", httpStatus = 401, retryable = false)
        )
        val forbidden = ApiCallException(
            ApiError(code = "ACCESS_DENIED", httpStatus = 403, retryable = false)
        )

        assertFalse(unauthorized.shouldAllowOfflineMealsFallback())
        assertFalse(forbidden.shouldAllowOfflineMealsFallback())
    }

    @Test
    fun `unknown non-api errors do not allow offline fallback`() {
        assertFalse(IllegalStateException("boom").shouldAllowOfflineMealsFallback())
    }
}
