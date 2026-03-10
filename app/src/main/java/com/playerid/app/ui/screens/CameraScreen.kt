@file:OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.camera.core.ExperimentalGetImage::class
)
package com.playerid.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween

// Only keep unambiguous, non-conflicting imports. Use fully qualified names for ambiguous types in code.
import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.animation.animateColorAsState
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.playerid.app.ar.JerseyDetectionManager
import com.playerid.app.ui.composables.PlayerBubblesOverlay
import com.playerid.app.ui.theme.ErrorRed
import com.playerid.app.ui.theme.SpotrSuccessGreen
import com.playerid.app.utils.RecordingManager
import com.playerid.app.utils.RecordingState
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.VoiceAction
import com.playerid.app.viewmodels.VoiceAssistantResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// Helper: Convert jersey color string to Color
fun jerseyColorFromString(color: String?): Color {
    return when (color) {
        "Red" -> Color.Red
        "Blue" -> Color.Blue
        "White" -> Color.White
        "Black" -> Color.Black
        "Green" -> Color.Green
        "Yellow" -> Color.Yellow
        else -> Color.Gray
    }
}
fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    analyzer: com.playerid.app.ar.JerseyDetectionManager,
    recordingManager: com.playerid.app.utils.RecordingManager,
    onCameraReady: (androidx.camera.core.Camera) -> Unit
) {
    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
    val cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    cameraProviderFuture.addListener({
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val recorder = androidx.camera.video.Recorder.Builder().setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HIGHEST)).build()
            val videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)
            recordingManager.setVideoCapture(videoCapture)
            val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture,
                    imageAnalyzer
                )
                onCameraReady(camera)
            } catch (e: Exception) {
                android.util.Log.e("CameraScreen", "Binding failed", e)
            }
        }
    }, cameraExecutor)
// ...existing code...
}

