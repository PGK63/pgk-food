package com.example.pgk_food.util

import com.example.pgk_food.model.MenuMealTypeUi
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuMealTypeCodecTest {

    @Test
    fun `encode and decode should keep meal type and text`() {
        val encoded = MenuMealTypeCodec.encode(MenuMealTypeUi.LUNCH, "Борщ")
        val decoded = MenuMealTypeCodec.decode(encoded)

        assertEquals(MenuMealTypeUi.LUNCH, decoded.mealType)
        assertEquals("Борщ", decoded.description)
    }

    @Test
    fun `decode without prefix should return UNKNOWN`() {
        val decoded = MenuMealTypeCodec.decode("Обычное описание")

        assertEquals(MenuMealTypeUi.UNKNOWN, decoded.mealType)
        assertEquals("Обычное описание", decoded.description)
    }
}
