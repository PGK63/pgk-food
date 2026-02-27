package com.example.pgk_food.shared.platform

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

actual object PlatformKeyValueStore {
    private fun prefixed(store: String, key: String): String = "$store:$key"
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual fun contains(store: String, key: String): Boolean =
        defaults.objectForKey(prefixed(store, key)) != null

    actual fun getString(store: String, key: String, default: String): String =
        defaults.stringForKey(prefixed(store, key)) ?: default

    actual fun putString(store: String, key: String, value: String) {
        defaults.setObject(value, forKey = prefixed(store, key))
    }

    actual fun getBoolean(store: String, key: String, default: Boolean): Boolean {
        val value = defaults.objectForKey(prefixed(store, key)) as? NSNumber ?: return default
        return value.boolValue
    }

    actual fun putBoolean(store: String, key: String, value: Boolean) {
        defaults.setBool(value, forKey = prefixed(store, key))
    }

    actual fun getLong(store: String, key: String, default: Long): Long {
        val value = defaults.objectForKey(prefixed(store, key)) as? NSNumber ?: return default
        return value.longLongValue
    }

    actual fun putLong(store: String, key: String, value: Long) {
        defaults.setInteger(value, forKey = prefixed(store, key))
    }

    actual fun remove(store: String, key: String) {
        defaults.removeObjectForKey(prefixed(store, key))
    }
}
