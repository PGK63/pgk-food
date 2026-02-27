package com.example.pgk_food.shared.util

import com.example.pgk_food.shared.model.MenuMealTypeUi

private val mealTypePrefixRegex = Regex("^\\[MT:([A-Z_]+)]\\s*(.*)$")

data class DecodedMenuDescription(
    val mealType: MenuMealTypeUi,
    val description: String,
)

object MenuMealTypeCodec {
    fun encode(mealType: MenuMealTypeUi, description: String?): String {
        val cleanDescription = description?.trim().orEmpty()
        val prefix = "[MT:${mealType.code}]"
        return if (cleanDescription.isBlank()) prefix else "$prefix $cleanDescription"
    }

    fun decode(description: String?): DecodedMenuDescription {
        val raw = description?.trim().orEmpty()
        if (raw.isBlank()) {
            return DecodedMenuDescription(
                mealType = MenuMealTypeUi.UNKNOWN,
                description = "",
            )
        }

        val match = mealTypePrefixRegex.matchEntire(raw)
        if (match != null) {
            return DecodedMenuDescription(
                mealType = MenuMealTypeUi.fromRaw(match.groupValues[1]),
                description = match.groupValues[2].trim(),
            )
        }

        return DecodedMenuDescription(
            mealType = MenuMealTypeUi.UNKNOWN,
            description = raw,
        )
    }
}
