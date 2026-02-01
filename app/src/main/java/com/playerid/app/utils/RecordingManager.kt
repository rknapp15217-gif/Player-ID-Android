package com.playerid.app.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.video.*
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
    private var ignoreNextResult = false

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    fun setVideoCapture(videoCapture: VideoCapture<Recorder>) {
        this.videoCapture = videoCapture
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onFinished: (Uri?) -> Unit) {
        if (_recordingState.value != RecordingState.IDLE) return

        val capture = videoCapture ?: return
        ignoreNextResult = false

        _recordingState.value = RecordingState.RECORDING
        val mediaStoreOutputOptions = createMediaStoreOutputOptions()

        try {
            activeRecording = capture.output
                .prepareRecording(context, mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    handleRecordingEvent(recordEvent, onFinished)
                }
            Log.d("RecordingManager", "Recording started")
        } catch (e: Exception) {
            Log.e("RecordingManager", "Failed to start recording", e)
            _recordingState.value = RecordingState.IDLE
        }
    }

    fun stopRecording() {
        if (_recordingState.value != RecordingState.RECORDING) return
        ignoreNextResult = false
        _recordingState.value = RecordingState.FINALIZING
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            _recordingState.value = RecordingState.IDLE
        }
    }

    fun stopAndDiscardRecording() {
        if (_recordingState.value != RecordingState.RECORDING) return
        // SET FLAG: Do not trigger navigation when this recording finishes
        ignoreNextResult = true
        _recordingState.value = RecordingState.FINALIZING
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            _recordingState.value = RecordingState.IDLE
        }
    }

    private fun handleRecordingEvent(recordEvent: VideoRecordEvent, onFinished: (Uri?) -> Unit) {
        if (recordEvent is VideoRecordEvent.Finalize) {
            val uri = if (recordEvent.hasError()) {
                null
            } else {
                recordEvent.outputResults.outputUri
            }
            
            _recordingState.value = RecordingState.IDLE
            activeRecording = null
            
            // Only call the finish callback if we AREN'T ignoring this result
            if (!ignoreNextResult) {
                onFinished(uri)
            } else {
                Log.d("RecordingManager", "Recording discarded silently to free mic.")
                ignoreNextResult = false
            }
        }
    }

    private fun createMediaStoreOutputOptions(): MediaStoreOutputOptions {
        val name = "Spotr-Clip-" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
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