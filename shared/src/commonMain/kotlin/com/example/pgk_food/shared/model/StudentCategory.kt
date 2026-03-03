package com.example.pgk_food.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class StudentCategory {
    SVO,
    MANY_CHILDREN
}

fun StudentCategory.titleRu(): String = when (this) {
    StudentCategory.SVO -> "СВО"
    StudentCategory.MANY_CHILDREN -> "Многодетные"
}
