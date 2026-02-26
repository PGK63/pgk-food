package com.example.pgk_food.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

actual fun platformName(): String = "iOS"

fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    PgkSharedApp()
}
