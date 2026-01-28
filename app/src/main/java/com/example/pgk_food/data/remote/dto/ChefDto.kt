package com.example.pgk_food.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class QrValidationRequest(
    val qrContent: String
)

@Serializable
data class QrValidationResponse(
    val isValid: Boolean,
    val studentName: String?,
    val groupName: String?,
    val mealType: String? = null,
    val errorMessage: String?
)

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
