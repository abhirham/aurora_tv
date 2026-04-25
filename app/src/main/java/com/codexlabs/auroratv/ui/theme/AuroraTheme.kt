package com.codexlabs.auroratv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuroraDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFE50914),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFFFB000),
    onSecondary = Color(0xFF1A0E00),
    tertiary = Color(0xFF9AFFFF),
    background = Color(0xFF050505),
    onBackground = Color(0xFFF5F5F1),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFF5F5F1),
    surfaceVariant = Color(0xFF1D1D1D),
    onSurfaceVariant = Color(0xFFB8B8B8),
    outline = Color(0xFF383838),
    error = Color(0xFFFF5C65),
)

@Composable
fun AuroraTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AuroraDarkScheme.takeIf { isSystemInDarkTheme() } ?: AuroraDarkScheme,
        content = content,
    )
}
