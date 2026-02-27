package com.example.pgk_food.shared.ui.viewmodels

import com.example.pgk_food.shared.data.remote.dto.LoginRequest
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.util.UxAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : KmpViewModelScopeOwner() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(loginReq: LoginRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            UxAnalytics.log(event = "action_started", role = "AUTH", screen = "LOGIN")
            authRepository.login(loginReq)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "AUTH", screen = "LOGIN")
                    notifySuccess("Вход выполнен")
                    _authState.value = AuthState.Success
                }
                .onFailure {
                    val err = it.toErrorInfo("Не удалось войти")
                    UxAnalytics.log(event = "action_error", role = "AUTH", screen = "LOGIN", code = err.code)
                    notifyError("Ошибка входа [${err.code}]")
                    _authState.value = AuthState.Error("Не удалось войти [${err.code}]: ${err.message}")
                }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
