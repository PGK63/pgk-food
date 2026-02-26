package com.example.pgk_food.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HintVisibilityPolicyTest {

    @Test
    fun `first launch should show hints`() {
        val now = 1_000_000L
        assertTrue(HintVisibilityPolicy.resolve(firstSeenAtMillis = now, nowMillis = now, override = null))
    }

    @Test
    fun `within three days should show hints`() {
        val firstSeen = 10_000L
        val now = firstSeen + (72L * 60L * 60L * 1000L) - 1L
        assertTrue(HintVisibilityPolicy.resolve(firstSeenAtMillis = firstSeen, nowMillis = now, override = null))
    }

    @Test
    fun `after three days should hide hints without override`() {
        val firstSeen = 10_000L
        val now = firstSeen + (72L * 60L * 60L * 1000L) + 1L
        assertFalse(HintVisibilityPolicy.resolve(firstSeenAtMillis = firstSeen, nowMillis = now, override = null))
    }

    @Test
    fun `override on should always show hints`() {
        val firstSeen = 10_000L
        val now = firstSeen + (72L * 60L * 60L * 1000L) + 10_000L
        assertTrue(HintVisibilityPolicy.resolve(firstSeenAtMillis = firstSeen, nowMillis = now, override = true))
    }

    @Test
    fun `override off should always hide hints`() {
        val firstSeen = 10_000L
        val now = firstSeen
        assertFalse(HintVisibilityPolicy.resolve(firstSeenAtMillis = firstSeen, nowMillis = now, override = false))
    }
}
