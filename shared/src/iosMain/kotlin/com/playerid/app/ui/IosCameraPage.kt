@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.playerid.app.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.playerid.app.domain.camera.CameraCaptureController
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.camera.CapturePhase
import com.playerid.app.domain.camera.CaptureMode
import com.playerid.app.domain.camera.organizeSavedCapture
import com.playerid.app.domain.camera.cameraJerseyOptions
import com.playerid.app.domain.team.*
import com.playerid.app.ui.screens.CameraGameSetupSheet
import com.playerid.app.ui.dialogs.teamComposeColor
import com.playerid.app.platform.IosCameraService
import com.playerid.app.platform.NSUserDefaultsPreferencesStore
import com.playerid.app.platform.MediaReference
import com.playerid.app.ui.screens.CameraCapturePage
import com.playerid.app.ui.screens.PostCaptureReviewPage
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import platform.AVFoundation.*
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSLog
import platform.AVFAudio.AVAudioSession
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*

@Composable
fun IosCameraPage(
    service: IosCameraService,
    controller: CameraCaptureController,
    scope: CoroutineScope,
    repository: LocalTeamRepository,
    onNavigateToTeams: () -> Unit
) {
    val state by controller.state.collectAsState()
    val preferences = remember { NSUserDefaultsPreferencesStore() }
    var rollingHintVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.mode, state.phase) {
        if (state.mode == CaptureMode.ROLLING && state.phase == CapturePhase.BUFFERING &&
            !preferences.getBoolean("playerid.rolling_capture_hint_shown")) {
            preferences.putBoolean("playerid.rolling_capture_hint_shown", true)
            rollingHintVisible = true
        }
    }
    LaunchedEffect(rollingHintVisible) {
        if (rollingHintVisible) {
            delay(5_000)
            rollingHintVisible = false
        }
    }
    val recordHaptic = remember { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy) }
    DisposableEffect(recordHaptic) {
        recordHaptic.prepare()
        onDispose {}
    }
    KeepRecordingScreenAwake(state.phase == CapturePhase.RECORDING || state.phase == CapturePhase.BUFFERING)
    LaunchedEffect(state.phase) {
        while (state.phase == CapturePhase.BUFFERING) {
            delay(30_000)
            controller.rotateRollingBuffer()
        }
    }
    LaunchedEffect(state.mode, state.phase, state.pendingReview) {
        controller.resumeRollingBuffer()
    }
    val snapshot by repository.snapshot.collectAsState()
    var setupOpen by remember { mutableStateOf(false) }
    var zoomError by remember { mutableStateOf<String?>(null) }
    var deletionError by remember { mutableStateOf<String?>(null) }
    var pendingDeletion by remember { mutableStateOf<MediaReference?>(null) }
    var deleting by remember { mutableStateOf(false) }
    fun deleteReview(media: MediaReference) {
        if (deleting) return
        pendingDeletion = media
        deleting = true
        deletionError = null
        controller.dismissReview(media)
        NSLog("PlayerID camera: review dismissed; deleting clip asynchronously.")
        scope.launch {
            withContext(NonCancellable) {
                try {
                    service.deleteSavedCapture(media)
                    controller.forgetDeletedCapture(media)
                    repository.forgetCapture(captureFileKey(media))
                    pendingDeletion = null
                } catch (failure: CameraCaptureException) {
                    deletionError = failure.message ?: "Couldn't delete the clip. Tap Retry delete."
                } catch (failure: TeamStorageException) {
                    deletionError = "The clip file was deleted, but its details couldn't be cleared. Tap Retry delete. ${failure.message.orEmpty()}"
                } finally {
                    deleting = false
                }
            }
        }
    }
    LaunchedEffect(state.phase) { zoomError = null }
    // Like Android, the setup pill pulses after every app launch until Game setup is opened.
    var hasOpenedSetup by remember { mutableStateOf(CameraSessionState.hasOpenedSetup) }
    // Follow the schedule: switch to the team's current game when its time window starts (rechecked every minute).
    LaunchedEffect(repository, setupOpen) {
        while (!setupOpen) {
            val now = Clock.System.now().toEpochMilliseconds()
            repository.snapshot.value.cameraOrganizationForCurrentGame(now, CameraSessionState.appliedGameIds)?.let { next ->
                CameraSessionState.appliedGameIds += next.gameId!!
                try {
                    repository.setCameraOrganization(next, repository.snapshot.value.cameraJerseyLabel)
                } catch (failure: Exception) {
                    NSLog("PlayerID camera: couldn't apply the scheduled game: ${failure.message}")
                }
            }
            delay(60_000)
        }
    }
    val organization = snapshot.linkedCameraOrganization()
    val selectedTeam = snapshot.teams.firstOrNull { it.name == organization?.teamName }
    val selectedPlayer = snapshot.players.firstOrNull { it.id == organization?.playerId }
    val selectedGame = snapshot.games.firstOrNull { it.id == organization?.gameId }
    val jerseyOptions = cameraJerseyOptions(selectedTeam)
    val jerseyColor = teamComposeColor(
        (jerseyOptions.firstOrNull { it.first == snapshot.cameraJerseyLabel } ?: jerseyOptions.first()).second
    )
    LaunchedEffect(service, controller) {
        service.recordingFinished.collect {
            controller.recordingFinished { service.isRecordingFinished }
        }
    }
    DisposableEffect(controller) {
        val center = NSNotificationCenter.defaultCenter
        val background = center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, null) {
            saveBeforeBackground(controller, scope)
        }
        val active = center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, null) {
            scope.launch { controller.open() }
        }
        val interrupted = center.addObserverForName(AVCaptureSessionWasInterruptedNotification, service.session, null) {
            scope.launch { controller.close("Camera interrupted. Resume when the camera is available.") }
        }
        val failed = center.addObserverForName(AVCaptureSessionRuntimeErrorNotification, service.session, null) {
            scope.launch { controller.close("The iPhone camera stopped unexpectedly. Resume the camera to retry.") }
        }
        scope.launch { controller.open() }
        onDispose {
            listOf(background, active, interrupted, failed).forEach { center.removeObserver(it) }
            scope.launch { controller.close() }
        }
    }
    val share = rememberGuardianMemorySharing(repository)
    val rosterVoice = remember { com.playerid.app.platform.IosVoiceSearch(shareCameraAudioSession = true) }
    val rosterSpeaker = remember { com.playerid.app.platform.IosSpeechSynthesis() }
    DisposableEffect(rosterVoice, rosterSpeaker) {
        onDispose { rosterVoice.cancel(); rosterSpeaker.stop() }
    }
    val review = state.pendingReview
    // Keep the camera and its native preview mounted under the review so dismissing it is instant.
    Box(Modifier.fillMaxSize()) {
    CameraCapturePage(
        state = state,
        preview = { modifier, zoomEnabled, onZoom ->
            // The preview stays mounted while Game setup is open: replacing it re-attaches a preview layer,
            // which reconfigures the running session (slow close, and it ends the Rolling recording).
            // Hiding it keeps the native view out of the keyboard-panned sheet.
            UIKitView(
                factory = { CameraPreviewView(service) },
                modifier = modifier,
                update = {
                    it.onZoom = onZoom
                    it.zoomEnabled = zoomEnabled && review == null && !setupOpen
                    it.hidden = setupOpen
                    it.layoutPreview()
                },
                onResize = { view: CameraPreviewView, rect: CValue<CGRect> -> view.setFrame(rect); view.layoutPreview() }
            )
        },
        onMode = { mode -> scope.launch { controller.changeMode(mode) } },
        onShutterPress = {
            val hapticsAllowed = try {
                service.allowRecordingHaptics()
                true
            } catch (failure: CameraCaptureException) {
                zoomError = failure.message
                false
            }
            NSLog("PlayerID camera: shutter pressed; recording haptics allowed=${AVAudioSession.sharedInstance().allowHapticsAndSystemSoundsDuringRecording}, reapply=$hapticsAllowed.")
            recordHaptic.impactOccurred()
            AudioServicesPlaySystemSound(1519u)
            recordHaptic.prepare()
        },
        onCapture = {
            val captureDetails = organization
            scope.launch {
                controller.capture { media ->
                    repository.organizeSavedCapture(media, captureDetails)
                }
            }
        },
        onRetry = { scope.launch { controller.open() } },
        teamName = organization?.teamName,
        opponent = selectedGame?.opponentName ?: organization?.opponentName.orEmpty(),
        player = selectedPlayer?.let { "${it.name} #${it.number}" } ?: "All",
        jerseyColor = jerseyColor,
        hasOpenedSetup = hasOpenedSetup,
        showRollingHint = rollingHintVisible,
        deletionMessage = if (deleting) "Deleting clip..." else deletionError,
        onRetryDelete = { pendingDeletion?.let { deleteReview(it) } },
        canRetryDelete = !deleting && deletionError != null,
        setupOpen = setupOpen,
        onSetup = { hasOpenedSetup = true; CameraSessionState.hasOpenedSetup = true; setupOpen = true },
        repository = repository,
        onNavigateToTeams = onNavigateToTeams,
        onZoom = { change ->
            if (zoomError == null) {
                try {
                    service.zoomBy(change)
                } catch (failure: CameraCaptureException) {
                    zoomError = failure.message ?: "Cannot adjust camera zoom."
                }
            }
        },
        zoomError = zoomError,
        onDismissZoomError = { zoomError = null },
        voiceSearch = rosterVoice,
        speaker = rosterSpeaker
    )
    if (review != null) {
        key(review.identifier) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }) {
                PostCaptureReviewPage(
                    media = review,
                    repository = repository,
                    shareMedia = { share(it, null) },
                    deleteMedia = service::deleteSavedCapture,
                    confirmBeforeDelete = false,
                    onImmediateDelete = { deleteReview(review) },
                    onDismiss = { deleted -> controller.dismissReview(review, deleted) }
                )
            }
        }
    }
    }
    if (setupOpen) CameraGameSetupSheet(repository, onDismiss = { setupOpen = false })
}

