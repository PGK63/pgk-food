package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.shared.data.remote.dto.SaveRosterRequest
import com.example.pgk_food.shared.data.remote.dto.StudentMealStatus
import com.example.pgk_food.shared.data.remote.dto.StudentRosterDto
import com.example.pgk_food.shared.network.SharedNetworkModule
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class CuratorRepository {
    suspend fun getRoster(token: String, date: String): Result<List<StudentRosterDto>> = runCatching {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/roster")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("date", date)
        }.body()
    }

    suspend fun updateRoster(token: String, request: SaveRosterRequest): Result<Unit> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/roster")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getMyGroupStatistics(token: String, date: String): Result<List<StudentMealStatus>> = runCatching {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/statistics/my-group")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("date", date)
        }.body()
    }

    suspend fun getRosterDeadlineNotification(token: String): Result<RosterDeadlineNotificationDto> = runCatching {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/notifications/roster-deadline")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }
}
