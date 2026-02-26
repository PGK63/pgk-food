package com.example.pgk_food.data.repository

import com.example.pgk_food.core.network.ApiResult
import com.example.pgk_food.core.network.safeApiCall
import com.example.pgk_food.data.local.dao.OfflineCouponDao
import com.example.pgk_food.data.local.entity.OfflineCouponEntity
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.MenuItemDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class MealsTodayResponse(
    val date: String,
    val isBreakfastAllowed: Boolean,
    val isLunchAllowed: Boolean,
    val isDinnerAllowed: Boolean,
    val isSnackAllowed: Boolean,
    val isSpecialAllowed: Boolean,
    val reason: String? = null
)

@Serializable
data class TimeResponse(
    val timestamp: Long,
    val iso8601: String
)

class StudentRepository(
    private val offlineCouponDao: OfflineCouponDao? = null
) {

    suspend fun getMealsToday(token: String): ApiResult<MealsTodayResponse> = withContext(Dispatchers.IO) {
        val remoteResult = safeApiCall {
            val response: MealsTodayResponse = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/student/meals/today")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

            offlineCouponDao?.saveDailyCoupons(
                OfflineCouponEntity(
                    date = response.date,
                    isBreakfastAllowed = response.isBreakfastAllowed,
                    isLunchAllowed = response.isLunchAllowed,
                    isDinnerAllowed = response.isDinnerAllowed,
                    isSnackAllowed = response.isSnackAllowed,
                    isSpecialAllowed = response.isSpecialAllowed
                )
            )

            response
        }

        when (remoteResult) {
            is ApiResult.Success -> remoteResult
            is ApiResult.Failure -> {
                val cached = offlineCouponDao?.getDailyCoupons()
                if (cached != null) {
                    ApiResult.Success(
                        MealsTodayResponse(
                            date = cached.date,
                            isBreakfastAllowed = cached.isBreakfastAllowed,
                            isLunchAllowed = cached.isLunchAllowed,
                            isDinnerAllowed = cached.isDinnerAllowed,
                            isSnackAllowed = cached.isSnackAllowed,
                            isSpecialAllowed = cached.isSpecialAllowed,
                            reason = "Оффлайн режим"
                        )
                    )
                } else {
                    remoteResult
                }
            }
        }
    }

    suspend fun getMenu(token: String, date: String? = null): ApiResult<List<MenuItemDto>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/menu")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (date != null) parameter("date", date)
            }.body()
        }
    }

    suspend fun getCurrentTime(): Long {
        return runCatching {
            val response: TimeResponse = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/time/current")).body()
            response.timestamp * 1000
        }.getOrDefault(System.currentTimeMillis())
    }
}
