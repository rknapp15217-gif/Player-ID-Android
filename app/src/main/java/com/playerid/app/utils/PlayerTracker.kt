package com.playerid.app.utils

import android.graphics.RectF
import com.playerid.app.data.TrackedPlayer
import java.util.UUID

/**
 * A simplified tracker that maintains stable IDs and performs basic temporal voting.
 */
class PlayerTracker {

    private data class PlayerMetadata(
        val trackedPlayer: TrackedPlayer,
        val numberHistory: MutableList<String> = mutableListOf()
    )

    private var playerMap = mutableMapOf<String, PlayerMetadata>()
    private val maxDisappearedFrames = 20 // Grace period for moving players
    // REDUCED for instant feedback: Players will be identified on the very first frame
    private val minConfirmationFrames = 1 

    fun update(detections: List<Pair<RectF, String>>, frameWidth: Int, frameHeight: Int): List<TrackedPlayer> {
        val unmatchedDetections = detections.toMutableList()
        val nextPlayerMap = mutableMapOf<String, PlayerMetadata>()

        // 1. Update existing players
        playerMap.values.forEach { metadata ->
            var bestMatch: Pair<RectF, String>? = null
            var maxIoU = 0f
            
            val currentBox = metadata.trackedPlayer.currentBox

            for (detection in unmatchedDetections) {
                val iou = calculateIoU(currentBox, detection.first)
                if (iou > maxIoU) {
                    maxIoU = iou
                    bestMatch = detection
                }
            }

            if (maxIoU > 0.12f && bestMatch != null) { // Lower threshold for more tolerant matching
                val detectedNumber = bestMatch.second
                
                metadata.numberHistory.add(detectedNumber)
                if (metadata.numberHistory.size > 10) metadata.numberHistory.removeAt(0)
                
                // Temporal voting: Find the number seen most frequently
                val bestCandidate = metadata.numberHistory.groupingBy { it }.eachCount().maxByOrNull { it.value }
                val consensusNumber = if (bestCandidate != null && bestCandidate.value >= minConfirmationFrames) {
                    bestCandidate.key
                } else {
                    detectedNumber 
                }

                val updatedPlayer = metadata.trackedPlayer.copy(
                    currentBox = bestMatch.first,
                    jerseyNumber = consensusNumber,
                    disappearedFrames = 0
                )
                nextPlayerMap[metadata.trackedPlayer.id] = metadata.copy(trackedPlayer = updatedPlayer)
                unmatchedDetections.remove(bestMatch)
            } else {
                val updatedPlayer = metadata.trackedPlayer.copy(
                    disappearedFrames = metadata.trackedPlayer.disappearedFrames + 1
                )
                if (updatedPlayer.disappearedFrames < maxDisappearedFrames) {
                    nextPlayerMap[metadata.trackedPlayer.id] = metadata.copy(trackedPlayer = updatedPlayer)
                }
            }
        }

        // 2. Create new players
        unmatchedDetections.forEach { (normBox, number) ->
            val pixelBox = RectF(
                normBox.left * frameWidth,
                normBox.top * frameHeight,
                normBox.right * frameWidth,
                normBox.bottom * frameHeight
            )
            
            val newId = UUID.randomUUID().toString()
            val newTrackedPlayer = TrackedPlayer(
                id = newId,
                initialBox = pixelBox,
                currentBox = normBox,
                jerseyNumber = number // INSTANT FEEDBACK: Show the number immediately
            )
            
            val metadata = PlayerMetadata(
                trackedPlayer = newTrackedPlayer,
                numberHistory = mutableListOf(number)
            )
            nextPlayerMap[newId] = metadata
        }

        playerMap = nextPlayerMap
        return playerMap.values.map { it.trackedPlayer }
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val xA = maxOf(box1.left, box2.left)
        val yA = maxOf(box1.top, box2.top)
        val xB = minOf(box1.right, box2.right)
        val yB = minOf(box1.bottom, box2.bottom)
        val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        if (interArea <= 0) return 0f
        val unionArea = (box1.width() * box1.height()) + (box2.width() * box2.height()) - interArea
        return interArea / unionArea
    }
}