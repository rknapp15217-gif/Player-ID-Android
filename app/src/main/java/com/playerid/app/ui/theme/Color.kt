package com.playerid.app.ui.theme

import androidx.compose.ui.graphics.Color

// === PLAYERID SPORTY PALETTE ===
val SpotrPrimaryBlue = Color(0xFF173B57)
val SpotrSuccessGreen = Color(0xFF35D0BA)
val SpotrHighlightOrange = Color(0xFFFF6B5B)
val SpotrDarkSurface = Color(0xFF0B172A)
val SpotrLightBackground = Color(0xFFF7F9FC)
val SpotrText = Color(0xFF102A43)

// Supporting colors
val SpotrSurfaceLight = Color(0xFFFFFFFF)
val SpotrSurfaceDark = Color(0xFF12243A)
val SpotrOutline = Color(0xFFD9E2EC)
val SpotrOutlineDark = Color(0xFF29445F)
const val SpotrSurfaceAlpha = 0.12f

// Action colors
val SuccessGreen = SpotrSuccessGreen
val WarningOrange = SpotrHighlightOrange
val ErrorRed = Color(0xFFE5484D)
val InfoBlue = SpotrPrimaryBlue

// AR & Camera Colors
val AROverlay = SpotrSuccessGreen.copy(alpha = 0.9f)
val CameraButton = SpotrPrimaryBlue
val RecordingRed = Color(0xFFEF4444)

// Team Colors - Dynamic set
val TeamColors = listOf(
    Color(0xFFE5484D), // Red
    Color(0xFF4C9AFF), // Blue
    Color(0xFF35D0BA), // Mint
    Color(0xFFFFD166), // Yellow
    Color(0xFF8B7CFF), // Purple
    Color(0xFFFF6B5B), // Coral
    Color(0xFF22B8A7), // Teal
    Color(0xFFFF7AA2)  // Pink
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