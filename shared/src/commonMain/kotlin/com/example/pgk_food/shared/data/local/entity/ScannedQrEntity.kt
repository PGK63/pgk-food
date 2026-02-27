package com.example.pgk_food.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_qrs")
data class ScannedQrEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val qrContent: String,
    val studentName: String,
    val mealType: String,
    val timestamp: Long,
    val status: String
)
