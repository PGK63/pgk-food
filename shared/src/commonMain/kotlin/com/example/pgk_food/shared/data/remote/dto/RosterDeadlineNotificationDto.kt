package com.example.pgk_food.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RosterDeadlineNotificationDto(
    val needsReminder: Boolean,
    val daysUntilDeadline: Int? = null,
    val deadlineDate: String? = null,
    val reason: String? = null,
    val cutoffDateTime: String? = null,
    val weekStart: String? = null,
    val isSubmitted: Boolean = false,
    val isLocked: Boolean = false,
    val severity: String? = null,
)