@Composable
private fun KeepRecordingScreenAwake(recording: Boolean) {
    DisposableEffect(recording) {
        if (recording) {
            val application = UIApplication.sharedApplication
            val previous = application.idleTimerDisabled
            val center = NSNotificationCenter.defaultCenter
            if (application.applicationState == UIApplicationState.UIApplicationStateActive) {
                application.idleTimerDisabled = true
            }
            val inactive = center.addObserverForName(UIApplicationWillResignActiveNotification, null, null) {
                application.idleTimerDisabled = previous
            }
            val active = center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, null) {
                application.idleTimerDisabled = true
            }
            onDispose {
                center.removeObserver(inactive)
                center.removeObserver(active)
                application.idleTimerDisabled = previous
            }
        } else {
            onDispose {}
        }
    }
}

private fun saveBeforeBackground(controller: CameraCaptureController, scope: CoroutineScope) {
    val application = UIApplication.sharedApplication
    var task = UIBackgroundTaskInvalid
    task = application.beginBackgroundTaskWithExpirationHandler {
        if (task != UIBackgroundTaskInvalid) {
            application.endBackgroundTask(task)
            task = UIBackgroundTaskInvalid
        }
        scope.launch { controller.close("iOS ended background saving time. Verify the latest capture before recording again.") }
    }
    scope.launch {
        try {
            controller.close()
        } finally {
            if (task != UIBackgroundTaskInvalid) {
                application.endBackgroundTask(task)
                task = UIBackgroundTaskInvalid
            }
        }
    }
}

