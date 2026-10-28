@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.camera.cameraZoomAfterGesture
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.AVFoundation.*
import platform.CoreImage.CIImage
import platform.AVFAudio.*
import platform.Foundation.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosCameraService : CameraService, RollingCameraService {
    val session = AVCaptureSession()
    private val photos = AVCapturePhotoOutput()
    private val movies = AVCaptureMovieFileOutput()
    private data class PendingPhoto(val id: Long, val result: CompletableDeferred<MediaReference>)
    private data class PendingMovie(
        val url: NSURL,
        val result: CompletableDeferred<MediaReference>,
        val started: CompletableDeferred<Unit>,
        val temporaryOnly: Boolean
    )
    private var rolling = false
    private val rollingFiles = mutableListOf<NSURL>()
    private var rollingOrientation = AVCaptureVideoOrientationPortrait
    private val pendingPhoto = MutableStateFlow<PendingPhoto?>(null)
    private val pendingMovie = MutableStateFlow<PendingMovie?>(null)
    private val completedRecordings = Channel<Unit>(Channel.CONFLATED)
    val recordingFinished = completedRecordings.receiveAsFlow()
    val isRecordingFinished: Boolean get() = pendingMovie.value?.result?.isCompleted == true
    var orientation: AVCaptureVideoOrientation = AVCaptureVideoOrientationPortrait
    private val delegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol, AVCaptureFileOutputRecordingDelegateProtocol {
        override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
            processPhoto(didFinishProcessingPhoto, error)
        }

        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishCaptureForResolvedSettings: AVCaptureResolvedPhotoSettings,
            error: NSError?
        ) {
            val pending = pendingPhoto.value
            if (error != null && pending?.id == didFinishCaptureForResolvedSettings.uniqueID) {
                pending.result.completeExceptionally(CameraCaptureException("Photo capture failed: ${error.localizedDescription}"))
            }
        }

        override fun captureOutput(
            output: AVCaptureFileOutput,
            didStartRecordingToOutputFileAtURL: NSURL,
            fromConnections: List<*>
        ) {
            pendingMovie.value?.takeIf { it.url == didStartRecordingToOutputFileAtURL }?.started?.complete(Unit)
        }

        override fun captureOutput(
            output: AVCaptureFileOutput,
            didFinishRecordingToOutputFileAtURL: NSURL,
            fromConnections: List<*>,
            error: NSError?
        ) {
            finishMovie(didFinishRecordingToOutputFileAtURL, error)
            completedRecordings.trySend(Unit)
        }
    }

    override suspend fun start(configuration: CameraConfiguration) {
        requestPermission(AVMediaTypeVideo, "Camera")
        if (configuration.includeAudio) requestPermission(AVMediaTypeAudio, "Microphone")
        withContext(Dispatchers.Default) {
            if (session.running) return@withContext
            val temporaryDirectory = NSTemporaryDirectory()
            val temporaryNames = NSFileManager.defaultManager.contentsOfDirectoryAtPath(temporaryDirectory, error = null)
                ?: throw CameraCaptureException("Cannot inspect temporary camera storage.")
            temporaryNames.filterIsInstance<String>().filter { it.startsWith("playerid-rolling-") && it.endsWith(".mov") }
                .forEach { removeRollingFile(NSURL.fileURLWithPath("$temporaryDirectory$it")) }
            session.beginConfiguration()
            try {
                session.automaticallyConfiguresApplicationAudioSession = false
                configureRecordingAudioSession()
                session.inputs.toList().forEach { session.removeInput(it as AVCaptureInput) }
                session.outputs.toList().forEach { session.removeOutput(it as AVCaptureOutput) }
                session.sessionPreset = AVCaptureSessionPresetHigh
                val position = if (configuration.facing == CameraFacing.BACK) AVCaptureDevicePositionBack else AVCaptureDevicePositionFront
                val camera = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
                    .filterIsInstance<AVCaptureDevice>().firstOrNull { it.position == position }
                    ?: throw CameraCaptureException("No requested camera is available on this iPhone.")
                addInput(camera)
                if (configuration.includeAudio) {
                    val microphone = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)
                        ?: throw CameraCaptureException("No microphone is available for video recording.")
                    addInput(microphone)
                }
                if (!session.canAddOutput(photos)) throw CameraCaptureException("This iPhone cannot configure photo capture.")
                session.addOutput(photos)
                if (!session.canAddOutput(movies)) throw CameraCaptureException("This iPhone cannot configure video capture.")
                session.addOutput(movies)
            } finally {
                session.commitConfiguration()
            }
            session.startRunning()
            if (!session.running) throw CameraCaptureException("The iPhone camera could not start. Close other camera apps and retry.")
            allowRecordingHaptics()
        }
    }

    private fun configureRecordingAudioSession() = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        val audio = AVAudioSession.sharedInstance()
        if (!audio.setCategory(AVAudioSessionCategoryPlayAndRecord, mode = AVAudioSessionModeVideoRecording,
                options = AVAudioSessionCategoryOptionDefaultToSpeaker, error = error.ptr) ||
            !audio.setActive(true, error.ptr)) {
            throw CameraCaptureException("Cannot configure camera audio: ${error.value?.localizedDescription.orEmpty()}")
        }
        allowRecordingHaptics()
    }

    fun allowRecordingHaptics() = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        if (!AVAudioSession.sharedInstance().setAllowHapticsAndSystemSoundsDuringRecording(true, error.ptr)) {
            throw CameraCaptureException("Cannot enable camera haptic feedback: ${error.value?.localizedDescription.orEmpty()}")
        }
    }

    private fun addInput(device: AVCaptureDevice) {
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null)
            ?: throw CameraCaptureException("Cannot open ${device.localizedName}. Check camera and microphone access in Settings.")
        if (!session.canAddInput(input)) throw CameraCaptureException("Cannot connect ${device.localizedName} to the camera.")
        session.addInput(input)
    }

    fun zoomBy(change: Float) {
        if (!session.running) throw CameraCaptureException("The camera is paused. Resume it before zooming.")
        val device = session.inputs.filterIsInstance<AVCaptureDeviceInput>()
            .firstOrNull { it.device.hasMediaType(AVMediaTypeVideo) }?.device
            ?: throw CameraCaptureException("The camera is not available for zoom.")
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            error.value = null
            if (!device.lockForConfiguration(error.ptr)) {
                throw CameraCaptureException("Cannot adjust camera zoom. ${error.value?.localizedDescription ?: "Try again."}")
            }
            try {
                val zoom = try {
                    cameraZoomAfterGesture(
                        device.videoZoomFactor,
                        change.toDouble(),
                        device.minAvailableVideoZoomFactor,
                        device.maxAvailableVideoZoomFactor
                    )
                } catch (failure: IllegalArgumentException) {
                    throw CameraCaptureException(failure.message ?: "Cannot adjust camera zoom.")
                }
                device.videoZoomFactor = zoom
            } finally {
                device.unlockForConfiguration()
            }
        }
    }

    private suspend fun requestPermission(type: String?, label: String) {
        val granted = when (AVCaptureDevice.authorizationStatusForMediaType(type)) {
            AVAuthorizationStatusAuthorized -> true
            AVAuthorizationStatusNotDetermined -> suspendCoroutine { continuation ->
                AVCaptureDevice.requestAccessForMediaType(type) { continuation.resume(it) }
            }
            else -> false
        }
        if (!granted) throw CameraCaptureException(
            "$label access is required. Enable it in iPhone Settings > Apps > PlayerID, then resume the camera."
        )
    }

    override suspend fun stop() {
        try {
            if (rolling || rollingFiles.isNotEmpty()) discardRollingBuffer()
            else if (pendingMovie.value != null) stopRecording()
        } finally {
            withContext(Dispatchers.Default) {
                if (session.running) session.stopRunning()
            }
        }
    }

    override suspend fun capturePhoto(): MediaReference {
        if (!session.running) throw CameraCaptureException("The camera is not running. Resume the camera and try again.")
        if (pendingPhoto.value?.result?.isCompleted == false) throw CameraCaptureException("The previous photo is still finishing. Please wait before retrying.")
        val result = CompletableDeferred<MediaReference>()
        setOrientation(photos.connectionWithMediaType(AVMediaTypeVideo))
        val settings = AVCapturePhotoSettings.photoSettingsWithFormat(mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG))
        val pending = PendingPhoto(settings.uniqueID, result)
        pendingPhoto.value = pending
        photos.capturePhotoWithSettings(settings, delegate = delegate)
        return try {
            withTimeoutOrNull(20_000) { result.await() }
                ?: throw CameraCaptureException("Photo capture timed out. Resume the camera and try again.")
        } finally {
            if (result.isCompleted) pendingPhoto.compareAndSet(pending, null)
        }
    }

    private fun processPhoto(photo: AVCapturePhoto, error: NSError?) {
        val pending = pendingPhoto.value?.takeIf { it.id == photo.resolvedSettings.uniqueID } ?: return
        val result = pending.result
        if (error != null) {
            result.completeExceptionally(CameraCaptureException("Photo capture failed: ${error.localizedDescription}"))
            return
        }
        val data = photo.fileDataRepresentation()
        if (data == null) {
            result.completeExceptionally(CameraCaptureException("The camera returned no photo data."))
            return
        }
        try {
            val path = "${mediaDirectory()}/${timestampPrefix()}-${NSUUID().UUIDString}.jpg"
            if (!data.writeToFile(path, atomically = true)) {
                throw CameraCaptureException("Cannot save the photo. Check the iPhone's available storage.")
            }
            result.complete(MediaReference(NSURL.fileURLWithPath(path).absoluteString!!, MediaKind.IMAGE, "image/jpeg"))
        } catch (failure: CameraCaptureException) {
            result.completeExceptionally(failure)
        }
    }

    override suspend fun startRecording() = beginRecording(temporaryOnly = false)

    private suspend fun beginRecording(temporaryOnly: Boolean): Unit = withContext(Dispatchers.Default) {
        if (!session.running || movies.recording || pendingMovie.value != null) {
            throw CameraCaptureException("The camera is not ready to start a video. Resume the camera and retry.")
        }
        // Check storage before recording, rather than discovering a missing destination after capture.
        mediaDirectory()
        val result = CompletableDeferred<MediaReference>()
        val started = CompletableDeferred<Unit>()
        val connection = movies.connectionWithMediaType(AVMediaTypeVideo)
        if (temporaryOnly && connection?.isVideoOrientationSupported() == true) {
            connection.videoOrientation = rollingOrientation
        } else setOrientation(connection)
        val prefix = if (temporaryOnly) "playerid-rolling" else "playerid"
        val temporary = NSURL.fileURLWithPath("${NSTemporaryDirectory()}$prefix-${timestampPrefix()}-${NSUUID().UUIDString}.mov")
        val pending = PendingMovie(temporary, result, started, temporaryOnly)
        pendingMovie.value = pending
        movies.startRecordingToOutputFileURL(temporary, recordingDelegate = delegate)
        try {
            if (withTimeoutOrNull(15_000) { started.await(); true } != true) {
                throw CameraCaptureException("Video recording did not start. Resume the camera and retry.")
            }
            allowRecordingHaptics()
        } catch (failure: CameraCaptureException) {
            if (movies.recording) movies.stopRecording()
            if (result.isCompleted) pendingMovie.compareAndSet(pending, null)
            throw failure
        }
    }

    override suspend fun startRollingBuffer() {
        discardRollingBuffer()
        rollingOrientation = orientation
        rolling = true
        beginRecording(temporaryOnly = true)
    }

    override suspend fun rotateRollingBuffer() {
        if (!rolling) throw CameraCaptureException("Rolling recording is not active.")
        val segment = stopRecording()
        rollingFiles += NSURL.URLWithString(segment.identifier)!!
        while (rollingFiles.size > 1) {
            removeRollingFile(rollingFiles.first())
            rollingFiles.removeAt(0)
        }
        beginRecording(temporaryOnly = true)
    }

    override suspend fun saveRollingBuffer(): MediaReference {
        if (!rolling) throw CameraCaptureException("Rolling recording is not active.")
        val segment = stopRecording()
        rollingFiles += NSURL.URLWithString(segment.identifier)!!
        val result = exportRollingVideo(rollingFiles.toList(), this)
        try {
            discardRollingBuffer()
        } catch (failure: CameraCaptureException) {
            NSLog("PlayerID saved the Rolling clip but couldn't clean its buffer: ${failure.message}")
        }
        return result
    }

    override suspend fun discardRollingBuffer() {
        rolling = false
        val pending = pendingMovie.value?.takeIf { it.temporaryOnly }
        if (pending != null) {
            try {
                stopRecording()
            } finally {
                if (pending.result.isCompleted && pending.url !in rollingFiles) rollingFiles += pending.url
            }
        }
        while (rollingFiles.isNotEmpty()) {
            removeRollingFile(rollingFiles.first())
            rollingFiles.removeAt(0)
        }
    }

    private fun removeRollingFile(url: NSURL) {
        if (NSFileManager.defaultManager.fileExistsAtPath(url.path!!) &&
            !NSFileManager.defaultManager.removeItemAtURL(url, error = null)) {
            throw CameraCaptureException("Cannot remove a temporary Rolling segment. Check iPhone storage and resume the camera.")
        }
    }

    override suspend fun stopRecording(): MediaReference = withContext(Dispatchers.Default) {
        val pending = pendingMovie.value ?: throw CameraCaptureException("There is no video recording to save.")
        val result = pending.result
        if (movies.recording) movies.stopRecording()
        try {
            withTimeoutOrNull(30_000) { result.await() }
                ?: throw CameraCaptureException("Video saving is taking too long. The temporary recording has not been deleted.")
        } finally {
            // Keep an unfinished native recording retained until its delegate finishes saving it.
            if (result.isCompleted) {
                pendingMovie.compareAndSet(pending, null)
            }
        }
    }

    private fun finishMovie(url: NSURL, error: NSError?) {
        val pending = pendingMovie.value?.takeIf { it.url == url } ?: return
        val result = pending.result
        val savedSuccessfully = error?.userInfo?.get(AVErrorRecordingSuccessfullyFinishedKey) as? NSNumber
        if (error != null && savedSuccessfully?.boolValue != true) {
            val failure = CameraCaptureException("Video recording failed: ${error.localizedDescription}")
            result.completeExceptionally(failure)
            pending.started.completeExceptionally(failure)
            return
        }
        try {
            if (pending.temporaryOnly) {
                result.complete(MediaReference(url.absoluteString!!, MediaKind.VIDEO, "video/quicktime"))
                pending.started.complete(Unit)
                return
            }
            val destination = NSURL.fileURLWithPath("${mediaDirectory()}/${timestampPrefix()}-${NSUUID().UUIDString}.mov")
            if (!NSFileManager.defaultManager.moveItemAtURL(url, destination, error = null)) {
                throw CameraCaptureException("Cannot save the video. Check available storage. The temporary recording has not been deleted.")
            }
            result.complete(MediaReference(destination.absoluteString!!, MediaKind.VIDEO, "video/quicktime"))
            pending.started.complete(Unit)
        } catch (failure: CameraCaptureException) {
            result.completeExceptionally(failure)
            pending.started.completeExceptionally(failure)
        }
    }

    private fun setOrientation(connection: AVCaptureConnection?) {
        if (connection?.isVideoOrientationSupported() == true) connection.videoOrientation = orientation
    }

    private fun mediaDirectory(): String {
        val manager = NSFileManager.defaultManager
        val support = manager.URLForDirectory(
            NSApplicationSupportDirectory, NSUserDomainMask, appropriateForURL = null, create = true, error = null
        )?.path ?: throw CameraCaptureException("Cannot locate the iPhone's application storage.")
        val directory = "$support/PlayerID/media"
        if (!manager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)) {
            throw CameraCaptureException("Cannot create media storage. Check available space on the iPhone.")
        }
        return directory
    }

    fun importPhotoLibraryFile(source: NSURL, video: Boolean): MediaReference = memScoped {
        val failure = alloc<ObjCObjectVar<NSError?>>()
        failure.value = null
        val extension = if (video) source.pathExtension?.lowercase() else "jpg"
        if (video && extension !in listOf("mov", "mp4", "m4v")) {
            throw CameraCaptureException("This video format is not supported. Choose a MOV, MP4 or M4V video.")
        }
        if (video && AVURLAsset(uRL = source, options = null).tracksWithMediaType(AVMediaTypeVideo).isEmpty()) {
            throw CameraCaptureException("Photos returned a video with no playable video track.")
        }
        // Name the copy with the original recording time so Memories dates and ordering reflect when it was captured.
        val capturedAt = originalCaptureMillis(source, video) ?: timestampPrefix().toLong()
        val destination = NSURL.fileURLWithPath("${mediaDirectory()}/$capturedAt-${NSUUID().UUIDString}.$extension")
        if (video) {
            val staging = NSURL.fileURLWithPath("${NSTemporaryDirectory()}${NSUUID().UUIDString}.$extension")
            try {
                if (!NSFileManager.defaultManager.copyItemAtURL(source, staging, error = failure.ptr) ||
                    !NSFileManager.defaultManager.moveItemAtURL(staging, destination, error = failure.ptr)) {
                    throw CameraCaptureException("Cannot import this video. Check available storage. ${failure.value?.localizedDescription.orEmpty()}")
                }
            } finally {
                if (NSFileManager.defaultManager.fileExistsAtPath(staging.path!!) &&
                    !NSFileManager.defaultManager.removeItemAtURL(staging, error = failure.ptr)) {
                    NSLog("PlayerID couldn't remove an import temporary file: ${failure.value?.localizedDescription}")
                }
            }
        } else {
            val image = source.path?.let { UIImage.imageWithContentsOfFile(it) }
                ?: throw CameraCaptureException("Cannot decode this photo. Choose a different image.")
            val data = UIImageJPEGRepresentation(image, 0.95)
                ?: throw CameraCaptureException("Cannot convert this photo for PlayerID.")
            if (!data.writeToURL(destination, options = NSDataWritingAtomic, error = failure.ptr)) {
                throw CameraCaptureException("Cannot save the imported photo. Check available storage. ${failure.value?.localizedDescription.orEmpty()}")
            }
        }
        MediaReference(destination.absoluteString!!, if (video) MediaKind.VIDEO else MediaKind.IMAGE,
            if (!video) "image/jpeg" else if (extension == "mov") "video/quicktime" else "video/mp4")
    }

    suspend fun deleteSavedCapture(media: MediaReference) = withContext(Dispatchers.Default) {
        val url = NSURL.URLWithString(media.identifier)
            ?: throw CameraCaptureException("Cannot delete a clip with an invalid file address.")
        val name = url.lastPathComponent
        val expected = name?.let { NSURL.fileURLWithPath("${mediaDirectory()}/$it") }
        if (!url.fileURL || name == null || savedCaptureKind(name) == null ||
            url.absoluteString != expected?.absoluteString) {
            throw CameraCaptureException("Only clips in PlayerID's private media folder can be deleted.")
        }
        memScoped {
            val failure = alloc<ObjCObjectVar<NSError?>>()
            // A retry may follow a successful file deletion but failed metadata cleanup.
            if (NSFileManager.defaultManager.fileExistsAtPath(url.path!!) &&
                !NSFileManager.defaultManager.removeItemAtURL(url, error = failure.ptr)) {
                throw CameraCaptureException("Cannot delete this clip. ${failure.value?.localizedDescription.orEmpty()}")
            }
        }
    }

    override suspend fun listSavedCaptures(): List<MediaReference> = withContext(Dispatchers.Default) {
        val directory = mediaDirectory()
        val names = NSFileManager.defaultManager.contentsOfDirectoryAtPath(directory, error = null)
            ?.filterIsInstance<String>()
            ?: throw CameraCaptureException("PlayerID couldn't read the saved-captures folder. Your media files have not been changed.")
        names.filter { savedCaptureKind(it) != null }
            .sortedDescending()
            .map { name ->
            MediaReference(
                NSURL.fileURLWithPath("$directory/$name").absoluteString!!,
                savedCaptureKind(name)!!,
                when (name.substringAfterLast('.').lowercase()) {
                    "jpg" -> "image/jpeg"
                    "mov" -> "video/quicktime"
                    else -> "video/mp4"
                }
            )
        }
    }

    private fun originalCaptureMillis(source: NSURL, video: Boolean): Long? {
        val date = if (video) {
            AVURLAsset(uRL = source, options = null).creationDate?.dateValue
        } else {
            val exif = CIImage.imageWithContentsOfURL(source)?.properties?.get("{Exif}") as? Map<*, *>
            (exif?.get("DateTimeOriginal") as? String)?.let { stamp ->
                val offset = exif["OffsetTimeOriginal"] as? String
                NSDateFormatter().apply {
                    locale = NSLocale(localeIdentifier = "en_US_POSIX")
                    dateFormat = if (offset != null) "yyyy:MM:dd HH:mm:ssXXX" else "yyyy:MM:dd HH:mm:ss"
                }.dateFromString(stamp + offset.orEmpty())
            }
        } ?: return null
        val millis = (date.timeIntervalSince1970 * 1000).toLong()
        // Ignore missing or nonsense metadata (before 2000 or in the future).
        return millis.takeIf { it in 946_684_800_000L..(NSDate().timeIntervalSince1970 * 1000).toLong() + 86_400_000L }
    }

    private fun timestampPrefix(): String =
        (NSDate().timeIntervalSince1970 * 1000).toLong().toString()
}
