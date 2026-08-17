package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ProgeniusDarkColorScheme = darkColorScheme(
    primary = ProgeniusPrimary,
    secondary = ProgeniusSecondary,
    tertiary = ProgeniusAccent,
    background = ProgeniusBg,
    surface = ProgeniusSurface,
    onPrimary = ProgeniusTextPrimary,
    onSecondary = ProgeniusTextPrimary,
    onTertiary = ProgeniusBg,
    onBackground = ProgeniusTextPrimary,
    onSurface = ProgeniusTextPrimary
)

private val ProgeniusLightColorScheme = lightColorScheme(
    primary = ProgeniusPrimary,
    secondary = ProgeniusSecondary,
    tertiary = ProgeniusAccent,
    background = ProgeniusBg,
    surface = ProgeniusSurface,
    onPrimary = ProgeniusSurface, // White text on primary color buttons
    onSecondary = ProgeniusSurface,
    onTertiary = ProgeniusTextPrimary,
    onBackground = ProgeniusTextPrimary,
    onSurface = ProgeniusTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to false for the light "Sleek Interface" theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ProgeniusDarkColorScheme else ProgeniusLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
