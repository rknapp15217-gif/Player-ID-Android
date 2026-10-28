package com.playerid.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerIDBrandThemeTest {
    @Test
    fun lightSchemePreservesAndroidBrandAndSurfaceColors() {
        with(PlayerIDLightColorScheme) {
            assertEquals(Color(0xFF173B57), primary)
            assertEquals(Color.White, onPrimary)
            assertEquals(Color(0xFFFF6B5B), secondary)
            assertEquals(Color(0xFF35D0BA), tertiary)
            assertEquals(Color(0xFFF7F9FC), background)
            assertEquals(Color(0xFF102A43), onBackground)
            assertEquals(Color.White, surface)
            assertEquals(Color(0xFF102A43), onSurface)
            assertEquals(Color(0xFFE8F0F5), surfaceVariant)
            assertEquals(Color(0xFF526477), onSurfaceVariant)
            assertEquals(Color(0xFFD9E2EC), outline)
            assertEquals(Color(0xFFE1EAF0), outlineVariant)
            assertEquals(Color(0xFFE5484D), error)
        }
    }

    @Test
    fun darkSchemePreservesAndroidBrandAndSurfaceColors() {
        with(PlayerIDDarkColorScheme) {
            assertEquals(Color(0xFF173B57), primary)
            assertEquals(Color(0xFFFF6B5B), secondary)
            assertEquals(Color(0xFF35D0BA), tertiary)
            assertEquals(Color(0xFF0B172A), background)
            assertEquals(Color.White, onBackground)
            assertEquals(Color(0xFF12243A), surface)
            assertEquals(Color.White, onSurface)
            assertEquals(Color(0xFF1B344D), surfaceVariant)
            assertEquals(Color(0xFFB8C7D9), onSurfaceVariant)
            assertEquals(Color(0xFF29445F), outline)
            assertEquals(Color(0xFF29445F), outlineVariant)
            assertEquals(Color(0xFFE5484D), error)
        }
    }

    @Test
    fun sharedTypographyPreservesAndroidTypeScale() {
        assertEquals(28.sp, Typography.displayLarge.fontSize)
        assertEquals(34.sp, Typography.displayLarge.lineHeight)
        assertEquals(FontWeight.Bold, Typography.headlineLarge.fontWeight)
        assertEquals(20.sp, Typography.headlineMedium.fontSize)
        assertEquals(26.sp, Typography.headlineMedium.lineHeight)
        assertEquals(FontWeight.SemiBold, Typography.titleLarge.fontWeight)
        assertEquals(18.sp, Typography.titleMedium.fontSize)
        assertEquals(24.sp, Typography.titleMedium.lineHeight)
        assertEquals(16.sp, Typography.bodyLarge.fontSize)
        assertEquals(24.sp, Typography.bodyLarge.lineHeight)
        assertEquals(16.sp, Typography.bodyMedium.fontSize)
        assertEquals(22.sp, Typography.bodyMedium.lineHeight)
        assertEquals(16.sp, Typography.labelLarge.fontSize)
        assertEquals(14.sp, Typography.labelSmall.fontSize)
        assertEquals(FontFamily.Default, Typography.bodyLarge.fontFamily)
    }
}
