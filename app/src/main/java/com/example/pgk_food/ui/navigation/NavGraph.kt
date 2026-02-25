package com.example.pgk_food.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pgk_food.data.local.AppDatabase
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.ui.screens.LoginScreen
import com.example.pgk_food.ui.screens.MainScreen
import com.example.pgk_food.ui.viewmodels.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
}

@Suppress("UNCHECKED_CAST")
@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    database: AppDatabase,
    startDestination: String
) {
    val authViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
    
    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                authRepository = authRepository,
                database = database,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}


