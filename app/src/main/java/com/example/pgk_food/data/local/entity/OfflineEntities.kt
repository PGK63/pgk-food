package com.example.pgk_food.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val weight: String?,
    val calories: Int?,
    val price: Double?,
    val category: String?
)

@Entity(tableName = "offline_transactions")
data class OfflineTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val qrContent: String,
    val timestamp: Long
)
