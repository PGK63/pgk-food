package com.example.pgk_food.util

import android.util.Log

object UxAnalytics {
    private const val TAG = "UX_ANALYTICS"

    fun log(
        event: String,
        role: String,
        screen: String,
        code: String? = null
    ) {
        val suffix = if (code == null) "" else ",code=$code"
        Log.d(TAG, "event=$event,role=$role,screen=$screen$suffix")
    }
}

