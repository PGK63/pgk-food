package com.example.pgk_food.data.repository

import com.example.pgk_food.core.network.ApiResult
import com.example.pgk_food.core.network.safeApiCall
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.CreateGroupRequest
import com.example.pgk_food.data.remote.dto.CreateUserRequest
import com.example.pgk_food.data.remote.dto.CreateUserResponse
import com.example.pgk_food.data.remote.dto.GroupDto
import com.example.pgk_food.data.remote.dto.ResetPasswordResponse
import com.example.pgk_food.data.remote.dto.UpdateRolesRequest
import com.example.pgk_food.data.remote.dto.UserDto
import com.example.pgk_food.model.UserRole
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

    suspend fun getUsers(token: String, groupId: Int? = null): ApiResult<List<UserDto>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/registrator/users")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (groupId != null) parameter("groupId", groupId)
            }.body()
        }
    }

    suspend fun createUser(token: String, request: CreateUserRequest): ApiResult<CreateUserResponse> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/registrator/users/create")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    suspend fun deleteUser(token: String, userId: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/registrator/users/$userId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun resetPassword(token: String, userId: String): ApiResult<ResetPasswordResponse> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/registrator/users/$userId/reset-password")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("userId", userId)
            }.body()
        }
    }

    suspend fun updateRoles(token: String, userId: String, roles: List<UserRole>): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.patch(NetworkModule.getUrl("/api/v1/registrator/users/$userId/roles")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("userId", userId)
                contentType(ContentType.Application.Json)
                setBody(UpdateRolesRequest(roles))
            }
            Unit
        }
    }

    suspend fun importStudents(token: String, fileBytes: ByteArray, fileName: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/registrator/import/students")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            })
                        }
                    )
                )
            }
            Unit
        }
    }

    suspend fun getGroups(token: String): ApiResult<List<GroupDto>> {
        return safeApiCall {
            NetworkModule.client.get(NetworkModule.getUrl("/api/v1/groups")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }

    suspend fun createGroup(token: String, name: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/groups")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(CreateGroupRequest(name))
            }
            Unit
        }
    }

    suspend fun deleteGroup(token: String, groupId: Int): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/$groupId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun assignCurator(token: String, groupId: Int, curatorId: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.put(NetworkModule.getUrl("/api/v1/groups/$groupId/curator/$curatorId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun removeCurator(token: String, groupId: Int): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/$groupId/curator")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun addStudentToGroup(token: String, groupId: Int, studentId: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/groups/$groupId/students/$studentId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun removeStudentFromGroup(token: String, studentId: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/students/$studentId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun removeStudentFromAnyGroup(token: String, studentId: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/students/$studentId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }
}

