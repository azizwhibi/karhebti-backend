package com.example.karhebti_android.ui.theme

// Thème Material3 Light/Dark pour le module Gestion des pannes (Karhebti)
// Couleurs : Rouge SOS, Vert résolu, Orange en cours, Bleu info

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RedSOS = Color(0xFFD21313)
val GreenResolved = Color(0xFF388E3C)
val OrangeInProgress = Color(0xFFF57C00)
val BlueInfo = Color(0xFF1976D2)

private val LightColors = lightColorScheme(
    primary = RedSOS,
    secondary = BlueInfo,
    tertiary = OrangeInProgress,
    background = Color(0xFFF8F8F8),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = RedSOS,
    secondary = BlueInfo,
    tertiary = OrangeInProgress,
    background = Color(0xFF181A1B),
    surface = Color(0xFF232323),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun BreakdownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
