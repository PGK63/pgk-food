package com.example.pgk_food.shared.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppMotion {
    val gentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    val bouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val snappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    val softSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )
}

fun Modifier.springEntrance(delayMs: Int = 0): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        launch { alphaAnim.animateTo(1f, AppMotion.gentleSpring) }
        launch { offsetY.animateTo(0f, AppMotion.gentleSpring) }
    }

    graphicsLayer {
        alpha = alphaAnim.value
        translationY = offsetY.value
    }
}

fun Modifier.springScale(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            scale.animateTo(0.94f, AppMotion.snappySpring)
        } else {
            scale.animateTo(1f, AppMotion.bouncySpring)
        }
    }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}
