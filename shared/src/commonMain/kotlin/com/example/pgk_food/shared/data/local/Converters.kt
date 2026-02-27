package com.example.pgk_food.shared.data.local

import androidx.room.TypeConverter
import com.example.pgk_food.shared.model.UserRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromRoles(roles: List<UserRole>): String = Json.encodeToString(roles)

    @TypeConverter
    fun toRoles(rolesString: String): List<UserRole> = Json.decodeFromString(rolesString)
}
