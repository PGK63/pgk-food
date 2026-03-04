package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.core.network.safeResultApiCall
import com.example.pgk_food.shared.data.remote.dto.CreateMenuItemRequest
import com.example.pgk_food.shared.data.remote.dto.ChefWeeklyReportDto
import com.example.pgk_food.shared.data.remote.dto.MenuItemDto
import com.example.pgk_food.shared.data.remote.dto.QrPayload
import com.example.pgk_food.shared.data.remote.dto.QrValidationRequest
import com.example.pgk_food.shared.data.remote.dto.QrValidationResponse
import com.example.pgk_food.shared.data.remote.dto.StudentKeyDto
import com.example.pgk_food.shared.data.remote.dto.StudentPermissionDto
import com.example.pgk_food.shared.data.remote.dto.SyncResponse
import com.example.pgk_food.shared.data.remote.dto.TransactionSyncItem
import com.example.pgk_food.shared.data.local.SharedDatabase
import com.example.pgk_food.shared.data.local.entity.OfflineTransactionEntity
import com.example.pgk_food.shared.data.local.entity.PermissionCacheEntity
import com.example.pgk_food.shared.data.local.entity.ScannedQrEntity
import com.example.pgk_food.shared.data.local.entity.StudentKeyEntity
import com.example.pgk_food.shared.network.SharedNetworkModule
import com.example.pgk_food.shared.platform.generateOfflineTransactionHash
import com.example.pgk_food.shared.platform.isQrTimestampValid
import com.example.pgk_food.shared.platform.verifyQrSignature
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

data class SharedScannedQrRecord(
    val qrContent: String,
    val studentName: String,
    val mealType: String,
    val timestamp: Long,
    val status: String
)

class ChefRepository {
    suspend fun downloadStudentKeys(token: String): Result<Unit> = safeResultApiCall {
        val keys: List<StudentKeyDto> = SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/chef/keys")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()

        val entities = keys.map {
            StudentKeyEntity(
                userId = it.userId,
                publicKey = it.publicKey,
                name = it.name,
                surname = it.surname,
                fatherName = it.fatherName,
                groupName = it.groupName,
            )
        }
        SharedDatabase.instance.studentKeyDao().clearAll()
        SharedDatabase.instance.studentKeyDao().saveKeys(entities)
        Unit
    }

    suspend fun downloadPermissions(token: String): Result<Unit> = safeResultApiCall {
        val perms: List<StudentPermissionDto> =
            SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/chef/permissions/today")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

        val dateStr = currentDateIsoString()
        val entities = perms.map {
            PermissionCacheEntity(
                id = "${it.studentId}_$dateStr",
                studentId = it.studentId,
                date = dateStr,
                breakfast = it.breakfast,
                lunch = it.lunch,
            )
        }
        SharedDatabase.instance.permissionCacheDao().clearAll()
        SharedDatabase.instance.permissionCacheDao().savePermissions(entities)
        Unit
    }

    suspend fun getWeeklyReport(token: String, weekStart: String): Result<ChefWeeklyReportDto> = safeResultApiCall {
        SharedNetworkModule.client.get(SharedNetworkModule.getUrl("/api/v1/chef/weekly-report")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("weekStart", weekStart)
        }.body()
    }

    suspend fun confirmWeeklyReport(token: String, weekStart: String): Result<Unit> = safeResultApiCall {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/chef/weekly-report/$weekStart/confirm")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun validateQr(token: String, qrContent: String, isOffline: Boolean = false): Result<QrValidationResponse> {
        val payload = parseQrPayload(qrContent)
            ?: return Result.success(
                QrValidationResponse(
                    isValid = false,
                    studentName = null,
                    groupName = null,
                    mealType = null,
                    errorMessage = "Неверный формат QR-кода",
                    errorCode = "INVALID_QR_FORMAT",
                )
            )

        if (isOffline) {
            return runCatching {
                validateQrLocal(payload = payload, qrContent = qrContent)
            }
        }
        return safeResultApiCall {
            validateQrRemote(token = token, payload = payload, qrContent = qrContent)
        }
    }

