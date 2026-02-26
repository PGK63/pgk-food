package com.example.pgk_food.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pgk_food.shared.data.repository.ChefRepository

@Composable
fun ChefScannerScreenShared(token: String, chefRepository: ChefRepository) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Сканер QR", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Камера/сканер будет подключен через platform implementation")
        Text("Токен получен: ${token.isNotBlank()}")
    }
}
