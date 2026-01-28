package com.example.pgk_food.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pgk_food.model.UserRole

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val userId: String,
    val token: String,
    val roles: List<UserRole>,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupId: Int?,
    val publicKey: String?,
    val privateKey: String?
)
