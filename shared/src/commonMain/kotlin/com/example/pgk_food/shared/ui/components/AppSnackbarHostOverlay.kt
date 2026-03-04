package com.example.pgk_food.shared.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AppSnackbarHostOverlay(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    autoDismissMillis: Long = 3_500L,
) {
    val currentSnackbar = hostState.currentSnackbarData
    LaunchedEffect(currentSnackbar) {
        val activeSnackbar = currentSnackbar ?: return@LaunchedEffect
        delay(autoDismissMillis)
        if (hostState.currentSnackbarData == activeSnackbar) {
            activeSnackbar.dismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 720.dp),
            snackbar = { data ->
                AppSnackbar(
                    data = data,
                    modifier = Modifier,
                )
            },
        )
    }
}
