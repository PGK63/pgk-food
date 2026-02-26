package com.example.pgk_food.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pgk_food.core.feedback.FeedbackController
import com.example.pgk_food.data.remote.dto.QrValidationResponse
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.data.repository.ChefRepository
import com.example.pgk_food.util.NetworkMonitor
import com.example.pgk_food.util.UxAnalytics
import kotlinx.coroutines.Dispatchers
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
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

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
                if (isConnected && _unsyncedCount.value > 0) {
                    syncTransactions()
                }
            }
        }
    }

    fun updateUnsyncedCount() {
        viewModelScope.launch(Dispatchers.IO) {
            _unsyncedCount.value = chefRepository.getUnsyncedCount()
        }
    }

    fun scanQr(qrData: String, forceOffline: Boolean = false) {
        val isOfflineScan = forceOffline || _isOffline.value
        viewModelScope.launch(Dispatchers.IO) {
            _scanState.value = ScanState.Loading
            UxAnalytics.log(event = "action_started", role = "CHEF", screen = "SCAN_QR")
            chefRepository.validateQr(token, qrData, isOfflineScan)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "CHEF", screen = "SCAN_QR")
                    FeedbackController.success("Сканирование завершено")
                    _scanState.value = ScanState.Success(it)
                    updateUnsyncedCount()
                }
                .onFailure {
                    UxAnalytics.log(event = "action_error", role = "CHEF", screen = "SCAN_QR", code = it.code)
                    FeedbackController.error("Ошибка сканирования [${it.code}]")
                    _scanState.value = ScanState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
                }
        }
    }

    fun downloadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Loading("Загрузка ключей...")
            UxAnalytics.log(event = "action_started", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA")
            val keysResult = chefRepository.downloadStudentKeys(token)
            if (keysResult is com.example.pgk_food.core.network.ApiResult.Failure) {
                UxAnalytics.log(event = "action_error", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA", code = keysResult.error.code)
                FeedbackController.error("Ошибка загрузки данных [${keysResult.error.code}]")
                _syncState.value = SyncState.Error(
                    message = keysResult.error.userMessage,
                    code = keysResult.error.code,
                    retryable = keysResult.error.retryable
                )
                return@launch
            }

            _syncState.value = SyncState.Loading("Загрузка разрешений...")
            chefRepository.downloadPermissions(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA")
                    FeedbackController.success("Офлайн-данные обновлены")
                    _syncState.value = SyncState.Success("Офлайн-данные обновлены")
                }
                .onFailure {
                    UxAnalytics.log(event = "action_error", role = "CHEF", screen = "DOWNLOAD_OFFLINE_DATA", code = it.code)
                    FeedbackController.error("Ошибка загрузки данных [${it.code}]")
                    _syncState.value = SyncState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
                }
        }
    }

    fun syncTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Loading("Синхронизация...")
            UxAnalytics.log(event = "action_started", role = "CHEF", screen = "SYNC_TRANSACTIONS")
            chefRepository.syncOfflineTransactions(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "CHEF", screen = "SYNC_TRANSACTIONS")
                    FeedbackController.success("Синхронизация выполнена")
                    _syncState.value = SyncState.Success("Синхронизировано: ${it.successCount}")
                    updateUnsyncedCount()
                }
                .onFailure {
                    UxAnalytics.log(event = "action_error", role = "CHEF", screen = "SYNC_TRANSACTIONS", code = it.code)
                    FeedbackController.error("Ошибка синхронизации [${it.code}]")
                    _syncState.value = SyncState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
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
