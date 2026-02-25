package com.example.pgk_food.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pgk_food.data.repository.MealsTodayResponse
import com.example.pgk_food.data.remote.dto.MenuItemDto
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MealsState {
    object Idle : MealsState()
    object Loading : MealsState()
    data class Success(val meals: MealsTodayResponse) : MealsState()
    data class Error(val message: String) : MealsState()
}

sealed class MenuState {
    object Idle : MenuState()
    object Loading : MenuState()
    data class Success(val items: List<MenuItemDto>) : MenuState()
    data class Error(val message: String) : MenuState()
}

class StudentViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val token get() = authRepository.getToken() ?: ""

    private val _mealsState = MutableStateFlow<MealsState>(MealsState.Idle)
    val mealsState: StateFlow<MealsState> = _mealsState.asStateFlow()

    private val _menuState = MutableStateFlow<MenuState>(MenuState.Idle)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    fun loadMealsToday() {
        viewModelScope.launch {
            _mealsState.value = MealsState.Loading
            studentRepository.getMealsToday(token).onSuccess {
                _mealsState.value = MealsState.Success(it)
            }.onFailure {
                _mealsState.value = MealsState.Error(it.message ?: "Error")
            }
        }
    }

    fun loadMenu(date: String? = null) {
        viewModelScope.launch {
            _menuState.value = MenuState.Loading
            studentRepository.getMenu(token, date).onSuccess {
                _menuState.value = MenuState.Success(it)
            }.onFailure {
                _menuState.value = MenuState.Error(it.message ?: "Error")
            }
        }
    }
}

