package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.platform.PlatformKeyValueStore
import com.example.pgk_food.shared.platform.currentTimeMillis
import kotlin.math.roundToInt

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

interface UiSettingsStore {
    fun contains(store: String, key: String): Boolean
    fun getString(store: String, key: String, default: String): String
    fun putString(store: String, key: String, value: String)
    fun getBoolean(store: String, key: String, default: Boolean): Boolean
    fun putBoolean(store: String, key: String, value: Boolean)
    fun getLong(store: String, key: String, default: Long): Long
    fun putLong(store: String, key: String, value: Long)
    fun remove(store: String, key: String)
}

object PlatformUiSettingsStore : UiSettingsStore {
    override fun contains(store: String, key: String): Boolean =
        PlatformKeyValueStore.contains(store, key)

    override fun getString(store: String, key: String, default: String): String =
        PlatformKeyValueStore.getString(store, key, default)

    override fun putString(store: String, key: String, value: String) {
        PlatformKeyValueStore.putString(store, key, value)
    }

    override fun getBoolean(store: String, key: String, default: Boolean): Boolean =
        PlatformKeyValueStore.getBoolean(store, key, default)

    override fun putBoolean(store: String, key: String, value: Boolean) {
        PlatformKeyValueStore.putBoolean(store, key, value)
    }

    override fun getLong(store: String, key: String, default: Long): Long =
        PlatformKeyValueStore.getLong(store, key, default)

    override fun putLong(store: String, key: String, value: Long) {
        PlatformKeyValueStore.putLong(store, key, value)
    }

    override fun remove(store: String, key: String) {
        PlatformKeyValueStore.remove(store, key)
    }
}

class UiSettingsManager(
    private val store: UiSettingsStore = PlatformUiSettingsStore,
) {
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
        store.putBoolean(PREFS_NAME, hintsOverrideKey(safeUserId), enabled)
        store.putBoolean(PREFS_NAME, legacyHintsEnabledKey(safeUserId), enabled)
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
        val isScreenHidden = store.getBoolean(PREFS_NAME, hiddenScreenKey(safeUserId, screen), false)
        return HintVisibilityPolicy.resolveScreen(globalVisible, isScreenHidden)
    }

    fun hideScreenHints(userId: String?, screen: HintScreenKey) {
        val safeUserId = safeUserId(userId)
        store.putBoolean(PREFS_NAME, hiddenScreenKey(safeUserId, screen), true)
    }

    fun clearScreenHints(userId: String?, screen: HintScreenKey) {
        val safeUserId = safeUserId(userId)
        store.remove(PREFS_NAME, hiddenScreenKey(safeUserId, screen))
    }

    fun clearAllScreenHints(userId: String?) {
        val safeUserId = safeUserId(userId)
        HintScreenKey.values().forEach { screen ->
            store.remove(PREFS_NAME, hiddenScreenKey(safeUserId, screen))
        }
    }

    fun getUiScalePercent(): Int {
        val rawValue = store.getLong(PREFS_NAME, UI_SCALE_PERCENT_KEY, UI_SCALE_DEFAULT_PERCENT.toLong())
            .toInt()
        return UiScalePolicy.clamp(rawValue)
    }

    fun setUiScalePercent(percent: Int) {
        store.putLong(PREFS_NAME, UI_SCALE_PERCENT_KEY, UiScalePolicy.clamp(percent).toLong())
    }

    fun clampUiScalePercent(percent: Int): Int = UiScalePolicy.clamp(percent)

    fun sliderPositionToPercent(position: Int): Int = UiScalePolicy.sliderToPercent(position)

    fun percentToSliderPosition(percent: Int): Int = UiScalePolicy.percentToSlider(percent)

    private fun ensureHintsFirstSeenAt(safeUserId: String, nowMillis: Long): Long {
        val key = hintsFirstSeenAtKey(safeUserId)
        val existing = store.getLong(PREFS_NAME, key, -1L)
        if (existing > 0L) return existing
        store.putLong(PREFS_NAME, key, nowMillis)
        return nowMillis
    }

    private fun readHintsOverride(safeUserId: String): Boolean? {
        val overrideKey = hintsOverrideKey(safeUserId)
        if (store.contains(PREFS_NAME, overrideKey)) {
            return store.getBoolean(PREFS_NAME, overrideKey, true)
        }
        val legacyKey = legacyHintsEnabledKey(safeUserId)
        return if (store.contains(PREFS_NAME, legacyKey)) {
            store.getBoolean(PREFS_NAME, legacyKey, true)
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
        const val UI_SCALE_MIN_PERCENT: Int = 85
        const val UI_SCALE_MAX_PERCENT: Int = 115
        const val UI_SCALE_DEFAULT_PERCENT: Int = 100
        const val UI_SCALE_SLIDER_MIN: Int = 98
        const val UI_SCALE_SLIDER_ANCHOR: Int = 102
        const val UI_SCALE_SLIDER_MAX: Int = 115
        private const val PREFS_NAME = "ui_settings"
        private const val UI_SCALE_PERCENT_KEY = "ui_scale_percent"
    }
}

