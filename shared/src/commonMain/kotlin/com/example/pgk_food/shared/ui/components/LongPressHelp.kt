package com.example.pgk_food.shared.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import com.example.pgk_food.shared.core.feedback.FeedbackController

fun Modifier.longPressHelp(
    actionId: String? = null,
    fallbackDescription: String? = null,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    val helpText = remember(actionId, fallbackDescription) {
        ActionHelpCatalog.resolve(actionId = actionId, fallbackDescription = fallbackDescription)
    }
    if (helpText.isNullOrBlank()) {
        this
    } else {
        this.pointerInput(helpText) {
            detectTapGestures(
                onLongPress = {
                    FeedbackController.info(helpText)
                }
            )
        }
    }
}
