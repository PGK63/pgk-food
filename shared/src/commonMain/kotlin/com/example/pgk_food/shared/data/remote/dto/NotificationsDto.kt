package com.example.pgk_food.shared.data.remote.dto

import kotlinx.serialization.Serializable

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
