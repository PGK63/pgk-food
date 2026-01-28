package com.example.pgk_food.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT,
    CHEF,
    REGISTRATOR,
    CURATOR,
    ADMIN
}
