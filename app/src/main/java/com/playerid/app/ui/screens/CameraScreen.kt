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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: PlayerViewModel,
    teamViewModel: com.playerid.app.viewmodels.TeamViewModel,
    showVoiceId: Boolean,
    isVoiceListening: Boolean,
    onVoiceIdToggle: () -> Unit,
    onVideoSaved: (Uri, Long) -> Unit,
    onNavigateToVideoLibrary: () -> Unit = { }
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var liveStartMs by remember { mutableStateOf<Long?>(null) }
    var liveElapsedMs by remember { mutableStateOf(0L) }

    var isCameraReady by remember { mutableStateOf(false) }
    LaunchedEffect(isCameraReady) {
        Log.d("CameraScreen", "isCameraReady changed: $isCameraReady")
    }
    var isStandby by remember { mutableStateOf(false) }
    // Only set isCameraReady to false on first composition
    LaunchedEffect(Unit) {
        isCameraReady = false
    }
    // Only show the overlay while the camera is not ready
    val showCameraOverlay = !isCameraReady && !isStandby
    val capturePastMode by viewModel.capturePastMode.collectAsState()
    var wasCapturePast by remember { mutableStateOf(capturePastMode) }
    var lastManualStop by remember { mutableStateOf(System.currentTimeMillis()) }
    var isAppInBackground by remember { mutableStateOf(false) }

    // Mode-aware handlers for recording completion
    val recordingCompleteHandler: (Uri, Long) -> Unit = { uri, timestamp ->
        // Save metadata for both modes
        val selectedTeam = viewModel.selectedTeam.value
        val videoPath = uri.path ?: ""
        val videoUriString = uri.toString()
        if (selectedTeam != null) {
            val prefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
            if (videoPath.isNotEmpty()) {
                prefs.edit().putString(videoPath, selectedTeam).apply()
            }
            if (videoUriString.isNotEmpty()) {
                prefs.edit().putString(videoUriString, selectedTeam).apply()
            }
        }
        if (timestamp > 0L) {
            val prefs = context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
            if (videoPath.isNotEmpty()) {
                prefs.edit().putLong(videoPath, timestamp).apply()
            }
            if (videoUriString.isNotEmpty()) {
                prefs.edit().putLong(videoUriString, timestamp).apply()
            }
        }
        
        // Both modes show post-recording screen
        onVideoSaved(uri, timestamp)
    }

    LaunchedEffect(recordingState, cameraPermissionsState.allPermissionsGranted, isVoiceSessionActive, capturePastMode, isStandby, isAppInBackground, isCameraReady) {
        if (capturePastMode &&
            !isStandby &&
            cameraPermissionsState.allPermissionsGranted &&
            recordingState == RecordingState.IDLE &&
            !isVoiceSessionActive &&
            !isAppInBackground &&
            isCameraReady) {
            // Prevent auto-start within 3 seconds of a manual stop, then resume automatically.
            val timeSinceStop = System.currentTimeMillis() - lastManualStop
            val remainingCooldown = 0L - timeSinceStop
            if (remainingCooldown > 0L) {
                delay(remainingCooldown)
            }
            if (capturePastMode &&
                !isStandby &&
                cameraPermissionsState.allPermissionsGranted &&
                recordingState == RecordingState.IDLE &&
                !isVoiceSessionActive &&
                !isAppInBackground &&
                isCameraReady) {
                delay(0)
                recordingManager.startRecording { uri ->
                    uri?.let { recordingCompleteHandler(it, recordingManager.getLastRecordingStartTimeMs()) }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, capturePastMode, recordingState) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    isAppInBackground = true
                    if (capturePastMode && recordingState == RecordingState.RECORDING) {
                        lastManualStop = System.currentTimeMillis()
                        recordingManager.stopAndDiscardRecording()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME,
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    isAppInBackground = false
                    lastManualStop = System.currentTimeMillis()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(capturePastMode) {
        if (wasCapturePast && !capturePastMode) {
            isStandby = false
            if (recordingState == RecordingState.RECORDING) {
                recordingManager.stopAndDiscardRecording()
            }
        }
        if (!wasCapturePast && capturePastMode) {
            // Give Capture Past a short cool-down when toggling back from Live.
            lastManualStop = System.currentTimeMillis()
        }
        wasCapturePast = capturePastMode
    }

    val isLiveRecording = !capturePastMode && recordingState == RecordingState.RECORDING
    val isRecording = recordingState == RecordingState.RECORDING

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
    DisposableEffect(isStandby, recordingState) {
        if (isStandby || recordingState == RecordingState.RECORDING) {
            activity?.window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (isStandby) {
                    attributes = attributes?.apply { screenBrightness = 0.01f }
                } else {
                    attributes = attributes?.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
                }
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
            containerColor = Color.Black,
            contentColor = Color.White,
            floatingActionButton = {
                if (!isStandby) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (!isCameraReady) return@FloatingActionButton
                                if (recordingState == RecordingState.RECORDING) {
                                    if (capturePastMode) {
                                        // In Capture Past: mark the stop time to prevent immediate auto-restart
                                        lastManualStop = System.currentTimeMillis()
                                    }
                                    recordingManager.stopRecording()
                                } else if (recordingState == RecordingState.IDLE) {
                                    // Both Live and Capture Past can manually start
                                    recordingManager.startRecording { uri ->
                                        uri?.let { recordingCompleteHandler(it, recordingManager.getLastRecordingStartTimeMs()) }
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.Center),
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                        ) {
                            val recordAlpha = if (isCameraReady) 1f else 0.45f
                            val innerSize by animateDpAsState(
                                targetValue = if (isRecording) 24.dp else 38.dp,
                                animationSpec = tween(durationMillis = 200),
                                label = "recordInnerSize"
                            )

                            Box(
                                modifier = Modifier.size(48.dp).alpha(recordAlpha),
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
            floatingActionButtonPosition = FabPosition.Center,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            var camera: Camera? by remember { mutableStateOf(null) }
            var scaleFactor by remember { mutableStateOf(1f) }
            var minZoom by remember { mutableStateOf(1f) }
            var maxZoom by remember { mutableStateOf(8f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black)
                    .pointerInput(Unit) {
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
                // Black fill layer to prevent white flash during camera initialization
                if (!isCameraReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { view ->
                            view.setBackgroundColor(android.graphics.Color.BLACK)
                            startCamera(ctx, lifecycleOwner, view, detectionManager, recordingManager) { cam ->
                                camera = cam
                                minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                                maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8f
                                Log.d("CameraScreen", "Camera ready callback - setting isCameraReady true")
                                isCameraReady = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay must be last so it cannot persist after isCameraReady is true
                if (showCameraOverlay) {
                    Log.d("CameraScreen", "Showing camera overlay: isCameraReady=$isCameraReady isStandby=$isStandby")
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
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "camera-rotate")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "rotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Camera Loading",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .graphicsLayer { rotationZ = rotation },
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
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

                // Bottom-left: Mode toggle
                if (!isStandby) {
                    FloatingActionButton(
                        onClick = { viewModel.setCapturePastMode(!capturePastMode) },
                        containerColor = if (capturePastMode) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .size(64.dp)
                    ) {
                        Icon(
                            if (capturePastMode) Icons.Default.History else Icons.Default.Videocam,
                            contentDescription = if (capturePastMode) "Capture Past" else "Capture Moment",
                            modifier = Modifier.size(32.dp),
                            tint = if (capturePastMode) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                
                // Bottom-right: Voice ID button
                if (!isStandby) {
                    FloatingActionButton(
                        onClick = onVoiceIdToggle,
                        containerColor = if (isVoiceListening) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(56.dp)
                    ) {
                        Icon(
                            if (isVoiceListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Voice Player ID",
                            modifier = Modifier.size(24.dp),
                            tint = if (isVoiceListening) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                if (arMode && !isStandby) {
                    PlayerBubblesOverlay(
                        trackedPlayers = trackedPlayersWithInfo,
                        processing = processing,
                        modifier = Modifier.fillMaxSize()
                    )
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
