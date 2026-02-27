package com.example.pgk_food.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MenuItemDto(
    val id: String,
    val date: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class CreateMenuItemRequest(
    val date: String,
    val name: String,
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
    val lunch: Boolean,
    val dinner: Boolean,
    val snack: Boolean,
    val special: Boolean
)

@Serializable
data class TransactionSyncItem(
    val studentId: String,
    val timestamp: String,
    val mealType: String,
    val transactionHash: String
)

@Serializable
data class SyncResponse(
    val successCount: Int,
    val errors: List<String>
)
