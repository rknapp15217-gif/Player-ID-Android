package com.playerid.app.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.BubbleMetadata
import com.playerid.app.data.Overlay
import com.playerid.app.data.VideoClip
import com.playerid.app.ui.screens.NameBubble
import com.playerid.app.video.VideoProcessingManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val videoProcessingManager = VideoProcessingManager(application.applicationContext)

    fun getBubblesForVideo(videoId: String): Flow<List<NameBubble>> {
        // This is a placeholder. In a real app, you would fetch this from a repository.
        return MutableStateFlow(emptyList<NameBubble>()).asStateFlow()
    }

    fun exportVideo(
        videoClip: VideoClip,
        selectedBubbles: List<String>,
        selectedOverlays: List<Overlay>,
        includeWatermark: Boolean,
        onProgress: (Float) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val success = videoProcessingManager.exportVideoWithBubbles(
                originalVideoUri = Uri.parse(videoClip.filePath),
                nameBubbles = emptyList(), // Placeholder
                outputPath = "", // Placeholder
                onProgress = onProgress
            )
            if (success) {
                onComplete("")
            } else {
                onError("Export failed")
            }
        }
    }
}