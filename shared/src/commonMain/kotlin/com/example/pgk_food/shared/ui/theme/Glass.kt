package com.example.pgk_food.shared.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.glassEffect(
    shape: Shape,
    fillColor: Color,
    borderColor: Color = Color.White.copy(alpha = 0.35f),
): Modifier = clip(shape)
    .background(fillColor)
    .border(1.dp, borderColor, shape)

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    fillColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    borderColor: Color = Color.White.copy(alpha = 0.35f),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.glassEffect(
            shape = shape,
            fillColor = fillColor,
            borderColor = borderColor,
        ),
        content = content,
    )
}
