package com.example.pgk_food

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pgk_food.shared.PgkSharedApp
import com.example.pgk_food.shared.data.local.initAndroidDatabaseContext
import com.example.pgk_food.shared.platform.initAndroidPlatformContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAndroidDatabaseContext(applicationContext)
        initAndroidPlatformContext(applicationContext)
        enableEdgeToEdge()
        setContent {
            PgkSharedApp()
        }
    }
}
