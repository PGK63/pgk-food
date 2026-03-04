package com.example.pgk_food.shared.data.remote.dto

import com.example.pgk_food.shared.model.StudentCategory
import com.example.pgk_food.shared.model.NoMealReasonType
import kotlinx.serialization.Serializable

@Serializable
data class StudentRosterDto(
    val studentId: String,
    val fullName: String,
    val studentCategory: StudentCategory? = null,
    val days: List<RosterDayDto>
)

@Serializable
data class SaveRosterRequest(
    val studentId: String,
    val permissions: List<RosterDayDto>
)

@Serializable
data class RosterDayDto(
    val date: String,
    val isBreakfast: Boolean,
    val isLunch: Boolean,
    val reason: String? = null,
    val noMealReasonType: NoMealReasonType? = null,
    val noMealReasonText: String? = null,
    val absenceFrom: String? = null,
    val absenceTo: String? = null,
    val comment: String? = null,
)

@Serializable
data class RosterResponse(
    val date: String,
    val entries: List<RosterEntryDto>
)

@Serializable
data class RosterEntryDto(
    val studentId: String,
    val studentName: String,
    val mealStatus: Map<String, Boolean>
)

@Serializable
data class StudentMealStatus(
    val studentId: String,
    val fullName: String,
    val hadBreakfast: Boolean,
    val hadLunch: Boolean
)

@Serializable
data class CuratorStudentAbsenceRequestDto(
    val noMealReasonType: NoMealReasonType,
    val noMealReasonText: String? = null,
    val absenceFrom: String,
    val absenceTo: String,
    val comment: String? = null,
)
