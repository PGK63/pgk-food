package com.example.pgk_food.shared.util

import java.nio.charset.Charset

private val cp1251: Charset = Charset.forName("windows-1251")

actual fun decodeCsvBytesPlatform(bytes: ByteArray): String {
    val utf8 = bytes.toString(Charsets.UTF_8)
    if ('\uFFFD' !in utf8) return utf8
    return bytes.toString(cp1251)
}
