package com.example.pgk_food.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Spring Physics Motion System — M3 Expressive
 * ───────────────────────────────────────────────
 * Replaces classical easing/duration with physical spring specs.
 * Every motion feels natural and predictable.
 */
object AppMotion {

    /** Gentle spring — cards, containers, list items */
    val gentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Bouncy spring — buttons, FABs, interactive elements */
    val bouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Snappy spring — navigation transitions, overlays */
    val snappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /** Soft spring — subtle shifts, background layers, parallax */
    val softSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Modifier.springEntrance() — animate element's entrance with spring physics.
 * Element slides up and fades in when first composed.
 *
 * @param delay stagger delay in ms for list items
 */
fun Modifier.springEntrance(delay: Int = 0): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        if (delay > 0) delay(delay.toLong())
        launch {
            alpha.animateTo(1f, AppMotion.gentleSpring)
        }
        launch {
            offsetY.animateTo(0f, AppMotion.gentleSpring)
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        translationY = offsetY.value
    }
}

/**
 * Modifier.springScale() — press-and-release spring scale effect.
 * Makes interactive elements feel alive and physical.
 */
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
            onClick = onClick
        )
}
