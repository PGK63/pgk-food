package com.example.pgk_food.shared.data.session

import com.example.pgk_food.shared.data.local.SharedDatabase
import com.example.pgk_food.shared.data.local.entity.UserSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private const val SECURE_TOKEN_SENTINEL = "__SECURE_TOKEN__"

object SessionStore {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()
    private val _isRestored = MutableStateFlow(false)
    val isRestored: StateFlow<Boolean> = _isRestored.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restored = false

    fun ensureRestored() {
        if (restored) return
        restored = true
        scope.launch {
            val entity = runCatching {
                SharedDatabase.instance.userSessionDao().getUserSession().firstOrNull()
            }.getOrNull()
            val restoredSession = entity?.toDomainSecureAware()
            _session.value = restoredSession
            if (restoredSession != null) {
                runCatching {
                    SharedDatabase.instance.userSessionDao().saveSession(restoredSession.toEntity())
                }
            }
            _isRestored.value = true
        }
    }

    fun save(session: UserSession) {
        SessionSecrets.saveToken(session.userId, session.token)
        if (!session.privateKey.isNullOrBlank()) {
            SessionSecrets.savePrivateKey(session.userId, session.privateKey)
        } else {
            SessionSecrets.removePrivateKey(session.userId)
        }
        _session.value = session
        scope.launch {
            SharedDatabase.instance.userSessionDao().saveSession(session.toEntity())
        }
    }

    fun clear() {
        _session.value?.userId?.let { userId ->
            SessionSecrets.removeSecrets(userId)
        }
        _session.value = null
        scope.launch {
            SharedDatabase.instance.userSessionDao().clearSession()
        }
    }
}

private fun UserSession.toEntity() = UserSessionEntity(
    userId = userId,
    token = SECURE_TOKEN_SENTINEL,
    roles = roles,
    name = name,
    surname = surname,
    fatherName = fatherName,
    groupId = groupId,
    studentCategory = studentCategory,
    publicKey = publicKey,
    privateKey = null,
)

private fun UserSessionEntity.toDomainSecureAware(): UserSession? {
    val secureToken = SessionSecrets.getToken(userId)
    val legacyToken = token.takeIf { it.isNotBlank() && it != SECURE_TOKEN_SENTINEL }
    if (secureToken == null && legacyToken != null) {
        SessionSecrets.saveToken(userId, legacyToken)
    }
    val tokenValue = secureToken ?: legacyToken ?: return null

    val securePrivateKey = SessionSecrets.getPrivateKey(userId)
    val legacyPrivateKey = privateKey.takeIf { !it.isNullOrBlank() }
    if (securePrivateKey == null && legacyPrivateKey != null) {
        SessionSecrets.savePrivateKey(userId, legacyPrivateKey)
    }
    val privateKeyValue = securePrivateKey ?: legacyPrivateKey

    return UserSession(
        userId = userId,
        token = tokenValue,
        roles = roles,
        name = name,
        surname = surname,
        fatherName = fatherName,
        groupId = groupId,
        studentCategory = studentCategory,
        publicKey = publicKey,
        privateKey = privateKeyValue,
    )
}
