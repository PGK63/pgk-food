package com.example.pgk_food.shared.ui.viewmodels

import com.example.pgk_food.shared.data.remote.dto.QrValidationResponse
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.ChefRepository
import com.example.pgk_food.shared.util.NetworkMonitor
import com.example.pgk_food.shared.util.UxAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class ScanState {
    data object Idle : ScanState()
    data object Loading : ScanState()
    data class Success(val response: QrValidationResponse) : ScanState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : ScanState()
}

sealed class SyncState {
    data object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : SyncState()
}

class ChefViewModel(
    private val authRepository: AuthRepository,
    private val chefRepository: ChefRepository,
    private val networkMonitor: NetworkMonitor,
) : KmpViewModelScopeOwner() {
    private val token get() = authRepository.getToken().orEmpty()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    val scanHistory = chefRepository.getScanHistory()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        updateUnsyncedCount()
        viewModelScope.launch {
            networkMonitor.isConnected.collectLatest { isConnected ->
                _isOffline.value = !isConnected
                if (
                    isConnected &&
                    _unsyncedCount.value > 0 &&
                    _syncState.value !is SyncState.Loading
                ) {
                    syncTransactions()
                }
            }
        }
    }

    fun updateUnsyncedCount() {
        viewModelScope.launch {
            _unsyncedCount.value = chefRepository.getUnsyncedCount()
        }
    }

    fun scanQr(qrData: String) {
        val offlineScan = _isOffline.value
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            UxAnalytics.log(event = "action_started", role = "CHEF", screen = "SCAN_QR")
            chefRepository.validateQr(token, qrData, offlineScan)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "CHEF", screen = "SCAN_QR")
                    notifySuccess("Сканирование завершено")
                    _scanState.value = ScanState.Success(it)
                    updateUnsyncedCount()
                }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка сканирования")
                    UxAnalytics.log(event = "action_error", role = "CHEF", screen = "SCAN_QR", code = err.code)
                    notifyError("Ошибка сканирования [${err.code}]")
                    _scanState.value = ScanState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun downloadData() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Загрузка ключей...")
            UxAnalytics.log(event = "action_started", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA")

            val keysResult = chefRepository.downloadStudentKeys(token)
            if (keysResult.isFailure) {
                val err = keysResult.exceptionOrNull()?.toErrorInfo("Ошибка загрузки данных") ?: ErrorInfo("Ошибка загрузки данных")
                UxAnalytics.log(event = "action_error", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA", code = err.code)
                notifyError("Ошибка загрузки данных [${err.code}]")
                _syncState.value = SyncState.Error(err.message, err.code, err.retryable)
                return@launch
            }

            _syncState.value = SyncState.Loading("Загрузка разрешений...")
            chefRepository.downloadPermissions(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA")
                    notifySuccess("Офлайн-данные обновлены")
                    _syncState.value = SyncState.Success("Офлайн-данные обновлены")
                }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка загрузки данных")
                    UxAnalytics.log(event = "action_error", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA", code = err.code)
                    notifyError("Ошибка загрузки данных [${err.code}]")
                    _syncState.value = SyncState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun syncTransactions() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Синхронизация...")
            UxAnalytics.log(event = "action_started", role = "CHEF", screen = "SYNC_TRANSACTIONS")
            chefRepository.syncOfflineTransactions(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "CHEF", screen = "SYNC_TRANSACTIONS")
                    notifySuccess("Синхронизация выполнена")
                    _syncState.value = SyncState.Success("Синхронизировано: ${it.successCount}")
                    updateUnsyncedCount()
                }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка синхронизации")
                    UxAnalytics.log(event = "action_error", role = "CHEF", screen = "SYNC_TRANSACTIONS", code = err.code)
                    notifyError("Ошибка синхронизации [${err.code}]")
                    _syncState.value = SyncState.Error(err.message, err.code, err.retryable)
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
