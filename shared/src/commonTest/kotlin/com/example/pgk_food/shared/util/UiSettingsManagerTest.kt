package com.example.pgk_food.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class UiSettingsManagerTest {
    @Test
    fun uiScale_uses_default_when_not_saved() {
        val manager = UiSettingsManager(store = FakeUiSettingsStore())
        assertEquals(UiSettingsManager.UI_SCALE_DEFAULT_PERCENT, manager.getUiScalePercent())
    }

    @Test
    fun uiScale_clamps_and_persists_value() {
        val store = FakeUiSettingsStore()
        val manager = UiSettingsManager(store = store)

        manager.setUiScalePercent(40)
        assertEquals(UiSettingsManager.UI_SCALE_MIN_PERCENT, manager.getUiScalePercent())

        manager.setUiScalePercent(200)
        assertEquals(UiSettingsManager.UI_SCALE_MAX_PERCENT, manager.getUiScalePercent())

        manager.setUiScalePercent(112)
        assertEquals(112, manager.getUiScalePercent())
    }
}

private class FakeUiSettingsStore : UiSettingsStore {
    private val data = mutableMapOf<String, Any>()
    private fun key(store: String, key: String): String = "$store::$key"

    override fun contains(store: String, key: String): Boolean = data.containsKey(key(store, key))

    override fun getString(store: String, key: String, default: String): String =
        data[key(store, key)] as? String ?: default

    override fun putString(store: String, key: String, value: String) {
        data[key(store, key)] = value
    }

    override fun getBoolean(store: String, key: String, default: Boolean): Boolean =
        data[key(store, key)] as? Boolean ?: default

    override fun putBoolean(store: String, key: String, value: Boolean) {
        data[key(store, key)] = value
    }

    override fun getLong(store: String, key: String, default: Long): Long =
        data[key(store, key)] as? Long ?: default

    override fun putLong(store: String, key: String, value: Long) {
        data[key(store, key)] = value
    }

    override fun remove(store: String, key: String) {
        data.remove(key(store, key))
    }
}
