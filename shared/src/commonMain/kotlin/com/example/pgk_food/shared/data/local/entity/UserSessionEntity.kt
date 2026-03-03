package com.example.pgk_food.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.model.StudentCategory

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val userId: String,
    val token: String,
    val roles: List<UserRole>,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupId: Int?,
    val studentCategory: StudentCategory?,
    val publicKey: String?,
    val privateKey: String?
)
