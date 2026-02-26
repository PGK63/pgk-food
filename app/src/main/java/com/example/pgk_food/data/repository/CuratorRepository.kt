package com.example.pgk_food.data.repository

import com.example.pgk_food.core.network.ApiResult
import com.example.pgk_food.core.network.safeApiCall
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.GroupDto
import com.example.pgk_food.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.data.remote.dto.SaveRosterRequest
import com.example.pgk_food.data.remote.dto.StudentMealStatus
import com.example.pgk_food.data.remote.dto.StudentRosterDto
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

    suspend fun getCuratorGroups(token: String, curatorId: String): ApiResult<List<GroupDto>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/groups")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body<List<GroupDto>>()
                .filter { it.curatorId == curatorId }
                .sortedBy { it.id }
        }
    }

    suspend fun getRoster(token: String, date: String, groupId: Int? = null): ApiResult<List<StudentRosterDto>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/roster")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("date", date)
                if (groupId != null) {
                    parameter("groupId", groupId)
                }
            }.body()
        }
    }

    suspend fun updateRoster(token: String, request: SaveRosterRequest): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/roster")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Unit
        }
    }

    suspend fun getMyGroupStatistics(
        token: String,
        date: String,
        groupId: Int? = null
    ): ApiResult<List<StudentMealStatus>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/statistics/my-group")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("date", date)
                if (groupId != null) {
                    parameter("groupId", groupId)
                }
            }.body()
        }
    }

    suspend fun getRosterDeadlineNotification(token: String): ApiResult<RosterDeadlineNotificationDto> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/notifications/roster-deadline")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }
}
