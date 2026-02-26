package com.example.pgk_food.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Expressive Shapes — M3 Expressive Design System
 * ─────────────────────────────────────────────────
 * Asymmetric corners create visual tension & unique silhouettes.
 * XL / XXL tokens for hero moments. Pill & Shard for functional contrast.
 */
val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 8.dp,
        bottomEnd = 8.dp
    ),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(
        topStart = 48.dp,
        topEnd = 48.dp,
        bottomStart = 32.dp,
        bottomEnd = 32.dp
    )
)

/** Fully rounded pill for CTAs and buttons */
val PillShape = RoundedCornerShape(50)

/** Sharp diagonal tension — info cards, badges */
val ShardShape = RoundedCornerShape(20.dp)

/** Hero card — asymmetric diagonal for branded containers */
val HeroCardShape = RoundedCornerShape(
    topStart = 48.dp,
    topEnd = 12.dp,
    bottomEnd = 48.dp,
    bottomStart = 12.dp
)

/** Asymmetric tag shape — notification & status badges */
val TagShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomEnd = 4.dp,
    bottomStart = 16.dp
)

/** Rounded container with softer bottom corners — dashboard sections */
val SectionShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomEnd = 16.dp,
    bottomStart = 16.dp
)
