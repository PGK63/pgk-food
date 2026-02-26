package com.example.pgk_food.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class QrPayload(
    val userId: String,
    val timestamp: Long,
    val mealType: String,
    val nonce: String,
    val signature: String
)

@Serializable
data class QrValidationRequest(
    val userId: String,
    val timestamp: Long,
    val mealType: String,
    val nonce: String,
    val signature: String
)

@Serializable
data class QrValidationResponse(
    val isValid: Boolean,
    val studentName: String?,
    val groupName: String?,
    val mealType: String? = null,
    val errorMessage: String?,
    val errorCode: String? = null
)
