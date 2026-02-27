package com.example.pgk_food.shared.platform

expect object PlatformKeyValueStore {
    fun contains(store: String, key: String): Boolean
    fun getBoolean(store: String, key: String, default: Boolean): Boolean
    fun putBoolean(store: String, key: String, value: Boolean)
    fun getLong(store: String, key: String, default: Long): Long
    fun putLong(store: String, key: String, value: Long)
    fun remove(store: String, key: String)
}
