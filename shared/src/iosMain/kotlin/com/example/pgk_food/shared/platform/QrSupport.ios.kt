@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import cnames.structs.__CFData
import cnames.structs.__CFDictionary
import cnames.structs.__SecKey
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlin.io.encoding.Base64
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSData
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyVerifySignature
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeEC
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode
import platform.darwin.NSObject
import qrcode.QRCode
import kotlin.math.abs

private var lastQrSignatureDebugInfo: String = "not-generated-yet"

actual fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
    publicKeyBase64: String?,
): String {
    fun fail(reason: String): String {
        val trace = lastQrSignatureDebugInfo
        lastQrSignatureDebugInfo = if (trace.isBlank() || trace == "not-generated-yet") {
            "error $reason"
        } else {
            "error $reason; trace=$trace"
        }
        return ""
    }

    return try {
        lastQrSignatureDebugInfo = "start keyLen=${privateKeyBase64.length} nonceLen=${nonce.length} ts=$timestamp"

        val keyBytes = decodeBase64Flexible(privateKeyBase64)
            ?: return fail("private-key base64 decode failed")
        if (keyBytes.isEmpty()) return fail("private-key decoded as empty")
        lastQrSignatureDebugInfo = "decoded keyBytes=${keyBytes.size} hex=${keyBytes.hexPrefix()}"

        val publicKeyBytes = publicKeyBase64
            ?.takeIf { it.isNotBlank() }
            ?.let { decodeBase64Flexible(it) }
        if (publicKeyBase64 != null && publicKeyBase64.isNotBlank()) {
            if (publicKeyBytes == null || publicKeyBytes.isEmpty()) {
                appendQrTrace("public-key decode failed")
            } else {
                appendQrTrace("public-key decoded bytes=${publicKeyBytes.size} hex=${publicKeyBytes.hexPrefix()}")
            }
        }

        val privateKey = createIosEcPrivateKey(
            keyBytes = keyBytes,
            publicKeyBytes = publicKeyBytes
        )
            ?: return fail("SecKeyCreateWithData failed for private key (raw=${keyBytes.size})")

        val payload = "$userId:$timestamp:$mealType:$nonce"
        val messageData = payload.encodeToByteArray().toCfData()
            ?: return fail("payload to CFData failed")

        val signatureData = SecKeyCreateSignature(
            privateKey,
            kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
            messageData,
            null,
        ) ?: return fail("SecKeyCreateSignature returned null")

        val signatureBase64 = Base64.Default.encode(signatureData.toByteArray())
        lastQrSignatureDebugInfo = "ok keyBytes=${keyBytes.size} signatureLen=${signatureBase64.length}"
        signatureBase64
    } catch (t: Throwable) {
        fail("${t::class.simpleName}: ${t.message ?: "no-message"}")
    }
}

actual fun getLastQrSignatureDebugInfo(): String = lastQrSignatureDebugInfo

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
        val keyBytes = decodeBase64Flexible(publicKeyBase64) ?: return false
        if (keyBytes.isEmpty()) return false
        val signatureBytes = decodeBase64Flexible(signatureBase64) ?: return false
        if (signatureBytes.isEmpty()) return false

        val attrs = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0,
            null,
            null
        )
            ?.reinterpret<__CFDictionary>() ?: return false
        CFDictionaryAddValue(attrs, kSecAttrKeyType, kSecAttrKeyTypeEC)
        CFDictionaryAddValue(attrs, kSecAttrKeyClass, kSecAttrKeyClassPublic)
        createKeySizeBitsCfNumber()?.let {
            CFDictionaryAddValue(attrs, kSecAttrKeySizeInBits, it)
        }

        val keyData = keyBytes.toCfData() ?: return false
        val publicKey = SecKeyCreateWithData(keyData, attrs, null) ?: return false

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
    val raw = "$userId:$timestamp:$mealType:$nonce"
    return raw.hashCode().toUInt().toString(16)
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
            .withSize((sizePx / 16).coerceAtLeast(8))
            .build(content)
            .renderToBytes()
    }.getOrNull() ?: return null
    val nsData = pngBytes.toNSData() ?: return null
    return UIImage.imageWithData(nsData)
}

