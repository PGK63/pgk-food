package com.example.pgk_food.data.repository

import com.example.pgk_food.core.network.ApiResult
import com.example.pgk_food.core.network.safeApiCall
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.DailyReportDto
import com.example.pgk_food.data.remote.dto.FraudReportDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders

class AdminRepository {

    suspend fun getFraudReports(token: String, startDate: String, endDate: String): ApiResult<List<FraudReportDto>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/fraud")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }.body()
        }
    }

    suspend fun resolveFraud(token: String, id: Int): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/reports/fraud/$id/resolve")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun getDailyReport(token: String, date: String): ApiResult<DailyReportDto> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/daily")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("date", date)
            }.body()
        }
    }

    suspend fun getWeeklyReport(token: String, startDate: String): ApiResult<List<DailyReportDto>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/weekly")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
            }.body()
        }
    }

    suspend fun exportReportsCsv(token: String, startDate: String, endDate: String): ApiResult<ByteArray> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/export/csv")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }.body()
        }
    }

    suspend fun exportReportsPdf(token: String, startDate: String, endDate: String): ApiResult<ByteArray> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/reports/export/pdf")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }.body()
        }
    }
}

