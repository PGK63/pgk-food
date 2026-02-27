package com.example.pgk_food.shared.util

actual fun decodeCsvBytesPlatform(bytes: ByteArray): String {
    val utf8 = bytes.decodeToString()
    if ('\uFFFD' !in utf8) return utf8
    return decodeCp1251(bytes)
}

private val cp1251ExtendedChars = charArrayOf(
    '\u0402', '\u0403', '\u201A', '\u0453', '\u201E', '\u2026', '\u2020', '\u2021',
    '\u20AC', '\u2030', '\u0409', '\u2039', '\u040A', '\u040C', '\u040B', '\u040F',
    '\u0452', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
    '\u0098', '\u2122', '\u0459', '\u203A', '\u045A', '\u045C', '\u045B', '\u045F',
    '\u00A0', '\u040E', '\u045E', '\u0408', '\u00A4', '\u0490', '\u00A6', '\u00A7',
    '\u0401', '\u00A9', '\u0404', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u0407',
    '\u00B0', '\u00B1', '\u0406', '\u0456', '\u0491', '\u00B5', '\u00B6', '\u00B7',
    '\u0451', '\u2116', '\u0454', '\u00BB', '\u0458', '\u0405', '\u0455', '\u0457',
)

private fun decodeCp1251(bytes: ByteArray): String {
    val result = StringBuilder(bytes.size)
    for (byte in bytes) {
        val value = byte.toInt() and 0xFF
        when {
            value < 0x80 -> result.append(value.toChar())
            value in 0xC0..0xFF -> result.append((0x0410 + (value - 0xC0)).toChar())
            else -> result.append(cp1251ExtendedChars[value - 0x80])
        }
    }
    return result.toString()
}