private fun decodeBase64Flexible(source: String): ByteArray? {
    val cleaned = source
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace("\\n", "")
        .replace("\\r", "")
        .filterNot { it.isWhitespace() }

    if (cleaned.isEmpty()) return null

    val normalized = cleaned
        .replace('-', '+')
        .replace('_', '/')
        .let { base ->
            val pad = (4 - base.length % 4) % 4
            if (pad == 0) base else base + "=".repeat(pad)
        }

    return runCatching { Base64.Default.decode(normalized) }.getOrNull()
}

private data class ImportAttrs(
    val label: String,
    val keyType: CValuesRef<*>?,
    val includeClass: Boolean,
    val includeSize: Boolean,
)

private fun createIosEcPrivateKey(
    keyBytes: ByteArray,
    publicKeyBytes: ByteArray?
): kotlinx.cinterop.CPointer<__SecKey>? {
    appendQrTrace("import start raw=${keyBytes.size} hex=${keyBytes.hexPrefix()}")

    val dataCandidates = ArrayList<Pair<String, ByteArray>>(6)
    dataCandidates += "raw" to keyBytes

    val wrappedPkcs8 = wrapSec1EcPrivateKeyIntoPkcs8(keyBytes)
    if (wrappedPkcs8 != null) dataCandidates += "sec1->pkcs8" to wrappedPkcs8

    val scalar = extractEcPrivateScalar(keyBytes)
    if (scalar != null) {
        appendQrTrace("ec-scalar extracted len=${scalar.size}")
        dataCandidates += "scalar32" to scalar
        dataCandidates += "sec1-from-scalar" to buildSec1EcPrivateKeyFromScalar(scalar)
        val publicPoint = publicKeyBytes?.let { extractEcPublicPoint(it) }
        if (publicPoint != null) {
            appendQrTrace("public-point extracted len=${publicPoint.size}")
            dataCandidates += "x963-public+scalar" to (publicPoint + scalar)
        } else if (publicKeyBytes != null) {
            appendQrTrace("public-point extract failed")
        }
    } else {
        appendQrTrace("ec-scalar extract failed")
    }

    val attrsCandidates = listOf(
        ImportAttrs("EC+CLASS+SIZE", kSecAttrKeyTypeEC, includeClass = true, includeSize = true),
        ImportAttrs("EC+CLASS", kSecAttrKeyTypeEC, includeClass = true, includeSize = false),
        ImportAttrs("EC+SIZE", kSecAttrKeyTypeEC, includeClass = false, includeSize = true),
        ImportAttrs("EC", kSecAttrKeyTypeEC, includeClass = false, includeSize = false),
        ImportAttrs("ECSEC+CLASS+SIZE", kSecAttrKeyTypeECSECPrimeRandom, includeClass = true, includeSize = true),
        ImportAttrs("ECSEC+CLASS", kSecAttrKeyTypeECSECPrimeRandom, includeClass = true, includeSize = false),
        ImportAttrs("ECSEC", kSecAttrKeyTypeECSECPrimeRandom, includeClass = false, includeSize = false),
    )

    for ((dataLabel, candidateBytes) in dataCandidates) {
        val data = candidateBytes.toCfData()
        if (data == null) {
            appendQrTrace("data=$dataLabel toCfData failed")
            continue
        }
        for (attr in attrsCandidates) {
            val attrs = createPrivateKeyAttrs(attr)
            if (attrs == null) {
                appendQrTrace("attrs=${attr.label} create failed")
                continue
            }
            val key = secKeyCreateWithDataDebug(
                data = data,
                attrs = attrs,
                traceLabel = "data=$dataLabel attrs=${attr.label}"
            )
            if (key != null) {
                return key
            }
        }
    }
    return null
}

