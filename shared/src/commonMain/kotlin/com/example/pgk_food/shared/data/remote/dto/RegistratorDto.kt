package com.example.pgk_food.shared.data.remote.dto

import com.example.pgk_food.shared.model.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val userId: String,
    val login: String,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val roles: List<UserRole>,
    val groupId: Int?
)

@Serializable
data class CreateUserRequest(
    val roles: List<UserRole>,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupId: Int?
)

@Serializable
data class CreateUserResponse(
    val userId: String,
    val login: String,
    val passwordClearText: String,
    val fullName: String
)

@Serializable
data class UpdateRolesRequest(
    val roles: List<UserRole>
)

@Serializable
data class CreateGroupRequest(
    val name: String
)

@Serializable
data class GroupDto(
    val id: Int,
    val name: String,
    val curatorId: String?,
    val curatorName: String?,
    val curatorSurname: String?,
    val curatorFatherName: String?,
    val studentCount: Int
)

@Serializable
data class ResetPasswordResponse(
    val login: String,
    val passwordClearText: String
)
