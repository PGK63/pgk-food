package com.example.pgk_food.shared.runtime

import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.data.session.UserSession
import com.example.pgk_food.shared.model.UserRole
import com.example.pgk_food.shared.util.DailyAutoSyncManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DailySyncOrchestrator(
    private val authRepository: AuthRepository = AuthRepository(),
    private val chefRepository: ChefRepository = ChefRepository(),
    private val dailyAutoSyncManager: DailyAutoSyncManager = DailyAutoSyncManager(),
) {
    private val mutex = Mutex()

    suspend fun runForSession(session: UserSession) {
        if (session.userId.isBlank() || session.token.isBlank()) return
        mutex.withLock {
            runStudentDailySyncIfNeeded(session)
            runChefDailySyncIfNeeded(session)
        }
    }

    private suspend fun runStudentDailySyncIfNeeded(session: UserSession) {
        if (UserRole.STUDENT !in session.roles) return
        if (!dailyAutoSyncManager.shouldRun(DailyAutoSyncManager.STUDENT_KEYS, session.userId)) return

        val keysResult = authRepository.getMyKeys(session.token)
        if (keysResult.isSuccess) {
            dailyAutoSyncManager.markRun(DailyAutoSyncManager.STUDENT_KEYS, session.userId)
        }
    }

    private suspend fun runChefDailySyncIfNeeded(session: UserSession) {
        if (UserRole.CHEF !in session.roles) return
        if (!dailyAutoSyncManager.shouldRun(DailyAutoSyncManager.CHEF_OFFLINE, session.userId)) return

        val keysResult = chefRepository.downloadStudentKeys(session.token)
        if (keysResult.isFailure) return

        val permissionsResult = chefRepository.downloadPermissions(session.token)
        if (permissionsResult.isSuccess) {
            dailyAutoSyncManager.markRun(DailyAutoSyncManager.CHEF_OFFLINE, session.userId)
        }
    }
}
