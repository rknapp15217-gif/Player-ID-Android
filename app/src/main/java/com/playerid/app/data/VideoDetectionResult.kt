package com.playerid.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persisted detection results for a video clip.
 * Stores player detection, tracking, and bubble positions tied to a video URI.
 */
@Entity(tableName = "video_detection_results")
data class VideoDetectionResultEntity(
    @PrimaryKey
    val videoUri: String,  // content://... URI or file path
    val detectionMode: String,  // "FAST" or "ACCURATE"
    val detectionJson: String,  // Full VideoPlayerDetectionResult serialized
    val detectionTimestampMs: Long,  // When analysis ran
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Helper to convert between VideoPlayerDetectionResult and JSON strings.
 */
object DetectionResultSerializer {

    fun serialize(result: com.playerid.app.video.VideoPlayerDetectionResult): String {
        val root = JSONObject()
        
        // Serialize bubbles
        val bubblesArray = JSONArray()
        result.bubbles.forEach { bubble ->
            val bubbleObj = JSONObject()
            bubbleObj.put("id", bubble.id)
            bubbleObj.put("playerName", bubble.playerName)
            bubbleObj.put("jerseyNumber", bubble.jerseyNumber)
            bubbleObj.put("positionX", bubble.position.x)
            bubbleObj.put("positionY", bubble.position.y)
            bubbleObj.put("isSelected", bubble.isSelected)
            bubblesArray.put(bubbleObj)
        }
        root.put("bubbles", bubblesArray)
        
        // Serialize tracks
        val tracksArray = JSONArray()
        result.tracks.forEach { track ->
            val trackObj = JSONObject()
            trackObj.put("playerId", track.playerId)
            val samplesArray = JSONArray()
            track.samples.forEach { sample ->
                val sampleObj = JSONObject()
                sampleObj.put("timestampMs", sample.timestampMs)
                sampleObj.put("positionX", sample.position.x)
                sampleObj.put("positionY", sample.position.y)
                sampleObj.put("confidence", sample.confidence)
                samplesArray.put(sampleObj)
            }
            trackObj.put("samples", samplesArray)
            tracksArray.put(trackObj)
        }
        root.put("tracks", tracksArray)
        
        // Serialize frame dimensions
        root.put("frameWidth", result.frameWidth)
        root.put("frameHeight", result.frameHeight)
        
        return root.toString()
    }

    fun deserialize(jsonString: String): com.playerid.app.video.VideoPlayerDetectionResult? {
        return try {
            val root = JSONObject(jsonString)
            
            // Deserialize bubbles
            val bubblesArray = root.getJSONArray("bubbles")
            val bubbles = mutableListOf<com.playerid.app.ui.screens.NameBubble>()
            for (i in 0 until bubblesArray.length()) {
                val bubbleObj = bubblesArray.getJSONObject(i)
                bubbles.add(
                    com.playerid.app.ui.screens.NameBubble(
                        id = bubbleObj.getString("id"),
                        playerName = bubbleObj.getString("playerName"),
                        jerseyNumber = bubbleObj.getString("jerseyNumber"),
                        position = Offset(
                            x = bubbleObj.getDouble("positionX").toFloat(),
                            y = bubbleObj.getDouble("positionY").toFloat()
                        ),
                        isSelected = bubbleObj.getBoolean("isSelected")
                    )
                )
            }
            
            // Deserialize tracks
            val tracksArray = root.getJSONArray("tracks")
            val tracks = mutableListOf<com.playerid.app.video.PlayerDetectionTrack>()
            for (i in 0 until tracksArray.length()) {
                val trackObj = tracksArray.getJSONObject(i)
                val samplesArray = trackObj.getJSONArray("samples")
                val samples = mutableListOf<com.playerid.app.video.PlayerTrackSample>()
                for (j in 0 until samplesArray.length()) {
                    val sampleObj = samplesArray.getJSONObject(j)
                    samples.add(
                        com.playerid.app.video.PlayerTrackSample(
                            timestampMs = sampleObj.getLong("timestampMs"),
                            position = Offset(
                                x = sampleObj.getDouble("positionX").toFloat(),
                                y = sampleObj.getDouble("positionY").toFloat()
                            ),
                            confidence = sampleObj.getDouble("confidence").toFloat()
                        )
                    )
                }
                tracks.add(
                    com.playerid.app.video.PlayerDetectionTrack(
                        playerId = trackObj.getString("playerId"),
                        samples = samples
                    )
                )
            }
            
            com.playerid.app.video.VideoPlayerDetectionResult(
                bubbles = bubbles,
                tracks = tracks,
                frameWidth = root.getInt("frameWidth"),
                frameHeight = root.getInt("frameHeight")
            )
        } catch (e: Exception) {
            android.util.Log.e("DetectionResultSerializer", "Failed to deserialize: ${e.message}")
            null
        }
    }
}