private fun extractEcPrivateScalar(derOrRaw: ByteArray): ByteArray? {
    if (derOrRaw.size == 32) return derOrRaw
    if (derOrRaw.size == 33 && derOrRaw.firstOrNull() == 0.toByte()) return derOrRaw.copyOfRange(1, 33)
    extractPkcs8EcScalar(derOrRaw)?.let { return normalizeScalar(it) }
    extractSec1EcScalar(derOrRaw)?.let { return normalizeScalar(it) }
    return null
}

private fun extractEcPublicPoint(derOrRaw: ByteArray): ByteArray? {
    if (derOrRaw.size == 65 && derOrRaw.firstOrNull() == 0x04.toByte()) return derOrRaw
    if (derOrRaw.size == 64) return byteArrayOf(0x04) + derOrRaw
    return extractSpkiEcPublicPoint(derOrRaw)
}

private fun wrapSec1EcPrivateKeyIntoPkcs8(sec1: ByteArray): ByteArray? {
    if (!looksLikeSec1EcPrivateKey(sec1)) return null

    val version = byteArrayOf(0x02, 0x01, 0x00)
    val ecAlgIdentifier = byteArrayOf(
        0x30, 0x13,
        0x06, 0x07, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01,
        0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07
    )
    val privateKeyOctet = derWithTag(0x04, sec1)
    val body = version + ecAlgIdentifier + privateKeyOctet
    return derWithTag(0x30, body)
}

private fun buildSec1EcPrivateKeyFromScalar(scalar: ByteArray): ByteArray {
    val scalarValue = normalizeScalar(scalar) ?: scalar
    val version = byteArrayOf(0x02, 0x01, 0x01)
    val privateKeyOctet = derWithTag(0x04, scalarValue)
    val namedCurve = byteArrayOf(0xA0.toByte(), 0x0A, 0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07)
    val body = version + privateKeyOctet + namedCurve
    return derWithTag(0x30, body)
}

private fun looksLikeSec1EcPrivateKey(bytes: ByteArray): Boolean {
    return extractSec1EcScalar(bytes) != null
}

private fun derWithTag(tag: Int, value: ByteArray): ByteArray {
    val len = encodeDerLength(value.size)
    return byteArrayOf(tag.toByte()) + len + value
}

private fun encodeDerLength(length: Int): ByteArray {
    if (length < 0x80) return byteArrayOf(length.toByte())
    var v = length
    val octetsReversed = ArrayList<Byte>(4)
    while (v > 0) {
        octetsReversed += (v and 0xFF).toByte()
        v = v ushr 8
    }
    val octets = octetsReversed.asReversed().toByteArray()
    return byteArrayOf((0x80 or octets.size).toByte()) + octets
}

private fun ByteArray.hexPrefix(bytesCount: Int = 12): String =
    take(bytesCount).joinToString("") { byte ->
        val v = byte.toInt() and 0xFF
        val hi = "0123456789abcdef"[v ushr 4]
        val lo = "0123456789abcdef"[v and 0x0F]
        "$hi$lo"
    }

private data class DerNode(
    val tag: Int,
    val value: ByteArray,
    val nextIndex: Int
)

private fun parseDerNode(bytes: ByteArray, offset: Int): DerNode? {
    if (offset + 2 > bytes.size) return null
    val tag = bytes[offset].toInt() and 0xFF
    var index = offset + 1

    val lengthByte = bytes[index].toInt() and 0xFF
    index++
    val length = if (lengthByte and 0x80 == 0) {
        lengthByte
    } else {
        val count = lengthByte and 0x7F
        if (count == 0 || count > 4 || index + count > bytes.size) return null
        var len = 0
        repeat(count) { i ->
            len = (len shl 8) or (bytes[index + i].toInt() and 0xFF)
        }
        index += count
        len
    }

    if (length < 0 || index + length > bytes.size) return null
    val value = bytes.copyOfRange(index, index + length)
    return DerNode(tag = tag, value = value, nextIndex = index + length)
}

