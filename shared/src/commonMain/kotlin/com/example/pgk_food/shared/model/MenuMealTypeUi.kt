package com.example.pgk_food.shared.model

enum class MenuMealTypeUi(
    val code: String,
    val titleRu: String,
    val sortOrder: Int,
) {
    BREAKFAST(code = "BREAKFAST", titleRu = "Завтрак", sortOrder = 0),
    LUNCH(code = "LUNCH", titleRu = "Обед", sortOrder = 1),
    UNKNOWN(code = "UNKNOWN", titleRu = "Не указан", sortOrder = 2),
    ;

    companion object {
        fun fromRaw(raw: String?): MenuMealTypeUi {
            val normalized = raw?.trim()?.uppercase().orEmpty()
            return when (normalized) {
                BREAKFAST.code, "ЗАВТРАК" -> BREAKFAST
                LUNCH.code, "ОБЕД" -> LUNCH
                else -> UNKNOWN
            }
        }
    }
}
