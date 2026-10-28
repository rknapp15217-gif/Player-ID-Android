package com.playerid.app.domain.camera

import com.playerid.app.platform.CameraConfiguration
import com.playerid.app.platform.CameraService
import com.playerid.app.platform.MediaReference
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.RollingCameraService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CameraCaptureException(message: String) : Exception(message)

enum class CaptureMode { PHOTO, VIDEO, ROLLING }
enum class CapturePhase { STOPPED, STARTING, READY, CAPTURING, RECORDING, BUFFERING, SAVING, FAILED }

data class CameraCaptureState(
    val phase: CapturePhase = CapturePhase.STOPPED,
    val mode: CaptureMode = CaptureMode.PHOTO,
    val lastSaved: MediaReference? = null,
    val savedCount: Int = 0,
    val error: String? = null,
    val pendingReview: MediaReference? = null
) {
    val canCapture: Boolean get() = phase == CapturePhase.READY || phase == CapturePhase.RECORDING || phase == CapturePhase.BUFFERING
    val canChangeMode: Boolean get() = phase == CapturePhase.READY || phase == CapturePhase.BUFFERING
}

class CameraCaptureController(
    private val service: CameraService,
    initialMode: CaptureMode = CaptureMode.PHOTO
) {
    private var saveDetails: suspend (MediaReference) -> Unit = {}
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(CameraCaptureState(mode = initialMode))
    val state = mutableState.asStateFlow()

    fun forgetDeletedCapture(media: MediaReference) {
        val current = mutableState.value
        if (current.lastSaved?.identifier == media.identifier) {
            mutableState.value = current.copy(lastSaved = null, savedCount = (current.savedCount - 1).coerceAtLeast(0))
        }
    }

    fun dismissReview(media: MediaReference, deleted: Boolean = false) {
        val current = mutableState.value
        if (current.pendingReview?.identifier != media.identifier) return
        mutableState.value = current.copy(
            pendingReview = null,
            lastSaved = if (deleted && current.lastSaved?.identifier == media.identifier) null else current.lastSaved,
            savedCount = if (deleted) (current.savedCount - 1).coerceAtLeast(0) else current.savedCount
        )
    }

    fun selectMode(mode: CaptureMode) {
        if (mutableState.value.phase == CapturePhase.READY && mode != CaptureMode.ROLLING) {
            mutableState.value = mutableState.value.copy(mode = mode)
        }
    }

    suspend fun changeMode(mode: CaptureMode) = mutex.withLock {
        if (!mutableState.value.canChangeMode || mutableState.value.mode == mode) return@withLock
        withContext(NonCancellable) {
            try {
                val previous = mutableState.value.mode
                mutableState.value = mutableState.value.copy(phase = CapturePhase.STARTING, error = null)
                if (previous == CaptureMode.ROLLING) rollingService().discardRollingBuffer()
                mutableState.value = mutableState.value.copy(mode = mode)
                ready()
            } catch (error: CameraCaptureException) {
                mutableState.value = mutableState.value.copy(phase = CapturePhase.FAILED, error = error.message)
            }
        }
    }

    private fun rollingService(): RollingCameraService = service as? RollingCameraService
        ?: throw CameraCaptureException("Rolling recording is not supported by this camera.")

    private suspend fun ready() {
        if (mutableState.value.mode == CaptureMode.ROLLING && mutableState.value.pendingReview == null) {
            rollingService().startRollingBuffer()
            mutableState.value = mutableState.value.copy(phase = CapturePhase.BUFFERING)
        } else mutableState.value = mutableState.value.copy(phase = CapturePhase.READY)
    }

    suspend fun rotateRollingBuffer() = mutex.withLock {
        if (mutableState.value.phase != CapturePhase.BUFFERING) return@withLock
        withContext(NonCancellable) {
            try {
                rollingService().rotateRollingBuffer()
            } catch (error: CameraCaptureException) {
                mutableState.value = mutableState.value.copy(phase = CapturePhase.FAILED, error = error.message)
            }
        }
    }

    suspend fun resumeRollingBuffer() = mutex.withLock {
        if (mutableState.value.mode != CaptureMode.ROLLING || mutableState.value.phase != CapturePhase.READY ||
            mutableState.value.pendingReview != null) return@withLock
        withContext(NonCancellable) {
            try {
                ready()
            } catch (error: CameraCaptureException) {
                mutableState.value = mutableState.value.copy(phase = CapturePhase.FAILED, error = error.message)
            }
        }
    }

    suspend fun open() = mutex.withLock {
        if (mutableState.value.canCapture) return@withLock
        mutableState.value = mutableState.value.copy(phase = CapturePhase.STARTING, error = null)
        try {
            service.stop()
            service.start(CameraConfiguration())
            ready()
        } catch (error: CameraCaptureException) {
            mutableState.value = mutableState.value.copy(phase = CapturePhase.FAILED, error = error.message)
        }
    }

    suspend fun capture(onSaved: suspend (MediaReference) -> Unit = {}) {
        // A shutter tap during segment rotation must save after rotation, not disappear.
        if (mutableState.value.phase == CapturePhase.BUFFERING) mutex.lock()
        else if (!mutex.tryLock()) return
        try {
            val current = mutableState.value
            if (!current.canCapture || current.pendingReview != null) return
            // Finish the durable save even when the screen leaves composition.
            withContext(NonCancellable) {
                try {
                    when {
                        current.phase == CapturePhase.BUFFERING -> {
                            saveDetails = onSaved
                            mutableState.value = current.copy(phase = CapturePhase.SAVING, error = null)
                            saved(rollingService().saveRollingBuffer())
                        }
                        current.mode == CaptureMode.ROLLING -> throw CameraCaptureException("Rolling is not buffering yet.")
                        current.phase == CapturePhase.RECORDING -> finishRecording()
                        current.mode == CaptureMode.PHOTO -> {
                            saveDetails = onSaved
                            mutableState.value = current.copy(phase = CapturePhase.CAPTURING, error = null)
                            val media = service.capturePhoto()
                            saved(media)
                        }
                        else -> {
                            mutableState.value = current.copy(phase = CapturePhase.STARTING, error = null)
                            service.startRecording()
                            saveDetails = onSaved
                            mutableState.value = mutableState.value.copy(phase = CapturePhase.RECORDING)
                        }
                    }
                } catch (error: CameraCaptureException) {
                    mutableState.value = mutableState.value.copy(phase = CapturePhase.FAILED, error = error.message)
                }
            }
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun finishRecording() {
        mutableState.value = mutableState.value.copy(phase = CapturePhase.SAVING)
        val media = service.stopRecording()
        saved(media)
    }

    private suspend fun saved(media: MediaReference) {
        mutableState.value = mutableState.value.copy(
            phase = CapturePhase.SAVING, lastSaved = media, savedCount = mutableState.value.savedCount + 1
        )
        val persistDetails = saveDetails
        saveDetails = {}
        persistDetails(media)
        mutableState.value = mutableState.value.copy(
            phase = CapturePhase.READY,
            pendingReview = media.takeIf { it.kind == MediaKind.VIDEO }
        )
    }

    suspend fun recordingFinished(isCurrentRecordingFinished: () -> Boolean = { true }) = mutex.withLock {
        if (mutableState.value.phase == CapturePhase.BUFFERING && isCurrentRecordingFinished()) {
            mutableState.value = mutableState.value.copy(
                phase = CapturePhase.FAILED, error = "Rolling recording stopped unexpectedly. Resume the camera to restart the buffer."
            )
        }
        if (mutableState.value.phase == CapturePhase.RECORDING && isCurrentRecordingFinished()) {
            withContext(NonCancellable) {
                try {
                    finishRecording()
                } catch (error: CameraCaptureException) {
                    mutableState.value = mutableState.value.copy(phase = CapturePhase.FAILED, error = error.message)
                }
            }
        }
    }

    suspend fun close(reason: String? = null) = mutex.withLock {
        withContext(NonCancellable) {
            var failure = mutableState.value.error ?: reason
            try {
                if (mutableState.value.phase == CapturePhase.RECORDING) finishRecording()
            } catch (error: CameraCaptureException) {
                failure = error.message
            } finally {
                try {
                    service.stop()
                } catch (error: CameraCaptureException) {
                    failure = error.message
                }
                mutableState.value = mutableState.value.copy(
                    phase = if (failure == null) CapturePhase.STOPPED else CapturePhase.FAILED,
                    error = failure
                )
            }
        }
    }
}
