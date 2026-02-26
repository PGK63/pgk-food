package com.example.pgk_food.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: Long,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)

@Serializable
data class NotificationPageDto(
    val items: List<NotificationDto>,
    val nextCursor: Long? = null,
    val hasMore: Boolean = false
)

@Serializable
data class UnreadCountDto(
    val count: Long
)

@Serializable
data class MarkReadBatchRequest(
    val ids: List<Long>
)
