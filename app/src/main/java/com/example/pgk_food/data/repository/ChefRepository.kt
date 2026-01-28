package com.example.pgk_food.data.repository

import com.example.pgk_food.data.local.dao.ScannedQrDao
import com.example.pgk_food.data.local.entity.ScannedQrEntity
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow

class ChefRepository(private val scannedQrDao: ScannedQrDao? = null) {

    suspend fun validateQr(token: String, qrContent: String, isOffline: Boolean = false): Result<QrValidationResponse> {
        return try {
            val endpoint = if (isOffline) "/api/v1/qr/validate-offline" else "/api/v1/qr/validate"
            val response: QrValidationResponse = NetworkModule.client.post(NetworkModule.getUrl(endpoint)) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(QrValidationRequest(qrContent))
            }.body()
            
            // Save to history if successful
            scannedQrDao?.insert(
                ScannedQrEntity(
                    qrContent = qrContent,
                    studentName = response.studentName ?: "Неизвестно",
                    mealType = response.mealType ?: "Неизвестно",
                    timestamp = System.currentTimeMillis(),
                    status = if (response.isValid) "success" else "error"
                )
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getScanHistory(): Flow<List<ScannedQrEntity>>? {
        return scannedQrDao?.getAllScannedQrs()
    }

    suspend fun addMenuItem(token: String, request: CreateMenuItemRequest): Result<Unit> {
        return try {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/menu")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(token: String, id: String): Result<Unit> {
        return try {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/menu/$id")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
