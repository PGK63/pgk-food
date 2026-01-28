package com.example.pgk_food.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StudentRosterDto(
    val studentId: String,
    val fullName: String,
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
    val isDinner: Boolean,
    val isSnack: Boolean,
    val isSpecial: Boolean,
    val reason: String?
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
    val hadLunch: Boolean,
    val hadDinner: Boolean,
    val hadSnack: Boolean,
    val hadSpecial: Boolean
)
