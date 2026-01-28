package com.example.pgk_food.data.local

import androidx.room.TypeConverter
import com.example.pgk_food.model.UserRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromRoles(roles: List<UserRole>): String {
        return Json.encodeToString(roles)
    }

    @TypeConverter
    fun toRoles(rolesString: String): List<UserRole> {
        return Json.decodeFromString(rolesString)
    }
}
