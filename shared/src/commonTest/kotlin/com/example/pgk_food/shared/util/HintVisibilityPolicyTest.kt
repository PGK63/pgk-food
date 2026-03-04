package com.example.pgk_food.shared.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HintVisibilityPolicyTest {
    @Test
    fun resolve_uses_override_when_set() {
        assertTrue(HintVisibilityPolicy.resolve(firstSeenAtMillis = 1L, nowMillis = 999999L, override = true))
        assertFalse(HintVisibilityPolicy.resolve(firstSeenAtMillis = 1L, nowMillis = 2L, override = false))
    }

    @Test
    fun resolve_allows_hints_inside_default_window() {
        val firstSeen = 1_000L
        val within72h = firstSeen + (72L * 60L * 60L * 1000L) - 1L
        assertTrue(HintVisibilityPolicy.resolve(firstSeenAtMillis = firstSeen, nowMillis = within72h, override = null))
    }

    @Test
    fun resolve_hides_hints_after_default_window() {
        val firstSeen = 1_000L
        val after72h = firstSeen + (72L * 60L * 60L * 1000L) + 1L
        assertFalse(HintVisibilityPolicy.resolve(firstSeenAtMillis = firstSeen, nowMillis = after72h, override = null))
    }

    @Test
    fun resolve_screen_depends_on_global_and_screen_flag() {
        assertTrue(HintVisibilityPolicy.resolveScreen(globalVisible = true, isScreenHidden = false))
        assertFalse(HintVisibilityPolicy.resolveScreen(globalVisible = true, isScreenHidden = true))
        assertFalse(HintVisibilityPolicy.resolveScreen(globalVisible = false, isScreenHidden = false))
    }
}
