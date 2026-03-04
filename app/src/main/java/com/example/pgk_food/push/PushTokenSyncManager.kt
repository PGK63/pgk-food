package com.example.pgk_food.push

import android.content.Context
import android.util.Log
import com.example.pgk_food.shared.data.remote.dto.PushTokenRegisterRequest
import com.example.pgk_food.shared.data.repository.NotificationRepository
import com.example.pgk_food.shared.data.session.SessionStore
import com.example.pgk_food.shared.data.session.UserSession
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object PushTokenSyncManager {
    private const val LOG_TAG = "PushTokenSyncManager"
    private const val PREFS_NAME = "push_token_sync"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_REGISTERED_USER_ID = "registered_user_id"
    private const val KEY_REGISTERED_TOKEN = "registered_token"
    private const val PLATFORM_ANDROID = "ANDROID"

    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationRepository = NotificationRepository()

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        SessionStore.ensureRestored()
        refreshFirebaseToken(appContext)

        scope.launch {
            var previousSession: UserSession? = null
            SessionStore.session.collectLatest { currentSession ->
                val currentToken = getFcmToken(appContext)

                val hadSessionAndLoggedOut = previousSession != null && currentSession == null
                if (hadSessionAndLoggedOut && !currentToken.isNullOrBlank()) {
                    unregisterToken(previousSession!!.token, currentToken)
                    clearRegistrationCache(appContext)
                }

                if (currentSession != null && !currentToken.isNullOrBlank()) {
                    val userChanged = previousSession?.userId != null &&
                        previousSession?.userId != currentSession.userId
                    maybeRegister(
                        context = appContext,
                        session = currentSession,
                        fcmToken = currentToken,
                        force = userChanged,
                    )
                } else if (currentSession == null) {
                    Log.d(LOG_TAG, "Skip push token register: no active session")
                } else {
                    Log.d(LOG_TAG, "Skip push token register: no FCM token available")
                }

                previousSession = currentSession
            }
        }
    }

    fun onNewToken(context: Context, token: String) {
        val normalized = token.trim()
        if (normalized.isBlank()) return
        val appContext = context.applicationContext
        saveFcmToken(appContext, normalized)
        scope.launch {
            val session = SessionStore.session.value
            if (session == null) {
                Log.d(LOG_TAG, "Skip push token register after token refresh: no active session")
                return@launch
            }
            maybeRegister(appContext, session, normalized, force = true)
        }
    }

    fun forceSync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val session = SessionStore.session.value
            if (session == null) {
                Log.d(LOG_TAG, "Skip push force sync: no active session")
                return@launch
            }
            val token = getFcmToken(appContext)
            if (token == null) {
                Log.d(LOG_TAG, "Skip push force sync: no FCM token available")
                return@launch
            }
            maybeRegister(appContext, session, token, force = true)
        }
    }

    private suspend fun maybeRegister(
        context: Context,
        session: UserSession,
        fcmToken: String,
        force: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val registeredUserId = prefs.getString(KEY_REGISTERED_USER_ID, null)
        val registeredToken = prefs.getString(KEY_REGISTERED_TOKEN, null)
        if (!force && registeredUserId == session.userId && registeredToken == fcmToken) {
            Log.d(LOG_TAG, "Skip push token register: registration cache hit")
            return
        }

        val result = notificationRepository.registerPushToken(
            token = session.token,
            request = PushTokenRegisterRequest(
                token = fcmToken,
                platform = PLATFORM_ANDROID,
                appVersion = appVersion(context),
                locale = Locale.getDefault().toLanguageTag(),
            ),
        )
        if (result.isSuccess) {
            prefs.edit()
                .putString(KEY_REGISTERED_USER_ID, session.userId)
                .putString(KEY_REGISTERED_TOKEN, fcmToken)
                .apply()
        } else {
            Log.w(LOG_TAG, "Push token registration failed")
        }
    }

    private suspend fun unregisterToken(authToken: String, fcmToken: String) {
        notificationRepository.unregisterPushToken(
            token = authToken,
            pushToken = fcmToken,
        )
    }

    private fun saveFcmToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    private fun getFcmToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun clearRegistrationCache(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_REGISTERED_USER_ID)
            .remove(KEY_REGISTERED_TOKEN)
            .apply()
    }

    private fun refreshFirebaseToken(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(LOG_TAG, "Failed to fetch FCM token", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result?.trim().orEmpty()
            if (token.isNotBlank()) {
                onNewToken(context, token)
            }
        }
    }

    private fun appVersion(context: Context): String? {
        return runCatching {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pkgInfo.versionName?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
