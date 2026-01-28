package com.example.pgk_food.data.repository

import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.MenuItemDto
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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

class StudentRepository {

    suspend fun getMealsToday(token: String): Result<MealsTodayResponse> {
        return try {
            val response: MealsTodayResponse = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/student/meals/today")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMenu(token: String, date: String? = null): Result<List<MenuItemDto>> {
        return try {
            val response: List<MenuItemDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/menu")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (date != null) parameter("date", date)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentTime(): Long {
        return try {
            val response: Long = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/time/current")).body()
            response
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
