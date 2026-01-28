package com.example.pgk_food.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pgk_food.data.local.AppDatabase
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.ui.screens.LoginScreen
import com.example.pgk_food.ui.screens.MainScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    database: AppDatabase,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authRepository = authRepository,
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
