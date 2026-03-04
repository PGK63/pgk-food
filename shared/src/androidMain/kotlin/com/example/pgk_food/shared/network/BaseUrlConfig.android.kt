package com.example.pgk_food.shared.network

import com.example.pgk_food.shared.BuildConfig

actual fun platformApiBaseUrlOverride(): String? {
    return BuildConfig.PGK_API_BASE_URL.takeIf { it.isNotBlank() }
}
