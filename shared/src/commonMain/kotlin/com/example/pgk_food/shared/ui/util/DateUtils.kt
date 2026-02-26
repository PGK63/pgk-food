package com.example.pgk_food.shared.ui.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

data class DateParts(val day: String, val month: String, val year: String)

fun todayLocalDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun todayDateParts(): DateParts {
    val d = todayLocalDate()
    return DateParts(
        day = d.dayOfMonth.toString().padStart(2, '0'),
        month = d.monthNumber.toString().padStart(2, '0'),
        year = d.year.toString()
    )
}

fun ymdFromParts(day: String, month: String, year: String): String? {
    val d = day.toIntOrNull() ?: return null
    val m = month.toIntOrNull() ?: return null
    val y = year.toIntOrNull() ?: return null
    return runCatching { LocalDate(y, m, d).toString() }.getOrNull()
}

fun parseRuDate(text: String): LocalDate? {
    val p = text.split('.')
    if (p.size != 3) return null
    val d = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    val y = p[2].toIntOrNull() ?: return null
    return runCatching { LocalDate(y, m, d) }.getOrNull()
}

fun formatRuDate(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}.${date.year}"

fun minusDays(date: LocalDate, days: Int): LocalDate = date - DatePeriod(days = days)
fun plusDays(date: LocalDate, days: Int): LocalDate = date + DatePeriod(days = days)
