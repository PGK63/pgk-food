package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.data.remote.dto.CreateMenuItemRequest
import com.example.pgk_food.shared.data.remote.dto.QrPayload
import com.example.pgk_food.shared.data.remote.dto.QrValidationRequest
import com.example.pgk_food.shared.data.remote.dto.QrValidationResponse
import com.example.pgk_food.shared.network.SharedNetworkModule
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

data class SharedScannedQrRecord(
    val qrContent: String,
    val studentName: String,
    val mealType: String,
    val timestamp: Long,
    val status: String
)

class ChefRepository {
    private val _scanHistory = MutableStateFlow<List<SharedScannedQrRecord>>(emptyList())

    suspend fun validateQr(token: String, qrContent: String, isOffline: Boolean = false): Result<QrValidationResponse> = runCatching {
        val payload = parseQrPayload(qrContent)
            ?: error("Неверный формат QR-кода")

        val endpoint = if (isOffline) "/api/v1/qr/validate-offline" else "/api/v1/qr/validate"
        val response: QrValidationResponse = SharedNetworkModule.client.post(SharedNetworkModule.getUrl(endpoint)) {
            if (!isOffline) header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                QrValidationRequest(
                    userId = payload.userId,
                    timestamp = payload.timestamp,
                    mealType = payload.mealType,
                    nonce = payload.nonce,
                    signature = payload.signature
                )
            )
        }.body()
        _scanHistory.value = listOf(
            SharedScannedQrRecord(
                qrContent = qrContent,
                studentName = response.studentName ?: "Неизвестно",
                mealType = response.mealType ?: "Неизвестно",
                timestamp = com.example.pgk_food.shared.platform.currentTimeMillis(),
                status = if (response.isValid) "success" else "error"
            )
        ) + _scanHistory.value
        response
    }

    private fun parseQrPayload(qrContent: String): QrPayload? {
        if (qrContent.isBlank()) return null

        if (qrContent.trimStart().startsWith("{")) {
            return runCatching {
                Json { ignoreUnknownKeys = true; isLenient = true }
                    .decodeFromString<QrPayload>(qrContent)
            }.getOrNull()
        }

        val parts = qrContent.split("&")
            .mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }
            .toMap()

        return QrPayload(
            userId = parts["userId"] ?: return null,
            timestamp = parts["ts"]?.toLongOrNull() ?: return null,
            mealType = parts["type"] ?: return null,
            nonce = parts["nonce"] ?: return null,
            signature = parts["sig"] ?: return null
        )
    }

    fun getScanHistory(): Flow<List<SharedScannedQrRecord>> = _scanHistory.asStateFlow()

    suspend fun addMenuItem(token: String, request: CreateMenuItemRequest): Result<Unit> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/menu")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteMenuItem(token: String, id: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/menu/$id")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
