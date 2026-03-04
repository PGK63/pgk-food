package com.example.pgk_food.shared.util

object UxAnalytics {
    fun log(
        event: String,
        role: String,
        screen: String,
        code: String? = null,
        requestId: String? = null,
    ) {
        val codeSuffix = if (code == null) "" else ",code=$code"
        val requestIdSuffix = if (requestId.isNullOrBlank()) "" else ",requestId=$requestId"
        val suffix = "$codeSuffix$requestIdSuffix"
        println("UX_ANALYTICS event=$event,role=$role,screen=$screen$suffix")
    }
}
