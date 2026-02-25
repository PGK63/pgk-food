package com.example.pgk_food.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pgk_food.data.remote.dto.DailyReportDto
import com.example.pgk_food.data.remote.dto.FraudReportDto
import com.example.pgk_food.data.repository.AdminRepository
import com.example.pgk_food.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminStatsState {
    object Idle : AdminStatsState()
    object Loading : AdminStatsState()
    data class Success(val stats: DailyReportDto) : AdminStatsState()
    data class Error(val message: String) : AdminStatsState()
}

sealed class SuspiciousState {
    object Idle : SuspiciousState()
    object Loading : SuspiciousState()
    data class Success(val transactions: List<FraudReportDto>) : SuspiciousState()
    data class Error(val message: String) : SuspiciousState()
}

class AdminViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {
    private val token get() = authRepository.getToken() ?: ""

    private val _statsState = MutableStateFlow<AdminStatsState>(AdminStatsState.Idle)
    val statsState: StateFlow<AdminStatsState> = _statsState.asStateFlow()

    private val _suspiciousState = MutableStateFlow<SuspiciousState>(SuspiciousState.Idle)
    val suspiciousState: StateFlow<SuspiciousState> = _suspiciousState.asStateFlow()

    fun loadStats(date: String) {
        viewModelScope.launch {
            _statsState.value = AdminStatsState.Loading
            adminRepository.getDailyReport(token, date).onSuccess {
                _statsState.value = AdminStatsState.Success(it)
            }.onFailure {
                _statsState.value = AdminStatsState.Error(it.message ?: "Error")
            }
        }
    }

    fun loadSuspiciousTransactions() {
        viewModelScope.launch {
            _suspiciousState.value = SuspiciousState.Loading
            adminRepository.getFraudReports(token, "", "").onSuccess {
                _suspiciousState.value = SuspiciousState.Success(it)
            }.onFailure {
                _suspiciousState.value = SuspiciousState.Error(it.message ?: "Error")
            }
        }
    }
}

