@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.example.pgk_food.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import cnames.structs.__CFData
import cnames.structs.__CFDictionary
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CValuesRef
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
import platform.Security.kSecAttrKeyTypeEC
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode
import platform.darwin.NSObject
import qrcode.QRCode
import kotlin.math.abs

private var lastQrSignatureDebugInfo: String = "SIG_NOT_RUN"

actual fun generateQrSignature(
    userId: String,
    timestamp: Long,
    mealType: String,
    nonce: String,
    privateKeyBase64: String,
    publicKeyBase64: String?,
): String {
    return runCatching {
        setSignatureDebugInfo("SIG_START", "ts=$timestamp nonce=${nonce.length} key=${privateKeyBase64.length}")
        val keyBytes = decodeBase64KeyMaterial(privateKeyBase64)
        if (keyBytes.isEmpty()) {
            setSignatureDebugInfo("SIG_PRIVATE_B64_FAIL")
            return ""
        }
        logQrSignature("decoded private key bytes=${keyBytes.size}")

        val publicKeyBytes = publicKeyBase64
            ?.takeIf { it.isNotBlank() }
            ?.let(::decodeBase64KeyMaterial)
            ?.takeIf { it.isNotEmpty() }
        if (!publicKeyBase64.isNullOrBlank() && publicKeyBytes == null) {
            logQrSignature("public key decode failed; fallback will continue without assisted import")
        }

        val payload = "$userId:$timestamp:$mealType:$nonce"
        val messageData = payload.encodeToByteArray().toCfData() ?: run {
            setSignatureDebugInfo("SIG_CF_PAYLOAD_FAIL")
            return ""
        }
        createPrivateEcKeys(
            source = keyBytes,
            publicKeySource = publicKeyBytes,
        ).forEach { privateKey ->
            val signatureData = SecKeyCreateSignature(
                privateKey,
                kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
                messageData,
                null,
            ) ?: return@forEach
            val signature = Base64.Default.encode(signatureData.toByteArray())
            setSignatureDebugInfo("SIG_OK", "len=${signature.length}")
            logQrSignature("signature generated len=${signature.length}")
            return signature
        }
        setSignatureDebugInfo("SIG_IOS_IMPORT_FAIL")
        logQrSignature("failed to import private key for signing")
        ""
    }.getOrElse { throwable ->
        setSignatureDebugInfo(
            "SIG_EXCEPTION",
            "${throwable::class.simpleName ?: "Throwable"}:${throwable.message ?: "no-message"}"
        )
        logQrSignature("exception: ${throwable::class.simpleName}: ${throwable.message}")
        ""
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
        val keyBytes = decodeBase64KeyMaterial(publicKeyBase64)
        if (keyBytes.isEmpty()) return false
        val signatureBytes = decodeBase64KeyMaterial(signatureBase64)
        if (signatureBytes.isEmpty()) return false

        val payload = "$userId:$timestamp:$mealType:$nonce"
        val messageData = payload.encodeToByteArray().toCfData() ?: return false
        val signatureData = signatureBytes.toCfData() ?: return false

        createPublicEcKeys(keyBytes).any { publicKey ->
            SecKeyVerifySignature(
                publicKey,
                kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
                messageData,
                signatureData,
                null,
            )
        }
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
    val pngBytes = renderQrPngBytes(content = content, targetSizePx = sizePx) ?: return null
    val nsData = pngBytes.toNSData() ?: return null
    return UIImage.imageWithData(nsData)
}

private fun renderQrPngBytes(content: String, targetSizePx: Int): ByteArray? {
    val targetSize = targetSizePx.coerceIn(256, 1024)
    val qr = runCatching { QRCode.ofSquares().build(content) }.getOrNull() ?: return null
    val moduleCount = qr.rawData.size.coerceAtLeast(21)
    val baseCellSize = (targetSize / (moduleCount + 2)).coerceIn(2, 24)
    val cellSizes = buildList {
        add(baseCellSize)
        add((baseCellSize - 1).coerceAtLeast(2))
        add((baseCellSize / 2).coerceAtLeast(2))
        add(2)
    }.distinct()

    cellSizes.forEach { cellSize ->
        val bytes = runCatching {
            QRCode.ofSquares()
                .withSize(cellSize)
                .build(content)
                .renderToBytes()
        }.getOrNull()
        if (bytes != null && bytes.isNotEmpty()) return bytes
    }
    return null
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
    base64DecodeCandidates(sanitized).forEach { candidate ->
        val decoded = runCatching { Base64.Default.decode(candidate) }.getOrNull()
        if (decoded != null && decoded.isNotEmpty()) return decoded
    }
    return ByteArray(0)
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

private fun base64DecodeCandidates(source: String): List<String> {
    val candidates = mutableListOf<String>()
    appendBase64Candidate(candidates, source)
    appendBase64Candidate(candidates, source.replace('-', '+').replace('_', '/'))
    return candidates
}

private fun appendBase64Candidate(candidates: MutableList<String>, value: String) {
    if (value.isBlank()) return
    if (value !in candidates) candidates += value
    val padded = padBase64(value)
    if (padded !in candidates) candidates += padded
}

private fun padBase64(value: String): String {
    val remainder = value.length % 4
    if (remainder == 0) return value
    return value + "=".repeat(4 - remainder)
}

private fun createEcAttributes(
    isPrivate: Boolean,
    keyType: CValuesRef<*>?,
    includeClass: Boolean,
    includeSize: Boolean,
): kotlinx.cinterop.CPointer<__CFDictionary>? {
    if (keyType == null) return null
    val attrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        ?.reinterpret<__CFDictionary>() ?: return null
    CFDictionaryAddValue(attrs, kSecAttrKeyType, keyType)
    if (includeClass) {
        CFDictionaryAddValue(
            attrs,
            kSecAttrKeyClass,
            if (isPrivate) kSecAttrKeyClassPrivate else kSecAttrKeyClassPublic
        )
    }
    if (includeSize) {
        createCfNumber(256)?.let { keySize ->
            CFDictionaryAddValue(attrs, kSecAttrKeySizeInBits, keySize)
        }
    }
    return attrs
}

private fun createCfNumber(value: Int): kotlinx.cinterop.CPointer<*>? {
    val raw = intArrayOf(value)
    return raw.usePinned { pinned ->
        CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, pinned.addressOf(0))
    }
}

private fun createPrivateEcKeys(
    source: ByteArray,
    publicKeySource: ByteArray?,
): List<SecKeyRef> {
    val sec1Blob = extractPkcs8PrivateKeyBlob(source)
    val sec1Source = sec1Blob ?: source.takeIf { isSec1EcPrivateKey(it) }
    val scalar = extractEcPrivateScalar(source)
    val scalarSec1Source = scalar?.let(::buildSec1EcPrivateKeyFromScalar)
    val publicPoint = publicKeySource?.let(::extractEcPublicPoint)
    val assistedCandidate = if (publicPoint != null && scalar != null) {
        publicPoint + scalar
    } else {
        null
    }
    val candidates = buildList {
        sec1Source?.let { add(it) }
        scalarSec1Source?.let { add(it) }
        assistedCandidate?.let { add(it) }
        add(source)
    }
    return createEcKeysFromCandidates(candidates, isPrivate = true, tracePrefix = "private")
}

private fun createPublicEcKeys(source: ByteArray): List<SecKeyRef> {
    val rawSpki = extractSpkiPublicKeyBytes(source)
    val rawPublic = normalizeRawPublicPoint(source)
    val candidates = buildList {
        rawSpki?.let { add(it) }
        rawPublic?.let { add(it) }
        add(source)
    }
    return createEcKeysFromCandidates(candidates, isPrivate = false, tracePrefix = "public")
}

private fun createEcKeysFromCandidates(
    candidates: List<ByteArray>,
    isPrivate: Boolean,
    tracePrefix: String,
): List<SecKeyRef> {
    val keys = mutableListOf<SecKeyRef>()
    val uniqueCandidates = uniqueKeyCandidates(candidates)
    val keyTypes = listOf(kSecAttrKeyTypeECSECPrimeRandom, kSecAttrKeyTypeEC)
    val attrVariants = listOf(
        AttrVariant(includeClass = true, includeSize = true),
        AttrVariant(includeClass = true, includeSize = false),
        AttrVariant(includeClass = false, includeSize = true),
        AttrVariant(includeClass = false, includeSize = false),
    )

    uniqueCandidates.forEachIndexed { candidateIndex, keyBytes ->
        val keyData = keyBytes.toCfData() ?: return@forEachIndexed
        keyTypes.forEach { keyType ->
            attrVariants.forEach { variant ->
                val attrs = createEcAttributes(
                    isPrivate = isPrivate,
                    keyType = keyType,
                    includeClass = variant.includeClass,
                    includeSize = variant.includeSize,
                ) ?: return@forEach
                val key = SecKeyCreateWithData(keyData, attrs, null)
                if (key != null) {
                    keys += key
                    logQrSignature(
                        "$tracePrefix import ok candidate=$candidateIndex size=${keyBytes.size} " +
                            "type=${keyTypeName(keyType)} class=${variant.includeClass} sizeAttr=${variant.includeSize}"
                    )
                }
            }
        }
    }
    if (keys.isEmpty()) {
        logQrSignature("$tracePrefix import failed candidates=${uniqueCandidates.size}")
    }
    return keys
}

private data class AttrVariant(
    val includeClass: Boolean,
    val includeSize: Boolean,
)

private fun uniqueKeyCandidates(candidates: List<ByteArray>): List<ByteArray> {
    val unique = mutableListOf<ByteArray>()
    candidates.filter { it.isNotEmpty() }.forEach { candidate ->
        if (unique.none { it.contentEquals(candidate) }) {
            unique += candidate
        }
    }
    return unique
}

private fun normalizeRawPublicPoint(source: ByteArray): ByteArray? {
    return when {
        source.size == 65 && source[0] == 0x04.toByte() -> source
        source.size == 64 -> byteArrayOf(0x04) + source
        else -> null
    }
}

private fun extractEcPublicPoint(source: ByteArray): ByteArray? {
    return normalizeRawPublicPoint(source) ?: extractSpkiPublicKeyBytes(source)
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
    val blob = der.copyOfRange(privateKeyNode.contentStart, privateKeyNode.contentEnd)
    return blob.takeIf { isSec1EcPrivateKey(it) }
}

private fun isSec1EcPrivateKey(der: ByteArray): Boolean {
    val root = readAsn1Node(der, 0) ?: return false
    val children = readAsn1Children(der, root)
    if (children.size < 2) return false
    val versionNode = children[0]
    if (versionNode.tag != 0x02) return false
    if (versionNode.contentEnd - versionNode.contentStart <= 0) return false
    val version = der[versionNode.contentEnd - 1].toInt() and 0xff
    if (version != 1) return false
    val keyNode = children[1]
    return keyNode.tag == 0x04 && keyNode.contentEnd > keyNode.contentStart
}

private fun extractEcPrivateScalar(source: ByteArray): ByteArray? {
    if (source.size == 32) return source
    if (source.size == 33 && source[0] == 0x00.toByte()) return source.copyOfRange(1, source.size)
    val sec1Source = when {
        isSec1EcPrivateKey(source) -> source
        else -> extractPkcs8PrivateKeyBlob(source)
    } ?: return null
    val root = readAsn1Node(sec1Source, 0) ?: return null
    val children = readAsn1Children(sec1Source, root)
    if (children.size < 2) return null
    val privateKeyNode = children[1]
    if (privateKeyNode.tag != 0x04) return null
    val scalar = sec1Source.copyOfRange(privateKeyNode.contentStart, privateKeyNode.contentEnd)
    return normalizePrivateScalar(scalar)
}

private fun normalizePrivateScalar(raw: ByteArray): ByteArray? {
    if (raw.isEmpty()) return null
    return when {
        raw.size == 32 -> raw
        raw.size > 32 -> raw.takeLast(32).toByteArray()
        else -> ByteArray(32 - raw.size) + raw
    }
}

private fun buildSec1EcPrivateKeyFromScalar(scalar: ByteArray): ByteArray {
    val normalizedScalar = normalizePrivateScalar(scalar) ?: scalar
    val versionNode = byteArrayOf(0x02, 0x01, 0x01)
    val privateKeyNode = derNode(0x04, normalizedScalar)
    val namedCurve = byteArrayOf(
        0xA0.toByte(), 0x0A,
        0x06, 0x08,
        0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07,
    )
    val body = versionNode + privateKeyNode + namedCurve
    return derNode(0x30, body)
}

private fun derNode(tag: Int, value: ByteArray): ByteArray {
    return byteArrayOf(tag.toByte()) + derLength(value.size) + value
}

private fun derLength(length: Int): ByteArray {
    if (length < 0x80) return byteArrayOf(length.toByte())
    var remainder = length
    val bytes = mutableListOf<Byte>()
    while (remainder > 0) {
        bytes += (remainder and 0xFF).toByte()
        remainder = remainder ushr 8
    }
    val lengthBytes = bytes.asReversed().toByteArray()
    return byteArrayOf((0x80 or lengthBytes.size).toByte()) + lengthBytes
}

private fun keyTypeName(keyType: CValuesRef<*>?): String {
    return when (keyType) {
        kSecAttrKeyTypeECSECPrimeRandom -> "ECSEC"
        kSecAttrKeyTypeEC -> "EC"
        else -> "UNKNOWN"
    }
}

private fun setSignatureDebugInfo(code: String, detail: String = "") {
    lastQrSignatureDebugInfo = if (detail.isBlank()) code else "$code|$detail"
    logQrSignature(lastQrSignatureDebugInfo)
}

private fun logQrSignature(message: String) {
    println("[QR_SIG_IOS] $message")
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
