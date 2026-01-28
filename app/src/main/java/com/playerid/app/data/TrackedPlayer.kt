package com.playerid.app.data

import android.graphics.RectF

/**
 * Represents a player being tracked across multiple video frames.
 *
 * @param id A unique, stable identifier for the tracked player.
 * @param initialBox The bounding box where the player was first detected.
 * @param currentBox The most recent bounding box for the player.
 * @param jerseyNumber The last known jersey number for this player.
 * @param disappearedFrames A counter for how many consecutive frames this player has not been detected.
 */
data class TrackedPlayer(
    val id: String,
    val initialBox: RectF,
    val currentBox: RectF,
    val jerseyNumber: String,
    val disappearedFrames: Int = 0
)
