package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.data.remote.dto.AuthResponse
import com.example.pgk_food.shared.data.remote.dto.LoginRequest
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.network.SharedNetworkModule
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthRepository {
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
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
                    publicKey = response.publicKey,
                    privateKey = response.privateKey
                )
            )
            Result.success(response)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun logout() {
        SessionStore.clear()
    }
}
