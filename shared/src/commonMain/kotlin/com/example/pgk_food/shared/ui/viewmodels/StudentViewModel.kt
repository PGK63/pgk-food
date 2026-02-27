package com.example.pgk_food.shared.ui.viewmodels

import com.example.pgk_food.shared.data.remote.dto.MenuItemDto
import com.example.pgk_food.shared.data.repository.AuthRepository
import com.example.pgk_food.shared.data.repository.MealsTodayResponse
import com.example.pgk_food.shared.data.repository.StudentRepository
import com.example.pgk_food.shared.util.UxAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MealsState {
    data object Idle : MealsState()
    data object Loading : MealsState()
    data class Success(val meals: MealsTodayResponse) : MealsState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : MealsState()
}

sealed class MenuState {
    data object Idle : MenuState()
    data object Loading : MenuState()
    data class Success(val items: List<MenuItemDto>) : MenuState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : MenuState()
}

sealed class DownloadKeysState {
    data object Idle : DownloadKeysState()
    data object Loading : DownloadKeysState()
    data object Success : DownloadKeysState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : DownloadKeysState()
}

class StudentViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository,
) : KmpViewModelScopeOwner() {
    private val token get() = authRepository.getToken().orEmpty()

    private val _mealsState = MutableStateFlow<MealsState>(MealsState.Idle)
    val mealsState: StateFlow<MealsState> = _mealsState.asStateFlow()

    private val _menuState = MutableStateFlow<MenuState>(MenuState.Idle)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    private val _downloadKeysState = MutableStateFlow<DownloadKeysState>(DownloadKeysState.Idle)
    val downloadKeysState: StateFlow<DownloadKeysState> = _downloadKeysState.asStateFlow()

    fun loadMealsToday() {
        viewModelScope.launch {
            _mealsState.value = MealsState.Loading
            UxAnalytics.log(event = "action_started", role = "STUDENT", screen = "MEALS_TODAY")
            studentRepository.getMealsToday(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "STUDENT", screen = "MEALS_TODAY")
                    notifySuccess("Талоны обновлены")
                    _mealsState.value = MealsState.Success(it)
                    if (authRepository.getToken() != null) downloadKeys()
                }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка талонов")
                    UxAnalytics.log(event = "action_error", role = "STUDENT", screen = "MEALS_TODAY", code = err.code)
                    notifyError("Ошибка талонов [${err.code}]")
                    _mealsState.value = MealsState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun loadMenu(date: String? = null) {
        viewModelScope.launch {
            _menuState.value = MenuState.Loading
            UxAnalytics.log(event = "action_started", role = "STUDENT", screen = "MENU")
            studentRepository.getMenu(token, date)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "STUDENT", screen = "MENU")
                    notifySuccess("Меню загружено")
                    _menuState.value = MenuState.Success(it)
                }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка меню")
                    UxAnalytics.log(event = "action_error", role = "STUDENT", screen = "MENU", code = err.code)
                    notifyError("Ошибка меню [${err.code}]")
                    _menuState.value = MenuState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun downloadKeys() {
        viewModelScope.launch {
            _downloadKeysState.value = DownloadKeysState.Loading
            UxAnalytics.log(event = "action_started", role = "STUDENT", screen = "DOWNLOAD_KEYS")
            authRepository.getMyKeys(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "STUDENT", screen = "DOWNLOAD_KEYS")
                    notifySuccess("Офлайн-ключи загружены")
                    _downloadKeysState.value = DownloadKeysState.Success
                }
                .onFailure {
                    val err = it.toErrorInfo("Ошибка загрузки ключей")
                    UxAnalytics.log(event = "action_error", role = "STUDENT", screen = "DOWNLOAD_KEYS", code = err.code)
                    notifyError("Ошибка загрузки ключей [${err.code}]")
                    _downloadKeysState.value = DownloadKeysState.Error(err.message, err.code, err.retryable)
                }
        }
    }

    fun resetDownloadKeysState() {
        _downloadKeysState.value = DownloadKeysState.Idle
    }
}
