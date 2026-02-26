package com.example.pgk_food.util

import com.example.pgk_food.model.MenuMealTypeUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.time.LocalDate

class MenuCsvParserTest {

    @Test
    fun `parse utf8 csv with header`() {
        val csv = """
            date,name,description,mealType
            2026-02-26,Каша,Овсяная,BREAKFAST
            ,Суп,Гороховый,LUNCH
        """.trimIndent()
        val result = MenuCsvParser.parse(csv, LocalDate.parse("2026-02-27"))

        assertEquals(2, result.rows.size)
        assertTrue(result.errors.isEmpty())
        assertEquals(MenuMealTypeUi.BREAKFAST, result.rows[0].mealType)
        assertEquals(LocalDate.parse("2026-02-27"), result.rows[1].date)
    }

    @Test
    fun `parse cp1251 bytes`() {
        val cp1251 = Charset.forName("windows-1251")
        val csv = "date,name,description,mealType\n2026-02-26,Борщ,Сметана,DINNER"
        val bytes = csv.toByteArray(cp1251)
        val result = MenuCsvParser.parse(bytes, LocalDate.parse("2026-02-26"))

        assertEquals(1, result.rows.size)
        assertEquals("Борщ", result.rows[0].name)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `parse utf8 bom bytes`() {
        val csv = "date,name,description,mealType\n2026-02-26,Каша,Овсяная,BREAKFAST"
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csv.toByteArray(Charsets.UTF_8)
        val result = MenuCsvParser.parse(bytes, LocalDate.parse("2026-02-26"))

        assertEquals(1, result.rows.size)
        assertEquals("Каша", result.rows[0].name)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `invalid meal type should be reported`() {
        val csv = "date,name,description,mealType\n2026-02-26,Блюдо,Описание,WRONG"
        val result = MenuCsvParser.parse(csv, LocalDate.parse("2026-02-26"))

        assertEquals(0, result.rows.size)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `invalid date should be reported`() {
        val csv = "date,name,description,mealType\n2026-26-02,Блюдо,Описание,LUNCH"
        val result = MenuCsvParser.parse(csv, LocalDate.parse("2026-02-26"))

        assertEquals(0, result.rows.size)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `empty name should be reported`() {
        val csv = "date,name,description,mealType\n2026-02-26,,Описание,LUNCH"
        val result = MenuCsvParser.parse(csv, LocalDate.parse("2026-02-26"))

        assertEquals(0, result.rows.size)
        assertEquals(1, result.errors.size)
    }
}