private fun extractPkcs8EcScalar(pkcs8: ByteArray): ByteArray? {
    val top = parseDerNode(pkcs8, 0) ?: return null
    if (top.tag != 0x30 || top.nextIndex != pkcs8.size) return null
    val body = top.value

    var pos = 0
    val version = parseDerNode(body, pos) ?: return null
    if (version.tag != 0x02) return null
    pos = version.nextIndex

    val algorithm = parseDerNode(body, pos) ?: return null
    if (algorithm.tag != 0x30) return null
    pos = algorithm.nextIndex

    val privateKey = parseDerNode(body, pos) ?: return null
    if (privateKey.tag != 0x04) return null
    return extractSec1EcScalar(privateKey.value)
}

private fun extractSec1EcScalar(sec1: ByteArray): ByteArray? {
    val top = parseDerNode(sec1, 0) ?: return null
    if (top.tag != 0x30 || top.nextIndex != sec1.size) return null
    val body = top.value

    var pos = 0
    val version = parseDerNode(body, pos) ?: return null
    if (version.tag != 0x02) return null
    pos = version.nextIndex

    val privateKey = parseDerNode(body, pos) ?: return null
    if (privateKey.tag != 0x04) return null
    return privateKey.value
}

private fun extractSpkiEcPublicPoint(spki: ByteArray): ByteArray? {
    val top = parseDerNode(spki, 0) ?: return null
    if (top.tag != 0x30 || top.nextIndex != spki.size) return null
    val body = top.value

    var pos = 0
    val algorithm = parseDerNode(body, pos) ?: return null
    if (algorithm.tag != 0x30) return null
    pos = algorithm.nextIndex

    val bitString = parseDerNode(body, pos) ?: return null
    if (bitString.tag != 0x03 || bitString.value.isEmpty()) return null
    if (bitString.value[0].toInt() != 0) return null

    val point = bitString.value.copyOfRange(1, bitString.value.size)
    return when {
        point.size == 65 && point.firstOrNull() == 0x04.toByte() -> point
        point.size == 64 -> byteArrayOf(0x04) + point
        else -> null
    }
}

private fun normalizeScalar(raw: ByteArray): ByteArray? {
    if (raw.isEmpty()) return null
    if (raw.size == 32) return raw
    if (raw.size > 32) return raw.takeLast(32).toByteArray()
    return ByteArray(32 - raw.size) + raw
}

private fun appendQrTrace(message: String) {
    lastQrSignatureDebugInfo = when (val prev = lastQrSignatureDebugInfo) {
        "", "not-generated-yet" -> message
        else -> "$prev | $message"
    }
}

private fun createKeySizeBitsCfNumber() =
    CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, cValuesOf(256))

private fun createPrivateKeyAttrs(config: ImportAttrs): kotlinx.cinterop.CPointer<__CFDictionary>? {
    val attrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        ?.reinterpret<__CFDictionary>() ?: return null
    CFDictionaryAddValue(attrs, kSecAttrKeyType, config.keyType)
    if (config.includeClass) {
        CFDictionaryAddValue(attrs, kSecAttrKeyClass, kSecAttrKeyClassPrivate)
    }
    if (config.includeSize) {
        createKeySizeBitsCfNumber()?.let { CFDictionaryAddValue(attrs, kSecAttrKeySizeInBits, it) }
    }
    return attrs
}

private fun secKeyCreateWithDataDebug(
    data: kotlinx.cinterop.CPointer<__CFData>,
    attrs: kotlinx.cinterop.CPointer<__CFDictionary>,
    traceLabel: String,
): kotlinx.cinterop.CPointer<__SecKey>? {
    val key = SecKeyCreateWithData(data, attrs, null)
    if (key != null) {
        appendQrTrace("import ok $traceLabel")
    } else {
        appendQrTrace("import fail $traceLabel")
    }
    return key
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
