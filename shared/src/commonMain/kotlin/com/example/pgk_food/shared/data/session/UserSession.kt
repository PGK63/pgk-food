package com.example.pgk_food.shared.data.session

import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.model.StudentCategory

data class UserSession(
    val userId: String,
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
