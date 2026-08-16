package com.xoplayer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val XODarkScheme = darkColorScheme(
    primary = XOGreen,
    onPrimary = XOGreenDeep,
    secondary = XOGreenBright,
    background = XOBackground,
    onBackground = XOOnSurface,
    surface = XOSurface,
    onSurface = XOOnSurface,
    surfaceVariant = XOSurfaceVariant,
    onSurfaceVariant = XOOnSurfaceMuted,
    error = XOError
)

private val XOLightScheme = lightColorScheme(
    primary = XOGreen,
    onPrimary = Color.White,
    secondary = XOGreenBright,
    background = Color(0xFFF3F8ED),
    onBackground = XOGreenDeep,
    surface = Color(0xFFFFFFFF),
    onSurface = XOGreenDeep,
    surfaceVariant = Color(0xFFE4EEDA),
    onSurfaceVariant = Color(0xFF4C5A3E),
    error = XOError
)

@Composable
fun XOPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) XODarkScheme else XOLightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = XOTypography,
        content = content
    )
}
