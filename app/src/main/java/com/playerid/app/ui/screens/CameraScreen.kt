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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import kotlinx.coroutines.Job
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
    onNavigateToClips: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val modeFabBottomPadding = remember(configuration.screenHeightDp) {
        when {
            configuration.screenHeightDp < 700 -> 124.dp
            configuration.screenHeightDp < 780 -> 112.dp
            else -> 104.dp
        }
    }
    val cameraPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    val trackedPlayersWithInfo by viewModel.detectedPlayersWithInfo.collectAsState()
    val selectedTeamByPlayerVm by viewModel.selectedTeam.collectAsState()
    val selectedTeamByTeamVm by teamViewModel.selectedTeam.collectAsState()
    val selectedTeam = selectedTeamByTeamVm ?: selectedTeamByPlayerVm
    var showSelectionSheet by remember { mutableStateOf(false) }
    var selectedJerseyColor by remember { mutableStateOf<String?>(null) }
    var selectedJerseyType by remember { mutableStateOf<String?>(null) }
    var selectedOpponent by remember { mutableStateOf("") }
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeamMeta = remember(subscribedTeams, selectedTeam) {
        subscribedTeams.firstOrNull { it.name == selectedTeam }
    }
    val teamJerseyOptions = remember(selectedTeamMeta) {
        listOf(
            "Home" to (selectedTeamMeta?.homeJerseyColor ?: selectedTeamMeta?.color ?: "#1976D2"),
            "Away" to (selectedTeamMeta?.awayJerseyColor ?: selectedTeamMeta?.awayColor ?: "#FFFFFF")
        )
    }
    val teamPrimary = parseCameraScreenColor(selectedTeamMeta?.color, Color(0xFF1976D2))
    val teamSecondary = parseCameraScreenColor(selectedTeamMeta?.awayColor, Color(0xFFE3F2FD))
    val teamJerseyColor = parseCameraScreenColor(
        selectedJerseyColor ?: teamJerseyOptions.firstOrNull()?.second,
        jerseyColorFromString(selectedJerseyColor)
    )
    val teamJerseyLabel = when (selectedJerseyType) {
        "Away" -> "Away Jersey"
        else -> "Home Jersey"
    }
    val onTeamPrimary = if (teamPrimary.luminance() > 0.55f) Color.Black else Color.White
    val onTeamSecondary = if (teamSecondary.luminance() > 0.55f) Color.Black else Color.White
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
    LaunchedEffect(selectedTeamByTeamVm, selectedTeamByPlayerVm) {
        when {
            !selectedTeamByTeamVm.isNullOrBlank() && selectedTeamByPlayerVm != selectedTeamByTeamVm -> {
                viewModel.setSelectedTeam(selectedTeamByTeamVm)
            }
            selectedTeamByTeamVm.isNullOrBlank() && !selectedTeamByPlayerVm.isNullOrBlank() -> {
                val fallbackTeam = selectedTeamByPlayerVm ?: return@LaunchedEffect
                teamViewModel.selectTeam(fallbackTeam)
            }
        }
    }
    val showCameraOverlay = !isCameraReady && !isStandby
    val capturePastMode by viewModel.capturePastMode.collectAsState()
    var wasCapturePast by remember { mutableStateOf(capturePastMode) }
    var lastManualStop by remember { mutableStateOf(System.currentTimeMillis()) }
    var isAppInBackground by remember { mutableStateOf(false) }

    val onRecordingSaved: (Uri?) -> Unit = { uri ->
        scope.launch {
            val clipTeam = selectedTeam
            val clipStartTime = recordingManager.getLastRecordingStartTimeMs()
            if (uri != null && !clipTeam.isNullOrBlank()) {
                persistClipTeamMetadata(
                    context = context,
                    videoUri = uri,
                    teamName = clipTeam,
                    startedAtMs = clipStartTime,
                    opponentName = selectedOpponent
                )
            }
            val teamLabel = selectedTeam ?: "selected team"
            val result = snackbarHostState.showSnackbar(
                message = "Saved to $teamLabel",
                actionLabel = "View Clip",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onNavigateToClips()
            }
        }
    }
    LaunchedEffect(selectedTeamMeta?.name) {
        val defaultJersey = teamJerseyOptions.firstOrNull()
        selectedJerseyType = defaultJersey?.first
        selectedJerseyColor = defaultJersey?.second
    }
    // Removed obsolete recordingCompleteHandler and onVideoSaved usage
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

    // Capture Past should keep a rolling recording active so a tap saves prior moments.
    LaunchedEffect(capturePastMode, isCameraReady, recordingState) {
        if (capturePastMode && isCameraReady && recordingState == RecordingState.IDLE) {
            recordingManager.startRecording(onRecordingSaved)
        }
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
    var lastSpokenText by remember { mutableStateOf("") }
    var recognitionError by remember { mutableStateOf<String?>(null) }
    var arMode by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { recordingManager.stopAndDiscardRecording() } }

    // Setup SpeechRecognizer and RecognitionListener
    LaunchedEffect(Unit) {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }
    DisposableEffect(speechRecognizer) {
        val recognizer = speechRecognizer
        if (recognizer != null) {
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("CameraScreen", "SpeechRecognizer: Ready for speech")
                    isSpeechActive = true
                }
                override fun onBeginningOfSpeech() {
                    Log.d("CameraScreen", "SpeechRecognizer: Beginning of speech")
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d("CameraScreen", "SpeechRecognizer: End of speech")
                    isSpeechActive = false
                }
                override fun onError(error: Int) {
                    val msg = "SpeechRecognizer error: $error"
                    Log.e("CameraScreen", msg)
                    recognitionError = msg
                    isSpeechActive = false
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim() ?: ""
                    Log.d("CameraScreen", "SpeechRecognizer: onResults: $spoken")
                    lastSpokenText = spoken
                    if (spoken.isNotBlank()) {
                        viewModel.processVoiceCommand(spoken)
                    }
                    isSpeechActive = false
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            recognizer.setRecognitionListener(listener)
        }
        onDispose {
            recognizer?.setRecognitionListener(null)
            recognizer?.destroy()
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
            Log.d("CameraScreen", "SpeechRecognizer: startListening() called")
            recognitionError = null
        } catch (e: Exception) {
            recognitionError = "Failed to start listening: ${e.message}"
            Log.e("CameraScreen", "SpeechRecognizer: Exception: ${e.message}")
        }
    }
    if (cameraPermissionsState.allPermissionsGranted) {
        Scaffold(
            containerColor = Color.Black,
            contentColor = Color.White,
            floatingActionButton = {
                if (!isStandby && !showSelectionSheet) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Record button (existing)
                        FloatingActionButton(
                            onClick = {
                                if (!isCameraReady) return@FloatingActionButton
                                if (recordingState == RecordingState.RECORDING) {
                                    if (capturePastMode) {
                                        lastManualStop = System.currentTimeMillis()
                                    }
                                    recordingManager.stopRecording()
                                } else if (recordingState == RecordingState.IDLE) {
                                    recordingManager.startRecording(onRecordingSaved)
                                }
                            },
                            modifier = Modifier,
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
                // Removed obsolete listening window with pulsing mic
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
                    }, Modifier.fillMaxSize()
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
                            teamSecondary.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = modeFabBottomPadding)
                            .size(64.dp)
                    ) {
                        Icon(
                            if (capturePastMode) Icons.Default.History else Icons.Default.Videocam,
                            contentDescription = if (capturePastMode) "Capture Past" else "Capture Moment",
                            modifier = Modifier.size(32.dp),
                            tint = if (capturePastMode) {
                                onTeamSecondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                // Removed obsolete mic button and pulse logic
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
                    color = teamPrimary,
                    modifier = androidx.compose.ui.Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .padding(top = 16.dp, start = 16.dp)
                        .clickable { showSelectionSheet = true }
                ) {
                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        androidx.compose.material3.Text(
                            text = selectedTeam ?: "Select Team",
                            color = onTeamPrimary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (!selectedOpponent.isNullOrBlank()) {
                            Text(
                                text = "Opponent: $selectedOpponent",
                                color = onTeamPrimary.copy(alpha = 0.9f),
                                fontSize = 16.sp
                            )
                        }
                        if (selectedTeam != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(teamJerseyColor, CircleShape)
                                        .border(2.dp, onTeamPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(teamJerseyLabel, color = onTeamPrimary, fontSize = 16.sp)
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
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = teamPrimary,
                                            unfocusedBorderColor = teamPrimary.copy(alpha = 0.45f),
                                            focusedLabelColor = teamPrimary,
                                            unfocusedLabelColor = teamPrimary.copy(alpha = 0.75f),
                                            cursorColor = teamPrimary
                                        ),
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
                                                    teamViewModel.selectTeam(team.name)
                                                    teamExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Jersey Color:", fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    teamJerseyOptions.forEach { (label, colorHex) ->
                                        val swatch = parseCameraScreenColor(colorHex, Color.Gray)
                                        OutlinedButton(
                                            onClick = {
                                                selectedJerseyType = label
                                                selectedJerseyColor = colorHex
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                2.dp,
                                                if (selectedJerseyType == label) teamPrimary else Color.White
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(swatch, CircleShape)
                                                        .border(1.dp, Color.White, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(label)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = selectedOpponent,
                                    onValueChange = { selectedOpponent = it },
                                    label = { Text("Enter opponent name") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = teamPrimary,
                                        unfocusedBorderColor = teamPrimary.copy(alpha = 0.45f),
                                        focusedLabelColor = teamPrimary,
                                        unfocusedLabelColor = teamPrimary.copy(alpha = 0.75f),
                                        cursorColor = teamPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { keyboardController?.hide() }) {
                                            Icon(Icons.Default.Done, contentDescription = "Done", tint = teamPrimary)
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

private fun parseCameraScreenColor(raw: String?, fallback: Color): Color {
    if (raw.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(raw))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}

private fun persistClipTeamMetadata(
    context: Context,
    videoUri: Uri,
    teamName: String,
    startedAtMs: Long,
    opponentName: String?
) {
    val teamPrefs = context.getSharedPreferences("video_team_names", Context.MODE_PRIVATE)
    val startPrefs = context.getSharedPreferences("video_start_times", Context.MODE_PRIVATE)
    val opponentPrefs = context.getSharedPreferences("video_opponent_names", Context.MODE_PRIVATE)
    val cleanedOpponent = opponentName?.trim().orEmpty()

    val uriKey = videoUri.toString()
    teamPrefs.edit().putString(uriKey, teamName).apply()
    if (startedAtMs > 0L) {
        startPrefs.edit().putLong(uriKey, startedAtMs).apply()
    }
    if (cleanedOpponent.isNotEmpty()) {
        opponentPrefs.edit().putString(uriKey, cleanedOpponent).apply()
    } else {
        opponentPrefs.edit().remove(uriKey).apply()
    }

    if (videoUri.scheme == "file") {
        val pathKey = videoUri.path
        if (!pathKey.isNullOrBlank()) {
            teamPrefs.edit().putString(pathKey, teamName).apply()
            if (startedAtMs > 0L) {
                startPrefs.edit().putLong(pathKey, startedAtMs).apply()
            }
            if (cleanedOpponent.isNotEmpty()) {
                opponentPrefs.edit().putString(pathKey, cleanedOpponent).apply()
            } else {
                opponentPrefs.edit().remove(pathKey).apply()
            }
        }
    }
}

