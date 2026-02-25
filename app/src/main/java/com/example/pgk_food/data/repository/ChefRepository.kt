package com.example.pgk_food.data.repository

import com.example.pgk_food.data.local.dao.ScannedQrDao
import com.example.pgk_food.data.local.entity.ScannedQrEntity
import com.example.pgk_food.data.remote.NetworkModule
import com.example.pgk_food.data.remote.dto.QrPayload
import com.example.pgk_food.data.remote.dto.QrValidationRequest
import com.example.pgk_food.data.remote.dto.QrValidationResponse
import com.example.pgk_food.data.remote.dto.CreateMenuItemRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import com.example.pgk_food.data.local.dao.TransactionDao
import com.example.pgk_food.data.local.entity.OfflineTransactionEntity

import com.example.pgk_food.data.local.dao.StudentKeyDao
import com.example.pgk_food.data.local.dao.PermissionCacheDao
import com.example.pgk_food.data.local.entity.StudentKeyEntity
import com.example.pgk_food.data.local.entity.PermissionCacheEntity
import com.example.pgk_food.data.remote.dto.*
import com.example.pgk_food.util.QrCrypto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChefRepository(
    private val scannedQrDao: ScannedQrDao? = null,
    private val transactionDao: TransactionDao? = null,
    private val studentKeyDao: StudentKeyDao? = null,
    private val permissionCacheDao: PermissionCacheDao? = null
) {

    suspend fun downloadStudentKeys(token: String): Result<Unit> {
        return try {
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadPermissions(token: String): Result<Unit> {
        return try {
            val perms: List<StudentPermissionDto> = NetworkModule.client.get(NetworkModule.getUrl("/api/v1/chef/permissions/today")) {
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateQr(token: String, qrContent: String, isOffline: Boolean = false): Result<QrValidationResponse> {
        return try {
            val payload = parseQrPayload(qrContent)
                ?: return Result.failure(IllegalArgumentException("Неверный формат QR-кода"))

            if (isOffline) {
                return validateQrLocal(payload, qrContent)
            }

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
            
            // Save to history if successful
            scannedQrDao?.insert(
                ScannedQrEntity(
                    qrContent = qrContent,
                    studentName = response.studentName ?: "Неизвестно",
                    mealType = response.mealType ?: "Неизвестно",
                    timestamp = System.currentTimeMillis(),
                    status = if (response.isValid) "success" else "error"
                )
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun validateQrLocal(payload: QrPayload, qrContent: String): Result<QrValidationResponse> {
        // 1. Найти ключ
        val key = studentKeyDao?.getKey(payload.userId)
            ?: return Result.success(QrValidationResponse(false, null, null, payload.mealType, "Студент не найден в локальной базе", "USER_NOT_FOUND"))

        // 2. Верифицировать подпись
        val isSigValid = QrCrypto.verifySignature(
            payload.userId, payload.timestamp, payload.mealType, payload.nonce, payload.signature, key.publicKey
        )
        if (!isSigValid) {
            return Result.success(QrValidationResponse(false, "${key.surname} ${key.name}", key.groupName, payload.mealType, "Неверная цифровая подпись", "INVALID_SIGNATURE"))
        }

        // 3. Проверить время (±2 минуты)
        if (!QrCrypto.isTimestampValid(payload.timestamp)) {
            return Result.success(QrValidationResponse(false, "${key.surname} ${key.name}", key.groupName, payload.mealType, "QR-код просрочен", "EXPIRED"))
        }

        // 4. Проверить разрешение
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val perm = permissionCacheDao?.getPermission(payload.userId, dateStr)
        val isAllowed = when(payload.mealType.uppercase()) {
            "BREAKFAST" -> perm?.breakfast ?: false
            "LUNCH" -> perm?.lunch ?: false
            "DINNER" -> perm?.dinner ?: false
            "SNACK" -> perm?.snack ?: false
            "SPECIAL" -> perm?.special ?: false
            else -> false
        }

        if (!isAllowed) {
            return Result.success(QrValidationResponse(false, "${key.surname} ${key.name}", key.groupName, payload.mealType, "Нет разрешения на этот прием пищи", "NO_PERMISSION"))
        }

        // 5. Проверить double-spending (локально)
        val dayStart = (System.currentTimeMillis() / 86400000) * 86400000
        val duplicates = transactionDao?.findByStudentAndMealToday(payload.userId, payload.mealType, dayStart)
        if (!duplicates.isNullOrEmpty()) {
            return Result.success(QrValidationResponse(false, "${key.surname} ${key.name}", key.groupName, payload.mealType, "Уже отмечен сегодня", "ALREADY_EATEN"))
        }

        // 6. Успех! Сохраняем транзакцию
        val txHash = QrCrypto.generateTransactionHash(payload.userId, payload.timestamp, payload.mealType, payload.nonce)
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

        return Result.success(response)
    }

    suspend fun syncOfflineTransactions(token: String): Result<SyncResponse> {
        return try {
            val unsynced = transactionDao?.getUnsyncedTransactions() ?: emptyList()
            if (unsynced.isEmpty()) return Result.success(SyncResponse(0, emptyList()))

            val items = unsynced.map {
                // Преобразуем Long timestamp (секунды) в ISO 8601 для бэкенда
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
                // Для простоты считаем все отправленные успешными, если сервер вернул успех
                // В идеале сервер должен возвращать список ID успешных
                transactionDao?.markSynced(unsynced.map { it.id })
            }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnsyncedCount(): Int {
        return transactionDao?.getUnsyncedCount() ?: 0
    }


    private fun parseQrPayload(qrContent: String): QrPayload? {
        if (qrContent.isBlank()) return null

        if (qrContent.trimStart().startsWith("{")) {
            return runCatching {
                kotlinx.serialization.json.Json {
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

    suspend fun addMenuItem(token: String, request: CreateMenuItemRequest): Result<Unit> {
        return try {
            NetworkModule.client.post(NetworkModule.getUrl("/api/v1/menu")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(token: String, id: String): Result<Unit> {
        return try {
            NetworkModule.client.delete(NetworkModule.getUrl("/api/v1/menu/$id")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
