@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import cnames.structs.__CFData
import cnames.structs.__CFDictionary
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlin.io.encoding.Base64
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyCreateWithData
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.darwin.NSObject
import qrcode.QRCode

actual fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
): String {
    return runCatching {
        val keyBytes = Base64.Default.decode(privateKeyBase64)
        if (keyBytes.isEmpty()) return ""

        val attrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
            ?.reinterpret<__CFDictionary>() ?: return ""
        CFDictionaryAddValue(attrs, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionaryAddValue(attrs, kSecAttrKeyClass, kSecAttrKeyClassPrivate)

        val keyData = keyBytes.toCfData() ?: return ""
        val privateKey = SecKeyCreateWithData(keyData, attrs, null) ?: return ""

        val payload = "$userId:$timestamp:$mealType:$nonce"
        val messageData = payload.encodeToByteArray().toCfData() ?: return ""

        val signatureData = SecKeyCreateSignature(
            privateKey,
            kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
            messageData,
            null,
        ) ?: return ""

        Base64.Default.encode(signatureData.toByteArray())
    }.getOrElse { "" }
}

actual fun generateQrNonce(): String = NSUUID().UUIDString()

@Composable
actual fun PlatformQrCodeImage(content: String, modifier: Modifier, sizePx: Int) {
    val image = remember(content, sizePx) { generateQrUiImage(content, sizePx) }
    UIKitView(
        factory = {
            UIImageView().apply {
                clipsToBounds = true
                this.image = image
            }
        },
        update = { imageView: NSObject ->
            (imageView as UIImageView).image = image
        },
        modifier = modifier,
    )
}

private fun generateQrUiImage(content: String, sizePx: Int): UIImage? {
    val pngBytes = runCatching {
        QRCode.ofSquares()
            .withSize((sizePx / 16).coerceAtLeast(8))
            .build(content)
            .renderToBytes()
    }.getOrNull() ?: return null
    val nsData = pngBytes.toNSData() ?: return null
    return UIImage.imageWithData(nsData)
}

private fun ByteArray.toCfData() = usePinned {
    CFDataCreate(
        kCFAllocatorDefault,
        it.addressOf(0).reinterpret<UByteVar>(),
        size.toLong()
    )?.reinterpret<__CFData>()
}

private fun ByteArray.toNSData(): NSData? = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

private fun kotlinx.cinterop.CPointer<__CFData>.toByteArray(): ByteArray {
    val len = CFDataGetLength(this).toInt()
    if (len <= 0) return ByteArray(0)
    val src = CFDataGetBytePtr(this) ?: return ByteArray(0)
    return src.reinterpret<kotlinx.cinterop.ByteVar>().readBytes(len)
}
