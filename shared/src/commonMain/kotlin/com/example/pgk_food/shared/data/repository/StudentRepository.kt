package com.example.pgk_food.shared.data.repository

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

class StudentRepository {
    suspend fun getMealsToday(token: String): Result<MealsTodayResponse> = withContext(Dispatchers.Default) {
        val remoteResult = runCatching {
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
                        isDinnerAllowed = response.isDinnerAllowed,
                        isSnackAllowed = response.isSnackAllowed,
                        isSpecialAllowed = response.isSpecialAllowed,
                    )
                )
            }
            response
        }

        remoteResult.recoverCatching {
            val cached = SharedDatabase.instance.offlineCouponDao().getDailyCoupons()
                ?: throw it
            MealsTodayResponse(
                date = cached.date,
                isBreakfastAllowed = cached.isBreakfastAllowed,
                isLunchAllowed = cached.isLunchAllowed,
                isDinnerAllowed = cached.isDinnerAllowed,
                isSnackAllowed = cached.isSnackAllowed,
                isSpecialAllowed = cached.isSpecialAllowed,
                reason = "Оффлайн режим",
            )
        }
    }

    suspend fun getMenu(token: String, date: String? = null): Result<List<MenuItemDto>> = runCatching {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/menu")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (date != null) url.parameters.append("date", date)
        }.body()
    }

    suspend fun getCurrentTime(): Long {
        return try {
            val response: TimeResponse = SharedNetworkModule.client.get(
                SharedNetworkModule.getUrl("/api/v1/time/current")
            ).body()
            response.timestamp * 1000
        } catch (_: Throwable) {
            currentTimeMillis()
        }
    }
}
