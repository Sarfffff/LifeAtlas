package com.xiaoyin.lifeatlas.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LifeAtlasColorScheme = lightColorScheme(
    primary = AtlasGreen,
    onPrimary = AtlasPaper,
    secondary = AtlasOchre,
    onSecondary = AtlasInk,
    background = AtlasPaper,
    onBackground = AtlasInk,
    surface = AtlasPaper,
    onSurface = AtlasInk,
    surfaceVariant = AtlasMist,
    outline = AtlasLine
)

@Composable
fun LifeAtlasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LifeAtlasColorScheme,
        typography = LifeAtlasTypography,
        content = content
    )
}

