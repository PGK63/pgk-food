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
    val isBreakfastConsumed: Boolean? = null,
    val isLunchConsumed: Boolean? = null,
    val timestamp: Long = currentTimeMillis(),
)
