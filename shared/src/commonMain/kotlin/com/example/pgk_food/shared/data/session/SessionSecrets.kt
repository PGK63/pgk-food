package com.example.pgk_food.shared.data.session

import com.example.pgk_food.shared.platform.PlatformSecureStore

internal object SessionSecrets {
    private const val TOKEN_PREFIX = "session_token:"
    private const val PRIVATE_KEY_PREFIX = "session_private_key:"

    fun saveToken(userId: String, token: String) {
        PlatformSecureStore.putString(TOKEN_PREFIX + userId, token)
    }

    fun getToken(userId: String): String? = PlatformSecureStore.getString(TOKEN_PREFIX + userId)

    fun savePrivateKey(userId: String, privateKey: String) {
        PlatformSecureStore.putString(PRIVATE_KEY_PREFIX + userId, privateKey)
    }

    fun getPrivateKey(userId: String): String? = PlatformSecureStore.getString(PRIVATE_KEY_PREFIX + userId)

    fun removeToken(userId: String) {
        PlatformSecureStore.remove(TOKEN_PREFIX + userId)
    }

    fun removePrivateKey(userId: String) {
        PlatformSecureStore.remove(PRIVATE_KEY_PREFIX + userId)
    }

    fun removeSecrets(userId: String) {
        removeToken(userId)
        removePrivateKey(userId)
    }
}
