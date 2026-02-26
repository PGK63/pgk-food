package com.example.pgk_food.util

import com.example.pgk_food.data.remote.dto.MenuItemDto
import java.util.Locale

fun sortMenuItemsForUi(items: List<MenuItemDto>): List<MenuItemDto> {
    return items.sortedWith(
        compareBy<MenuItemDto>(
            { MenuMealTypeCodec.decode(it.description).mealType.sortOrder },
            { it.name.lowercase(Locale.getDefault()) }
        )
    )
}
