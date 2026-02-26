package com.example.pgk_food.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = SecondaryColor,
    onSecondary = Color.White,
    tertiary = AccentColor,
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    error = Error
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = SecondaryColor,
    onSecondary = Color.White,
    tertiary = AccentColor,
    background = BackgroundColor,
    surface = SurfaceColor,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Error,
    surfaceVariant = Color(0xFFF1F0F4),
    onSurfaceVariant = TextSecondary
)

@Composable
fun PgkfoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