    private suspend fun validateQrRemote(
        token: String,
        payload: QrPayload,
        qrContent: String,
    ): QrValidationResponse {
        val response: QrValidationResponse = SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/qr/validate")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                QrValidationRequest(
                    userId = payload.userId,
                    timestamp = payload.timestamp,
                    mealType = payload.mealType,
                    nonce = payload.nonce,
                    signature = payload.signature,
                )
            )
        }.body()

        recordScanResult(qrContent = qrContent, response = response)
        return response
    }

    private suspend fun validateQrLocal(payload: QrPayload, qrContent: String): QrValidationResponse {
        val key = SharedDatabase.instance.studentKeyDao().getKey(payload.userId)
            ?: return invalidLocalResponseAndRecord(
                qrContent = qrContent,
                payload = payload,
                studentName = null,
                groupName = null,
                errorMessage = "Студент не найден в локальном хранилище",
                errorCode = "USER_NOT_FOUND",
            )

        val studentName = "${key.surname} ${key.name}".trim()
        val groupName = key.groupName

        val isSigValid = verifyQrSignature(
            userId = payload.userId,
            timestamp = payload.timestamp,
            mealType = payload.mealType,
            nonce = payload.nonce,
            signatureBase64 = payload.signature,
            publicKeyBase64 = key.publicKey,
        )
        if (!isSigValid) {
            return invalidLocalResponseAndRecord(
                qrContent = qrContent,
                payload = payload,
                studentName = studentName,
                groupName = groupName,
                errorMessage = "Неверная цифровая подпись",
                errorCode = "INVALID_SIGNATURE",
            )
        }

        if (!isQrTimestampValid(payload.timestamp)) {
            return invalidLocalResponseAndRecord(
                qrContent = qrContent,
                payload = payload,
                studentName = studentName,
                groupName = groupName,
                errorMessage = "QR-код просрочен",
                errorCode = "EXPIRED_QR",
            )
        }

        val permission = SharedDatabase.instance.permissionCacheDao()
            .getPermission(payload.userId, currentDateIsoString())
        val isAllowed = when (payload.mealType.uppercase()) {
            "BREAKFAST" -> permission?.breakfast ?: false
            "LUNCH" -> permission?.lunch ?: false
            else -> false
        }
        if (!isAllowed) {
            return invalidLocalResponseAndRecord(
                qrContent = qrContent,
                payload = payload,
                studentName = studentName,
                groupName = groupName,
                errorMessage = "Нет разрешения на этот тип питания",
                errorCode = "NO_PERMISSION",
            )
        }

        val transactionDao = SharedDatabase.instance.transactionDao()
        val transactionHash = buildTransactionHash(payload)
        if (transactionDao.existsByTransactionHash(transactionHash) > 0) {
            return invalidLocalResponseAndRecord(
                qrContent = qrContent,
                payload = payload,
                studentName = studentName,
                groupName = groupName,
                errorMessage = "Этот QR-код уже был отсканирован",
                errorCode = "DUPLICATE_SCAN",
            )
        }

        val dayStart = (com.example.pgk_food.shared.platform.currentTimeMillis() / 86_400_000L) * 86_400_000L
        val duplicates = transactionDao.findByStudentAndMealTodayAnyStatus(
            studentId = payload.userId,
            mealType = payload.mealType,
            dayStart = dayStart,
        )
        if (duplicates.isNotEmpty()) {
            return invalidLocalResponseAndRecord(
                qrContent = qrContent,
                payload = payload,
                studentName = studentName,
                groupName = groupName,
                errorMessage = "Питание уже отмечено сегодня",
                errorCode = "ALREADY_EATEN",
            )
        }

        saveOfflineTransaction(
            payload = payload,
            studentName = studentName,
            groupName = groupName,
            transactionHash = transactionHash,
        )

        val response = QrValidationResponse(
            isValid = true,
            studentName = studentName,
            groupName = groupName,
            mealType = payload.mealType,
            errorMessage = null,
            errorCode = null,
        )
        recordScanResult(qrContent = qrContent, response = response)
        return response
    }

    private fun invalidLocalResponse(
        payload: QrPayload,
        studentName: String?,
        groupName: String?,
        errorMessage: String,
        errorCode: String,
    ): QrValidationResponse {
        return QrValidationResponse(
            isValid = false,
            studentName = studentName,
            groupName = groupName,
            mealType = payload.mealType,
            errorMessage = errorMessage,
            errorCode = errorCode,
        )
    }

    private suspend fun invalidLocalResponseAndRecord(
        qrContent: String,
        payload: QrPayload,
        studentName: String?,
        groupName: String?,
        errorMessage: String,
        errorCode: String,
    ): QrValidationResponse {
        val response = invalidLocalResponse(
            payload = payload,
            studentName = studentName,
            groupName = groupName,
            errorMessage = errorMessage,
            errorCode = errorCode,
        )
        recordScanResult(qrContent = qrContent, response = response)
        return response
    }

    private suspend fun saveOfflineTransaction(
        payload: QrPayload,
        studentName: String,
        groupName: String?,
        transactionHash: String,
    ) {
        runCatching {
            SharedDatabase.instance.transactionDao().saveTransaction(
                OfflineTransactionEntity(
                    studentId = payload.userId,
                    studentName = studentName,
                    groupName = groupName,
                    mealType = payload.mealType,
                    timestamp = payload.timestamp,
                    nonce = payload.nonce,
                    signature = payload.signature,
                    transactionHash = transactionHash,
                )
            )
        }
    }

    private suspend fun recordScanResult(qrContent: String, response: QrValidationResponse) {
        val normalizedQrContent = qrContent.trim().ifEmpty { qrContent }
        val now = com.example.pgk_food.shared.platform.currentTimeMillis()
        val record = SharedScannedQrRecord(
            qrContent = normalizedQrContent,
            studentName = response.studentName ?: "Неизвестно",
            mealType = response.mealType ?: "Неизвестно",
            timestamp = now,
            status = if (response.isValid) "success" else "error",
        )
        runCatching {
            val scannedQrDao = SharedDatabase.instance.scannedQrDao()
            val latest = scannedQrDao.getLatestByQrContent(normalizedQrContent)
            if (latest != null && now >= latest.timestamp && now - latest.timestamp <= HISTORY_DEDUPE_WINDOW_MS) {
                scannedQrDao.update(
                    latest.copy(
                        qrContent = record.qrContent,
                        studentName = record.studentName,
                        mealType = record.mealType,
                        timestamp = record.timestamp,
                        status = record.status,
                    )
                )
            } else {
                scannedQrDao.insert(record.toEntity())
            }
        }
    }

    private fun parseQrPayload(qrContent: String): QrPayload? {
        if (qrContent.isBlank()) return null

        if (qrContent.trimStart().startsWith("{")) {
            return runCatching {
                Json { ignoreUnknownKeys = true; isLenient = true }
                    .decodeFromString<QrPayload>(qrContent)
            }.getOrNull()
        }

        val query = qrContent.substringAfter('?', qrContent)
        val parts = query.split("&")
            .mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) {
                    decodeLegacyQueryComponent(kv[0]) to decodeLegacyQueryComponent(kv[1])
                } else {
                    null
                }
            }
            .toMap()

        val nonce = normalizeLegacyBase64Like(parts["nonce"] ?: return null)
        val signature = normalizeLegacyBase64Like(parts["sig"] ?: return null)

        return QrPayload(
            userId = parts["userId"] ?: return null,
            timestamp = parts["ts"]?.toLongOrNull() ?: return null,
            mealType = parts["type"] ?: return null,
            nonce = nonce,
            signature = signature,
        )
    }

    private fun decodeLegacyQueryComponent(source: String): String {
        if (source.isEmpty()) return source
        val out = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            val ch = source[index]
            if (ch == '%' && index + 2 < source.length) {
                val hi = hexDigitToInt(source[index + 1])
                val lo = hexDigitToInt(source[index + 2])
                if (hi >= 0 && lo >= 0) {
                    out.append(((hi shl 4) or lo).toChar())
                    index += 3
                    continue
                }
            }
            out.append(ch)
            index++
        }
        return out.toString()
    }

    private fun normalizeLegacyBase64Like(source: String): String {
        return source.replace(' ', '+')
    }

    private fun hexDigitToInt(ch: Char): Int {
        return when (ch) {
            in '0'..'9' -> ch.code - '0'.code
            in 'a'..'f' -> ch.code - 'a'.code + 10
            in 'A'..'F' -> ch.code - 'A'.code + 10
            else -> -1
        }
    }

    fun getScanHistory(): Flow<List<SharedScannedQrRecord>> {
        return SharedDatabase.instance.scannedQrDao()
            .getAllScannedQrs()
            .map { items -> items.map { it.toDomain() } }
    }

    suspend fun addMenuItem(token: String, request: CreateMenuItemRequest): Result<Unit> = safeResultApiCall {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/menu")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteMenuItem(token: String, id: String): Result<Unit> = safeResultApiCall {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/menu/$id")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun addMenuItemsBatch(
        token: String,
        items: List<CreateMenuItemRequest>,
    ): Result<List<MenuItemDto>> = safeResultApiCall {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/menu/batch")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(items)
        }.body()
    }

    suspend fun syncOfflineTransactions(token: String): Result<SyncResponse> = safeResultApiCall {
        val transactionDao = SharedDatabase.instance.transactionDao()
        val unsynced = transactionDao.getUnsyncedTransactions()
        if (unsynced.isEmpty()) return@safeResultApiCall SyncResponse(0, emptyList())

        val items = unsynced.map {
            TransactionSyncItem(
                studentId = it.studentId,
                mealType = it.mealType,
                transactionHash = it.transactionHash,
                timestampEpochSec = it.timestamp,
            )
        }

        val response: SyncResponse = SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/transactions/batch")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(items)
        }.body()

        if (response.successCount > 0) {
            val syncedIds = resolveSyncedTransactionIds(unsynced, response)
            if (syncedIds.isNotEmpty()) {
                transactionDao.markSynced(syncedIds)
            }
        }
        response
    }

    suspend fun getUnsyncedCount(): Int {
        return runCatching { SharedDatabase.instance.transactionDao().getUnsyncedCount() }.getOrDefault(0)
    }

    private fun resolveSyncedTransactionIds(
        unsynced: List<OfflineTransactionEntity>,
        response: SyncResponse,
    ): List<Int> {
        val cappedSuccessCount = response.successCount.coerceIn(0, unsynced.size)
        if (cappedSuccessCount == 0) return emptyList()
        if (response.processed.isNotEmpty()) {
            val unsyncedByHash = unsynced.associateBy { it.transactionHash }
            val ids = response.processed.asSequence()
                .filter { it.status == "SUCCESS" || it.status == "IDEMPOTENT" }
                .mapNotNull { processed ->
                    val hash = processed.transactionHash ?: return@mapNotNull null
                    unsyncedByHash[hash]?.id
                }
                .toSet()
                .toList()
            return ids.take(cappedSuccessCount)
        }

        // Legacy fallback for older server contracts without per-item statuses.
        return unsynced.take(cappedSuccessCount).map { it.id }
    }

    private companion object {
        const val HISTORY_DEDUPE_WINDOW_MS = 30_000L
    }
}

private fun SharedScannedQrRecord.toEntity() = ScannedQrEntity(
    qrContent = qrContent,
    studentName = studentName,
    mealType = mealType,
    timestamp = timestamp,
    status = status,
)

private fun ScannedQrEntity.toDomain() = SharedScannedQrRecord(
    qrContent = qrContent,
    studentName = studentName,
    mealType = mealType,
    timestamp = timestamp,
    status = status,
)

private fun currentDateIsoString(): String {
    val dt = Instant.fromEpochMilliseconds(com.example.pgk_food.shared.platform.currentTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val y = dt.year.toString().padStart(4, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    return "$y-$m-$d"
}

private fun deterministicFallbackTransactionHash(payload: QrPayload): String {
    return "${payload.userId}:${payload.timestamp}:${payload.mealType}:${payload.nonce}"
}

private fun buildTransactionHash(payload: QrPayload): String {
    return generateOfflineTransactionHash(
        userId = payload.userId,
        timestamp = payload.timestamp,
        mealType = payload.mealType,
        nonce = payload.nonce,
    ).ifBlank { deterministicFallbackTransactionHash(payload) }
}
