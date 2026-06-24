package com.xiaoyin.lifeatlas.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeAtlasColorScheme = lightColorScheme(
    primary = WildernessTeal,
    onPrimary = WildernessPaper,
    secondary = WildernessSunset,
    onSecondary = WildernessTeal,
    tertiary = WildernessGreen,
    onTertiary = WildernessPaper,
    background = WildernessCream,
    onBackground = WildernessTeal,
    surface = WildernessPaper,
    onSurface = WildernessTeal,
    surfaceVariant = Color(0xFFFFF0D6),
    onSurfaceVariant = Color(0xFF5F756E),
    outline = WildernessLine,
    error = WildernessCoral
)

@Composable
fun LifeAtlasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LifeAtlasColorScheme,
        typography = LifeAtlasTypography,
        content = content
    )
}
