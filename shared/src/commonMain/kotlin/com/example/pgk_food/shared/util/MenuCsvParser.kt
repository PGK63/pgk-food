package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.model.MenuMealTypeUi
import kotlinx.datetime.LocalDate

data class MenuCsvRow(
    val date: LocalDate,
    val name: String,
    val description: String,
    val mealType: MenuMealTypeUi,
)

data class MenuCsvError(
    val lineNumber: Int,
    val reason: String,
)

data class MenuCsvParseResult(
    val rows: List<MenuCsvRow>,
    val errors: List<MenuCsvError>,
)

object MenuCsvParser {
    private val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun decodeContent(bytes: ByteArray): String {
        val stripped = if (bytes.startsWith(utf8Bom)) {
            bytes.copyOfRange(utf8Bom.size, bytes.size)
        } else {
            bytes
        }
        return decodeCsvBytesPlatform(stripped)
    }

    fun parse(bytes: ByteArray, fallbackDate: LocalDate): MenuCsvParseResult {
        return parse(decodeContent(bytes), fallbackDate)
    }

    fun parse(content: String, fallbackDate: LocalDate): MenuCsvParseResult {
        val rows = mutableListOf<MenuCsvRow>()
        val errors = mutableListOf<MenuCsvError>()
        val lines = content.replace("\r\n", "\n").replace('\r', '\n').lines()

        if (lines.isEmpty()) {
            return MenuCsvParseResult(emptyList(), listOf(MenuCsvError(0, "Пустой CSV файл")))
        }

        val headerOffset = if (isHeaderLine(lines.firstOrNull().orEmpty())) 1 else 0
        lines.drop(headerOffset).forEachIndexed { idx, line ->
            val lineNumber = idx + 1 + headerOffset
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEachIndexed

            val columns = splitCsvLine(trimmed)
            if (columns.size < 4) {
                errors += MenuCsvError(lineNumber, "Ожидалось 4 столбца: date,name,description,mealType")
                return@forEachIndexed
            }

            val date = parseDate(columns[0], fallbackDate)
            if (date == null) {
                errors += MenuCsvError(lineNumber, "Некорректная дата: '${columns[0]}'")
                return@forEachIndexed
            }

            val name = columns[1].trim()
            if (name.isBlank()) {
                errors += MenuCsvError(lineNumber, "Поле 'name' не может быть пустым")
                return@forEachIndexed
            }

            val description = columns[2].trim()
            val mealType = MenuMealTypeUi.fromRaw(columns[3])
            if (mealType == MenuMealTypeUi.UNKNOWN) {
                errors += MenuCsvError(lineNumber, "Некорректный mealType: '${columns[3]}'")
                return@forEachIndexed
            }

            rows += MenuCsvRow(date = date, name = name, description = description, mealType = mealType)
        }

        return MenuCsvParseResult(rows, errors)
    }

    private fun parseDate(rawDate: String, fallbackDate: LocalDate): LocalDate? {
        val clean = rawDate.trim()
        if (clean.isBlank()) return fallbackDate
        return runCatching { LocalDate.parse(clean) }.getOrNull()
    }

    private fun isHeaderLine(line: String): Boolean {
        val header = splitCsvLine(line).map { it.trim().lowercase() }
        return header.size >= 4 &&
            header[0] == "date" &&
            header[1] == "name" &&
            header[2] == "description" &&
            header[3] == "mealtype"
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val token = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    val hasEscapedQuote = inQuotes && i + 1 < line.length && line[i + 1] == '"'
                    if (hasEscapedQuote) {
                        token.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result += token.toString()
                    token.clear()
                }
                else -> token.append(ch)
            }
            i++
        }
        result += token.toString()
        return result
    }
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) return false
    }
    return true
}

expect fun decodeCsvBytesPlatform(bytes: ByteArray): String
