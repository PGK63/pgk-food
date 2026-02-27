package com.example.pgk_food.shared.ui.screens

import androidx.compose.runtime.Composable
import com.example.pgk_food.shared.ui.viewmodels.ChefViewModel

@Composable
expect fun ChefScannerScreenShared(
    token: String,
    viewModel: ChefViewModel,
    showHints: Boolean = true,
    onHideHints: () -> Unit = {},
)
