package com.example.pgk_food.data.repository

import com.example.pgk_food.data.local.dao.UserSessionDao
import com.example.pgk_food.data.local.entity.UserSessionEntity
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.AuthResponse
import com.example.pgk_food.data.remote.dto.LoginRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val userSessionDao: UserSessionDao) {

    fun getUserSession(): Flow<UserSessionEntity?> = userSessionDao.getUserSession()

    suspend fun login(loginRequest: LoginRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = NetworkModule.client.post(NetworkModule.getUrl("/api/v1/auth/login")) {
                contentType(ContentType.Application.Json)
                setBody(loginRequest)
            }.body()
            
            val entity = UserSessionEntity(
                userId = response.userId,
                token = response.token,
                roles = response.roles,
                name = response.name,
                surname = response.surname,
                fatherName = response.fatherName,
                groupId = response.groupId,
                publicKey = response.publicKey,
                privateKey = response.privateKey
            )
            userSessionDao.saveSession(entity)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        userSessionDao.clearSession()
    }
}
