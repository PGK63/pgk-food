@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import cnames.structs.__CFData
import cnames.structs.__CFDictionary
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlin.io.encoding.Base64
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFNumberIntType
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Security.SecKeyRef
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyVerifySignature
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode
import platform.darwin.NSObject
import qrcode.QRCode
import kotlin.math.abs

actual fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
): String {
    return runCatching {
        val keyBytes = decodeBase64KeyMaterial(privateKeyBase64)
        if (keyBytes.isEmpty()) return ""

        val privateKey = createPrivateEcKey(keyBytes) ?: return ""

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

actual fun verifyQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    signatureBase64: String,
    publicKeyBase64: String,
): Boolean {
    return runCatching {
        val keyBytes = decodeBase64KeyMaterial(publicKeyBase64)
        if (keyBytes.isEmpty()) return false
        val signatureBytes = decodeBase64KeyMaterial(signatureBase64)
        if (signatureBytes.isEmpty()) return false

        val publicKey = createPublicEcKey(keyBytes) ?: return false

        val payload = "$userId:$timestamp:$mealType:$nonce"
        val messageData = payload.encodeToByteArray().toCfData() ?: return false
        val signatureData = signatureBytes.toCfData() ?: return false

        SecKeyVerifySignature(
            publicKey,
            kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
            messageData,
            signatureData,
            null,
        )
    }.getOrElse { false }
}

actual fun generateOfflineTransactionHash(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
): String {
    val raw = "$userId:$timestamp:$mealType:$nonce".encodeToByteArray()
    if (raw.isEmpty()) return ""
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    raw.usePinned { input ->
        digest.usePinned { output ->
            CC_SHA256(
                input.addressOf(0).reinterpret<ByteVar>(),
                raw.size.toUInt(),
                output.addressOf(0).reinterpret<UByteVar>(),
            )
        }
    }
    return digest.joinToString(separator = "") { byte ->
        val value = byte.toInt() and 0xff
        value.toString(16).padStart(2, '0')
    }
}

actual fun isQrTimestampValid(timestamp: Long, toleranceSeconds: Long): Boolean {
    val currentSeconds = currentTimeMillis() / 1000
    return abs(currentSeconds - timestamp) <= toleranceSeconds
}

@Composable
actual fun PlatformQrCodeImage(content: String, modifier: Modifier, sizePx: Int) {
    val image = remember(content, sizePx) { generateQrUiImage(content, sizePx) }
    UIKitView(
        factory = {
            UIImageView().apply {
                clipsToBounds = true
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
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
            .withSize(sizePx.coerceIn(256, 1024))
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

private fun decodeBase64KeyMaterial(raw: String): ByteArray {
    val sanitized = normalizeKeyBase64(raw)
    if (sanitized.isBlank()) return ByteArray(0)
    return runCatching { Base64.Default.decode(sanitized) }.getOrDefault(ByteArray(0))
}

private fun normalizeKeyBase64(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    val withoutPem = if (trimmed.contains("-----BEGIN")) {
        trimmed.lineSequence()
            .filterNot { line -> line.startsWith("-----BEGIN") || line.startsWith("-----END") }
            .joinToString("")
    } else {
        trimmed
    }
    return buildString(withoutPem.length) {
        withoutPem.forEach { ch ->
            if (!ch.isWhitespace()) append(ch)
        }
    }
}

private fun createEcAttributes(isPrivate: Boolean): kotlinx.cinterop.CPointer<__CFDictionary>? {
    val attrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        ?.reinterpret<__CFDictionary>() ?: return null
    CFDictionaryAddValue(attrs, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
    CFDictionaryAddValue(attrs, kSecAttrKeyClass, if (isPrivate) kSecAttrKeyClassPrivate else kSecAttrKeyClassPublic)
    createCfNumber(256)?.let { keySize ->
        CFDictionaryAddValue(attrs, kSecAttrKeySizeInBits, keySize)
    }
    return attrs
}

private fun createCfNumber(value: Int): kotlinx.cinterop.CPointer<*>? {
    val raw = intArrayOf(value)
    return raw.usePinned { pinned ->
        CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, pinned.addressOf(0))
    }
}

private fun createPrivateEcKey(source: ByteArray): SecKeyRef? {
    val sec1Blob = extractPkcs8PrivateKeyBlob(source)
    val scalar = extractSec1PrivateScalar(sec1Blob ?: source)
    val candidates = buildList {
        add(source)
        sec1Blob?.let { add(it) }
        scalar?.let { add(it) }
    }
    return createEcKeyFromCandidates(candidates, isPrivate = true)
}

private fun createPublicEcKey(source: ByteArray): SecKeyRef? {
    val rawSpki = extractSpkiPublicKeyBytes(source)
    val candidates = buildList {
        add(source)
        rawSpki?.let { add(it) }
    }
    return createEcKeyFromCandidates(candidates, isPrivate = false)
}

private fun createEcKeyFromCandidates(
    candidates: List<ByteArray>,
    isPrivate: Boolean,
): SecKeyRef? {
    val uniqueCandidates = candidates.filter { it.isNotEmpty() }.distinctBy { it.joinToString(",") }
    uniqueCandidates.forEach { keyBytes ->
        val attrs = createEcAttributes(isPrivate) ?: return@forEach
        val keyData = keyBytes.toCfData() ?: return@forEach
        val key = SecKeyCreateWithData(keyData, attrs, null)
        if (key != null) return key
    }
    return null
}

private data class Asn1Node(
    val tag: Int,
    val contentStart: Int,
    val contentEnd: Int,
    val nextOffset: Int,
)

private fun readAsn1Node(data: ByteArray, offset: Int): Asn1Node? {
    if (offset + 2 > data.size) return null
    val tag = data[offset].toInt() and 0xff
    val lenByte = data[offset + 1].toInt() and 0xff
    var contentLength = 0
    var headerLength = 2
    if (lenByte and 0x80 == 0) {
        contentLength = lenByte
    } else {
        val lenBytesCount = lenByte and 0x7f
        if (lenBytesCount == 0 || lenBytesCount > 4 || offset + 2 + lenBytesCount > data.size) return null
        repeat(lenBytesCount) { idx ->
            contentLength = (contentLength shl 8) or (data[offset + 2 + idx].toInt() and 0xff)
        }
        headerLength += lenBytesCount
    }
    val contentStart = offset + headerLength
    val contentEnd = contentStart + contentLength
    if (contentEnd > data.size) return null
    return Asn1Node(
        tag = tag,
        contentStart = contentStart,
        contentEnd = contentEnd,
        nextOffset = contentEnd,
    )
}

private fun readAsn1Children(data: ByteArray, node: Asn1Node): List<Asn1Node> {
    if (node.tag != 0x30) return emptyList()
    val children = mutableListOf<Asn1Node>()
    var cursor = node.contentStart
    while (cursor < node.contentEnd) {
        val child = readAsn1Node(data, cursor) ?: return emptyList()
        children += child
        cursor = child.nextOffset
    }
    return if (cursor == node.contentEnd) children else emptyList()
}

private fun extractPkcs8PrivateKeyBlob(der: ByteArray): ByteArray? {
    val root = readAsn1Node(der, 0) ?: return null
    val children = readAsn1Children(der, root)
    if (children.size < 3) return null
    val privateKeyNode = children[2]
    if (privateKeyNode.tag != 0x04) return null
    return der.copyOfRange(privateKeyNode.contentStart, privateKeyNode.contentEnd)
}

private fun extractSec1PrivateScalar(der: ByteArray): ByteArray? {
    val root = readAsn1Node(der, 0) ?: return null
    val children = readAsn1Children(der, root)
    if (children.size < 2) return null
    val keyNode = children[1]
    if (keyNode.tag != 0x04) return null
    val scalar = der.copyOfRange(keyNode.contentStart, keyNode.contentEnd)
    return when {
        scalar.size == 32 -> scalar
        scalar.size > 32 -> scalar.copyOfRange(scalar.size - 32, scalar.size)
        else -> null
    }
}

private fun extractSpkiPublicKeyBytes(der: ByteArray): ByteArray? {
    val root = readAsn1Node(der, 0) ?: return null
    val children = readAsn1Children(der, root)
    if (children.size < 2) return null
    val bitStringNode = children[1]
    if (bitStringNode.tag != 0x03) return null
    val raw = der.copyOfRange(bitStringNode.contentStart, bitStringNode.contentEnd)
    if (raw.isEmpty()) return null
    return raw.copyOfRange(1, raw.size).takeIf { it.isNotEmpty() }
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
