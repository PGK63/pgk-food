package com.example.pgk_food.shared.model

enum class MenuMealTypeUi(
    val code: String,
    val titleRu: String,
    val sortOrder: Int,
) {
    BREAKFAST(code = "BREAKFAST", titleRu = "Завтрак", sortOrder = 0),
    LUNCH(code = "LUNCH", titleRu = "Обед", sortOrder = 1),
    DINNER(code = "DINNER", titleRu = "Ужин", sortOrder = 2),
    SNACK(code = "SNACK", titleRu = "Полдник", sortOrder = 3),
    SPECIAL(code = "SPECIAL", titleRu = "Спецпитание", sortOrder = 4),
    UNKNOWN(code = "UNKNOWN", titleRu = "Не указан", sortOrder = 5),
    ;

    companion object {
        fun fromRaw(raw: String?): MenuMealTypeUi {
            val normalized = raw?.trim()?.uppercase().orEmpty()
            return when (normalized) {
                BREAKFAST.code, "ЗАВТРАК" -> BREAKFAST
                LUNCH.code, "ОБЕД" -> LUNCH
                DINNER.code, "УЖИН" -> DINNER
                SNACK.code, "ПОЛДНИК" -> SNACK
                SPECIAL.code, "СПЕЦ", "СПЕЦПИТАНИЕ", "СПЕЦ_ПИТАНИЕ", "СПЕЦИАЛЬНОЕ" -> SPECIAL
                else -> UNKNOWN
            }
        }
    }
}
