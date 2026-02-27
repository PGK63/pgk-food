package com.example.pgk_food.shared.ui.screens

import androidx.compose.runtime.Composable
import com.example.pgk_food.shared.data.repository.ChefRepository

@Composable
expect fun ChefScannerScreenShared(token: String, chefRepository: ChefRepository)
