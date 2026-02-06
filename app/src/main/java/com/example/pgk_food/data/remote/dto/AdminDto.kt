package com.example.pgk_food.data.remote.dto

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
data class DailyReportDto(
    val date: String,
    val breakfastCount: Long,
    val lunchCount: Long,
    val dinnerCount: Long,
    val snackCount: Long,
    val specialCount: Long,
    val totalCount: Long,
    val offlineTransactions: Long
)
