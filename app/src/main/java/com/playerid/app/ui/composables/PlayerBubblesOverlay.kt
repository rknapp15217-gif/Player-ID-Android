package com.playerid.app.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.playerid.app.data.Player
import com.playerid.app.data.TrackedPlayer

/**
 * A composable overlay that draws player name bubbles on the screen.
 * Displays detection size frozen at the moment of first identification.
 */
@Composable
fun PlayerBubblesOverlay(
    trackedPlayers: List<Pair<TrackedPlayer, Player?>>,
    processing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        trackedPlayers.filter { it.first.disappearedFrames == 0 }.forEach { (trackedPlayer, player) ->
            val playerName = player?.name ?: "Unknown"
            val jerseyNumber = trackedPlayer.jerseyNumber

            // Detection size frozen at first identification
            val frozenWidth = trackedPlayer.initialBox.width().toInt()
            val frozenHeight = trackedPlayer.initialBox.height().toInt()

            // SCALE COORDINATES: currentBox is normalized [0,1], scale to canvas pixels
            // CRITICAL FIX: The preview resolution is likely 1280x720, but the Canvas is screen size.
            // We scale the normalized box [0,1] to the Canvas dimensions.
            val centerPosition = Offset(
                trackedPlayer.currentBox.centerX() * canvasWidth,
                trackedPlayer.currentBox.centerY() * canvasHeight
            )

            // Draw player bubble and box
            drawPlayerOverlay(
                playerName = playerName,
                jerseyNumber = jerseyNumber,
                position = centerPosition,
                isSelected = false,
                debugWidth = frozenWidth,
                debugHeight = frozenHeight,
                pulseFraction = 1f
            )
        }
    }
}