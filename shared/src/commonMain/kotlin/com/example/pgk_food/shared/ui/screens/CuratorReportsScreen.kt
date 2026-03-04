package com.example.pgk_food.shared.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.pgk_food.shared.data.repository.AdminRepository
import com.example.pgk_food.shared.data.repository.CuratorRepository
import com.example.pgk_food.shared.util.HintScreenKey

@Composable
fun CuratorReportsScreen(
    token: String,
    curatorId: String,
    curatorRepository: CuratorRepository,
    showHints: Boolean = true,
    onDismissHints: () -> Unit = {},
) {
    // Backend ограничивает куратора только своими группами.
    val adminRepository = remember { AdminRepository() }
    AdminReportsScreen(
        token = token,
        adminRepository = adminRepository,
        showFraudTab = false,
        loadGroups = { curatorRepository.getCuratorGroups(token, curatorId) },
        showHints = showHints,
        onDismissHints = onDismissHints,
        hintScreen = HintScreenKey.CURATOR_REPORTS,
    )
}
