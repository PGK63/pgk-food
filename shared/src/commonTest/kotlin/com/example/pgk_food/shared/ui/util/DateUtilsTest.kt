package com.example.pgk_food.shared.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class DateUtilsTest {
    private val fixedNow = LocalDateTime(2026, 3, 5, 10, 0) // Thursday
    private val currentWeekMonday = LocalDate(2026, 3, 2)
    private val nextWeekMonday = LocalDate(2026, 3, 9)

    @Test
    fun rosterRules_prod_mode_disallow_current_week() {
        assertFalse(
            isRosterDateReadable(
                date = currentWeekMonday,
                now = fixedNow,
                testMode = false,
            )
        )
        assertFalse(
            isRosterDateEditable(
                date = currentWeekMonday,
                now = fixedNow,
                testMode = false,
            )
        )
        assertEquals(
            nextWeekMonday,
            firstEditableRosterDate(now = fixedNow, testMode = false),
        )
    }

    @Test
    fun rosterRules_test_mode_allow_current_week() {
        assertTrue(
            isRosterDateReadable(
                date = currentWeekMonday,
                now = fixedNow,
                testMode = true,
            )
        )
        assertTrue(
            isRosterDateEditable(
                date = currentWeekMonday,
                now = fixedNow,
                testMode = true,
            )
        )
        assertEquals(
            currentWeekMonday,
            firstEditableRosterDate(now = fixedNow, testMode = true),
        )
    }
}
