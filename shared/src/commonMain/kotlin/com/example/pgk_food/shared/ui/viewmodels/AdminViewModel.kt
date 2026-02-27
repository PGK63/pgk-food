package com.example.pgk_food.shared.ui.viewmodels

import com.example.pgk_food.shared.data.remote.dto.DailyReportDto
import com.example.pgk_food.shared.data.remote.dto.FraudReportDto
import com.example.pgk_food.shared.data.repository.AdminRepository
import com.example.pgk_food.shared.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminStatsState {
    data object Idle : AdminStatsState()
    data object Loading : AdminStatsState()
    data class Success(val stats: DailyReportDto) : AdminStatsState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : AdminStatsState()
}

sealed class SuspiciousState {
    data object Idle : SuspiciousState()
    data object Loading : SuspiciousState()
    data class Success(val transactions: List<FraudReportDto>) : SuspiciousState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : SuspiciousState()
}

class AdminViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : KmpViewModelScopeOwner() {
    private val token get() = authRepository.getToken().orEmpty()

    private val _statsState = MutableStateFlow<AdminStatsState>(AdminStatsState.Idle)
    val statsState: StateFlow<AdminStatsState> = _statsState.asStateFlow()

    private val _suspiciousState = MutableStateFlow<SuspiciousState>(SuspiciousState.Idle)
    val suspiciousState: StateFlow<SuspiciousState> = _suspiciousState.asStateFlow()

    fun loadStats(date: String) {
        viewModelScope.launch {
            _statsState.value = AdminStatsState.Loading
            adminRepository.getDailyReport(token, date)
                .onSuccess { _statsState.value = AdminStatsState.Success(it) }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка загрузки отчета")
                    _statsState.value = AdminStatsState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun loadSuspiciousTransactions(startDate: String, endDate: String) {
        viewModelScope.launch {
            _suspiciousState.value = SuspiciousState.Loading
            adminRepository.getFraudReports(token, startDate, endDate)
                .onSuccess { _suspiciousState.value = SuspiciousState.Success(it) }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка загрузки нарушений")
                    _suspiciousState.value = SuspiciousState.Error(err.message, err.code, err.retryable)
                }
        }
    }
}