private class CameraPreviewView(private val service: IosCameraService) : UIView(frame = CGRectZero.readValue()) {
    var onZoom: (Float) -> Unit = {}
    var zoomEnabled: Boolean = false
        set(value) {
            field = value
            pinch.enabled = value
        }
    private val pinch = UIPinchGestureRecognizer(target = this, action = NSSelectorFromString("handlePinch:"))
    private val preview = AVCaptureVideoPreviewLayer(session = service.session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        backgroundColor = UIColor.blackColor
        userInteractionEnabled = true
        multipleTouchEnabled = true
        addGestureRecognizer(pinch)
        layer.addSublayer(preview)
    }

    @ObjCAction
    fun handlePinch(recognizer: UIPinchGestureRecognizer) {
        if (zoomEnabled && (recognizer.state == UIGestureRecognizerStateBegan ||
                recognizer.state == UIGestureRecognizerStateChanged)) {
            onZoom(recognizer.scale.toFloat())
        }
        recognizer.scale = 1.0
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        layoutPreview()
    }

    fun layoutPreview() {
        preview.setFrame(bounds)
        val orientation = when (window?.windowScene?.interfaceOrientation) {
            UIInterfaceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeLeft
            UIInterfaceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeRight
            UIInterfaceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            else -> AVCaptureVideoOrientationPortrait
        }
        service.orientation = orientation
        val connection = preview.connection
        if (connection?.isVideoOrientationSupported() == true) connection.videoOrientation = orientation
    }
}

/** Per-launch camera state (Android's CameraSessionState): reset when the app process starts. */
private object CameraSessionState {
    var hasOpenedSetup = false
    var appliedGameIds: Set<String> = emptySet()
}
