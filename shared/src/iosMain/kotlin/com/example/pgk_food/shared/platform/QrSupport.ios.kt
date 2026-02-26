package com.example.pgk_food.shared.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import platform.Foundation.NSUUID

actual fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
): String {
    // TODO: Implement Security.framework ECDSA signing for iOS actual.
    return ""
}

actual fun generateQrNonce(): String = NSUUID().UUIDString()

@Composable
actual fun PlatformQrCodeImage(content: String, modifier: Modifier, sizePx: Int) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
