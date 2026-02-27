package com.example.pgk_food.shared.util

import platform.Foundation.NSUserDefaults

actual object PlatformStringStore {
    private fun prefixed(store: String, key: String) = "$store:$key"
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual fun get(store: String, key: String): String? =
        defaults.stringForKey(prefixed(store, key))

    actual fun put(store: String, key: String, value: String) {
        defaults.setObject(value, forKey = prefixed(store, key))
    }
}
