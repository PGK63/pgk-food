package com.example.pgk_food.shared.data.remote.dto

import com.example.pgk_food.shared.model.StudentCategory
import kotlinx.serialization.Serializable

@Serializable
data class FraudReportDto(
    val id: Int,
    val date: String,
    val mealType: String,
    val studentId: String,
    val studentName: String,
    val groupName: String,
    val chefId: String,
    val chefName: String,
    val reason: String,
    val attemptTimestamp: String,
    val resolved: Boolean
)

@Serializable
data class ConsumptionReportRowDto(
    val date: String,
    val groupId: Int,
    val groupName: String,
    val studentId: String,
    val studentName: String,
    val category: StudentCategory?,
    val assignedByRole: String?,
    val assignedByName: String?,
    val breakfastUsed: Boolean,
    val breakfastTransactionId: Int?,
    val breakfastScannedByName: String?,
    val lunchUsed: Boolean,
    val lunchTransactionId: Int?,
    val lunchScannedByName: String?
)
