package com.example.pgk_food.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pgk_food.shared.platform.currentTimeMillis

@Entity(tableName = "offline_coupons")
data class OfflineCouponEntity(
    @PrimaryKey val id: String = "DAILY_COUPONS",
    val date: String,
    val isBreakfastAllowed: Boolean,
    val isLunchAllowed: Boolean,
    val isDinnerAllowed: Boolean,
    val isSnackAllowed: Boolean,
    val isSpecialAllowed: Boolean,
    val timestamp: Long = currentTimeMillis(),
)
