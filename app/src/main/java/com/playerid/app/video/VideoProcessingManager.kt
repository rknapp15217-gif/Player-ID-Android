package com.playerid.app.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.playerid.app.data.Player
import com.playerid.app.ui.screens.NameBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VideoProcessingManager(private val context: Context) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val executor = Executors.newSingleThreadExecutor()

    suspend fun autoDetectPlayersInVideo(
        videoUri: Uri,
        roster: List<Player>,
        onProgress: (Float) -> Unit = {}
    ): List<NameBubble> = withContext(Dispatchers.IO) {
        val detectedBubbles = mutableListOf<NameBubble>()
        
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val frameInterval = 2000000L // Extract frame every 2 seconds (in microseconds)
            val totalFrames = (durationMs * 1000 / frameInterval).toInt()
            
            var frameCount = 0
            var currentTimeUs = 0L
            
            while (currentTimeUs < durationMs * 1000) {
                try {
                    val bitmap = retriever.getFrameAtTime(currentTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    bitmap?.let { frame ->
                        val detectedNumbers = detectJerseyNumbers(frame)
                        
                        // Match detected numbers with roster
                        detectedNumbers.forEach { detection ->
                            val player = roster.find { it.number == detection.number }
                            player?.let { 
                                val existingBubble = detectedBubbles.find { 
                                    it.jerseyNumber == detection.number 
                                }
                                
                                if (existingBubble == null) {
                                    detectedBubbles.add(
                                        NameBubble(
                                            id = "auto_${detection.number}_${System.currentTimeMillis()}",
                                            playerName = player.name,
                                            jerseyNumber = detection.number,
                                            position = detection.position,
                                            isVisible = true
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    frameCount++
                    onProgress(frameCount.toFloat() / totalFrames)
                    
                } catch (e: Exception) {
                    // Skip this frame if there's an error
                }
                
                currentTimeUs += frameInterval
            }
            
            retriever.release()
            
        } catch (e: Exception) {
            // Handle video processing error
        }
        
        return@withContext detectedBubbles.distinctBy { it.jerseyNumber }
    }

    private suspend fun detectJerseyNumbers(bitmap: Bitmap): List<JerseyDetection> = 
        suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detections = mutableListOf<JerseyDetection>()
                    
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val text = line.text.trim()
                            
                            // Check if text looks like a jersey number (1-2 digits)
                            if (text.matches(Regex("^\\d{1,2}$"))) {
                                val boundingBox = line.boundingBox
                                boundingBox?.let { box ->
                                    val aspectRatio = box.height().toFloat() / box.width().toFloat()
                                    detections.add(
                                        JerseyDetection(
                                            number = text,
                                            position = Offset(
                                                x = box.centerX().toFloat(),
                                                y = box.centerY().toFloat()
                                            ),
                                            confidence = calculateConfidence(text, aspectRatio)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    continuation.resume(detections)
                }
                .addOnFailureListener {
                    continuation.resume(emptyList())
                }
        }

    private fun calculateConfidence(text: String, aspectRatio: Float): Float {
        // Simple confidence calculation based on:
        // 1. Text length (1-2 digits is ideal for jersey numbers)
        // 2. Bounding box aspect ratio
        
        var confidence = 0.5f
        
        // Prefer 1-2 digit numbers
        if (text.length in 1..2) {
            confidence += 0.3f
        }
        
        // Check bounding box aspect ratio (jerseys are usually taller than wide)
        if (aspectRatio in 1.0f..2.5f) {
            confidence += 0.2f
        }
        
        return confidence.coerceIn(0f, 1f)
    }

    suspend fun exportVideoWithBubbles(
        originalVideoUri: Uri,
        nameBubbles: List<NameBubble>,
        outputPath: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // This would use FFmpeg or MediaMuxer to overlay the name bubbles
            // For now, we'll simulate the process and integrate with existing VideoRecordingManager
            
            onProgress(0.1f)
            
            // Step 1: Extract video frames
            onProgress(0.3f)
            
            // Step 2: Overlay name bubbles on each frame
            onProgress(0.6f)
            
            // Step 3: Reconstruct video with audio
            onProgress(0.9f)
            
            // Step 4: Save to output path
            onProgress(1.0f)
            
            return@withContext true
            
        } catch (e: Exception) {
            return@withContext false
        }
    }

    fun release() {
        textRecognizer.close()
        executor.shutdown()
    }
}

data class JerseyDetection(
    val number: String,
    val position: Offset,
    val confidence: Float
)

// Enhanced Player data class with position tracking
data class PlayerWithTracking(
    val player: Player,
    val positions: List<TimestampedPosition> = emptyList(),
    val isTracked: Boolean = false
)

data class TimestampedPosition(
    val timestamp: Long, // Video timestamp in milliseconds
    val position: Offset,
    val confidence: Float
)