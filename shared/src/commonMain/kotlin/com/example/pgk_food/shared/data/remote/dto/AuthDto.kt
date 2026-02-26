package com.example.pgk_food.shared.data.remote.dto

import com.example.pgk_food.shared.model.UserRole
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
    val publicKey: String?,
    val privateKey: String?
)
