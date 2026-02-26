package com.example.pgk_food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.pgk_food.data.local.AppDatabase
import com.example.pgk_food.data.repository.AuthRepository
import com.example.pgk_food.ui.navigation.NavGraph
import com.example.pgk_food.ui.theme.PgkfoodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val authRepository = AuthRepository(database.userSessionDao())

        enableEdgeToEdge()
        setContent {
            PgkfoodTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    authRepository = authRepository,
                    database = database
                )
            }
        }
    }
}
