package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.safeResultApiCall
import com.example.pgk_food.shared.data.remote.dto.DailyReportDto
import com.example.pgk_food.shared.data.remote.dto.FraudReportDto
import com.example.pgk_food.shared.network.SharedNetworkModule
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders

class AdminRepository {
    suspend fun getFraudReports(token: String, startDate: String, endDate: String): Result<List<FraudReportDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/fraud")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
        }.body()
    }

    suspend fun resolveFraud(token: String, id: Int): Result<Unit> = safeResultApiCall {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/reports/fraud/$id/resolve")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getDailyReport(token: String, date: String): Result<DailyReportDto> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/daily")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("date", date)
        }.body()
    }

    suspend fun getWeeklyReport(token: String, startDate: String): Result<List<DailyReportDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/weekly")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
        }.body()
    }

    suspend fun exportReportsCsv(token: String, startDate: String, endDate: String): Result<ByteArray> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/export/csv")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
        }.body()
    }

    suspend fun exportReportsPdf(token: String, startDate: String, endDate: String): Result<ByteArray> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/export/pdf")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
        }.body()
    }
}
