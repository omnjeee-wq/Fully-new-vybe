package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalVybeAccentColor = staticCompositionLocalOf { VybeOrange }

fun vybeDarkColorScheme(accentColor: Color = VybeOrange) = darkColorScheme(
    primary = accentColor,
    onPrimary = Color.White,
    primaryContainer = VybeSurfaceVariant,
    onPrimaryContainer = accentColor,
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

fun vybeLightColorScheme(accentColor: Color = VybeOrange) = lightColorScheme(
    primary = accentColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1F5F9),
    onPrimaryContainer = accentColor,
    secondary = VybeCoral,
    onSecondary = Color.White,
    tertiary = VybeElectricCyan,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun VybeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = VybeOrange,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> vybeDarkColorScheme(accentColor)
        else -> vybeLightColorScheme(accentColor)
    }

    CompositionLocalProvider(LocalVybeAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
