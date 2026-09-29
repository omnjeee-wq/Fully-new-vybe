package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VybeDarkColorScheme = darkColorScheme(
    primary = VybeOrange,
    onPrimary = Color.White,
    primaryContainer = VybeSurfaceVariant,
    onPrimaryContainer = VybeOrange,
    secondary = VybeCoral,
    onSecondary = Color.White,
    tertiary = VybeElectricCyan,
    onTertiary = Color.White,
    background = VybeBlack,
    onBackground = VybeTextPrimary,
    surface = VybeNearBlack,
    onSurface = VybeTextPrimary,
    surfaceVariant = VybeSurface,
    onSurfaceVariant = VybeTextSecondary,
    outline = VybeBorder
)

private val VybeLightColorScheme = lightColorScheme(
    primary = VybeOrange,
    onPrimary = Color.White,
    primaryContainer = VybeSurfaceVariant,
    onPrimaryContainer = VybeOrange,
    secondary = VybeCoral,
    onSecondary = Color.White,
    tertiary = VybeElectricCyan,
    onTertiary = Color.Black,
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE9E9E9),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFF888888)
)

@Composable
fun VybeTheme(
    themeMode: String = "Dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "Light" -> VybeLightColorScheme

        "System" -> {
            if (isSystemInDarkTheme()) {
                VybeDarkColorScheme
            } else {
                VybeLightColorScheme
            }
        }

        else -> VybeDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
