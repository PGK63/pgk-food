package com.example.pgk_food.shared.util

actual fun decodeCsvBytesPlatform(bytes: ByteArray): String {
    return bytes.decodeToString()
}
