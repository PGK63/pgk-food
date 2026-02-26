package com.example.pgk_food.data.repository

import com.example.pgk_food.core.network.ApiError
import com.example.pgk_food.core.network.ApiResult
import com.example.pgk_food.core.network.safeApiCall
import com.example.pgk_food.data.local.dao.PermissionCacheDao
import com.example.pgk_food.data.local.dao.ScannedQrDao
import com.example.pgk_food.data.local.dao.StudentKeyDao
import com.example.pgk_food.data.local.dao.TransactionDao
import com.example.pgk_food.data.local.entity.OfflineTransactionEntity
import com.example.pgk_food.data.local.entity.PermissionCacheEntity
import com.example.pgk_food.data.local.entity.ScannedQrEntity
import com.example.pgk_food.data.local.entity.StudentKeyEntity
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.CreateMenuItemRequest
import com.example.pgk_food.data.remote.dto.MenuItemDto
import com.example.pgk_food.data.remote.dto.QrPayload
import com.example.pgk_food.data.remote.dto.QrValidationRequest
import com.example.pgk_food.data.remote.dto.QrValidationResponse
import com.example.pgk_food.data.remote.dto.StudentKeyDto
import com.example.pgk_food.data.remote.dto.StudentPermissionDto
import com.example.pgk_food.data.remote.dto.SyncResponse
import com.example.pgk_food.data.remote.dto.TransactionSyncItem
import com.example.pgk_food.util.QrCrypto
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChefRepository(
    private val scannedQrDao: ScannedQrDao? = null,
    private val transactionDao: TransactionDao? = null,
    private val studentKeyDao: StudentKeyDao? = null,
    private val permissionCacheDao: PermissionCacheDao? = null
) {

    suspend fun downloadStudentKeys(token: String): ApiResult<Unit> {
        return safeApiCall {
            val keys: List<StudentKeyDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/chef/keys")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

            val entities = keys.map {
                StudentKeyEntity(
                    userId = it.userId,
                    publicKey = it.publicKey,
                    name = it.name,
                    surname = it.surname,
                    fatherName = it.fatherName,
                    groupName = it.groupName
                )
            }
            studentKeyDao?.clearAll()
            studentKeyDao?.saveKeys(entities)
            Unit
        }
    }

    suspend fun downloadPermissions(token: String): ApiResult<Unit> {
        return safeApiCall {
            val perms: List<StudentPermissionDto> =
                NetworkModule.client.get(NetworkModule.getUrl("/api/v1/chef/permissions/today")) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body()

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val entities = perms.map {
                PermissionCacheEntity(
                    id = "${it.studentId}_$dateStr",
                    studentId = it.studentId,
                    date = dateStr,
                    breakfast = it.breakfast,
                    lunch = it.lunch,
                    dinner = it.dinner,
                    snack = it.snack,
                    special = it.special
                )
            }
            permissionCacheDao?.clearAll()
            permissionCacheDao?.savePermissions(entities)
            Unit
        }
    }

    suspend fun validateQr(token: String, qrContent: String, isOffline: Boolean = false): ApiResult<QrValidationResponse> {
        val payload = parseQrPayload(qrContent)
            ?: return ApiResult.Failure(
                ApiError(
                    code = "INVALID_QR_FORMAT",
                    userMessage = "Неверный формат QR-кода",
                    retryable = true
                )
            )

        if (isOffline) {
            return validateQrLocal(payload, qrContent)
        }

        val remoteResult = safeApiCall {
            val response: QrValidationResponse = NetworkModule.client.post(NetworkModule.getUrl("/api/v1/qr/validate")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(
                    QrValidationRequest(
                        userId = payload.userId,
                        timestamp = payload.timestamp,
                        mealType = payload.mealType,
                        nonce = payload.nonce,
                        signature = payload.signature
                    )
                )
            }.body()

            scannedQrDao?.insert(
                ScannedQrEntity(
                    qrContent = qrContent,
                    studentName = response.studentName ?: "Неизвестно",
                    mealType = response.mealType ?: "Неизвестно",
                    timestamp = System.currentTimeMillis(),
                    status = if (response.isValid) "success" else "error"
                )
            )

            response
        }

        return remoteResult
    }

    private suspend fun validateQrLocal(payload: QrPayload, qrContent: String): ApiResult<QrValidationResponse> {
        val key = studentKeyDao?.getKey(payload.userId)
            ?: return ApiResult.Success(
                QrValidationResponse(
                    isValid = false,
                    studentName = null,
                    groupName = null,
                    mealType = payload.mealType,
                    errorMessage = "Студент не найден в локальном хранилище",
                    errorCode = "USER_NOT_FOUND"
                )
            )

        val isSigValid = QrCrypto.verifySignature(
            payload.userId,
            payload.timestamp,
            payload.mealType,
            payload.nonce,
            payload.signature,
            key.publicKey
        )
        if (!isSigValid) {
            return ApiResult.Success(
                QrValidationResponse(
                    isValid = false,
                    studentName = "${key.surname} ${key.name}",
                    groupName = key.groupName,
                    mealType = payload.mealType,
                    errorMessage = "Неверная цифровая подпись",
                    errorCode = "INVALID_SIGNATURE"
                )
            )
        }

        if (!QrCrypto.isTimestampValid(payload.timestamp)) {
            return ApiResult.Success(
                QrValidationResponse(
                    isValid = false,
                    studentName = "${key.surname} ${key.name}",
                    groupName = key.groupName,
                    mealType = payload.mealType,
                    errorMessage = "QR-код просрочен",
                    errorCode = "EXPIRED_QR"
                )
            )
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val perm = permissionCacheDao?.getPermission(payload.userId, dateStr)
        val isAllowed = when (payload.mealType.uppercase()) {
            "BREAKFAST" -> perm?.breakfast ?: false
            "LUNCH" -> perm?.lunch ?: false
            "DINNER" -> perm?.dinner ?: false
            "SNACK" -> perm?.snack ?: false
            "SPECIAL" -> perm?.special ?: false
            else -> false
        }

        if (!isAllowed) {
            return ApiResult.Success(
                QrValidationResponse(
                    isValid = false,
                    studentName = "${key.surname} ${key.name}",
                    groupName = key.groupName,
                    mealType = payload.mealType,
                    errorMessage = "Нет разрешения на этот тип питания",
                    errorCode = "NO_PERMISSION"
                )
            )
        }

        val dayStart = (System.currentTimeMillis() / 86400000) * 86400000
        val duplicates = transactionDao?.findByStudentAndMealToday(payload.userId, payload.mealType, dayStart)
        if (!duplicates.isNullOrEmpty()) {
            return ApiResult.Success(
                QrValidationResponse(
                    isValid = false,
                    studentName = "${key.surname} ${key.name}",
                    groupName = key.groupName,
                    mealType = payload.mealType,
                    errorMessage = "Питание уже отмечено сегодня",
                    errorCode = "ALREADY_EATEN"
                )
            )
        }

        val txHash = QrCrypto.generateTransactionHash(
            payload.userId,
            payload.timestamp,
            payload.mealType,
            payload.nonce
        )
        transactionDao?.saveTransaction(
            OfflineTransactionEntity(
                studentId = payload.userId,
                studentName = "${key.surname} ${key.name}",
                groupName = key.groupName,
                mealType = payload.mealType,
                timestamp = payload.timestamp,
                nonce = payload.nonce,
                signature = payload.signature,
                transactionHash = txHash
            )
        )

        val response = QrValidationResponse(
            isValid = true,
            studentName = "${key.surname} ${key.name}",
            groupName = key.groupName,
            mealType = payload.mealType,
            errorMessage = null
        )

        scannedQrDao?.insert(
            ScannedQrEntity(
                qrContent = qrContent,
                studentName = response.studentName ?: "Неизвестно",
                mealType = response.mealType ?: "Неизвестно",
                timestamp = System.currentTimeMillis(),
                status = "success"
            )
        )

        return ApiResult.Success(response)
    }

    suspend fun syncOfflineTransactions(token: String): ApiResult<SyncResponse> {
        return safeApiCall {
            val unsynced = transactionDao?.getUnsyncedTransactions() ?: emptyList()
            if (unsynced.isEmpty()) {
                return@safeApiCall SyncResponse(0, emptyList())
            }

            val items = unsynced.map {
                val date = Date(it.timestamp * 1000)
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(date)
                TransactionSyncItem(
                    studentId = it.studentId,
                    timestamp = isoDate,
                    mealType = it.mealType,
                    transactionHash = it.transactionHash
                )
            }

            val response: SyncResponse = NetworkModule.client.post(NetworkModule.getUrl("/api/v1/transactions/batch")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(items)
            }.body()

            if (response.successCount > 0) {
                transactionDao?.markSynced(unsynced.map { it.id })
            }
            response
        }
    }

    suspend fun getUnsyncedCount(): Int {
        return transactionDao?.getUnsyncedCount() ?: 0
    }

    private fun parseQrPayload(qrContent: String): QrPayload? {
        if (qrContent.isBlank()) return null

        if (qrContent.trimStart().startsWith("{")) {
            return runCatching {
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.decodeFromString(QrPayload.serializer(), qrContent)
            }.getOrNull()
        }

        val parts = qrContent.split("&")
            .mapNotNull { it.split("=", limit = 2).takeIf { kv -> kv.size == 2 } }
            .associate { it[0] to it[1] }

        val userId = parts["userId"] ?: return null
        val timestamp = parts["ts"]?.toLongOrNull() ?: return null
        val mealType = parts["type"] ?: return null
        val nonce = parts["nonce"] ?: return null
        val signature = parts["sig"] ?: return null

        return QrPayload(
            userId = userId,
            timestamp = timestamp,
            mealType = mealType,
            nonce = nonce,
            signature = signature
        )
    }

    fun getScanHistory(): Flow<List<ScannedQrEntity>>? {
        return scannedQrDao?.getAllScannedQrs()
    }

    suspend fun addMenuItem(token: String, request: CreateMenuItemRequest): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/menu")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Unit
        }
    }

    suspend fun deleteMenuItem(token: String, id: String): ApiResult<Unit> {
        return safeApiCall {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/menu/$id")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Unit
        }
    }

    suspend fun addMenuItemsBatch(
        token: String,
        items: List<CreateMenuItemRequest>
    ): ApiResult<List<MenuItemDto>> {
        return safeApiCall {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/menu/batch")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(items)
            }.body()
        }
    }
}
