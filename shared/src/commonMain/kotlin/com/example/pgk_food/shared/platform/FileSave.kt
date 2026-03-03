package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable

data class FileSaveRequest(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

@Composable
expect fun rememberFileSaveLauncher(
    onResult: (success: Boolean, message: String?) -> Unit,
): (FileSaveRequest) -> Unit
