package com.example.pgk_food.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberIcePalette.Primary80,
    onPrimary = CyberIcePalette.Primary20,
    primaryContainer = CyberIcePalette.Primary30,
    onPrimaryContainer = CyberIcePalette.Primary90,

    secondary = CyberIcePalette.Secondary80,
    onSecondary = CyberIcePalette.Secondary20,
    secondaryContainer = CyberIcePalette.Secondary30,
    onSecondaryContainer = CyberIcePalette.Secondary90,

    tertiary = CyberIcePalette.Tertiary80,
    onTertiary = CyberIcePalette.Tertiary20,
    tertiaryContainer = CyberIcePalette.Tertiary30,
    onTertiaryContainer = CyberIcePalette.Tertiary90,

    error = CyberIcePalette.Error80,
    onError = CyberIcePalette.Error20,
    errorContainer = CyberIcePalette.Error30,
    onErrorContainer = CyberIcePalette.Error90,

    background = CyberIcePalette.Neutral6,
    onBackground = CyberIcePalette.Neutral90,
    surface = CyberIcePalette.Neutral10,
    onSurface = CyberIcePalette.Neutral90,
    surfaceVariant = CyberIcePalette.NeutralVariant30,
    onSurfaceVariant = CyberIcePalette.NeutralVariant80,

    outline = CyberIcePalette.NeutralVariant60,
    outlineVariant = CyberIcePalette.NeutralVariant30,

    inverseSurface = CyberIcePalette.Neutral90,
    inverseOnSurface = CyberIcePalette.Neutral20,
    inversePrimary = CyberIcePalette.Primary40,

    scrim = CyberIcePalette.Neutral4,
)

private val LightColorScheme = lightColorScheme(
    primary = CyberIcePalette.Primary40,
    onPrimary = CyberIcePalette.Primary99,
    primaryContainer = CyberIcePalette.Primary90,
    onPrimaryContainer = CyberIcePalette.Primary10,

    secondary = CyberIcePalette.Secondary40,
    onSecondary = CyberIcePalette.Neutral99,
    secondaryContainer = CyberIcePalette.Secondary90,
    onSecondaryContainer = CyberIcePalette.Secondary10,

    tertiary = CyberIcePalette.Tertiary40,
    onTertiary = CyberIcePalette.Neutral99,
    tertiaryContainer = CyberIcePalette.Tertiary90,
    onTertiaryContainer = CyberIcePalette.Tertiary10,

    error = CyberIcePalette.Error40,
    onError = CyberIcePalette.Neutral99,
    errorContainer = CyberIcePalette.Error90,
    onErrorContainer = CyberIcePalette.Error10,

    background = CyberIcePalette.Neutral98,
    onBackground = CyberIcePalette.Neutral10,
    surface = CyberIcePalette.Neutral99,
    onSurface = CyberIcePalette.Neutral10,
    surfaceVariant = CyberIcePalette.NeutralVariant90,
    onSurfaceVariant = CyberIcePalette.NeutralVariant30,

    outline = CyberIcePalette.NeutralVariant50,
    outlineVariant = CyberIcePalette.NeutralVariant80,

    inverseSurface = CyberIcePalette.Neutral20,
    inverseOnSurface = CyberIcePalette.Neutral95,
    inversePrimary = CyberIcePalette.Primary80,

    scrim = CyberIcePalette.Neutral4,
)

@Composable
fun PgkfoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography(),
        content = content
    )
}
