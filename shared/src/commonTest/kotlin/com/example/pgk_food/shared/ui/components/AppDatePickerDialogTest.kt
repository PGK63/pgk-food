package com.example.pgk_food.shared.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class AppDatePickerDialogTest {
    @Test
    fun toDatePickerUtcMillis_roundTrips_without_day_shift() {
        val dates = listOf(
            LocalDate(2026, 1, 1),
            LocalDate(2026, 3, 4),
            LocalDate(2026, 12, 31),
        )

        dates.forEach { date ->
            val roundTrip = date.toDatePickerUtcMillis().toLocalDateFromPickerMillis()
            assertEquals(date, roundTrip)
        }
    }

    @Test
    fun toLocalDateFromPickerMillis_handles_leap_day() {
        val leapDay = LocalDate(2028, 2, 29)
        val restored = leapDay.toDatePickerUtcMillis().toLocalDateFromPickerMillis()
        assertEquals(leapDay, restored)
    }
}
