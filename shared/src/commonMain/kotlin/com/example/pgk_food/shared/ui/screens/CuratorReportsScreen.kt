package com.example.pgk_food.shared.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.pgk_food.shared.data.repository.AdminRepository
import com.example.pgk_food.shared.data.repository.CuratorRepository

@Composable
fun CuratorReportsScreen(
    token: String,
    curatorId: String,
    curatorRepository: CuratorRepository,
) {
    // Backend ограничивает куратора только своими группами.
    val adminRepository = remember { AdminRepository() }
    AdminReportsScreen(
        token = token,
        adminRepository = adminRepository,
        showFraudTab = false,
    )
}
