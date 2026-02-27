package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.platform.PlatformKeyValueStore
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class DailyAutoSyncManager {
    fun shouldRun(scope: String, userId: String, dateKey: String = todayKey()): Boolean {
        return readString(key(scope, userId)) != dateKey
    }

    fun markRun(scope: String, userId: String, dateKey: String = todayKey()) {
        writeString(key(scope, userId), dateKey)
    }

    fun todayKey(): String {
        val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val y = dt.year.toString().padStart(4, '0')
        val m = dt.monthNumber.toString().padStart(2, '0')
        val d = dt.dayOfMonth.toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    private fun key(scope: String, userId: String): String = "$scope:$userId"

    private fun readString(key: String): String? {
        val store = STORE
        if (!PlatformKeyValueStore.contains(store, key)) return null
        return PlatformStringStore.get(store, key)
    }

    private fun writeString(key: String, value: String) {
        PlatformStringStore.put(STORE, key, value)
    }

    companion object {
        private const val STORE = "daily_auto_sync"
        const val STUDENT_KEYS = "student_keys"
        const val CHEF_OFFLINE = "chef_offline"
    }
}

expect object PlatformStringStore {
    fun get(store: String, key: String): String?
    fun put(store: String, key: String, value: String)
}
