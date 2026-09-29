package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme


private val VybeColorScheme = lightColorScheme (
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

@Composable
fun VybeTheme(
    themeMode: String = "Dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "Light" -> VybeLightColorScheme

        "System" -> {
            if (isSystemInDarkTheme()) {
                VybeColorScheme
            } else {
                VybeLightColorScheme
            }
        }

        else -> VybeColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
