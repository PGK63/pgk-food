package com.example.pgk_food.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism / Liquid Glass System
 * ────────────────────────────────────
 * Multi-layered frosted glass effect.
 * • Background blur (10-30dp) for depth
 * • Semi-transparent fill for readability
 * • Thin white border (1dp) for crisp edges
 *
 * Falls back to higher-opacity surface on API < 31 (no RenderEffect).
 */

/**
 * Modifier.glassEffect() — standard glass: blur 20dp + translucent fill + thin border.
 */
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(28.dp),
    blurRadius: Dp = 20.dp,
    fillColor: Color = CyberIcePalette.GlassWhite,
    borderColor: Color = CyberIcePalette.GlassBorder,
    borderWidth: Dp = 1.dp
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .clip(shape)
            .background(fillColor)
            .border(borderWidth, borderColor, shape)
    } else {
        // Fallback: higher opacity, no blur
        this
            .clip(shape)
            .background(fillColor.copy(alpha = 0.85f))
            .border(borderWidth, borderColor, shape)
    }
}

/**
 * Modifier.frostedGlass() — heavier glass: blur 30dp, more opaque.
 * For panels, bottom sheets, overlays.
 */
fun Modifier.frostedGlass(
    shape: Shape = RoundedCornerShape(28.dp)
): Modifier = glassEffect(
    shape = shape,
    blurRadius = 30.dp,
    fillColor = CyberIcePalette.GlassWhiteMedium,
    borderColor = CyberIcePalette.GlassBorder,
    borderWidth = 1.dp
)

/**
 * GlassSurface — Composable wrapper for glass containers.
 * Provides a semantically correct glass card with proper layering.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    blurRadius: Dp = 20.dp,
    fillColor: Color = CyberIcePalette.GlassWhite,
    borderColor: Color = CyberIcePalette.GlassBorder,
    content: @Composable BoxScope.() -> Unit
) {
    val baseModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        modifier
            .clip(shape)
            .background(fillColor)
            .border(1.dp, borderColor, shape)
    } else {
        modifier
            .clip(shape)
            .background(fillColor.copy(alpha = 0.88f))
            .border(1.dp, borderColor, shape)
    }

    Box(
        modifier = baseModifier,
        content = content
    )
}

/**
 * GlassSurfaceDark — glass for dark theme / dark backgrounds.
 */
@Composable
fun GlassSurfaceDark(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        fillColor = CyberIcePalette.GlassDark,
        borderColor = CyberIcePalette.GlassDarkBorder,
        content = content
    )
}
