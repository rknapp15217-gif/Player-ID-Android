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
    private var lastRecordingStartTimeMs: Long = 0L

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    fun setVideoCapture(videoCapture: VideoCapture<Recorder>) {
        this.videoCapture = videoCapture
    }

    fun getLastRecordingStartTimeMs(): Long {
        return lastRecordingStartTimeMs
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onFinished: (Uri?) -> Unit) {
        if (_recordingState.value != RecordingState.IDLE) return

        val capture = videoCapture ?: return
        ignoreNextResult = false
        lastRecordingStartTimeMs = System.currentTimeMillis()

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

            if (recordEvent.hasError()) {
                Log.e("RecordingManager", "Recording failed: ${recordEvent.error}", recordEvent.cause)
            } else {
                logOutputDetails(uri)
            }
            
            _recordingState.value = RecordingState.IDLE
            activeRecording = null
            
            // Only call the finish callback if we AREN'T ignoring this result
            if (!ignoreNextResult) {
                onFinished(uri)
            } else {
                deleteOutputUri(uri)
                Log.d("RecordingManager", "Recording discarded and deleted to free mic.")
                ignoreNextResult = false
            }
        }
    }

    private fun deleteOutputUri(uri: Uri?) {
        if (uri == null) return
        try {
            when (uri.scheme) {
                "content" -> context.contentResolver.delete(uri, null, null)
                "file" -> runCatching { java.io.File(uri.path ?: "").delete() }
            }
        } catch (e: Exception) {
            Log.w("RecordingManager", "Failed to delete discarded recording", e)
        }
    }

    private fun logOutputDetails(uri: Uri?) {
        if (uri == null) {
            Log.w("RecordingManager", "Recording finalized with null output URI")
            return
        }

        Log.d("RecordingManager", "Recording finalized. outputUri=$uri")
        if (uri.scheme != "content") {
            return
        }

        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    Log.d("RecordingManager", "Output URI query returned no rows")
                    return
                }

                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                val relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                val dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN))
                val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))

                Log.d(
                    "RecordingManager",
                    "Output details name=$name relativePath=$relativePath size=$size durationMs=$duration dateTaken=$dateTaken dateAdded=$dateAdded"
                )
            }
        } catch (e: Exception) {
            Log.w("RecordingManager", "Failed to query output URI details", e)
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