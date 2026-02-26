package com.example.pgk_food.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pgk_food.data.remote.dto.GroupDto
import com.example.pgk_food.data.remote.dto.UserDto
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.data.repository.RegistratorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GroupsState {
    data object Idle : GroupsState()
    data object Loading : GroupsState()
    data class Success(val groups: List<GroupDto>) : GroupsState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : GroupsState()
}

sealed class UsersState {
    data object Idle : UsersState()
    data object Loading : UsersState()
    data class Success(val users: List<UserDto>) : UsersState()
    data class Error(val message: String, val code: String, val retryable: Boolean) : UsersState()
}

class RegistratorViewModel(
    private val authRepository: AuthRepository,
    private val registratorRepository: RegistratorRepository
) : ViewModel() {

    private val token get() = authRepository.getToken().orEmpty()

    private val _groupsState = MutableStateFlow<GroupsState>(GroupsState.Idle)
    val groupsState: StateFlow<GroupsState> = _groupsState.asStateFlow()

    private val _usersState = MutableStateFlow<UsersState>(UsersState.Idle)
    val usersState: StateFlow<UsersState> = _usersState.asStateFlow()

    fun loadGroups() {
        viewModelScope.launch {
            _groupsState.value = GroupsState.Loading
            registratorRepository.getGroups(token)
                .onSuccess { _groupsState.value = GroupsState.Success(it) }
                .onFailure {
                    _groupsState.value = GroupsState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
                }
        }
    }

    fun loadUsers(groupId: Int? = null) {
        viewModelScope.launch {
            _usersState.value = UsersState.Loading
            registratorRepository.getUsers(token, groupId)
                .onSuccess { _usersState.value = UsersState.Success(it) }
                .onFailure {
                    _usersState.value = UsersState.Error(
                        message = it.userMessage,
                        code = it.code,
                        retryable = it.retryable
                    )
                }
        }
    }
}

