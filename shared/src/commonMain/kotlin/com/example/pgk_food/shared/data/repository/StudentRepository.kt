package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.core.network.safeResultApiCall
import com.example.pgk_food.shared.data.remote.dto.MenuItemDto
import com.example.pgk_food.shared.data.local.SharedDatabase
import com.example.pgk_food.shared.data.local.entity.OfflineCouponEntity
import com.example.pgk_food.shared.network.SharedNetworkModule
import com.example.pgk_food.shared.platform.currentTimeMillis
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

@Serializable
data class MealsTodayResponse(
    val date: String,
    val isBreakfastAllowed: Boolean,
    val isLunchAllowed: Boolean,
    val reason: String? = null,
    val isBreakfastConsumed: Boolean? = null,
    val isLunchConsumed: Boolean? = null,
)

@Serializable
data class TimeResponse(
    val timestamp: Long,
    val iso8601: String
)

class StudentRepository {
    suspend fun getMealsToday(token: String): Result<MealsTodayResponse> = withContext(Dispatchers.Default) {
        val remoteResult = safeResultApiCall {
            val response: MealsTodayResponse =
                SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/student/meals/today")) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body()

            runCatching {
                SharedDatabase.instance.offlineCouponDao().saveDailyCoupons(
                    OfflineCouponEntity(
                        date = response.date,
                        isBreakfastAllowed = response.isBreakfastAllowed,
                        isLunchAllowed = response.isLunchAllowed,
                        isBreakfastConsumed = response.isBreakfastConsumed,
                        isLunchConsumed = response.isLunchConsumed,
                    )
                )
            }
            response
        }

        if (remoteResult.isSuccess) {
            return@withContext remoteResult
        }

        val failure = remoteResult.exceptionOrNull() ?: return@withContext remoteResult
        if (!failure.shouldAllowOfflineMealsFallback()) {
            return@withContext Result.failure(failure)
        }

        val cached = SharedDatabase.instance.offlineCouponDao().getDailyCoupons()
            ?: return@withContext Result.failure(failure)

        Result.success(
            MealsTodayResponse(
                date = cached.date,
                isBreakfastAllowed = cached.isBreakfastAllowed,
                isLunchAllowed = cached.isLunchAllowed,
                reason = "Оффлайн режим",
                isBreakfastConsumed = cached.isBreakfastConsumed,
                isLunchConsumed = cached.isLunchConsumed,
            )
        )
    }

    suspend fun getMenu(
        token: String,
        date: String? = null,
        location: String? = null,
    ): Result<List<MenuItemDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/menu")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (date != null) url.parameters.append("date", date)
            if (!location.isNullOrBlank()) url.parameters.append("location", location)
        }.body()
    }

    suspend fun getMenuLocations(token: String, date: String? = null): Result<List<String>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/menu/locations")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (date != null) url.parameters.append("date", date)
        }.body()
    }

    suspend fun getCurrentTime(timeoutMs: Long = 3000): Long {
        return try {
            withTimeoutOrNull(timeoutMs) {
                val response: TimeResponse = SharedNetworkModule.client.get(
                    SharedNetworkModule.getUrl("/api/v1/time/current")
                ).body()
                response.timestamp * 1000
            } ?: currentTimeMillis()
        } catch (_: Throwable) {
            currentTimeMillis()
        }
    }

    suspend fun getMealsTodayCached(): MealsTodayResponse? {
        return runCatching {
            val cached = SharedDatabase.instance.offlineCouponDao().getDailyCoupons()
                ?: return null
            MealsTodayResponse(
                date = cached.date,
                isBreakfastAllowed = cached.isBreakfastAllowed,
                isLunchAllowed = cached.isLunchAllowed,
                reason = "Оффлайн режим",
                isBreakfastConsumed = cached.isBreakfastConsumed,
                isLunchConsumed = cached.isLunchConsumed,
            )
        }.getOrNull()
    }
}

internal fun Throwable.shouldAllowOfflineMealsFallback(): Boolean {
    val api = this as? ApiCallException ?: return false
    val code = api.apiError.code
    val status = api.apiError.httpStatus
    if (status == 401 || status == 403) return false
    if (code == "HTTP_401" || code == "HTTP_403" || code == "ACCESS_DENIED") return false
    return api.apiError.retryable || code == "NETWORK_ERROR" || code == "TIMEOUT"
}
