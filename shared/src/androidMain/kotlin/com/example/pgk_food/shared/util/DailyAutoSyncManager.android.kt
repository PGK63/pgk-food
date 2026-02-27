package com.example.pgk_food.shared.util

import android.content.Context
import com.example.pgk_food.shared.platform.requireAndroidAppContext

actual object PlatformStringStore {
    private fun prefs(name: String) =
        requireAndroidAppContext().getSharedPreferences(name, Context.MODE_PRIVATE)

    actual fun get(store: String, key: String): String? = prefs(store).getString(key, null)

    actual fun put(store: String, key: String, value: String) {
        prefs(store).edit().putString(key, value).apply()
    }
}