internal object UiScalePolicy {
    private const val PercentAtAnchor = UiSettingsManager.UI_SCALE_DEFAULT_PERCENT
    private const val PercentAtSliderMin = UiSettingsManager.UI_SCALE_MIN_PERCENT
    private const val PercentAtSliderMax = UiSettingsManager.UI_SCALE_MAX_PERCENT

    fun clamp(percent: Int): Int {
        return percent.coerceIn(
            minimumValue = UiSettingsManager.UI_SCALE_MIN_PERCENT,
            maximumValue = UiSettingsManager.UI_SCALE_MAX_PERCENT,
        )
    }

    fun sliderToPercent(position: Int): Int {
        val clamped = position.coerceIn(
            UiSettingsManager.UI_SCALE_SLIDER_MIN,
            UiSettingsManager.UI_SCALE_SLIDER_MAX
        )
        if (clamped <= UiSettingsManager.UI_SCALE_SLIDER_ANCHOR) {
            val progress = (clamped - UiSettingsManager.UI_SCALE_SLIDER_MIN).toFloat() /
                (UiSettingsManager.UI_SCALE_SLIDER_ANCHOR - UiSettingsManager.UI_SCALE_SLIDER_MIN).toFloat()
            val value = PercentAtSliderMin + progress * (PercentAtAnchor - PercentAtSliderMin)
            return clamp(value.roundToInt())
        }
        val progress = (clamped - UiSettingsManager.UI_SCALE_SLIDER_ANCHOR).toFloat() /
            (UiSettingsManager.UI_SCALE_SLIDER_MAX - UiSettingsManager.UI_SCALE_SLIDER_ANCHOR).toFloat()
        val value = PercentAtAnchor + progress * (PercentAtSliderMax - PercentAtAnchor)
        return clamp(value.roundToInt())
    }

    fun percentToSlider(percent: Int): Int {
        val clampedPercent = clamp(percent)
        if (clampedPercent <= PercentAtAnchor) {
            val progress = (clampedPercent - PercentAtSliderMin).toFloat() /
                (PercentAtAnchor - PercentAtSliderMin).toFloat()
            val value = UiSettingsManager.UI_SCALE_SLIDER_MIN +
                progress * (UiSettingsManager.UI_SCALE_SLIDER_ANCHOR - UiSettingsManager.UI_SCALE_SLIDER_MIN)
            return value.roundToInt().coerceIn(
                UiSettingsManager.UI_SCALE_SLIDER_MIN,
                UiSettingsManager.UI_SCALE_SLIDER_MAX,
            )
        }
        val progress = (clampedPercent - PercentAtAnchor).toFloat() /
            (PercentAtSliderMax - PercentAtAnchor).toFloat()
        val value = UiSettingsManager.UI_SCALE_SLIDER_ANCHOR +
            progress * (UiSettingsManager.UI_SCALE_SLIDER_MAX - UiSettingsManager.UI_SCALE_SLIDER_ANCHOR)
        return value.roundToInt().coerceIn(
            UiSettingsManager.UI_SCALE_SLIDER_MIN,
            UiSettingsManager.UI_SCALE_SLIDER_MAX,
        )
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
