package com.example.pgk_food.util

import android.content.Context

class UiSettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShowHints(
        userId: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val safeUserId = safeUserId(userId)
        val firstSeenAt = ensureHintsFirstSeenAt(safeUserId, nowMillis)
        val override = readHintsOverride(safeUserId)
        return HintVisibilityPolicy.resolve(
            firstSeenAtMillis = firstSeenAt,
            nowMillis = nowMillis,
            override = override
        )
    }

    fun areHintsEnabled(userId: String?): Boolean {
        return shouldShowHints(userId)
    }

    fun setHintsOverride(userId: String?, enabled: Boolean) {
        val safeUserId = safeUserId(userId)
        prefs.edit()
            .putBoolean(hintsOverrideKey(safeUserId), enabled)
            // Keep legacy key synchronized for backward compatibility.
            .putBoolean(legacyHintsEnabledKey(safeUserId), enabled)
            .apply()
    }

    fun hideHints(userId: String?) {
        setHintsOverride(userId, false)
    }

    fun setHintsEnabled(userId: String?, enabled: Boolean) {
        setHintsOverride(userId, enabled)
    }

    fun isGuideSeen(userId: String?, guideKey: String): Boolean {
        return prefs.getBoolean(guideKey(userId, guideKey), false)
    }

    fun markGuideSeen(userId: String?, guideKey: String) {
        prefs.edit()
            .putBoolean(guideKey(userId, guideKey), true)
            .apply()
    }

    fun clearGuideSeen(userId: String?, guideKey: String) {
        prefs.edit()
            .remove(guideKey(userId, guideKey))
            .apply()
    }

    private fun ensureHintsFirstSeenAt(safeUserId: String, nowMillis: Long): Long {
        val key = hintsFirstSeenAtKey(safeUserId)
        val existing = prefs.getLong(key, -1L)
        if (existing > 0L) return existing
        prefs.edit().putLong(key, nowMillis).apply()
        return nowMillis
    }

    private fun readHintsOverride(safeUserId: String): Boolean? {
        val overrideKey = hintsOverrideKey(safeUserId)
        if (prefs.contains(overrideKey)) {
            return prefs.getBoolean(overrideKey, true)
        }

        // Legacy fallback: old app versions used this key directly.
        val legacyKey = legacyHintsEnabledKey(safeUserId)
        return if (prefs.contains(legacyKey)) prefs.getBoolean(legacyKey, true) else null
    }

    private fun safeUserId(userId: String?): String {
        return userId?.takeIf { it.isNotBlank() } ?: "anonymous"
    }

    private fun legacyHintsEnabledKey(safeUserId: String): String {
        return "hints_enabled:$safeUserId"
    }

    private fun hintsOverrideKey(safeUserId: String): String {
        return "hints_override:$safeUserId"
    }

    private fun hintsFirstSeenAtKey(safeUserId: String): String {
        return "hints_first_seen_at:$safeUserId"
    }

    private fun guideKey(userId: String?, guideKey: String): String {
        val safeUserId = safeUserId(userId)
        return "guide_seen:$safeUserId:$guideKey"
    }

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
