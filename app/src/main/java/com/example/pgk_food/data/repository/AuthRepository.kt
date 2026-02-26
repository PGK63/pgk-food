package com.example.pgk_food.data.repository

import com.example.pgk_food.core.network.ApiResult
import com.example.pgk_food.core.network.safeApiCall
import com.example.pgk_food.data.local.dao.UserSessionDao
import com.example.pgk_food.data.local.entity.UserSessionEntity
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.AuthResponse
import com.example.pgk_food.data.remote.dto.AuthKeysDto
import com.example.pgk_food.data.remote.dto.LoginRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val userSessionDao: UserSessionDao) {

    fun getUserSession(): Flow<UserSessionEntity?> = userSessionDao.getUserSession()
    
    fun getToken(): String? = userSessionDao.getTokenSync()

    suspend fun login(loginRequest: LoginRequest): ApiResult<AuthResponse> {
        return safeApiCall(emitSessionEventsOn401 = false) {
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
            response
        }
    }

    suspend fun logout() {
        userSessionDao.clearSession()
    }

    suspend fun getMyKeys(token: String): ApiResult<AuthKeysDto> {
        return safeApiCall {
            val response: AuthKeysDto = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/auth/my-keys")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

            val currentSession = userSessionDao.getUserSessionSync()
            if (currentSession != null) {
                userSessionDao.saveSession(
                    currentSession.copy(
                        publicKey = response.publicKey,
                        privateKey = response.privateKey
                    )
                )
            }
            response
        }
    }
}
