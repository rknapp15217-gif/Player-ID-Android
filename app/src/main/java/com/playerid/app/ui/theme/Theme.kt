package com.playerid.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SpotrGreen,
    onPrimary = Color.Black,
    secondary = SpotrTeal,
    onSecondary = Color.Black,
    tertiary = SpotrPurple,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = Color.White,
    surface = CardDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF2A2A2A)
)

private val LightColorScheme = lightColorScheme(
    primary = SpotrBlue,
    onPrimary = Color.White,
    secondary = SpotrTeal,
    onSecondary = Color.White,
    tertiary = SpotrOrange,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = Color(0xFF1A1A1A),
    surface = CardLight,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF464646),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF0F0F0)
)

@Composable
fun PlayerIDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}