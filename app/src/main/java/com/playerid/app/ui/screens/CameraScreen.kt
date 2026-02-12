@file:OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.camera.core.ExperimentalGetImage::class
)
package com.playerid.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.playerid.app.ar.JerseyDetectionManager
import com.playerid.app.ui.composables.PlayerBubblesOverlay
import com.playerid.app.ui.theme.ErrorRed
import com.playerid.app.ui.theme.SpotrHighlightOrange
import com.playerid.app.ui.theme.SpotrPrimaryBlue
import com.playerid.app.ui.theme.SpotrSuccessGreen
import com.playerid.app.utils.RecordingManager
import com.playerid.app.utils.RecordingState
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.VoiceAction
import com.playerid.app.viewmodels.VoiceAssistantResult
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween

@Composable
fun CameraScreen(
    viewModel: PlayerViewModel,
    teamViewModel: com.playerid.app.viewmodels.TeamViewModel,
    showVoiceId: Boolean,
    isVoiceListening: Boolean,
    onVoiceIdToggle: () -> Unit,
    onVideoSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    val trackedPlayersWithInfo by viewModel.detectedPlayersWithInfo.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val currentRoster by viewModel.filteredPlayers.collectAsState()
    val voiceResult by viewModel.voiceResult.collectAsState()
    val isVoiceSessionActive by viewModel.isVoiceSessionActive.collectAsState()
    
    val recordingManager = remember { RecordingManager(context) }
    val recordingState by recordingManager.recordingState.collectAsState()

    var liveStartMs by remember { mutableStateOf<Long?>(null) }
    var liveElapsedMs by remember { mutableStateOf(0L) }

    var isStandby by remember { mutableStateOf(false) }
    val capturePastMode by viewModel.capturePastMode.collectAsState()
    var wasCapturePast by remember { mutableStateOf(capturePastMode) }

    // Auto-start background recording
    LaunchedEffect(recordingState, cameraPermissionsState.allPermissionsGranted, isVoiceSessionActive, capturePastMode, isStandby) {
        if (capturePastMode &&
            !isStandby &&
            cameraPermissionsState.allPermissionsGranted &&
            recordingState == RecordingState.IDLE &&
            !isVoiceSessionActive) {
            delay(1000)
            recordingManager.startRecording { uri ->
                uri?.let { onVideoSaved(it) }
            }
        }
    }

    LaunchedEffect(capturePastMode) {
        if (wasCapturePast && !capturePastMode) {
            isStandby = false
            if (recordingState == RecordingState.RECORDING) {
                recordingManager.stopAndDiscardRecording()
            }
        }
        wasCapturePast = capturePastMode
    }

    val isLiveRecording = !capturePastMode && recordingState == RecordingState.RECORDING

    LaunchedEffect(isLiveRecording) {
        liveStartMs = if (isLiveRecording) SystemClock.elapsedRealtime() else null
        liveElapsedMs = 0L
        while (isLiveRecording && liveStartMs != null) {
            liveElapsedMs = SystemClock.elapsedRealtime() - (liveStartMs ?: 0L)
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.voiceActions.collect { action ->
            when (action) {
                is VoiceAction.StopRecording -> {
                    if (recordingState == RecordingState.RECORDING) {
                        recordingManager.stopRecording()
                    }
                }
                is VoiceAction.StopRecordingSilent -> {
                    if (recordingState == RecordingState.RECORDING) {
                        recordingManager.stopAndDiscardRecording()
                    }
                }
            }
        }
    }

    var processing by remember { mutableStateOf(false) }
    val detectionManager = remember {
        JerseyDetectionManager(
            context = context,
            onPlayersTracked = { tracked ->
                if (!isStandby) {
                    viewModel.updateTrackedPlayers(tracked)
                }
                processing = false
            },
            onDetectionProcessing = { processing = true }
        )
    }

    LaunchedEffect(isStandby) {
        detectionManager.setPaused(isStandby)
    }

    LaunchedEffect(currentRoster) {
        val validNumbers = currentRoster.map { it.number }.toSet()
        detectionManager.setRosterFilter(validNumbers)
    }

    val activity = context as? Activity
    DisposableEffect(isStandby) {
        if (isStandby) {
            activity?.window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                attributes = attributes?.apply { screenBrightness = 0.01f }
            }
        } else {
            activity?.window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                attributes = attributes?.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
            }
        }
        onDispose {}
    }

    var arMode by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { recordingManager.stopAndDiscardRecording() }
    }

    if (cameraPermissionsState.allPermissionsGranted) {
        Scaffold(
            floatingActionButton = {
                if (!isStandby) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (recordingState == RecordingState.RECORDING) {
                                    recordingManager.stopRecording()
                                } else if (recordingState == RecordingState.IDLE) {
                                    recordingManager.startRecording { uri ->
                                        uri?.let { onVideoSaved(it) }
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.Center),
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                        ) {
                            val innerSize by animateDpAsState(
                                targetValue = if (isLiveRecording) 24.dp else 38.dp,
                                animationSpec = tween(durationMillis = 200),
                                label = "recordInnerSize"
                            )

                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Transparent, CircleShape)
                                        .border(width = 2.dp, color = Color.White, shape = CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(innerSize)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { padding ->
            var camera: Camera? by remember { mutableStateOf(null) }
            var scaleFactor by remember { mutableStateOf(1f) }
            var minZoom by remember { mutableStateOf(1f) }
            var maxZoom by remember { mutableStateOf(8f) }
            var isCameraReady by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier.fillMaxSize().padding(padding).pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (!isStandby) {
                            camera?.let { cam ->
                                scaleFactor = (scaleFactor * zoomChange).coerceIn(minZoom, maxZoom)
                                cam.cameraControl.setZoomRatio(scaleFactor)
                            }
                        }
                    }
                }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { view ->
                            startCamera(ctx, lifecycleOwner, view, detectionManager, recordingManager) { cam ->
                                camera = cam
                                minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                                maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8f
                                isCameraReady = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (!isCameraReady && !isStandby) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Warming camera...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (isLiveRecording) {
                    val totalSeconds = (liveElapsedMs / 1000).toInt()
                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("%d:%02d", minutes, seconds),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                if (capturePastMode && !isStandby) {
                    FloatingActionButton(
                        onClick = { isStandby = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Bedtime, "Sleep")
                    }
                }

                if (showVoiceId && !isStandby) {
                    FloatingActionButton(
                        onClick = onVoiceIdToggle,
                        containerColor = if (isVoiceListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isVoiceListening) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.PersonSearch, "Voice Player ID")
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModeChip(
                                text = "Capture Past",
                                selected = capturePastMode,
                                selectedColor = MaterialTheme.colorScheme.primary,
                                onClick = { viewModel.setCapturePastMode(true) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            ModeChip(
                                text = "Live Camera",
                                selected = !capturePastMode,
                                selectedColor = MaterialTheme.colorScheme.secondary,
                                onClick = { viewModel.setCapturePastMode(false) }
                            )
                        }
                    }
                }

                if (arMode && !isStandby) {
                    PlayerBubblesOverlay(
                        trackedPlayers = trackedPlayersWithInfo,
                        processing = processing,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    ) {
                        val visiblePlayers = trackedPlayersWithInfo.filter { it.first.disappearedFrames == 0 }
                        if (visiblePlayers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Spotr AR Active", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Tracking: ${visiblePlayers.size} players", style = MaterialTheme.typography.bodySmall)
                                        val firstPlayer = visiblePlayers.first().first
                                        val frozenW = firstPlayer.initialBox.width().toInt()
                                        val frozenH = firstPlayer.initialBox.height().toInt()
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("| Size: ${frozenW}x${frozenH}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    visiblePlayers.firstOrNull()?.let { (tracked, player) ->
                                        val displayText = player?.let { "${it.name} #${it.number}" } ?: "Unknown #${tracked.jerseyNumber}"
                                        val textColor = if (player != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        Text(displayText, style = MaterialTheme.typography.bodyMedium, color = textColor)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isStandby) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.98f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { isStandby = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FlashOn, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("CAPTURE PAST", color = Color.White.copy(alpha = 0.25f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Recording the last moments... Tap to wake UI", color = Color.White.copy(alpha = 0.15f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Voice result is handled at the app level to avoid duplicate overlays
            }
        }
    } else {
        CameraPermissionScreen { cameraPermissionsState.launchMultiplePermissionRequest() }
    }
}

@Composable
fun VoiceResultCard(result: VoiceAssistantResult, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(result) {
        delay(4000)
        onDismiss()
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is VoiceAssistantResult.Success -> SpotrSuccessGreen
                is VoiceAssistantResult.Error -> ErrorRed
            },
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (result) { 
                    is VoiceAssistantResult.Success -> Icons.Default.PersonSearch 
                    is VoiceAssistantResult.Error -> Icons.Default.Error 
                }, 
                contentDescription = null, 
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val message = when (result) {
                    is VoiceAssistantResult.Success -> result.message
                    is VoiceAssistantResult.Error -> result.message
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (result is VoiceAssistantResult.Success && result.player != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Name: ${result.player.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Number: ${result.player.number}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Position: ${result.player.position.ifBlank { "Unknown" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Year: ${result.player.academicYear.ifBlank { "Unknown" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Dismiss", tint = Color.White) }
        }
    }
}

@Composable
private fun CameraPermissionScreen(onRequestPermission: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Camera Access Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text("Grant Permissions") }
    }
}

@Composable
private fun ModeChip(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) selectedColor else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private fun startCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView, analyzer: JerseyDetectionManager, recordingManager: RecordingManager, onCameraReady: (Camera) -> Unit) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
        val videoCapture = VideoCapture.withOutput(recorder)
        recordingManager.setVideoCapture(videoCapture)
        val imageAnalyzer = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture, imageAnalyzer)
            onCameraReady(camera)
        } catch (e: Exception) { Log.e("CameraScreen", "Binding failed", e) }
    }, ContextCompat.getMainExecutor(context))
}
