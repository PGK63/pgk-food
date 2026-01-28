package com.example.pgk_food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.pgk_food.data.local.AppDatabase
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.ui.navigation.NavGraph
import com.example.pgk_food.ui.navigation.Screen
import com.example.pgk_food.ui.theme.PgkfoodTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val authRepository = AuthRepository(database.userSessionDao())

        // Simple check for start destination (ideally async, but for simplicity here)
        val startDestination = runBlocking {
            val session = authRepository.getUserSession().first()
            if (session != null) Screen.Main.route else Screen.Login.route
        }

        enableEdgeToEdge()
        setContent {
            PgkfoodTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    authRepository = authRepository,
                    database = database,
                    startDestination = startDestination
                )
            }
        }
    }
}