@Composable
fun ModeChip(
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val cameraPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    val trackedPlayersWithInfo by viewModel.detectedPlayersWithInfo.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    var showSelectionSheet by remember { mutableStateOf(false) }
    var selectedJerseyColor by remember { mutableStateOf<String?>(null) }
    var availableJerseyColors by remember { mutableStateOf(listOf<String>()) }
    var selectedOpponent by remember { mutableStateOf("") }
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val currentRoster by viewModel.filteredPlayers.collectAsState()
    val recordingManager = remember { RecordingManager(context) }
    val recordingState by recordingManager.recordingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var liveStartMs by remember { mutableStateOf<Long?>(null) }
    var liveElapsedMs by remember { mutableStateOf(0L) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isStandby by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isCameraReady = false }
    val showCameraOverlay = !isCameraReady && !isStandby
    val capturePastMode by viewModel.capturePastMode.collectAsState()
    var wasCapturePast by remember { mutableStateOf(capturePastMode) }
    var lastManualStop by remember { mutableStateOf(System.currentTimeMillis()) }
    var isAppInBackground by remember { mutableStateOf(false) }
    val recordingCompleteHandler: (Uri, Long) -> Unit = { uri, timestamp ->
        lastManualStop = System.currentTimeMillis()
        onVideoSaved(uri, timestamp)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ -> }
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
    LaunchedEffect(isStandby) { detectionManager.setPaused(isStandby) }
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

    // --- Speech Recognition Integration ---
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isSpeechActive by rememberSaveable { mutableStateOf(false) }
    val lastVoiceListening = remember { mutableStateOf(false) }

    DisposableEffect(isVoiceListening) {
        if (isVoiceListening && !lastVoiceListening.value) {
            // Start speech recognition
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent().apply {
                    action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle) {}
                    override fun onBeginningOfSpeech() { isSpeechActive = true }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray) {}
                    override fun onEndOfSpeech() { isSpeechActive = false }
                    override fun onError(error: Int) {
                        isSpeechActive = false
                        viewModel.clearVoiceResult()
                        onVoiceIdToggle() // turn off listening UI
                    }
                    override fun onResults(results: Bundle) {
                        isSpeechActive = false
                        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()
                        if (!spoken.isNullOrBlank()) {
                            viewModel.processVoiceCommand(spoken)
                        }
                        onVoiceIdToggle() // turn off listening UI
                    }
                    override fun onPartialResults(partialResults: Bundle) {}
                    override fun onEvent(eventType: Int, params: Bundle) {}
                })
                recognizer.startListening(intent)
                speechRecognizer = recognizer
            }
        } else if (!isVoiceListening && lastVoiceListening.value) {
            // Stop speech recognition
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isSpeechActive = false
        }
        lastVoiceListening.value = isVoiceListening
        onDispose {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isSpeechActive = false
        }
    }
    var arMode by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { recordingManager.stopAndDiscardRecording() } }
    if (cameraPermissionsState.allPermissionsGranted) {
        Scaffold(
            containerColor = Color.Black,
            contentColor = Color.White,
            floatingActionButton = {
                if (!isStandby && !showSelectionSheet) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (!isCameraReady) return@FloatingActionButton
                                if (recordingState == RecordingState.RECORDING) {
                                    if (capturePastMode) {
                                        lastManualStop = System.currentTimeMillis()
                                    }
                                    recordingManager.stopRecording()
                                } else if (recordingState == RecordingState.IDLE) {
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
                // Listening window with pulsing mic
                if (isVoiceListening) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .width(120.dp)
                                .height(80.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val pulse = remember { Animatable(1f) }
                                LaunchedEffect(isVoiceListening) {
                                    while (isVoiceListening) {
                                        pulse.animateTo(1.2f, animationSpec = tween(500))
                                        pulse.animateTo(1f, animationSpec = tween(500))
                                    }
                                }
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = if (isSpeechActive) "Listening" else "Mic",
                                    modifier = Modifier.size((40 * pulse.value).dp),
                                    tint = if (isSpeechActive) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                                Text(
                                    if (isSpeechActive) "Listening..." else "Processing...",
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
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
                            startCamera(
                                context = ctx,
                                lifecycleOwner = lifecycleOwner,
                                previewView = view,
                                analyzer = detectionManager,
                                recordingManager = recordingManager
                            ) { cam: Camera ->
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
                if (showCameraOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
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
                                // ...overlay animation code...
                            }
                        }
                    }
                }
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
                if (!isStandby && recordingState != RecordingState.RECORDING && selectedTeam != null) {
                    var pulseUp by remember { mutableStateOf(true) }
                    val pulse: Dp by animateDpAsState(
                        targetValue = if (isVoiceListening && pulseUp) 66.dp else 56.dp,
                        animationSpec = tween(durationMillis = 500), label = "micPulse"
                    )
                    val pulseColor = animateColorAsState(
                        targetValue = if (isVoiceListening && pulseUp) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        animationSpec = tween(durationMillis = 500), label = "micPulseColor"
                    )
                    LaunchedEffect(isVoiceListening) {
                        if (isVoiceListening) {
                            while (isVoiceListening) {
                                pulseUp = !pulseUp
                                delay(500)
                            }
                        } else {
                            pulseUp = true
                        }
                    }
                    FloatingActionButton(
                        onClick = onVoiceIdToggle,
                        containerColor = if (isVoiceListening) pulseColor.value else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(pulse)
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
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.98f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
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
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    color = androidx.compose.ui.graphics.Color.Magenta,
                    modifier = androidx.compose.ui.Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .padding(top = 16.dp, start = 16.dp)
                        .clickable { showSelectionSheet = true }
                ) {
                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        androidx.compose.material3.Text(
                            text = "Team: ${selectedTeam ?: "Select"}",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (!selectedOpponent.isNullOrBlank()) {
                            Text(
                                text = "Opponent: $selectedOpponent",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        if (selectedJerseyColor != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(jerseyColorFromString(selectedJerseyColor), CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Jersey", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
                if (showSelectionSheet) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    ) {
                        // Overlay for outside click
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showSelectionSheet = false }
                        ) {}
                        Surface(
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .heightIn(min = 320.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                var teamExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = teamExpanded,
                                    onExpandedChange = { teamExpanded = !teamExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedTeam ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select team") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = teamExpanded,
                                        onDismissRequest = { teamExpanded = false }
                                    ) {
                                        subscribedTeams.forEach { team ->
                                            DropdownMenuItem(
                                                text = { Text(team.name) },
                                                onClick = {
                                                    viewModel.setSelectedTeam(team.name)
                                                    availableJerseyColors = listOf("Red", "Blue")
                                                    teamExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Jersey Color:", fontWeight = FontWeight.Medium)
                                Row {
                                    availableJerseyColors.forEach { colorName ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(jerseyColorFromString(colorName), CircleShape)
                                                .border(2.dp, if (selectedJerseyColor == colorName) Color.Black else Color.White, CircleShape)
                                                .clickable { selectedJerseyColor = colorName }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = selectedOpponent,
                                    onValueChange = { selectedOpponent = it },
                                    label = { Text("Enter opponent name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { keyboardController?.hide() }) {
                                            Icon(Icons.Default.Done, contentDescription = "Done")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    } else {
        CameraPermissionScreen { cameraPermissionsState.launchMultiplePermissionRequest() }
    }
}

@Composable
fun CameraPermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Camera Access Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text("Grant Permissions") }
    }
}

