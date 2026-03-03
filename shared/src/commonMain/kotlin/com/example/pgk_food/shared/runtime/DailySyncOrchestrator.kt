package com.example.pgk_food.shared.runtime

import com.example.pgk_food.shared.core.network.ApiCallException
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.util.DailyAutoSyncManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncExecutionStatus {
    SUCCESS,
    SKIPPED,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE,
}

data class DailySyncRunResult(
    val student: SyncExecutionStatus,
    val chef: SyncExecutionStatus,
) {
    val overall: SyncExecutionStatus = when {
        student == SyncExecutionStatus.RETRYABLE_FAILURE || chef == SyncExecutionStatus.RETRYABLE_FAILURE ->
            SyncExecutionStatus.RETRYABLE_FAILURE
        student == SyncExecutionStatus.PERMANENT_FAILURE || chef == SyncExecutionStatus.PERMANENT_FAILURE ->
            SyncExecutionStatus.PERMANENT_FAILURE
        student == SyncExecutionStatus.SUCCESS || chef == SyncExecutionStatus.SUCCESS ->
            SyncExecutionStatus.SUCCESS
        else -> SyncExecutionStatus.SKIPPED
    }
}

class DailySyncOrchestrator(
    private val authRepository: AuthRepository = AuthRepository(),
    private val chefRepository: ChefRepository = ChefRepository(),
    private val dailyAutoSyncManager: DailyAutoSyncManager = DailyAutoSyncManager(),
) {
    private val mutex = Mutex()

    suspend fun runForSession(session: UserSession): DailySyncRunResult {
        if (session.userId.isBlank() || session.token.isBlank()) {
            return DailySyncRunResult(
                student = SyncExecutionStatus.SKIPPED,
                chef = SyncExecutionStatus.SKIPPED
            )
        }

        return mutex.withLock {
            val studentResult = runStudentDailySyncIfNeeded(session)
            val chefResult = runChefDailySyncIfNeeded(session)
            DailySyncRunResult(
                student = studentResult,
                chef = chefResult
            )
        }
    }

    private suspend fun runStudentDailySyncIfNeeded(session: UserSession): SyncExecutionStatus {
        if (UserRole.STUDENT !in session.roles) return SyncExecutionStatus.SKIPPED
        if (!dailyAutoSyncManager.shouldRun(DailyAutoSyncManager.STUDENT_KEYS, session.userId)) {
            return SyncExecutionStatus.SKIPPED
        }

        val keysResult = authRepository.getMyKeys(session.token)
        return if (keysResult.isSuccess) {
            dailyAutoSyncManager.markRun(
                scope = DailyAutoSyncManager.STUDENT_KEYS,
                userId = session.userId,
            )
            SyncExecutionStatus.SUCCESS
        } else {
            keysResult.exceptionOrNull().toSyncFailureStatus()
        }
    }

    private suspend fun runChefDailySyncIfNeeded(session: UserSession): SyncExecutionStatus {
        if (UserRole.CHEF !in session.roles) return SyncExecutionStatus.SKIPPED
        if (!dailyAutoSyncManager.shouldRun(DailyAutoSyncManager.CHEF_OFFLINE, session.userId)) {
            return SyncExecutionStatus.SKIPPED
        }

        val keysResult = chefRepository.downloadStudentKeys(session.token)
        if (keysResult.isFailure) {
            return keysResult.exceptionOrNull().toSyncFailureStatus()
        }

        val permissionsResult = chefRepository.downloadPermissions(session.token)
        return if (permissionsResult.isSuccess) {
            dailyAutoSyncManager.markRun(
                scope = DailyAutoSyncManager.CHEF_OFFLINE,
                userId = session.userId,
            )
            SyncExecutionStatus.SUCCESS
        } else {
            permissionsResult.exceptionOrNull().toSyncFailureStatus()
        }
    }
}

private fun Throwable?.toSyncFailureStatus(): SyncExecutionStatus {
    val retryable = (this as? ApiCallException)?.apiError?.retryable == true
    return if (retryable) {
        SyncExecutionStatus.RETRYABLE_FAILURE
    } else {
        SyncExecutionStatus.PERMANENT_FAILURE
    }
}
