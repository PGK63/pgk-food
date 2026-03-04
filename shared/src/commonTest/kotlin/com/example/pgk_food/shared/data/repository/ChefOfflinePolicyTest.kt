package com.example.pgk_food.shared.data.repository

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ChefOfflinePolicyTest {

    @Test
    fun `business day range follows Samara midnight boundary`() {
        val beforeMidnightSamara = Instant.parse("2026-03-04T19:59:59Z").epochSeconds
        val afterMidnightSamara = Instant.parse("2026-03-04T20:00:00Z").epochSeconds

        val beforeRange = businessDayEpochRange(beforeMidnightSamara)
        val afterRange = businessDayEpochRange(afterMidnightSamara)

        assertNotEquals(beforeRange.first, afterRange.first)
        assertEquals(beforeRange.second, afterRange.first)
    }

    @Test
    fun `date formatter uses Samara business date`() {
        val justBeforeMidnightSamara = Instant.parse("2026-03-04T19:59:59Z").epochSeconds
        val midnightSamara = Instant.parse("2026-03-04T20:00:00Z").epochSeconds

        assertEquals("2026-03-04", dateIsoStringFromEpochSeconds(justBeforeMidnightSamara))
        assertEquals("2026-03-05", dateIsoStringFromEpochSeconds(midnightSamara))
    }

    @Test
    fun `fast duplicate reject applies only after successful scan`() {
        val now = 10_000L
        assertTrue(
            com.example.pgk_food.shared.ui.viewmodels.shouldFastRejectDuplicateScan(
                normalizedQrData = "QR-1",
                cachedQrContent = "QR-1",
                cachedWasValid = true,
                cachedTsMillis = 5_000L,
                nowMillis = now,
                windowMillis = 30_000L,
            )
        )

        assertFalse(
            com.example.pgk_food.shared.ui.viewmodels.shouldFastRejectDuplicateScan(
                normalizedQrData = "QR-1",
                cachedQrContent = "QR-1",
                cachedWasValid = false,
                cachedTsMillis = 5_000L,
                nowMillis = now,
                windowMillis = 30_000L,
            )
        )
    }
}
