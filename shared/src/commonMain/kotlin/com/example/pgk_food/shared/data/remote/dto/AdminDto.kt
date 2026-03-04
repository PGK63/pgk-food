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
    val date: String = "",
    val groupId: Int = -1,
    val groupName: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val category: StudentCategory? = null,
    val assignedByRole: String? = null,
    val assignedByName: String? = null,
    val breakfastUsed: Boolean = false,
    val breakfastTransactionId: Int? = null,
    val breakfastScannedByName: String? = null,
    val lunchUsed: Boolean = false,
    val lunchTransactionId: Int? = null,
    val lunchScannedByName: String? = null
)
