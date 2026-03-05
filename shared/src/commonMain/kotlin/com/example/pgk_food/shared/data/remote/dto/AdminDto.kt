package com.example.pgk_food.shared.data.remote.dto

import com.example.pgk_food.shared.model.CuratorWeekFillStatus
import com.example.pgk_food.shared.model.NoMealReasonType
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
    val lunchScannedByName: String? = null,
    val plannedBreakfast: Boolean = false,
    val plannedLunch: Boolean = false,
    val noMealReasonType: NoMealReasonType? = null,
    val noMealReasonText: String? = null,
    val absenceFrom: String? = null,
    val absenceTo: String? = null,
    val comment: String? = null,
    val isSyntheticMissingRoster: Boolean = false,
)

@Serializable
data class ConsumptionSummaryDayDto(
    val date: String,
    val breakfastCount: Int,
    val lunchCount: Int,
    val bothCount: Int,
)

@Serializable
data class ZeroFillCuratorSummaryDto(
    val curatorId: String,
    val curatorName: String,
    val weekStart: String,
    val groupIds: List<Int>,
    val filledCells: Int,
    val expectedCells: Int,
    val fillStatus: CuratorWeekFillStatus,
)

@Serializable
data class ConsumptionSummaryResponseDto(
    val startDate: String,
    val endDate: String,
    val days: List<ConsumptionSummaryDayDto>,
    val totalBreakfastCount: Int,
    val totalLunchCount: Int,
    val totalBothCount: Int,
    val usedBreakfastCount: Int = 0,
    val usedLunchCount: Int = 0,
    val usedBothCount: Int = 0,
    val missingRosterRowsCount: Int,
    val zeroFillCurators: List<ZeroFillCuratorSummaryDto>,
)
