package com.playerid.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PlayerIDDarkColorScheme = darkColorScheme(
    primary = SpotrPrimaryBlue,
    onPrimary = Color.White,
    secondary = SpotrHighlightOrange,
    onSecondary = Color.White,
    tertiary = SpotrSuccessGreen,
    onTertiary = Color.White,
    background = SpotrDarkSurface,
    onBackground = Color.White,
    surface = SpotrSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1B344D),
    onSurfaceVariant = Color(0xFFB8C7D9),
    error = ErrorRed,
    onError = Color.White,
    outline = SpotrOutlineDark,
    outlineVariant = Color(0xFF29445F)
)

val PlayerIDLightColorScheme = lightColorScheme(
    primary = SpotrPrimaryBlue,
    onPrimary = Color.White,
    secondary = SpotrHighlightOrange,
    onSecondary = Color.White,
    tertiary = SpotrSuccessGreen,
    onTertiary = Color.White,
    background = SpotrLightBackground,
    onBackground = SpotrText,
    surface = SpotrSurfaceLight,
    onSurface = SpotrText,
    surfaceVariant = Color(0xFFE8F0F5),
    onSurfaceVariant = Color(0xFF526477),
    error = ErrorRed,
    onError = Color.White,
    outline = SpotrOutline,
    outlineVariant = Color(0xFFE1EAF0)
)

@Composable
fun PlayerIDBrandTheme(
    // PlayerID always uses its light look, regardless of the phone's dark mode setting.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PlayerIDDarkColorScheme else PlayerIDLightColorScheme,
        typography = Typography,
        content = content
    )
}
