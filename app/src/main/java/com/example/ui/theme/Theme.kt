package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val CosmicColorScheme = lightColorScheme(
    primary = CosmicPrimary,
    onPrimary = CosmicOnPrimary,
    primaryContainer = CosmicAccent,
    secondary = CosmicSecondary,
    secondaryContainer = CosmicSurfaceVariant,
    tertiary = CosmicTertiary,
    background = CosmicBackground,
    onBackground = CosmicOnSurface,
    surface = CosmicSurface,
    onSurface = CosmicOnSurface,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = CosmicMuted,
    outline = CosmicBorder
)

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CosmicDarkPrimary,
    onPrimary = CosmicDarkOnPrimary,
    primaryContainer = CosmicDarkAccent,
    secondary = CosmicDarkSecondary,
    secondaryContainer = CosmicDarkSurfaceVariant,
    tertiary = CosmicDarkTertiary,
    background = CosmicDarkBackground,
    onBackground = CosmicDarkOnSurface,
    surface = CosmicDarkSurface,
    onSurface = CosmicDarkOnSurface,
    surfaceVariant = CosmicDarkSurfaceVariant,
    onSurfaceVariant = CosmicDarkMuted,
    outline = CosmicDarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicColorScheme
    val cosmicColors = if (darkTheme) DarkCosmicColors else LightCosmicColors

    CompositionLocalProvider(
        LocalCosmicColors provides cosmicColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

