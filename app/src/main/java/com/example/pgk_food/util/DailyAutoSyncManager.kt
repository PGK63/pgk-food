package com.example.pgk_food.util

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

class DailyAutoSyncManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldRun(scope: String, userId: String, dateKey: String = todayKey()): Boolean {
        return prefs.getString(key(scope, userId), null) != dateKey
    }

    fun markRun(scope: String, userId: String, dateKey: String = todayKey()) {
        prefs.edit()
            .putString(key(scope, userId), dateKey)
            .apply()
    }

    fun todayKey(): String = LocalDate.now(ZoneId.systemDefault()).toString()

    private fun key(scope: String, userId: String): String = "$scope:$userId"

    companion object {
        private const val PREFS_NAME = "daily_auto_sync"
        const val STUDENT_KEYS = "student_keys"
        const val CHEF_OFFLINE = "chef_offline"
    }
}

