package com.example.pgk_food.shared.platform

expect object PlatformSecureStore {
    fun contains(key: String): Boolean
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}
