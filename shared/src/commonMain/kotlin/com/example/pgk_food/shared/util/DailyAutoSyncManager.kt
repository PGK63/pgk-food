package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.platform.PlatformKeyValueStore
import com.example.pgk_food.shared.platform.currentTimeMillis

class DailyAutoSyncManager {
    fun shouldRun(
        scope: String,
        userId: String,
        minIntervalMs: Long = DEFAULT_SYNC_INTERVAL_MS,
        nowMillis: Long = currentTimeMillis(),
    ): Boolean {
        if (scope.isBlank() || userId.isBlank()) return false
        val lastSuccessAt = getLastSuccessAtMillis(scope, userId) ?: return true
        if (nowMillis < lastSuccessAt) return true
        return nowMillis - lastSuccessAt >= minIntervalMs
    }

    fun markRun(
        scope: String,
        userId: String,
        nowMillis: Long = currentTimeMillis(),
    ) {
        if (scope.isBlank() || userId.isBlank()) return
        PlatformKeyValueStore.putLong(STORE, key(scope, userId), nowMillis)
    }

    fun getLastSuccessAtMillis(scope: String, userId: String): Long? {
        val namespacedKey = key(scope, userId)
        if (!PlatformKeyValueStore.contains(STORE, namespacedKey)) return null
        val value = PlatformKeyValueStore.getLong(STORE, namespacedKey, -1L)
        return if (value < 0) null else value
    }

    private fun key(scope: String, userId: String): String = "$scope:$userId"

    companion object {
        private const val STORE = "daily_auto_sync"
        const val DEFAULT_SYNC_INTERVAL_MS: Long = 6L * 60L * 60L * 1000L
        const val STUDENT_KEYS = "student_keys"
        const val CHEF_OFFLINE = "chef_offline"
    }
}
