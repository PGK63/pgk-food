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

    suspend fun getDailyReport(token: String, date: String): Result<DailyReportDto> {
        return try {
            val response: DailyReportDto = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/daily")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("date", date)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeeklyReport(token: String, startDate: String): Result<List<DailyReportDto>> {
        return try {
            val response: List<DailyReportDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/weekly")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportReportsCsv(token: String, startDate: String, endDate: String): Result<ByteArray> {
        return try {
            val response: ByteArray = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/export/csv")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportReportsPdf(token: String, startDate: String, endDate: String): Result<ByteArray> {
        return try {
            val response: ByteArray = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/export/pdf")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
