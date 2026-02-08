package com.example.pgk_food.data.repository

import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class CuratorRepository {

    suspend fun getRoster(token: String, date: String): Result<List<StudentRosterDto>> {
        return try {
            val response: List<StudentRosterDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/roster")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("date", date)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRoster(token: String, request: SaveRosterRequest): Result<Unit> {
        return try {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/roster")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyGroupStatistics(token: String, date: String): Result<List<StudentMealStatus>> {
        return try {
            val response: List<StudentMealStatus> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/statistics/my-group")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("date", date)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRosterDeadlineNotification(token: String): Result<RosterDeadlineNotificationDto> {
        return try {
            val response: RosterDeadlineNotificationDto = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/notifications/roster-deadline")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
