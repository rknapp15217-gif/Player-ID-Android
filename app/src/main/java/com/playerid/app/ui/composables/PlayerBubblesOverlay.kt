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
        trackedPlayers.filter { it.first.disappearedFrames == 0 }.forEach { (trackedPlayer, player) ->
            val playerName = player?.name ?: "Unknown"
            val jerseyNumber = trackedPlayer.jerseyNumber

            // "FREEZE" LOGIC: We use initialBox dimensions instead of currentBox
            val frozenWidth = trackedPlayer.initialBox.width().toInt()
            val frozenHeight = trackedPlayer.initialBox.height().toInt()

            val centerPosition = Offset(
                trackedPlayer.currentBox.centerX(),
                trackedPlayer.currentBox.centerY()
            )

            // Always draw blue box around detected number
            drawPlayerOverlay(
                playerName = playerName,
                jerseyNumber = jerseyNumber,
                position = centerPosition,
                isSelected = false,
                debugWidth = frozenWidth,
                debugHeight = frozenHeight,
                pulseFraction = 1f // Always show box for detected
            )
        }
    }
}