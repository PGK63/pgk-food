package com.example.pgk_food.shared.ui.screens

import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.core.network.ApiError
import com.example.pgk_food.shared.data.repository.MealsTodayResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudentQrScreenPolicyTest {

    @Test
    fun `meal status is unknown when coupon allowed but consume state missing`() {
        val meals = MealsTodayResponse(
            date = "2026-03-04",
            isBreakfastAllowed = true,
            isLunchAllowed = true,
            isBreakfastConsumed = null,
            isLunchConsumed = null,
        )

        assertEquals(MealCouponStatus.UNKNOWN, meals.statusForMealType("BREAKFAST"))
        assertEquals(MealCouponStatus.UNKNOWN, meals.statusForMealType("LUNCH"))
    }

    @Test
    fun `meal status prefers consumed and unavailable states`() {
        val meals = MealsTodayResponse(
            date = "2026-03-04",
            isBreakfastAllowed = false,
            isLunchAllowed = true,
            isBreakfastConsumed = false,
            isLunchConsumed = true,
        )

        assertEquals(MealCouponStatus.UNAVAILABLE, meals.statusForMealType("BREAKFAST"))
        assertEquals(MealCouponStatus.USED, meals.statusForMealType("LUNCH"))
    }

    @Test
    fun `auth failure detection works for 401 and 403`() {
        val unauthorized = ApiCallException(
            ApiError(code = "HTTP_401", httpStatus = 401, retryable = false)
        )
        val forbidden = ApiCallException(
            ApiError(code = "ACCESS_DENIED", httpStatus = 403, retryable = false)
        )
        val timeout = ApiCallException(
            ApiError(code = "TIMEOUT", httpStatus = null, retryable = true)
        )

        assertTrue(unauthorized.isAuthFailure())
        assertTrue(forbidden.isAuthFailure())
        assertFalse(timeout.isAuthFailure())
        assertFalse(IllegalStateException("boom").isAuthFailure())
    }

    @Test
    fun `unknown status is non-blocking for online and offline sources`() {
        val online = resolveMealStatusPolicy(MealCouponStatus.UNKNOWN, MealStatusSource.ONLINE)
        val offline = resolveMealStatusPolicy(MealCouponStatus.UNKNOWN, MealStatusSource.OFFLINE_CACHE)

        assertEquals(null, online.errorCode)
        assertEquals("WARN_PERMISSION_UNKNOWN_ONLINE", online.warningCode)
        assertEquals(null, offline.errorCode)
        assertEquals("WARN_PERMISSION_UNKNOWN_OFFLINE", offline.warningCode)
    }

    @Test
    fun `used and unavailable statuses remain blocking`() {
        val used = resolveMealStatusPolicy(MealCouponStatus.USED, MealStatusSource.ONLINE)
        val unavailable = resolveMealStatusPolicy(MealCouponStatus.UNAVAILABLE, MealStatusSource.OFFLINE_CACHE)

        assertEquals("ERROR_COUPON_USED", used.errorCode)
        assertEquals(null, used.warningCode)
        assertEquals("ERROR_COUPON_UNAVAILABLE", unavailable.errorCode)
        assertEquals(null, unavailable.warningCode)
    }
}
