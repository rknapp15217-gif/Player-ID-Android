package com.playerid.app.domain.team

import kotlin.math.floor
import kotlin.math.roundToInt

private const val DefaultTeamHue = 210f
private const val TeamColorSaturation = 0.9f
private const val TeamColorValue = 0.95f

fun teamHueToHex(hue: Float): String {
    val normalizedHue = ((hue % 360f) + 360f) % 360f
    val sectorPosition = normalizedHue / 60f
    val sector = floor(sectorPosition).toInt()
    val fraction = sectorPosition - sector
    val low = TeamColorValue * (1f - TeamColorSaturation)
    val descending = TeamColorValue * (1f - TeamColorSaturation * fraction)
    val ascending = TeamColorValue * (1f - TeamColorSaturation * (1f - fraction))
    val (red, green, blue) = when (sector) {
        0 -> Triple(TeamColorValue, ascending, low)
        1 -> Triple(descending, TeamColorValue, low)
        2 -> Triple(low, TeamColorValue, ascending)
        3 -> Triple(low, descending, TeamColorValue)
        4 -> Triple(ascending, low, TeamColorValue)
        else -> Triple(TeamColorValue, low, descending)
    }
    return "#${red.toHexComponent()}${green.toHexComponent()}${blue.toHexComponent()}"
}

fun teamHexToHue(hex: String, fallbackHue: Float = DefaultTeamHue): Float {
    val rgb = hex.removePrefix("#")
    if (rgb.length != 6 || rgb.any { it.digitToIntOrNull(16) == null }) return fallbackHue

    val red = rgb.substring(0, 2).toInt(16) / 255f
    val green = rgb.substring(2, 4).toInt(16) / 255f
    val blue = rgb.substring(4, 6).toInt(16) / 255f
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta == 0f) return 0f

    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return if (hue < 0f) hue + 360f else hue
}

private fun Float.toHexComponent(): String {
    val component = (this * 255f).roundToInt().coerceIn(0, 255)
    return component.toString(16).uppercase().padStart(2, '0')
}