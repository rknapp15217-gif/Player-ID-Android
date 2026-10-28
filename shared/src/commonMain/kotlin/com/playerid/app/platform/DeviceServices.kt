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
    suspend fun listSavedCaptures(): List<MediaReference> = emptyList()
}

interface RollingCameraService {
    suspend fun startRollingBuffer()
    suspend fun rotateRollingBuffer()
    suspend fun saveRollingBuffer(): MediaReference
    suspend fun discardRollingBuffer()
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
    suspend fun recognizeOnce(onPartialResult: (String) -> Unit): String? = recognizeOnce()
    fun cancel()
}

interface SpeechSynthesisService {
    fun speak(text: String)
    fun stop()
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
