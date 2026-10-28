package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import platform.AVFAudio.*
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.NSNotificationCenter
import platform.QuartzCore.CACurrentMediaTime
import platform.Speech.*
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UIApplicationWillResignActiveNotification
import kotlin.coroutines.resume

/**
 * One-shot dictation; stops after a short pause in speech.
 * With [shareCameraAudioSession] the camera's active PlayAndRecord session is reused and left active,
 * so listening does not interrupt the running capture session.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosVoiceSearch(private val shareCameraAudioSession: Boolean = false) : SpeechRecognitionService {
    private var engine: AVAudioEngine? = null
    private var task: SFSpeechRecognitionTask? = null
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var stopRequested = false
    private val micHaptic = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)

    override suspend fun recognizeOnce(): String? = recognizeOnce {}

    override suspend fun recognizeOnce(onPartialResult: (String) -> Unit): String? = withContext(Dispatchers.Main) {
        stopRequested = false
        // Fire before the microphone session starts; iOS mutes haptics during recording unless allowed.
        micHaptic.impactOccurred()
        micHaptic.prepare()
        requireSpeechPermission()
        ensureActive()
        if (stopRequested) return@withContext null
        requireMicrophonePermission()
        ensureActive()
        if (stopRequested) return@withContext null
        val recognizer = SFSpeechRecognizer(locale = NSLocale.currentLocale)
        if (!recognizer.isAvailable()) throw CameraCaptureException("Voice search isn't available right now. Check your connection and retry.")

        val audio = AVAudioSession.sharedInstance()
        val previousCategory = audio.category
        val previousMode = audio.mode
        val previousOptions = audio.categoryOptions
        val reuseSession = shareCameraAudioSession && previousCategory == AVAudioSessionCategoryPlayAndRecord
        val transcript = MutableStateFlow("")
        val finished = MutableStateFlow(false)
        val failure = MutableStateFlow<String?>(null)
        val center = NSNotificationCenter.defaultCenter
        val background = center.addObserverForName(UIApplicationWillResignActiveNotification, null, null) {
            stopRequested = true
            engine?.pause()
        }
        try {
            if (!reuseSession) memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                if (!audio.setCategory(AVAudioSessionCategoryPlayAndRecord, mode = AVAudioSessionModeMeasurement,
                        options = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionDuckOthers,
                        error = error.ptr) || !audio.setActive(true, error.ptr)) {
                    throw CameraCaptureException("Cannot start the microphone: ${error.value?.localizedDescription.orEmpty()}")
                }
                // Lets the stop tap's haptic play while listening.
                audio.setAllowHapticsAndSystemSoundsDuringRecording(true, null)
            }
            val speech = SFSpeechAudioBufferRecognitionRequest().apply {
                shouldReportPartialResults = true
                if (recognizer.supportsOnDeviceRecognition) requiresOnDeviceRecognition = true
            }
            request = speech
            val audioEngine = AVAudioEngine()
            engine = audioEngine
            val input = audioEngine.inputNode
            input.installTapOnBus(0u, 1024u, input.outputFormatForBus(0u)) { buffer, _ ->
                if (buffer != null) speech.appendAudioPCMBuffer(buffer)
            }
            audioEngine.prepare()
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                if (!audioEngine.startAndReturnError(error.ptr)) {
                    throw CameraCaptureException("Cannot start the microphone: ${error.value?.localizedDescription.orEmpty()}")
                }
            }
            task = recognizer.recognitionTaskWithRequest(speech) { result, error ->
                if (stopRequested) return@recognitionTaskWithRequest
                result?.let {
                    transcript.value = it.bestTranscription.formattedString
                    if (it.isFinal()) finished.value = true
                }
                if (error != null) {
                    if (!finished.value) failure.value = error.localizedDescription
                    finished.value = true
                }
            }

            val started = CACurrentMediaTime()
            var lastText = ""
            var lastChange = started
            while (!finished.value && !stopRequested) {
                delay(100)
                val now = CACurrentMediaTime()
                if (transcript.value != lastText) {
                    lastText = transcript.value
                    lastChange = now
                    onPartialResult(lastText)
                }
                val pausedAfterSpeech = lastText.isNotEmpty() && now - lastChange > 1.4
                if (pausedAfterSpeech || now - started > 12.0) break
            }
            ensureActive()
            if (stopRequested) return@withContext null
            failure.value?.let { throw CameraCaptureException("Voice recognition stopped: $it. Try again or type your request.") }
            transcript.value.trim().ifEmpty {
                throw CameraCaptureException("No speech was heard. Try again or type your request.")
            }
        } finally {
            stopRequested = true
            center.removeObserver(background)
            stopAudio()
            if (!reuseSession) memScoped {
                audio.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
                audio.setCategory(previousCategory, mode = previousMode, options = previousOptions, error = null)
            }
        }
    }

    override fun cancel() {
        if (!stopRequested) micHaptic.impactOccurred()
        stopRequested = true
    }

    private fun stopAudio() {
        engine?.let {
            it.stop()
            it.inputNode.removeTapOnBus(0u)
        }
        request?.endAudio()
        task?.cancel()
        engine = null
        request = null
        task = null
    }

    private suspend fun requireSpeechPermission() {
        val status = when (SFSpeechRecognizer.authorizationStatus()) {
            SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined ->
                suspendCancellableCoroutine { continuation ->
                    SFSpeechRecognizer.requestAuthorization { result ->
                        dispatchResume { if (continuation.isActive) continuation.resume(result) }
                    }
                }
            else -> SFSpeechRecognizer.authorizationStatus()
        }
        if (status != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
            throw CameraCaptureException("Allow Speech Recognition for PlayerID in Settings to use voice search.")
        }
    }

    private suspend fun requireMicrophonePermission() {
        val audio = AVAudioSession.sharedInstance()
        val granted = when (audio.recordPermission) {
            AVAudioSessionRecordPermissionGranted -> true
            AVAudioSessionRecordPermissionDenied -> false
            else -> suspendCancellableCoroutine { continuation ->
                audio.requestRecordPermission { allowed -> dispatchResume { if (continuation.isActive) continuation.resume(allowed) } }
            }
        }
        if (!granted) throw CameraCaptureException("Allow Microphone access for PlayerID in Settings to use voice search.")
    }

    private fun dispatchResume(block: () -> Unit) {
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue(), block)
    }
}
