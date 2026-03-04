package com.example.pgk_food.shared.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val SECURE_PREF_NAME = "pgk_food_secure_store"

private fun securePrefs(): SharedPreferences {
    val context: Context = requireAndroidAppContext()
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        SECURE_PREF_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}

actual object PlatformSecureStore {
    actual fun contains(key: String): Boolean = securePrefs().contains(key)

    actual fun getString(key: String): String? = securePrefs().getString(key, null)

    actual fun putString(key: String, value: String) {
        securePrefs().edit().putString(key, value).apply()
    }

    actual fun remove(key: String) {
        securePrefs().edit().remove(key).apply()
    }
}
