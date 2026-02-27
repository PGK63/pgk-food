package com.example.pgk_food.shared.platform

import android.content.Context
import android.content.SharedPreferences

internal lateinit var appContext: Context

fun initAndroidPlatformContext(context: Context) {
    appContext = context.applicationContext
}

internal fun requireAndroidAppContext(): Context {
    check(::appContext.isInitialized) { "Android platform context is not initialized" }
    return appContext
}

private fun prefs(name: String): SharedPreferences {
    return requireAndroidAppContext().getSharedPreferences(name, Context.MODE_PRIVATE)
}

actual object PlatformKeyValueStore {
    actual fun contains(store: String, key: String): Boolean = prefs(store).contains(key)

    actual fun getString(store: String, key: String, default: String): String =
        prefs(store).getString(key, default) ?: default

    actual fun putString(store: String, key: String, value: String) {
        prefs(store).edit().putString(key, value).apply()
    }

    actual fun getBoolean(store: String, key: String, default: Boolean): Boolean =
        prefs(store).getBoolean(key, default)

    actual fun putBoolean(store: String, key: String, value: Boolean) {
        prefs(store).edit().putBoolean(key, value).apply()
    }

    actual fun getLong(store: String, key: String, default: Long): Long =
        prefs(store).getLong(key, default)

    actual fun putLong(store: String, key: String, value: Long) {
        prefs(store).edit().putLong(key, value).apply()
    }

    actual fun remove(store: String, key: String) {
        prefs(store).edit().remove(key).apply()
    }
}
