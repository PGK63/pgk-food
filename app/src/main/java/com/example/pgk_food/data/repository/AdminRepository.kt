package com.example.pgk_food.data.repository

import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AdminRepository {

    suspend fun getFraudReports(token: String, startDate: String, endDate: String): Result<List<FraudReportDto>> {
        return try {
            val response: List<FraudReportDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/fraud")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveFraud(token: String, id: Int): Result<Unit> {
        return try {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/reports/fraud/$id/resolve")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
