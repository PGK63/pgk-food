package com.example.pgk_food.data.repository

import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.*
import com.example.pgk_food.model.UserRole
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class RegistratorRepository {

    suspend fun getUsers(token: String, groupId: Int? = null): Result<List<UserDto>> {
        return try {
            val response: List<UserDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/registrator/users")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                if (groupId != null) {
                    parameter("groupId", groupId)
                }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(token: String, request: CreateUserRequest): Result<CreateUserResponse> {
        return try {
            val response: CreateUserResponse = NetworkModule.client.post(NetworkModule.getUrl("/api/v1/registrator/users/create")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(token: String, userId: String): Result<Unit> {
        return try {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/registrator/users/$userId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(token: String, userId: String): Result<ResetPasswordResponse> {
        return try {
            val response: ResetPasswordResponse = NetworkModule.client.post(NetworkModule.getUrl("/api/v1/registrator/users/$userId/reset-password")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("userId", userId)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRoles(token: String, userId: String, roles: List<UserRole>): Result<Unit> {
        return try {
            NetworkModule.client.patch(NetworkModule.getUrl("/api/v1/registrator/users/$userId/roles")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("userId", userId)
                contentType(ContentType.Application.Json)
                setBody(UpdateRolesRequest(roles))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importStudents(token: String, fileBytes: ByteArray, fileName: String): Result<Unit> {
        return try {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/registrator/import/students")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                ))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGroups(token: String): Result<List<GroupDto>> {
        return try {
            val response: List<GroupDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/groups")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(token: String, name: String): Result<Unit> {
        return try {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/groups")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(CreateGroupRequest(name))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(token: String, groupId: Int): Result<Unit> {
        return try {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/$groupId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignCurator(token: String, groupId: Int, curatorId: String): Result<Unit> {
        return try {
            NetworkModule.client.put(NetworkModule.getUrl("/api/v1/groups/$groupId/curator/$curatorId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCurator(token: String, groupId: Int): Result<Unit> {
        return try {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/$groupId/curator")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStudentToGroup(token: String, groupId: Int, studentId: String): Result<Unit> {
        return try {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/groups/$groupId/students/$studentId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeStudentFromGroup(token: String, studentId: String): Result<Unit> {
        return try {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/students/$studentId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeStudentFromAnyGroup(token: String, studentId: String): Result<Unit> {
        return try {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/groups/students/$studentId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
