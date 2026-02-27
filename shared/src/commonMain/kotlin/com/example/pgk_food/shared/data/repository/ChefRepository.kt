package com.example.pgk_food.shared.data.repository

import com.example.pgk_food.shared.data.remote.dto.CreateMenuItemRequest
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val _scanHistory = MutableStateFlow<List<SharedScannedQrRecord>>(emptyList())

    suspend fun downloadStudentKeys(token: String): Result<Unit> = runCatching {
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

    suspend fun downloadPermissions(token: String): Result<Unit> = runCatching {
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
                dinner = it.dinner,
                snack = it.snack,
                special = it.special,
            )
        }
        SharedDatabase.instance.permissionCacheDao().clearAll()
        SharedDatabase.instance.permissionCacheDao().savePermissions(entities)
        Unit
    }

    suspend fun validateQr(token: String, qrContent: String, isOffline: Boolean = false): Result<QrValidationResponse> = runCatching {
        val payload = parseQrPayload(qrContent)
            ?: error("Неверный формат QR-кода")

        val endpoint = if (isOffline) "/api/v1/qr/validate-offline" else "/api/v1/qr/validate"
        val response: QrValidationResponse = SharedNetworkModule.client.post(SharedNetworkModule.getUrl(endpoint)) {
            if (!isOffline) header(HttpHeaders.Authorization, "Bearer $token")
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
        val record = SharedScannedQrRecord(
            qrContent = qrContent,
            studentName = response.studentName ?: "Неизвестно",
            mealType = response.mealType ?: "Неизвестно",
            timestamp = com.example.pgk_food.shared.platform.currentTimeMillis(),
            status = if (response.isValid) "success" else "error"
        )
        _scanHistory.value = listOf(record) + _scanHistory.value
        runCatching {
            SharedDatabase.instance.scannedQrDao().insert(record.toEntity())
        }
        if (isOffline && response.isValid) {
            runCatching {
                SharedDatabase.instance.transactionDao().saveTransaction(
                    OfflineTransactionEntity(
                        studentId = payload.userId,
                        studentName = response.studentName ?: "Неизвестно",
                        groupName = response.groupName,
                        mealType = payload.mealType,
                        timestamp = payload.timestamp,
                        nonce = payload.nonce,
                        signature = payload.signature,
                        transactionHash = fallbackTransactionHash(payload),
                    )
                )
            }
        }
        response
    }

    private fun parseQrPayload(qrContent: String): QrPayload? {
        if (qrContent.isBlank()) return null

        if (qrContent.trimStart().startsWith("{")) {
            return runCatching {
                Json { ignoreUnknownKeys = true; isLenient = true }
                    .decodeFromString<QrPayload>(qrContent)
            }.getOrNull()
        }

        val parts = qrContent.split("&")
            .mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }
            .toMap()

        return QrPayload(
            userId = parts["userId"] ?: return null,
            timestamp = parts["ts"]?.toLongOrNull() ?: return null,
            mealType = parts["type"] ?: return null,
            nonce = parts["nonce"] ?: return null,
            signature = parts["sig"] ?: return null
        )
    }

    fun getScanHistory(): Flow<List<SharedScannedQrRecord>> {
        return SharedDatabase.instance.scannedQrDao()
            .getAllScannedQrs()
            .map { items -> items.map { it.toDomain() } }
    }

    suspend fun addMenuItem(token: String, request: CreateMenuItemRequest): Result<Unit> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/menu")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteMenuItem(token: String, id: String): Result<Unit> = runCatching {
        SharedNetworkModule.client.delete(SharedNetworkModule.getUrl("/api/v1/menu/$id")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun addMenuItemsBatch(
        token: String,
        items: List<CreateMenuItemRequest>,
    ): Result<List<MenuItemDto>> = runCatching {
        SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/menu/batch")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(items)
        }.body()
    }

    suspend fun syncOfflineTransactions(token: String): Result<SyncResponse> = runCatching {
        val transactionDao = SharedDatabase.instance.transactionDao()
        val unsynced = transactionDao.getUnsyncedTransactions()
        if (unsynced.isEmpty()) return@runCatching SyncResponse(0, emptyList())

        val items = unsynced.map {
            TransactionSyncItem(
                studentId = it.studentId,
                timestamp = epochSecondsToLocalIso(it.timestamp),
                mealType = it.mealType,
                transactionHash = it.transactionHash,
            )
        }

        val response: SyncResponse = SharedNetworkModule.client.post(SharedNetworkModule.getUrl("/api/v1/transactions/batch")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(items)
        }.body()

        if (response.successCount > 0) {
            transactionDao.markSynced(unsynced.map { it.id })
        }
        response
    }

    suspend fun getUnsyncedCount(): Int {
        return runCatching { SharedDatabase.instance.transactionDao().getUnsyncedCount() }.getOrDefault(0)
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

private fun epochSecondsToLocalIso(epochSeconds: Long): String {
    val dt = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
    val y = dt.year.toString().padStart(4, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    val hh = dt.hour.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    val ss = dt.second.toString().padStart(2, '0')
    return "$y-$m-$d" + "T$hh:$mm:$ss"
}

private fun fallbackTransactionHash(payload: QrPayload): String {
    val raw = "${payload.userId}|${payload.timestamp}|${payload.mealType}|${payload.nonce}"
    return raw.hashCode().toUInt().toString(16)
}
