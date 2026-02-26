package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.data.remote.dto.MenuItemDto
import com.example.pgk_food.shared.network.SharedNetworkModule
import com.example.pgk_food.shared.platform.currentTimeMillis
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
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
    suspend fun getMealsToday(token: String): Result<MealsTodayResponse> = runCatching {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/student/meals/today")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
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
