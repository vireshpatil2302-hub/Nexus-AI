package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Bento Grid Theme Palette - Light Mode
val CosmicBackground = Color(0xFFFEF7FF) // Light lavender background
val CosmicSurface = Color(0xFFFFFFFF)    // Pure white for bento boxes
val CosmicSurfaceVariant = Color(0xFFF3EDF7) // Muted container color
val CosmicPrimary = Color(0xFF6750A4)    // Theme deep purple
val CosmicSecondary = Color(0xFF21005D)  // Contrast dark violet
val CosmicTertiary = Color(0xFF6750A4)   // Deep purple for status/branding
val CosmicAccent = Color(0xFFEADDFF)     // Light violet accent
val CosmicOnPrimary = Color(0xFFFFFFFF)  // White text on dark elements
val CosmicOnSurface = Color(0xFF1D1B20)  // Deep charcoal/almost black text
val CosmicMuted = Color(0xFF49454F)      // Muted dark gray
val CosmicBorder = Color(0xFFCAC4D0)     // Classic material outline gray

// Bento Grid Theme Palette - Dark Mode
val CosmicDarkBackground = Color(0xFF0F0C1B) // Deep space obsidian background
val CosmicDarkSurface = Color(0xFF17132A)    // Premium dark bento boxes
val CosmicDarkSurfaceVariant = Color(0xFF231E3D) // Muted dark purple container
val CosmicDarkPrimary = Color(0xFFBB86FC)    // Electric glowing violet
val CosmicDarkSecondary = Color(0xFF03DAC6)  // Tech cyan secondary
val CosmicDarkTertiary = Color(0xFFCF6679)   // Coral pink tertiary
val CosmicDarkAccent = Color(0xFF3700B3)     // Rich purple accent
val CosmicDarkOnPrimary = Color(0xFF000000)  // Black text on bright elements
val CosmicDarkOnSurface = Color(0xFFE2E0EC)  // Crisp light-silver text
val CosmicDarkMuted = Color(0xFF9894A6)      // Muted cool grey-purple
val CosmicDarkBorder = Color(0xFF3F3B54)     // Clean defined dark border

data class CosmicColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val accent: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val muted: Color,
    val border: Color
)

val LightCosmicColors = CosmicColors(
    primary = CosmicPrimary,
    secondary = CosmicSecondary,
    tertiary = CosmicTertiary,
    background = CosmicBackground,
    surface = CosmicSurface,
    surfaceVariant = CosmicSurfaceVariant,
    accent = CosmicAccent,
    onPrimary = CosmicOnPrimary,
    onSurface = CosmicOnSurface,
    muted = CosmicMuted,
    border = CosmicBorder
)

val DarkCosmicColors = CosmicColors(
    primary = CosmicDarkPrimary,
    secondary = CosmicDarkSecondary,
    tertiary = CosmicDarkTertiary,
    background = CosmicDarkBackground,
    surface = CosmicDarkSurface,
    surfaceVariant = CosmicDarkSurfaceVariant,
    accent = CosmicDarkAccent,
    onPrimary = CosmicDarkOnPrimary,
    onSurface = CosmicDarkOnSurface,
    muted = CosmicDarkMuted,
    border = CosmicDarkBorder
)

val LocalCosmicColors = staticCompositionLocalOf { LightCosmicColors }

