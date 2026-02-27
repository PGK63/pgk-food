package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.safeResultApiCall
import com.example.pgk_food.shared.data.remote.dto.GroupDto
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
    suspend fun getCuratorGroups(token: String, curatorId: String): Result<List<GroupDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/groups")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body<List<GroupDto>>()
            .filter { it.curatorId == curatorId }
            .sortedBy { it.id }
    }

    suspend fun getRoster(token: String, date: String, groupId: Int? = null): Result<List<StudentRosterDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/roster")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("date", date)
            if (groupId != null) parameter("groupId", groupId)
        }.body()
    }

    suspend fun updateRoster(token: String, request: SaveRosterRequest): Result<Unit> = safeResultApiCall {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/roster")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getMyGroupStatistics(token: String, date: String, groupId: Int? = null): Result<List<StudentMealStatus>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/statistics/my-group")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("date", date)
            if (groupId != null) parameter("groupId", groupId)
        }.body()
    }

    suspend fun getRosterDeadlineNotification(token: String): Result<RosterDeadlineNotificationDto> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/notifications/roster-deadline")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }
}
