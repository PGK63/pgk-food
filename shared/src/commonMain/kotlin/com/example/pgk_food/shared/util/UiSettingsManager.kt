package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.platform.PlatformKeyValueStore
import com.example.pgk_food.shared.platform.currentTimeMillis

class UiSettingsManager {
    fun shouldShowHints(
        userId: String?,
        nowMillis: Long = currentTimeMillis(),
    ): Boolean {
        val safeUserId = safeUserId(userId)
        val firstSeenAt = ensureHintsFirstSeenAt(safeUserId, nowMillis)
        val override = readHintsOverride(safeUserId)
        return HintVisibilityPolicy.resolve(firstSeenAt, nowMillis, override)
    }

    fun areHintsEnabled(userId: String?): Boolean = shouldShowHints(userId)

    fun setHintsOverride(userId: String?, enabled: Boolean) {
        val safeUserId = safeUserId(userId)
        PlatformKeyValueStore.putBoolean(PREFS_NAME, hintsOverrideKey(safeUserId), enabled)
        PlatformKeyValueStore.putBoolean(PREFS_NAME, legacyHintsEnabledKey(safeUserId), enabled)
    }

    fun hideHints(userId: String?) = setHintsOverride(userId, false)
    fun setHintsEnabled(userId: String?, enabled: Boolean) = setHintsOverride(userId, enabled)

    fun isGuideSeen(userId: String?, guideKey: String): Boolean =
        PlatformKeyValueStore.getBoolean(PREFS_NAME, guideKey(userId, guideKey), false)

    fun markGuideSeen(userId: String?, guideKey: String) {
        PlatformKeyValueStore.putBoolean(PREFS_NAME, guideKey(userId, guideKey), true)
    }

    fun clearGuideSeen(userId: String?, guideKey: String) {
        PlatformKeyValueStore.remove(PREFS_NAME, guideKey(userId, guideKey))
    }

    private fun ensureHintsFirstSeenAt(safeUserId: String, nowMillis: Long): Long {
        val key = hintsFirstSeenAtKey(safeUserId)
        val existing = PlatformKeyValueStore.getLong(PREFS_NAME, key, -1L)
        if (existing > 0L) return existing
        PlatformKeyValueStore.putLong(PREFS_NAME, key, nowMillis)
        return nowMillis
    }

    private fun readHintsOverride(safeUserId: String): Boolean? {
        val overrideKey = hintsOverrideKey(safeUserId)
        if (PlatformKeyValueStore.contains(PREFS_NAME, overrideKey)) {
            return PlatformKeyValueStore.getBoolean(PREFS_NAME, overrideKey, true)
        }
        val legacyKey = legacyHintsEnabledKey(safeUserId)
        return if (PlatformKeyValueStore.contains(PREFS_NAME, legacyKey)) {
            PlatformKeyValueStore.getBoolean(PREFS_NAME, legacyKey, true)
        } else {
            null
        }
    }

    private fun safeUserId(userId: String?): String = userId?.takeIf { it.isNotBlank() } ?: "anonymous"
    private fun legacyHintsEnabledKey(safeUserId: String): String = "hints_enabled:$safeUserId"
    private fun hintsOverrideKey(safeUserId: String): String = "hints_override:$safeUserId"
    private fun hintsFirstSeenAtKey(safeUserId: String): String = "hints_first_seen_at:$safeUserId"
    private fun guideKey(userId: String?, guideKey: String): String = "guide_seen:${safeUserId(userId)}:$guideKey"

    companion object {
        private const val PREFS_NAME = "ui_settings"
        const val GUIDE_CSV_IMPORT = "csv_import"
        const val GUIDE_MANUAL_USER_CREATE = "manual_user_create"
        const val GUIDE_GROUP_TRANSFER = "group_transfer"
    }
}

internal object HintVisibilityPolicy {
    private const val HINT_WINDOW_MILLIS = 72L * 60L * 60L * 1000L
    fun resolve(firstSeenAtMillis: Long, nowMillis: Long, override: Boolean?): Boolean {
        if (override != null) return override
        if (firstSeenAtMillis <= 0L) return true
        return nowMillis - firstSeenAtMillis <= HINT_WINDOW_MILLIS
    }
}
