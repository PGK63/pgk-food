package com.example.pgk_food.util

import com.example.pgk_food.data.remote.dto.MenuItemDto
import com.example.pgk_food.model.MenuMealTypeUi
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuSortingTest {

    @Test
    fun `sort should order by meal type then by name`() {
        val items = listOf(
            MenuItemDto(id = "1", date = "2026-02-26", name = "Яблоко", description = MenuMealTypeCodec.encode(MenuMealTypeUi.DINNER, "")),
            MenuItemDto(id = "2", date = "2026-02-26", name = "Борщ", description = MenuMealTypeCodec.encode(MenuMealTypeUi.LUNCH, "")),
            MenuItemDto(id = "3", date = "2026-02-26", name = "Каша", description = MenuMealTypeCodec.encode(MenuMealTypeUi.BREAKFAST, "")),
            MenuItemDto(id = "4", date = "2026-02-26", name = "Арбуз", description = MenuMealTypeCodec.encode(MenuMealTypeUi.DINNER, "")),
            MenuItemDto(id = "5", date = "2026-02-26", name = "Салат", description = "Без префикса")
        )

        val sorted = sortMenuItemsForUi(items)

        assertEquals(listOf("3", "2", "4", "1", "5"), sorted.map { it.id })
    }
}
