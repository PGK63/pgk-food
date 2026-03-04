package com.example.pgk_food.shared.ui.util

import com.example.pgk_food.shared.platform.currentTimeMillis
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class DateParts(val day: String, val month: String, val year: String)

private val SAMARA_TIME_ZONE = TimeZone.of("Europe/Samara")

fun todayLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(currentTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

fun nowSamara(): LocalDateTime =
    Instant.fromEpochMilliseconds(currentTimeMillis()).toLocalDateTime(SAMARA_TIME_ZONE)

fun todaySamara(): LocalDate = nowSamara().date

fun mondayOfWeek(date: LocalDate): LocalDate =
    date - DatePeriod(days = date.dayOfWeek.ordinal)

fun nextWeekStart(date: LocalDate = todaySamara()): LocalDate =
    mondayOfWeek(date) + DatePeriod(days = 7)

fun isWeekdayMonToFri(date: LocalDate): Boolean = date.dayOfWeek.ordinal in 0..4

fun rosterWeekDeadline(weekStart: LocalDate): LocalDateTime =
    (weekStart - DatePeriod(days = 3)).let { friday ->
        LocalDateTime(
            year = friday.year,
            monthNumber = friday.monthNumber,
            dayOfMonth = friday.dayOfMonth,
            hour = 12,
            minute = 0,
        )
    }

fun isRosterDateReadable(
    date: LocalDate,
    now: LocalDateTime = nowSamara(),
): Boolean {
    if (!isWeekdayMonToFri(date)) return false
    val targetWeek = mondayOfWeek(date)
    val nextWeek = nextWeekStart(now.date)
    return targetWeek >= nextWeek
}

fun isRosterDateEditable(
    date: LocalDate,
    now: LocalDateTime = nowSamara(),
): Boolean {
    if (!isRosterDateReadable(date, now)) return false
    val targetWeek = mondayOfWeek(date)
    val nextWeek = nextWeekStart(now.date)
    if (targetWeek == nextWeek && now >= rosterWeekDeadline(nextWeek)) return false
    return true
}

fun firstEditableRosterDate(now: LocalDateTime = nowSamara()): LocalDate {
    var candidate = nextWeekStart(now.date)
    repeat(20) {
        if (isRosterDateEditable(candidate, now)) return candidate
        candidate = plusDays(candidate, 1)
    }
    return candidate
}

fun nextEditableRosterDateFrom(
    startDate: LocalDate,
    now: LocalDateTime = nowSamara(),
): LocalDate {
    var candidate = startDate
    repeat(40) {
        if (isRosterDateEditable(candidate, now)) return candidate
        candidate = plusDays(candidate, 1)
    }
    return candidate
}

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

fun formatRuDateTime(dateTime: LocalDateTime): String {
    val hh = dateTime.hour.toString().padStart(2, '0')
    val mm = dateTime.minute.toString().padStart(2, '0')
    return "${formatRuDate(dateTime.date)} $hh:$mm"
}

fun parseIsoDateTimeOrNull(value: String?): LocalDateTime? {
    if (value.isNullOrBlank()) return null
    return runCatching { LocalDateTime.parse(value) }.getOrNull()
}

fun minusDays(date: LocalDate, days: Int): LocalDate = date - DatePeriod(days = days)
fun plusDays(date: LocalDate, days: Int): LocalDate = date + DatePeriod(days = days)
