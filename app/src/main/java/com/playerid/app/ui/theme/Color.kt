package com.playerid.app.ui.theme

import androidx.compose.ui.graphics.Color

// === SPOTR YOUTH SPORTS PALETTE ===
val SpotrPrimaryBlue = Color(0xFF2563EB)
val SpotrSuccessGreen = Color(0xFF22C55E)
val SpotrHighlightOrange = Color(0xFFF97316)
val SpotrDarkSurface = Color(0xFF111827)
val SpotrLightBackground = Color(0xFFF5F6FA)
val SpotrText = Color(0xFF1F2937)

// Supporting colors
val SpotrSurfaceLight = Color(0xFFFFFFFF)
val SpotrSurfaceDark = Color(0xFF0F172A)
val SpotrOutline = Color(0xFFE5E7EB)
val SpotrOutlineDark = Color(0xFF273047)

// Action colors
val SuccessGreen = SpotrSuccessGreen
val WarningOrange = SpotrHighlightOrange
val ErrorRed = Color(0xFFEF4444)
val InfoBlue = SpotrPrimaryBlue

// AR & Camera Colors
val AROverlay = SpotrSuccessGreen.copy(alpha = 0.9f)
val CameraButton = SpotrPrimaryBlue
val RecordingRed = Color(0xFFEF4444)

// Team Colors - Dynamic set
val TeamColors = listOf(
    Color(0xFFEF4444), // Red
    Color(0xFF3B82F6), // Blue
    Color(0xFF22C55E), // Green
    Color(0xFFF59E0B), // Yellow
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF97316), // Orange
    Color(0xFF14B8A6), // Teal
    Color(0xFFEC4899)  // Pink
)

// Academic year colors (kept for UI compatibility)
val FreshmanGreen = Color(0xFF22C55E)
val SophomoreBlue = Color(0xFF3B82F6)
val JuniorOrange = Color(0xFFF59E0B)
val SeniorRed = Color(0xFFEF4444)

// Legacy colors for compatibility
val SpotrGreen = SpotrSuccessGreen
val SpotrBlue = SpotrPrimaryBlue
val SpotrOrange = SpotrHighlightOrange
val SpotrDeepBlue = Color(0xFF1E40AF)
val SpotrTeal = Color(0xFF14B8A6)
val SpotrPurple = Color(0xFF8B5CF6)

val SurfaceLight = SpotrLightBackground
val SurfaceDark = SpotrDarkSurface
val CardLight = SpotrSurfaceLight
val CardDark = SpotrSurfaceDark

val PlayerIDBlue = SpotrPrimaryBlue
val PlayerIDDarkBlue = SpotrDeepBlue
val PlayerIDGreen = SpotrSuccessGreen
val PlayerIDOrange = SpotrHighlightOrange
val PlayerIDRed = ErrorRed

// Standard Material colors (unused but kept for compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)