package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
): String

expect fun generateQrNonce(): String

@Composable
expect fun PlatformQrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512,
)
