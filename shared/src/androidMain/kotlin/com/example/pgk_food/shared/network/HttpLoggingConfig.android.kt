package com.example.pgk_food.shared.network

import com.example.pgk_food.shared.BuildConfig

actual fun isDebugHttpLoggingEnabled(): Boolean = BuildConfig.PGK_DEBUG_HTTP
