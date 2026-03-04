package com.example.pgk_food.shared.network

import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo

actual fun platformApiBaseUrlOverride(): String? {
    val envOverride = NSProcessInfo.processInfo.environment["PGK_API_BASE_URL"] as? String
    if (!envOverride.isNullOrBlank()) {
        return envOverride
    }

    val plistOverride = NSBundle.mainBundle.objectForInfoDictionaryKey("PGK_API_BASE_URL") as? String
    return plistOverride?.takeIf { it.isNotBlank() }
}
