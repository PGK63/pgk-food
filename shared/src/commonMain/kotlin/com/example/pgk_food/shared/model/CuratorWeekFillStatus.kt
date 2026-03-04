package com.example.pgk_food.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class CuratorWeekFillStatus {
    ZERO_FILL,
    PARTIAL,
    FULL
}
