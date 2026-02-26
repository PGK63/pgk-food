package com.example.pgk_food.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pgk_food.core.feedback.FeedbackController
import com.example.pgk_food.data.remote.dto.MenuItemDto
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.data.repository.MealsTodayResponse
import com.example.pgk_food.data.repository.StudentRepository
import com.example.pgk_food.util.UxAnalytics
import kotlinx.coroutines.Dispatchers
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
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val token get() = authRepository.getToken().orEmpty()

    private val _mealsState = MutableStateFlow<MealsState>(MealsState.Idle)
    val mealsState: StateFlow<MealsState> = _mealsState.asStateFlow()

    private val _menuState = MutableStateFlow<MenuState>(MenuState.Idle)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    private val _downloadKeysState = MutableStateFlow<DownloadKeysState>(DownloadKeysState.Idle)
    val downloadKeysState: StateFlow<DownloadKeysState> = _downloadKeysState.asStateFlow()

    fun loadMealsToday() {
        viewModelScope.launch(Dispatchers.IO) {
            _mealsState.value = MealsState.Loading
            UxAnalytics.log(event = "action_started", role = "STUDENT", screen = "MEALS_TODAY")
            studentRepository.getMealsToday(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "STUDENT", screen = "MEALS_TODAY")
                    FeedbackController.success("Талоны обновлены")
                    _mealsState.value = MealsState.Success(it)
                    if (authRepository.getToken() != null) {
                        downloadKeys()
                    }
                }
                .onFailure {
                    UxAnalytics.log(event = "action_error", role = "STUDENT", screen = "MEALS_TODAY", code = it.code)
                    FeedbackController.error("Ошибка талонов [${it.code}]")
                    _mealsState.value = MealsState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
                }
        }
    }

    fun loadMenu(date: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _menuState.value = MenuState.Loading
            UxAnalytics.log(event = "action_started", role = "STUDENT", screen = "MENU")
            studentRepository.getMenu(token, date)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "STUDENT", screen = "MENU")
                    FeedbackController.success("Меню загружено")
                    _menuState.value = MenuState.Success(it)
                }
                .onFailure {
                    UxAnalytics.log(event = "action_error", role = "STUDENT", screen = "MENU", code = it.code)
                    FeedbackController.error("Ошибка меню [${it.code}]")
                    _menuState.value = MenuState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
                }
        }
    }

    fun downloadKeys() {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadKeysState.value = DownloadKeysState.Loading
            UxAnalytics.log(event = "action_started", role = "STUDENT", screen = "DOWNLOAD_KEYS")
            authRepository.getMyKeys(token)
                .onSuccess {
                    UxAnalytics.log(event = "action_success", role = "STUDENT", screen = "DOWNLOAD_KEYS")
                    FeedbackController.success("Офлайн-ключи загружены")
                    _downloadKeysState.value = DownloadKeysState.Success
                }
                .onFailure {
                    UxAnalytics.log(event = "action_error", role = "STUDENT", screen = "DOWNLOAD_KEYS", code = it.code)
                    FeedbackController.error("Ошибка загрузки ключей [${it.code}]")
                    _downloadKeysState.value = DownloadKeysState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
                }
        }
    }

    fun resetDownloadKeysState() {
        _downloadKeysState.value = DownloadKeysState.Idle
    }
}

