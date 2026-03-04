package com.example.pgk_food.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MenuItemDto(
    val id: String,
    val date: String,
    val name: String,
    val location: String,
    val description: String? = null
)

@Serializable
data class CreateMenuItemRequest(
    val date: String,
    val name: String,
    val location: String,
    val description: String
)

@Serializable
data class MenuBatchRequest(
    val items: List<MenuItemDto>
)

@Serializable
data class StatisticsResponse(
    val totalServed: Int,
    val byMealType: Map<String, Int>
)

@Serializable
data class StudentKeyDto(
    val userId: String,
    val publicKey: String,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupName: String?
)

@Serializable
data class StudentPermissionDto(
    val studentId: String,
    val name: String,
    val surname: String,
    val breakfast: Boolean,
    val lunch: Boolean
)

@Serializable
data class QrOfflineSyncItem(
    val userId: String,
    val timestamp: Long,
    val mealType: String,
    val nonce: String,
    val signature: String,
)

@Serializable
data class SyncResponse(
    val successCount: Int,
    val failedCount: Int,
    val errors: List<SyncError>,
)

@Serializable
data class SyncError(
    val userId: String,
    val reason: String,
    val transactionHash: String? = null,
)

@Serializable
data class ChefWeeklyReportDayDto(
    val date: String,
    val breakfastCount: Int,
    val lunchCount: Int,
    val bothCount: Int,
)

@Serializable
data class ChefWeeklyReportDto(
    val weekStart: String,
    val days: List<ChefWeeklyReportDayDto>,
    val totalBreakfastCount: Int,
    val totalLunchCount: Int,
    val totalBothCount: Int,
    val confirmed: Boolean,
    val confirmedAt: String? = null,
    val canConfirmNow: Boolean = false,
    val confirmWindowStart: String? = null,
    val confirmWindowEnd: String? = null,
    val confirmWindowHint: String? = null,
)
