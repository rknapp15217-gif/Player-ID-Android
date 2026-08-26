package com.playerid.app.platform

enum class CameraFacing {
    FRONT,
    BACK
}

enum class RecordingStatus {
    IDLE,
    RECORDING,
    FINALIZING
}

data class CameraConfiguration(
    val facing: CameraFacing = CameraFacing.BACK,
    val includeAudio: Boolean = true
)

interface CameraService {
    suspend fun start(configuration: CameraConfiguration)
    suspend fun stop()
    suspend fun capturePhoto(): MediaReference
    suspend fun startRecording()
    suspend fun stopRecording(): MediaReference
}

data class VideoCompositionRequest(
    val clips: List<MediaReference>,
    val title: String? = null
)

interface VideoService {
    suspend fun compose(request: VideoCompositionRequest): MediaReference
    suspend fun durationMs(media: MediaReference): Long
}

interface SpeechRecognitionService {
    suspend fun recognizeOnce(): String?
    fun cancel()
}

interface PreferencesStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

interface PlatformClock {
    fun nowEpochMilliseconds(): Long
}
