package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.data.remote.dto.MenuItemDto

fun sortMenuItemsForUi(items: List<MenuItemDto>): List<MenuItemDto> {
    return items.sortedWith(
        compareBy<MenuItemDto>(
            { MenuMealTypeCodec.decode(it.description).mealType.sortOrder },
            { it.name.lowercase() },
        )
    )
}
