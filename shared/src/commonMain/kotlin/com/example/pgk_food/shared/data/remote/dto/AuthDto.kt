package com.example.pgk_food.shared.data.remote.dto

import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.model.StudentCategory
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val roles: List<UserRole>,
    val userId: String,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupId: Int?,
    val studentCategory: StudentCategory?,
    val publicKey: String?,
    val privateKey: String?
)

@Serializable
data class AuthKeysDto(
    val publicKey: String,
    val privateKey: String,
)

@Serializable
data class AuthMeResponse(
    val userId: String,
    val roles: List<UserRole>,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupId: Int?,
    val studentCategory: StudentCategory?,
    val publicKey: String,
    val privateKey: String,
)
