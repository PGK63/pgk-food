package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.safeResultApiCall
import com.example.pgk_food.shared.data.remote.dto.ConsumptionReportRowDto
import com.example.pgk_food.shared.data.remote.dto.ConsumptionSummaryResponseDto
import com.example.pgk_food.shared.data.remote.dto.CuratorStudentAbsenceRequestDto
import com.example.pgk_food.shared.data.remote.dto.CuratorStudentRow
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.shared.data.remote.dto.SaveRosterRequest
import com.example.pgk_food.shared.data.remote.dto.StudentMealStatus
import com.example.pgk_food.shared.data.remote.dto.StudentRosterDto
import com.example.pgk_food.shared.data.remote.dto.UpdateCategoryRequest
import com.example.pgk_food.shared.network.SharedNetworkModule
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class CuratorRepository {
    suspend fun getCuratorGroups(token: String): Result<List<GroupDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/groups/my")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body<List<GroupDto>>()
            .sortedBy { it.id }
    }

    suspend fun getRoster(token: String, date: String, groupId: Int? = null): Result<List<StudentRosterDto>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/roster")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("date", date)
            if (groupId != null) parameter("groupId", groupId)
        }.body()
    }

    suspend fun updateRoster(token: String, request: SaveRosterRequest): Result<Unit> = safeResultApiCall {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/roster")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getMyGroupStatistics(token: String, date: String, groupId: Int? = null): Result<List<StudentMealStatus>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/statistics/my-group")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("date", date)
            if (groupId != null) parameter("groupId", groupId)
        }.body()
    }

    suspend fun getRosterDeadlineNotification(token: String): Result<RosterDeadlineNotificationDto> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/notifications/roster-deadline")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun getConsumptionReport(
        token: String,
        startDate: String,
        endDate: String,
        groupId: Int? = null,
        assignedByRole: String = "ALL",
        accessScope: String = "CURATOR",
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
        accessScope: String = "CURATOR",
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

    suspend fun listMyStudents(token: String, groupId: Int? = null): Result<List<CuratorStudentRow>> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/curator/students")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (groupId != null) parameter("groupId", groupId)
        }.body()
    }

    suspend fun updateStudentCategory(token: String, studentId: String, category: com.example.pgk_food.shared.model.StudentCategory): Result<Unit> =
        safeResultApiCall {
            SharedNetworkModule.client.patch(SharedNetworkModule.getUrl("/api/v1/curator/students/$studentId/category")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(UpdateCategoryRequest(category))
            }
        }

    suspend fun applyStudentAbsence(
        token: String,
        studentId: String,
        request: CuratorStudentAbsenceRequestDto
    ): Result<Unit> = safeResultApiCall {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/curator/students/$studentId/absence")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
