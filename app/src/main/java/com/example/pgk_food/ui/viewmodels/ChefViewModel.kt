package com.example.pgk_food.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pgk_food.data.local.entity.ScannedQrEntity
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.data.repository.ChefRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.pgk_food.data.remote.dto.QrValidationResponse
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    object Loading : ScanState()
    data class Success(val response: QrValidationResponse) : ScanState()
    data class Error(val message: String) : ScanState()
}

class ChefViewModel(
    private val authRepository: AuthRepository,
    private val chefRepository: ChefRepository
) : ViewModel() {

    private val token get() = authRepository.getToken() ?: ""

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    val scanHistory = chefRepository.getScanHistory()

    init {
        updateUnsyncedCount()
    }

    fun updateUnsyncedCount() {
        viewModelScope.launch {
            _unsyncedCount.value = chefRepository.getUnsyncedCount()
        }
    }

    fun scanQr(qrData: String, isOffline: Boolean = false) {
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            chefRepository.validateQr(token, qrData, isOffline).onSuccess {
                _scanState.value = ScanState.Success(it)
                updateUnsyncedCount()
            }.onFailure {
                _scanState.value = ScanState.Error(it.message ?: "Error")
            }
        }
    }

    fun downloadData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Загрузка ключей...")
            chefRepository.downloadStudentKeys(token).onFailure {
                _syncState.value = SyncState.Error("Ошибка загрузки ключей: ${it.localizedMessage}")
                return@launch
            }

            _syncState.value = SyncState.Loading("Загрузка разрешений...")
            chefRepository.downloadPermissions(token).onSuccess {
                _syncState.value = SyncState.Success("Данные успешно обновлены")
            }.onFailure {
                _syncState.value = SyncState.Error("Ошибка загрузки разрешений: ${it.localizedMessage}")
            }
        }
    }

    fun syncTransactions() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Синхронизация...")
            chefRepository.syncOfflineTransactions(token).onSuccess {
                _syncState.value = SyncState.Success("Синхронизировано: ${it.successCount}")
                updateUnsyncedCount()
            }.onFailure {
                _syncState.value = SyncState.Error("Ошибка синхронизации: ${it.localizedMessage}")
            }
        }
    }
    
    fun resetScanState() {
        _scanState.value = ScanState.Idle
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}


