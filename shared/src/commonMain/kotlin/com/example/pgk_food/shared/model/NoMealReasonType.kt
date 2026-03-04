package com.example.pgk_food.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class NoMealReasonType {
    EXPELLED,
    SICK_LEAVE,
    OTHER,
    MISSING_ROSTER
}

fun NoMealReasonType.titleRu(): String = when (this) {
    NoMealReasonType.EXPELLED -> "Отчислен"
    NoMealReasonType.SICK_LEAVE -> "Больничный"
    NoMealReasonType.OTHER -> "Иное"
    NoMealReasonType.MISSING_ROSTER -> "Куратор не заполнил табель"
}
