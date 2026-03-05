package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.safeResultApiCall
import com.example.pgk_food.shared.data.remote.dto.ConsumptionReportRowDto
import com.example.pgk_food.shared.data.remote.dto.ConsumptionSummaryResponseDto
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

    suspend fun getConsumptionReport(
        token: String,
        startDate: String,
        endDate: String,
        groupId: Int? = null,
        assignedByRole: String = "ALL",
        accessScope: String = "AUTO",
    ): Result<List<ConsumptionReportRowDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/consumption")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
            if (groupId != null) parameter("groupId", groupId)
            parameter("assignedByRole", assignedByRole)
            parameter("accessScope", accessScope)
        }.body()
    }

    suspend fun getConsumptionSummary(
        token: String,
        startDate: String,
        endDate: String,
        groupId: Int? = null,
        assignedByRole: String = "ALL",
        accessScope: String = "AUTO",
    ): Result<ConsumptionSummaryResponseDto> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/consumption/summary")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
            if (groupId != null) parameter("groupId", groupId)
            parameter("assignedByRole", assignedByRole)
            parameter("accessScope", accessScope)
        }.body()
    }

    suspend fun exportConsumptionCsv(
        token: String,
        startDate: String,
        endDate: String,
        groupId: Int? = null,
        assignedByRole: String = "ALL",
        accessScope: String = "AUTO",
    ): Result<ByteArray> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/consumption/export/csv")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
            if (groupId != null) parameter("groupId", groupId)
            parameter("assignedByRole", assignedByRole)
            parameter("accessScope", accessScope)
        }.body()
    }

    suspend fun exportConsumptionPdf(
        token: String,
        startDate: String,
        endDate: String,
        groupId: Int? = null,
        assignedByRole: String = "ALL",
        accessScope: String = "AUTO",
    ): Result<ByteArray> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/reports/consumption/export/pdf")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
            if (groupId != null) parameter("groupId", groupId)
            parameter("assignedByRole", assignedByRole)
            parameter("accessScope", accessScope)
        }.body()
    }
}
