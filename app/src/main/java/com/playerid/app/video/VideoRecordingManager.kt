package com.playerid.app.video

import android.content.Context
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Environment
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.playerid.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class VideoRecordingManager(private val context: Context) {
    
    private var activeRecording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentMetadata = mutableListOf<BubbleMetadata>()
    private var recordingStartTime: Long = 0
    
    fun initializeVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        
        videoCapture = VideoCapture.withOutput(recorder)
        return videoCapture!!
    }
    
    fun startRecording(
        onVideoSaved: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val videoCapture = this.videoCapture ?: return
        
        // Create output file
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val videoFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "spotr_video_$timestamp.mp4"
        )
        
        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()
        
        recordingStartTime = System.currentTimeMillis()
        currentMetadata.clear()
        
        try {
            activeRecording = videoCapture.output
                .prepareRecording(context, fileOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            // Recording started
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (recordEvent.hasError()) {
                                onError("Recording failed: ${recordEvent.cause?.message}")
                            } else {
                                onVideoSaved(Uri.fromFile(videoFile))
                            }
                            activeRecording = null
                        }
                    }
                }
        } catch (e: SecurityException) {
            onError("Missing required permission: ${e.message}")
        }
    }
    
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }
    
    fun addBubbleMetadata(
        playerId: String,
        playerName: String,
        playerNumber: Int,
        team: String,
        position: BubblePosition,
        isVisible: Boolean = true
    ) {
        if (activeRecording != null) {
            val currentTime = System.currentTimeMillis() - recordingStartTime
            
            // Check if there's an existing metadata for this player
            val existingIndex = currentMetadata.indexOfFirst { 
                it.playerId == playerId && it.endTime == -1L 
            }
            
            if (existingIndex != -1) {
                // Update existing metadata
                if (!isVisible) {
                    // Player is no longer visible, set end time
                    currentMetadata[existingIndex] = currentMetadata[existingIndex].copy(
                        endTime = currentTime
                    )
                } else {
                    // Update position
                    currentMetadata[existingIndex] = currentMetadata[existingIndex].copy(
                        position = position
                    )
                }
            } else if (isVisible) {
                // Add new metadata for newly visible player
                currentMetadata.add(
                    BubbleMetadata(
                        playerId = playerId,
                        playerName = playerName,
                        playerNumber = playerNumber,
                        team = team,
                        startTime = currentTime,
                        endTime = -1L, // Will be set when player becomes invisible
                        position = position,
                        isVisible = true
                    )
                )
            }
        }
    }
    
    suspend fun exportVideoWithOverlays(
        originalVideoPath: String,
        selectedBubbles: List<String>, // Player IDs to include
        selectedOverlays: List<Overlay>,
        includeWatermark: Boolean = true,
        onProgress: (Int) -> Unit = {},
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val outputFile = createExportFile()
            
            // This is a simplified implementation
            // In production, you'd use FFmpeg or MediaMetadataRetriever
            val success = processVideoWithOverlays(
                inputPath = originalVideoPath,
                outputPath = outputFile.absolutePath,
                bubbleMetadata = currentMetadata.filter { selectedBubbles.contains(it.playerId) },
                overlays = selectedOverlays.filter { it.isEnabled },
                includeWatermark = includeWatermark,
                onProgress = onProgress
            )
            
            if (success) {
                withContext(Dispatchers.Main) {
                    onComplete(outputFile.absolutePath)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("Failed to process video")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Export failed: ${e.message}")
            }
        }
    }
    
    private fun createExportFile(): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "spotr_highlight_$timestamp.mp4"
        )
    }
    
    private suspend fun processVideoWithOverlays(
        inputPath: String,
        outputPath: String,
        bubbleMetadata: List<BubbleMetadata>,
        overlays: List<Overlay>,
        includeWatermark: Boolean,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        // This is a simplified implementation
        // In production, you'd use FFmpeg for video processing
        
        try {
            // For MVP, we'll copy the original file and save metadata separately
            // Full video processing with overlays would require FFmpeg integration
            
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)
            
            inputFile.copyTo(outputFile, overwrite = true)
            
            // Save overlay metadata for future processing
            saveOverlayMetadata(outputPath, bubbleMetadata, overlays, includeWatermark)
            
            onProgress(100)
            return@withContext true
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    private fun saveOverlayMetadata(
        videoPath: String,
        bubbles: List<BubbleMetadata>,
        overlays: List<Overlay>,
        includeWatermark: Boolean
    ) {
        // Save metadata as JSON for later processing
        val metadataFile = File("${videoPath}.metadata.json")
        // Implementation would serialize data to JSON
    }
    
    fun createThumbnail(videoPath: String): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val bitmap = retriever.getFrameAtTime(1000000) // 1 second
            retriever.release()
            
            // Save thumbnail
            val thumbnailFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "thumb_${File(videoPath).nameWithoutExtension}.jpg"
            )
            
            val outputStream = FileOutputStream(thumbnailFile)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()
            
            thumbnailFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}