package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
    publicKeyBase64: String? = null,
): String
expect fun getLastQrSignatureDebugInfo(): String

expect fun generateQrNonce(): String

expect fun verifyQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    signatureBase64: String,
    publicKeyBase64: String,
): Boolean

expect fun generateOfflineTransactionHash(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
): String

expect fun isQrTimestampValid(timestamp: Long, toleranceSeconds: Long = 120): Boolean

@Composable
expect fun PlatformQrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512,
)
