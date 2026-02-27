package com.example.pgk_food.shared.runtime

import com.example.pgk_food.shared.platform.PlatformKeyValueStore

data class MainLoopSnapshot(
    val selectedRole: String?,
    val currentSubScreen: String,
    val selectedMealType: String,
)

class MainLoopStateStore {
    fun save(userId: String, snapshot: MainLoopSnapshot) {
        val safeUserId = sanitize(userId)
        if (snapshot.selectedRole.isNullOrBlank()) {
            PlatformKeyValueStore.remove(STORE, roleKey(safeUserId))
        } else {
            PlatformKeyValueStore.putString(STORE, roleKey(safeUserId), snapshot.selectedRole)
        }
        PlatformKeyValueStore.putString(STORE, subScreenKey(safeUserId), snapshot.currentSubScreen)
        PlatformKeyValueStore.putString(STORE, mealTypeKey(safeUserId), snapshot.selectedMealType)
    }

    fun restore(userId: String): MainLoopSnapshot? {
        val safeUserId = sanitize(userId)
        val role = if (PlatformKeyValueStore.contains(STORE, roleKey(safeUserId))) {
            PlatformKeyValueStore.getString(STORE, roleKey(safeUserId), "")
                .ifBlank { null }
        } else {
            null
        }
        val hasSubScreen = PlatformKeyValueStore.contains(STORE, subScreenKey(safeUserId))
        val hasMealType = PlatformKeyValueStore.contains(STORE, mealTypeKey(safeUserId))
        if (!hasSubScreen && !hasMealType && role == null) return null

        val subScreen = PlatformKeyValueStore.getString(STORE, subScreenKey(safeUserId), DASHBOARD)
            .ifBlank { DASHBOARD }
        val mealType = PlatformKeyValueStore.getString(STORE, mealTypeKey(safeUserId), "")
        return MainLoopSnapshot(
            selectedRole = role,
            currentSubScreen = subScreen,
            selectedMealType = mealType,
        )
    }

    fun clear(userId: String) {
        val safeUserId = sanitize(userId)
        PlatformKeyValueStore.remove(STORE, roleKey(safeUserId))
        PlatformKeyValueStore.remove(STORE, subScreenKey(safeUserId))
        PlatformKeyValueStore.remove(STORE, mealTypeKey(safeUserId))
    }

    private fun sanitize(value: String): String = value.trim()

    private fun roleKey(userId: String): String = "role:$userId"
    private fun subScreenKey(userId: String): String = "sub_screen:$userId"
    private fun mealTypeKey(userId: String): String = "meal_type:$userId"

    private companion object {
        const val STORE = "main_loop_state"
        const val DASHBOARD = "dashboard"
    }
}
