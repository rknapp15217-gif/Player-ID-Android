package com.playerid.app.data

import android.graphics.RectF

/**
 * Data class representing a detected player from jersey detection.
 * This is the object that flows from the ML detector to the UI.
 */
data class DetectedPlayer(
    val number: String, // Use String to preserve leading zeros like "00"
    val boundingBox: RectF,
    val confidence: Float,
    val player: Player? // Null if no player with this number is on the active team
)
