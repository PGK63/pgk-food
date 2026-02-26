package com.example.pgk_food.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pgk_food.core.feedback.FeedbackController
import com.example.pgk_food.data.remote.dto.LoginRequest
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.util.UxAnalytics
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

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(loginReq: LoginRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            UxAnalytics.log(event = "action_started", role = "AUTH", screen = "LOGIN")
            authRepository.login(loginReq)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "AUTH", screen = "LOGIN")
                    FeedbackController.success("Вход выполнен")
                    _authState.value = AuthState.Success
                }
                .onFailure {
                    UxAnalytics.log(event = "action_error", role = "AUTH", screen = "LOGIN", code = it.code)
                    FeedbackController.error("Ошибка входа [${it.code}]")
                    _authState.value = AuthState.Error("Не удалось войти [${it.code}]: ${it.userMessage}")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

