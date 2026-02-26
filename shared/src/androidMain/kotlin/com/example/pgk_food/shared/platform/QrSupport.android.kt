package com.example.pgk_food.shared.platform

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.UUID

actual fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
): String {
    return try {
        val data = "$userId:$timestamp:$mealType:$nonce"
        val keyBytes = Base64.decode(privateKeyBase64, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(keySpec)
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())
        Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    } catch (t: Throwable) {
        Log.e("QrSupport", "Failed to generate QR signature", t)
        ""
    }
}

actual fun generateQrNonce(): String = UUID.randomUUID().toString()

@Composable
actual fun PlatformQrCodeImage(content: String, modifier: Modifier, sizePx: Int) {
    val bitmap = remember(content, sizePx) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
                )
            }
        }
        bitmap
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR Code",
        modifier = modifier,
    )
}
