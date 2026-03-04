package com.example.pgk_food.shared.data.remote.dto

import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.model.AccountStatus
import com.example.pgk_food.shared.model.StudentCategory
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val userId: String,
    val login: String,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val roles: List<UserRole>,
    val groupId: Int?,
    val studentCategory: StudentCategory?,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
)

@Serializable
data class CreateUserRequest(
    val roles: List<UserRole>,
    val name: String,
    val surname: String,
    val fatherName: String?,
    val groupId: Int?,
    val studentCategory: StudentCategory?
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
    val roles: List<UserRole>,
    val groupId: Int? = null,
    val studentCategory: StudentCategory? = null,
)

@Serializable
data class UpdateCategoryRequest(
    val studentCategory: StudentCategory
)

@Serializable
data class CreateGroupRequest(
    val name: String
)

@Serializable
data class GroupDto(
    val id: Int,
    val name: String,
    val curators: List<CuratorSummaryDto>,
    val studentCount: Int
)

@Serializable
data class CuratorSummaryDto(
    val id: String,
    val name: String,
    val surname: String,
    val fatherName: String
)

@Serializable
data class ResetPasswordResponse(
    val login: String,
    val passwordClearText: String
)

@Serializable
data class CuratorStudentRow(
    val userId: String,
    val fullName: String,
    val groupId: Int,
    val groupName: String,
    val studentCategory: StudentCategory?
)

@Serializable
data class UpdateLifecycleRequest(
    val status: AccountStatus,
    val expelNote: String? = null,
)
