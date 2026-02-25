package com.example.pgk_food.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * PGK Food — Cyber-Ice Theme
 * ───────────────────────────
 * 26+ color roles mapped from HCT tonal palette.
 * Tonal elevation replaces shadow-based elevation.
 */

private val LightColorScheme = lightColorScheme(
    // ── Primary ──
    primary              = CyberIcePalette.Primary40,
    onPrimary            = CyberIcePalette.Primary99,
    primaryContainer     = CyberIcePalette.Primary90,
    onPrimaryContainer   = CyberIcePalette.Primary10,

    // ── Secondary ──
    secondary            = CyberIcePalette.Secondary40,
    onSecondary          = CyberIcePalette.Neutral99,
    secondaryContainer   = CyberIcePalette.Secondary90,
    onSecondaryContainer = CyberIcePalette.Secondary10,

    // ── Tertiary (Cyan accent for hero moments) ──
    tertiary             = CyberIcePalette.Tertiary40,
    onTertiary           = CyberIcePalette.Neutral99,
    tertiaryContainer    = CyberIcePalette.Tertiary90,
    onTertiaryContainer  = CyberIcePalette.Tertiary10,

    // ── Error ──
    error                = CyberIcePalette.Error40,
    onError              = CyberIcePalette.Neutral99,
    errorContainer       = CyberIcePalette.Error90,
    onErrorContainer     = CyberIcePalette.Error10,

    // ── Background & Surface (Tone 98 — Cyber-Ice ultra-light) ──
    background           = CyberIcePalette.Neutral98,
    onBackground         = CyberIcePalette.Neutral10,
    surface              = CyberIcePalette.Neutral99,
    onSurface            = CyberIcePalette.Neutral10,
    surfaceVariant       = CyberIcePalette.NeutralVariant90,
    onSurfaceVariant     = CyberIcePalette.NeutralVariant30,

    // ── Outline ──
    outline              = CyberIcePalette.NeutralVariant50,
    outlineVariant       = CyberIcePalette.NeutralVariant80,

    // ── Inverse ──
    inverseSurface       = CyberIcePalette.Neutral20,
    inverseOnSurface     = CyberIcePalette.Neutral95,
    inversePrimary       = CyberIcePalette.Primary80,

    // ── Scrim ──
    scrim                = CyberIcePalette.Neutral4
)

private val DarkColorScheme = darkColorScheme(
    // ── Primary ──
    primary              = CyberIcePalette.Primary80,
    onPrimary            = CyberIcePalette.Primary20,
    primaryContainer     = CyberIcePalette.Primary30,
    onPrimaryContainer   = CyberIcePalette.Primary90,

    // ── Secondary ──
    secondary            = CyberIcePalette.Secondary80,
    onSecondary          = CyberIcePalette.Secondary20,
    secondaryContainer   = CyberIcePalette.Secondary30,
    onSecondaryContainer = CyberIcePalette.Secondary90,

    // ── Tertiary ──
    tertiary             = CyberIcePalette.Tertiary80,
    onTertiary           = CyberIcePalette.Tertiary20,
    tertiaryContainer    = CyberIcePalette.Tertiary30,
    onTertiaryContainer  = CyberIcePalette.Tertiary90,

    // ── Error ──
    error                = CyberIcePalette.Error80,
    onError              = CyberIcePalette.Error20,
    errorContainer       = CyberIcePalette.Error30,
    onErrorContainer     = CyberIcePalette.Error90,

    // ── Background & Surface ──
    background           = CyberIcePalette.Neutral6,
    onBackground         = CyberIcePalette.Neutral90,
    surface              = CyberIcePalette.Neutral10,
    onSurface            = CyberIcePalette.Neutral90,
    surfaceVariant       = CyberIcePalette.NeutralVariant30,
    onSurfaceVariant     = CyberIcePalette.NeutralVariant80,

    // ── Outline ──
    outline              = CyberIcePalette.NeutralVariant60,
    outlineVariant       = CyberIcePalette.NeutralVariant30,

    // ── Inverse ──
    inverseSurface       = CyberIcePalette.Neutral90,
    inverseOnSurface     = CyberIcePalette.Neutral20,
    inversePrimary       = CyberIcePalette.Primary40,

    // ── Scrim ──
    scrim                = CyberIcePalette.Neutral4
)

@Composable
fun PgkfoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
