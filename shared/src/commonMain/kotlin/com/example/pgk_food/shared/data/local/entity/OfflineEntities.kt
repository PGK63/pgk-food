package com.example.pgk_food.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pgk_food.shared.platform.currentTimeMillis

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
    val studentId: String,
    val studentName: String,
    val groupName: String?,
    val mealType: String,
    val timestamp: Long,
    val nonce: String,
    val signature: String,
    val transactionHash: String,
    val scannedAt: Long = currentTimeMillis(),
    val synced: Boolean = false,
)

@Entity(tableName = "student_keys")
data class StudentKeyEntity(
    @PrimaryKey val userId: String,
    val publicKey: String,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupName: String?,
    val downloadedAt: Long = currentTimeMillis(),
)

@Entity(tableName = "permission_cache")
data class PermissionCacheEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val date: String,
    val breakfast: Boolean,
    val lunch: Boolean,
    val dinner: Boolean,
    val snack: Boolean,
    val special: Boolean,
)
