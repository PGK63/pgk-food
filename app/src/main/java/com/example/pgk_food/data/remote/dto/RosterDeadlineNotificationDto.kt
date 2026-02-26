package com.example.pgk_food.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RosterDeadlineNotificationDto(
    val needsReminder: Boolean,
    val daysUntilDeadline: Int? = null,
    val deadlineDate: String? = null,
    val reason: String? = null
)

