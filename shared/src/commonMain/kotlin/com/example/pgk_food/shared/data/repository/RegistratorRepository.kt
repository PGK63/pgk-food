package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.data.remote.dto.CreateGroupRequest
import com.example.pgk_food.shared.data.remote.dto.CreateUserRequest
import com.example.pgk_food.shared.data.remote.dto.CreateUserResponse
import com.example.pgk_food.shared.data.remote.dto.GroupDto
import com.example.pgk_food.shared.data.remote.dto.ResetPasswordResponse
import com.example.pgk_food.shared.data.remote.dto.UpdateRolesRequest
import com.example.pgk_food.shared.data.remote.dto.UserDto
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.network.SharedNetworkModule
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class RegistratorRepository {
    suspend fun getUsers(token: String, groupId: Int? = null): Result<List<UserDto>> = runCatching {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/registrator/users")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            if (groupId != null) parameter("groupId", groupId)
        }.body()
    }

    suspend fun createUser(token: String, request: CreateUserRequest): Result<CreateUserResponse> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/registrator/users/create")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteUser(token: String, userId: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/registrator/users/$userId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun resetPassword(token: String, userId: String): Result<ResetPasswordResponse> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/registrator/users/$userId/reset-password")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("userId", userId)
        }.body()
    }

    suspend fun updateRoles(token: String, userId: String, roles: List<UserRole>): Result<Unit> = runCatching {
        SharedNetworkModule.client.patch(SharedNetworkModule.getUrl("/api/v1/registrator/users/$userId/roles")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("userId", userId)
            contentType(ContentType.Application.Json)
            setBody(UpdateRolesRequest(roles))
        }
    }

    suspend fun importStudents(token: String, fileBytes: ByteArray, fileName: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/registrator/import/students")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\\\"$fileName\\\"")
                    })
                }
            ))
        }
    }

    suspend fun getGroups(token: String): Result<List<GroupDto>> = runCatching {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/groups")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun createGroup(token: String, name: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/groups")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateGroupRequest(name))
        }
    }

    suspend fun deleteGroup(token: String, groupId: Int): Result<Unit> = runCatching {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/groups/$groupId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun assignCurator(token: String, groupId: Int, curatorId: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.put(SharedNetworkModule.getUrl("/api/v1/groups/$groupId/curator/$curatorId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun removeCurator(token: String, groupId: Int): Result<Unit> = runCatching {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/groups/$groupId/curator")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun addStudentToGroup(token: String, groupId: Int, studentId: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/groups/$groupId/students/$studentId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun removeStudentFromGroup(token: String, studentId: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/groups/students/$studentId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun removeStudentFromAnyGroup(token: String, studentId: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/groups/students/$studentId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
