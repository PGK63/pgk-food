package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.safeResultApiCall
import com.example.pgk_food.shared.data.remote.dto.AuthMeResponse
import com.example.pgk_food.shared.data.remote.dto.AuthResponse
import com.example.pgk_food.shared.data.remote.dto.AuthKeysDto
import com.example.pgk_food.shared.data.remote.dto.LoginRequest
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.network.SharedNetworkModule
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class AuthRepository {
    fun getToken(): String? = SessionStore.session.value?.token

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return safeResultApiCall(emitSessionEventsOn401 = false) {
            val response: AuthResponse =
                SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/auth/login")) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()

            SessionStore.save(
                UserSession(
                    userId = response.userId,
                    token = response.token,
                    roles = response.roles,
                    name = response.name,
                    surname = response.surname,
                    fatherName = response.fatherName,
                    groupId = response.groupId,
                    studentCategory = response.studentCategory,
                    publicKey = response.publicKey,
                    privateKey = response.privateKey
                )
            )
            response
        }
    }

    fun logout() {
        SessionStore.clear()
    }

    suspend fun getMyKeys(token: String): Result<AuthKeysDto> {
        return safeResultApiCall {
            val response: AuthKeysDto = SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/auth/my-keys")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

            SessionStore.session.value?.let { current ->
                SessionStore.save(
                    current.copy(
                        publicKey = response.publicKey,
                        privateKey = response.privateKey,
                    )
                )
            }
            response
        }
    }

    suspend fun getMyProfile(token: String): Result<AuthMeResponse> {
        return safeResultApiCall {
            SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/auth/me")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }

    suspend fun refreshCurrentSession(token: String): Result<UserSession> {
        return getMyProfile(token).mapCatching { profile ->
            val session = UserSession(
                userId = profile.userId,
                token = SessionStore.session.value?.token ?: token,
                roles = profile.roles,
                name = profile.name,
                surname = profile.surname,
                fatherName = profile.fatherName,
                groupId = profile.groupId,
                studentCategory = profile.studentCategory,
                publicKey = profile.publicKey,
                privateKey = profile.privateKey
            )
            SessionStore.save(session)
            session
        }
    }
}
