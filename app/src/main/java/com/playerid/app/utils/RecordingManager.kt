package com.playerid.app.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale

enum class RecordingState {
    IDLE, RECORDING, FINALIZING
}

class RecordingManager(private val context: Context) {

    private var activeRecording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    fun setVideoCapture(videoCapture: VideoCapture<Recorder>) {
        this.videoCapture = videoCapture
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onFinished: (Uri?) -> Unit) {
        if (_recordingState.value != RecordingState.IDLE) return

        val capture = videoCapture ?: return

        _recordingState.value = RecordingState.RECORDING
        val mediaStoreOutputOptions = createMediaStoreOutputOptions()

        activeRecording = capture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                handleRecordingEvent(recordEvent, onFinished)
            }
    }

    fun stopRecording() {
        if (_recordingState.value != RecordingState.RECORDING) return
        _recordingState.value = RecordingState.FINALIZING
        activeRecording?.stop()
    }

    fun stopAndDiscardRecording() {
        try {
            activeRecording?.close()
        } catch (e: Exception) {
            // Ignore
        }
        activeRecording = null
        _recordingState.value = RecordingState.IDLE
    }

    private fun handleRecordingEvent(recordEvent: VideoRecordEvent, onFinished: (Uri?) -> Unit) {
        if (recordEvent is VideoRecordEvent.Finalize) {
            val uri = if (recordEvent.hasError()) {
                Log.e("RecordingManager", "Recording failed: ${recordEvent.error}")
                null
            } else {
                recordEvent.outputResults.outputUri
            }
            _recordingState.value = RecordingState.IDLE
            activeRecording = null
            onFinished(uri)
        }
    }

    private fun createMediaStoreOutputOptions(): MediaStoreOutputOptions {
        val name = "PlayerID-video-" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PlayerID")
        }

        return MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }
}
