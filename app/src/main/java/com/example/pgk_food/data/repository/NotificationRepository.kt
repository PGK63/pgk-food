package com.example.pgk_food.data.repository

import com.example.pgk_food.core.network.ApiResult
import com.example.pgk_food.core.network.safeApiCall
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.MarkReadBatchRequest
import com.example.pgk_food.data.remote.dto.NotificationPageDto
import com.example.pgk_food.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.data.remote.dto.UnreadCountDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class NotificationRepository {

    suspend fun getNotifications(
        token: String,
        cursor: Long? = null,
        limit: Int = 30
    ): ApiResult<NotificationPageDto> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/notifications")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (cursor != null) {
                    parameter("cursor", cursor)
                }
                parameter("limit", limit.coerceIn(1, 100))
            }.body()
        }
    }

    suspend fun getUnreadCount(token: String): ApiResult<UnreadCountDto> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/notifications/unread-count")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }

    suspend fun markAsRead(token: String, id: Long): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/notifications/$id/read")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun markAsReadBatch(token: String, ids: List<Long>): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/notifications/read-batch")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(MarkReadBatchRequest(ids = ids))
            }
            Unit
        }
    }

    suspend fun getRosterDeadline(token: String): ApiResult<RosterDeadlineNotificationDto> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/notifications/roster-deadline")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }
}
