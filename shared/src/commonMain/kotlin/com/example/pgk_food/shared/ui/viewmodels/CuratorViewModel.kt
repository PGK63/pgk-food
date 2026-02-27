package com.example.pgk_food.shared.ui.viewmodels

import com.example.pgk_food.shared.data.remote.dto.RosterDeadlineNotificationDto
import com.example.pgk_food.shared.data.remote.dto.SaveRosterRequest
import com.example.pgk_food.shared.data.remote.dto.StudentMealStatus
import com.example.pgk_food.shared.data.remote.dto.StudentRosterDto
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.CuratorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RosterState {
    data object Idle : RosterState()
    data object Loading : RosterState()
    data class Success(val roster: List<StudentRosterDto>) : RosterState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : RosterState()
}

sealed class StatsState {
    data object Idle : StatsState()
    data object Loading : StatsState()
    data class Success(val stats: List<StudentMealStatus>) : StatsState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : StatsState()
}

class CuratorViewModel(
    private val authRepository: AuthRepository,
    private val curatorRepository: CuratorRepository,
) : KmpViewModelScopeOwner() {
    private val token get() = authRepository.getToken().orEmpty()

    private val _rosterState = MutableStateFlow<RosterState>(RosterState.Idle)
    val rosterState: StateFlow<RosterState> = _rosterState.asStateFlow()

    private val _statsState = MutableStateFlow<StatsState>(StatsState.Idle)
    val statsState: StateFlow<StatsState> = _statsState.asStateFlow()

    private val _notificationState = MutableStateFlow<RosterDeadlineNotificationDto?>(null)
    val notificationState: StateFlow<RosterDeadlineNotificationDto?> = _notificationState.asStateFlow()

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus: StateFlow<String?> = _saveStatus.asStateFlow()

    fun loadRoster(date: String) {
        viewModelScope.launch {
            _rosterState.value = RosterState.Loading
            curatorRepository.getRoster(token, date)
                .onSuccess { _rosterState.value = RosterState.Success(it) }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка загрузки табеля")
                    _rosterState.value = RosterState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun saveRoster(request: SaveRosterRequest) {
        viewModelScope.launch {
            _saveStatus.value = "Сохранение..."
            curatorRepository.updateRoster(token, request)
                .onSuccess { _saveStatus.value = "Сохранено" }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка сохранения")
                    _saveStatus.value = "Ошибка сохранения [${err.code}]: ${err.message}"
                }
        }
    }

    fun loadStats(date: String) {
        viewModelScope.launch {
            _statsState.value = StatsState.Loading
            curatorRepository.getMyGroupStatistics(token, date)
                .onSuccess { _statsState.value = StatsState.Success(it) }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка загрузки статистики")
                    _statsState.value = StatsState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            curatorRepository.getRosterDeadlineNotification(token).onSuccess { _notificationState.value = it }
        }
    }

    fun clearSaveStatus() {
        _saveStatus.value = null
    }
}
