package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun UltraPlayerTheme(
    darkTheme: Boolean = false, // White theme by default as requested
    accentColor: Color = ProBlue,
    content: @Composable () -> Unit
) {
    val lightScheme = lightColorScheme(
        primary = accentColor,
        onPrimary = Color.White,
        primaryContainer = accentColor.copy(alpha = 0.12f),
        onPrimaryContainer = accentColor,
        secondary = AccentPurple,
        onSecondary = Color.White,
        secondaryContainer = AccentPurple.copy(alpha = 0.12f),
        onSecondaryContainer = AccentPurple,
        background = LightBg,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfacePillInactive,
        onSurfaceVariant = LightTextSecondary,
        outline = LightSurfaceBorder,
        outlineVariant = LightSurfaceBorder.copy(alpha = 0.6f),
        error = StatusRed,
        onError = Color.White
    )

    val darkScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = AmoledBlack,
        primaryContainer = accentColor.copy(alpha = 0.2f),
        onPrimaryContainer = Color.White,
        secondary = AccentPurple,
        onSecondary = Color.White,
        secondaryContainer = AccentPurple.copy(alpha = 0.2f),
        onSecondaryContainer = Color.White,
        background = AmoledBlack,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceElevated,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkSurfaceBorder,
        outlineVariant = DarkSurfaceBorder.copy(alpha = 0.5f),
        error = StatusRed,
        onError = Color.White
    )

    val colorScheme = if (darkTheme) darkScheme else lightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
