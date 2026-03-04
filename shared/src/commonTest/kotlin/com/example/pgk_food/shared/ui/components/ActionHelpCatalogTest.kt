package com.example.pgk_food.shared.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ActionHelpCatalogTest {
    @Test
    fun resolve_prefers_detailed_catalog_text() {
        val resolved = ActionHelpCatalog.resolve(
            actionId = "groups.delete",
            fallbackDescription = "Удалить группу",
        )
        assertEquals(
            "Удаляет группу после проверки, что студенты перенесены.",
            resolved,
        )
    }

    @Test
    fun resolve_falls_back_to_description_when_action_missing() {
        val resolved = ActionHelpCatalog.resolve(
            actionId = "unknown.action",
            fallbackDescription = "Открыть",
        )
        assertEquals("Открыть", resolved)
    }
}
