package com.example.pgk_food.shared.util

object UxAnalytics {
    fun log(
        event: String,
        role: String,
        screen: String,
        code: String? = null,
    ) {
        val suffix = if (code == null) "" else ",code=$code"
        println("UX_ANALYTICS event=$event,role=$role,screen=$screen$suffix")
    }
}
