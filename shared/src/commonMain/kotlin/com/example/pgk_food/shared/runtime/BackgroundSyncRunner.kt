package com.example.pgk_food.shared.runtime

import com.example.pgk_food.shared.data.local.SharedDatabase
import com.example.pgk_food.shared.data.local.entity.UserSessionEntity
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.data.session.SessionSecrets
import com.example.pgk_food.shared.data.session.UserSession
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

private const val SECURE_TOKEN_SENTINEL = "__SECURE_TOKEN__"

enum class BackgroundSyncOutcome {
    SUCCESS,
    SKIPPED,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE,
}

class BackgroundSyncRunner(
    private val orchestrator: DailySyncOrchestrator = DailySyncOrchestrator(
        authRepository = AuthRepository(),
        chefRepository = ChefRepository(),
    ),
) {
    suspend fun runOnce(): BackgroundSyncOutcome {
        val session = runCatching {
            SharedDatabase.instance.userSessionDao().getUserSession().firstOrNull()
        }.getOrElse {
            return BackgroundSyncOutcome.RETRYABLE_FAILURE
        }?.toDomain() ?: return BackgroundSyncOutcome.SKIPPED

        val result = orchestrator.runForSession(session)
        return when (result.overall) {
            SyncExecutionStatus.SUCCESS -> BackgroundSyncOutcome.SUCCESS
            SyncExecutionStatus.SKIPPED -> BackgroundSyncOutcome.SKIPPED
            SyncExecutionStatus.RETRYABLE_FAILURE -> BackgroundSyncOutcome.RETRYABLE_FAILURE
            SyncExecutionStatus.PERMANENT_FAILURE -> BackgroundSyncOutcome.PERMANENT_FAILURE
        }
    }
}

object BackgroundSyncBridge {
    fun runOnceBlocking(): BackgroundSyncOutcome = runBlocking {
        BackgroundSyncRunner().runOnce()
    }

    fun runOnceBlockingNeedsRetry(): Boolean {
        return runOnceBlocking() == BackgroundSyncOutcome.RETRYABLE_FAILURE
    }
}

private fun UserSessionEntity.toDomain(): UserSession = UserSession(
    userId = userId,
    token = SessionSecrets.getToken(userId)
        ?: token.takeIf { it.isNotBlank() && it != SECURE_TOKEN_SENTINEL }
        ?: "",
    roles = roles,
    name = name,
    surname = surname,
    fatherName = fatherName,
    groupId = groupId,
    studentCategory = studentCategory,
    publicKey = publicKey,
    privateKey = SessionSecrets.getPrivateKey(userId) ?: privateKey,
)
