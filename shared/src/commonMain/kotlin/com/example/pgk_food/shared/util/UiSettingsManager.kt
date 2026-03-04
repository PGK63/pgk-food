package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.platform.PlatformKeyValueStore
import com.example.pgk_food.shared.platform.currentTimeMillis

enum class HintScreenKey {
    STUDENT_DASHBOARD,
    STUDENT_COUPONS,
    STUDENT_QR,
    STUDENT_MENU,
    CHEF_DASHBOARD,
    CHEF_SCANNER,
    CHEF_MENU_MANAGE,
    CHEF_STATS,
    REGISTRATOR_DASHBOARD,
    REGISTRATOR_USERS,
    REGISTRATOR_USER_CREATE,
    REGISTRATOR_GROUPS,
    CURATOR_DASHBOARD,
    CURATOR_ROSTER,
    CURATOR_STATS,
    CURATOR_CATEGORIES,
    CURATOR_REPORTS,
    ADMIN_DASHBOARD,
    ADMIN_REPORTS,
}

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
        if (enabled) {
            clearAllScreenHints(userId)
        }
    }

    fun hideHints(userId: String?) = setHintsOverride(userId, false)
    fun setHintsEnabled(userId: String?, enabled: Boolean) = setHintsOverride(userId, enabled)

    fun shouldShowScreenHints(
        userId: String?,
        screen: HintScreenKey,
        nowMillis: Long = currentTimeMillis(),
    ): Boolean {
        val globalVisible = shouldShowHints(userId, nowMillis)
        val safeUserId = safeUserId(userId)
        val isScreenHidden = PlatformKeyValueStore.getBoolean(PREFS_NAME, hiddenScreenKey(safeUserId, screen), false)
        return HintVisibilityPolicy.resolveScreen(globalVisible, isScreenHidden)
    }

    fun hideScreenHints(userId: String?, screen: HintScreenKey) {
        val safeUserId = safeUserId(userId)
        PlatformKeyValueStore.putBoolean(PREFS_NAME, hiddenScreenKey(safeUserId, screen), true)
    }

    fun clearScreenHints(userId: String?, screen: HintScreenKey) {
        val safeUserId = safeUserId(userId)
        PlatformKeyValueStore.remove(PREFS_NAME, hiddenScreenKey(safeUserId, screen))
    }

    fun clearAllScreenHints(userId: String?) {
        val safeUserId = safeUserId(userId)
        HintScreenKey.values().forEach { screen ->
            PlatformKeyValueStore.remove(PREFS_NAME, hiddenScreenKey(safeUserId, screen))
        }
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
    private fun hiddenScreenKey(safeUserId: String, screen: HintScreenKey): String =
        "hints_hidden_screen:$safeUserId:${screen.name}"

    companion object {
        private const val PREFS_NAME = "ui_settings"
    }
}

internal object HintVisibilityPolicy {
    private const val HINT_WINDOW_MILLIS = 72L * 60L * 60L * 1000L
    fun resolve(firstSeenAtMillis: Long, nowMillis: Long, override: Boolean?): Boolean {
        if (override != null) return override
        if (firstSeenAtMillis <= 0L) return true
        return nowMillis - firstSeenAtMillis <= HINT_WINDOW_MILLIS
    }

    fun resolveScreen(globalVisible: Boolean, isScreenHidden: Boolean): Boolean {
        return globalVisible && !isScreenHidden
    }
}
