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
            _session.value = entity?.toDomain()
            _isRestored.value = true
        }
    }

    fun save(session: UserSession) {
        _session.value = session
        scope.launch {
            SharedDatabase.instance.userSessionDao().saveSession(session.toEntity())
        }
    }

    fun clear() {
        _session.value = null
        scope.launch {
            SharedDatabase.instance.userSessionDao().clearSession()
        }
    }
}

private fun UserSession.toEntity() = UserSessionEntity(
    userId = userId,
    token = token,
    roles = roles,
    name = name,
    surname = surname,
    fatherName = fatherName,
    groupId = groupId,
    publicKey = publicKey,
    privateKey = privateKey
)

private fun UserSessionEntity.toDomain() = UserSession(
    userId = userId,
    token = token,
    roles = roles,
    name = name,
    surname = surname,
    fatherName = fatherName,
    groupId = groupId,
    publicKey = publicKey,
    privateKey = privateKey
)
