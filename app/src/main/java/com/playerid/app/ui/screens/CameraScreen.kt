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
import com.playerid.app.data.Player
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import com.playerid.app.video.VideoProcessingManager
import com.playerid.app.video.VideoSharePreparationCache
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.VoiceAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.concurrent.Executors
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

enum class CameraFeature {
    PHOTO,
    VIDEO,
    CAPTURE_PAST
}

fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    analyzer: com.playerid.app.ar.JerseyDetectionManager,
    recordingManager: com.playerid.app.utils.RecordingManager,
    onCameraReady: (androidx.camera.core.Camera, androidx.camera.core.ImageCapture) -> Unit
) {
    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
    val cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    cameraProviderFuture.addListener({
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val recorder = androidx.camera.video.Recorder.Builder()
                .setQualitySelector(
                    androidx.camera.video.QualitySelector.fromOrderedList(
                        listOf(
                            androidx.camera.video.Quality.FHD,
                            androidx.camera.video.Quality.HD,
                            androidx.camera.video.Quality.SD
                        )
                    )
                )
                .build()
            val videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)
            val imageCapture = androidx.camera.core.ImageCapture.Builder()
                .setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
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
                    imageCapture,
                    imageAnalyzer
                )
                onCameraReady(camera, imageCapture)
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
    val cameraPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    val trackedPlayersWithInfo by viewModel.detectedPlayersWithInfo.collectAsState()
    val selectedTeamByTeamVm by teamViewModel.selectedTeam.collectAsState()
    val selectedTeam = selectedTeamByTeamVm
    var showSelectionSheet by remember { mutableStateOf(false) }
    var selectedJerseyColor by remember { mutableStateOf<String?>(null) }
    var selectedJerseyType by remember { mutableStateOf<String?>(null) }
    var selectedOpponent by rememberSaveable { mutableStateOf("") }
    val cameraPrefs = remember(context) {
        context.getSharedPreferences("camera_preferences", Context.MODE_PRIVATE)
    }
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeamMeta = remember(subscribedTeams, selectedTeam) {
        subscribedTeams.firstOrNull { it.name == selectedTeam }
    }
    val teamJerseyOptions = remember(selectedTeamMeta) {
        if (selectedTeamMeta?.name == "North Allegheny Lacrosse") {
            listOf(
                "Black" to "#000000",
                "White" to "#FFFFFF",
                "Gold" to "#FFB81C"
            )
        } else {
            listOf(
                "Home" to (selectedTeamMeta?.homeJerseyColor ?: selectedTeamMeta?.color ?: "#1976D2"),
                "Away" to (selectedTeamMeta?.awayJerseyColor ?: selectedTeamMeta?.awayColor ?: "#FFFFFF")
            )
        }
    }
    val teamPrimary = parseCameraScreenColor(selectedTeamMeta?.color, Color(0xFF1976D2))
    val teamJerseyColor = parseCameraScreenColor(
        selectedJerseyColor ?: teamJerseyOptions.firstOrNull()?.second,
        jerseyColorFromString(selectedJerseyColor)
    )
    val currentRoster by viewModel.filteredPlayers.collectAsState()
    val allPlayersForShare by viewModel.allPlayers.collectAsState(initial = emptyList())
    val shareRosterPlayers = remember(allPlayersForShare, selectedTeam) {
        if (selectedTeam != null) {
            allPlayersForShare
                .filter { it.team == selectedTeam }
                .sortedWith(
                    compareBy<Player>(
                        { it.number.toIntOrNull() ?: Int.MAX_VALUE },
                        { it.number },
                        { it.name }
                    )
                )
        } else {
            emptyList()
        }
    }
    val recordingManager = remember { RecordingManager(context) }
    val recordingState by recordingManager.recordingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var liveStartMs by remember { mutableStateOf<Long?>(null) }
    var liveElapsedMs by remember { mutableStateOf(0L) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isStandby by remember { mutableStateOf(false) }
    var postCapturePromptUri by remember { mutableStateOf<Uri?>(null) }
    var showPostCaptureShareDialog by remember { mutableStateOf(false) }
    var isPostCaptureShareDismissed by remember { mutableStateOf(false) }
    var shareTileDragOffsetX by remember { mutableStateOf(0f) }
    var shareSelectedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSharePlayerList by remember { mutableStateOf(false) }
    var showManualShareOptions by remember { mutableStateOf(false) }
    val shareContactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val savedUri = postCapturePromptUri
            if (savedUri != null) {
                val contact = readSelectedContact(context, contactUri)
                if (contact != null) {
                    shareVideoToPhoneContact(context, savedUri, emptyList(), contact)
                    showPostCaptureShareDialog = false
                    postCapturePromptUri = null
                }
            }
        }
    }
    var imageCapture by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }
    var selectedCameraFeature by rememberSaveable { mutableStateOf(CameraFeature.VIDEO) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    LaunchedEffect(showPostCaptureShareDialog) {
        if (showPostCaptureShareDialog) {
            shareSelectedPlayerIds = emptySet()
            showSharePlayerList = false
            showManualShareOptions = false
        }
    }
    LaunchedEffect(Unit) { isCameraReady = false }
    LaunchedEffect(selectedTeam) {
        val teamKey = selectedTeam?.trim().orEmpty().ifEmpty { "__no_team__" }
        selectedOpponent = cameraPrefs.getString("last_opponent_$teamKey", "").orEmpty()
    }
    LaunchedEffect(selectedTeam, selectedOpponent) {
        val teamKey = selectedTeam?.trim().orEmpty().ifEmpty { "__no_team__" }
        cameraPrefs.edit().putString("last_opponent_$teamKey", selectedOpponent.trim()).apply()
    }
    val showCameraOverlay = !isCameraReady && !isStandby
    val capturePastMode by viewModel.capturePastMode.collectAsState()
    LaunchedEffect(capturePastMode) {
        if (capturePastMode) {
            selectedCameraFeature = CameraFeature.CAPTURE_PAST
        } else if (selectedCameraFeature == CameraFeature.CAPTURE_PAST) {
            selectedCameraFeature = CameraFeature.VIDEO
        }
    }
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

            if (uri != null) {
                postCapturePromptUri = uri
                showPostCaptureShareDialog = false
                isPostCaptureShareDismissed = false
                shareTileDragOffsetX = 0f
                launch {
                    try {
                        // Run background FAST detection
                        val videoProcessingManager = com.playerid.app.video.VideoProcessingManager(context)
                        val analysisResult = videoProcessingManager.autoDetectPlayersWithTracksInVideo(
                            videoUri = uri,
                            roster = currentRoster,
                            mode = com.playerid.app.video.VideoProcessingManager.DetectionMode.FAST
                        )
                        
                        // Persist result to database for future plays
                        val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                        val dao = database.videoDetectionResultDao()
                        val detectionJson = com.playerid.app.data.DetectionResultSerializer.serialize(analysisResult)
                        dao.insertDetectionResult(
                            com.playerid.app.data.VideoDetectionResultEntity(
                                videoUri = uri.toString(),
                                detectionMode = "FAST",
                                detectionJson = detectionJson,
                                detectionTimestampMs = System.currentTimeMillis()
                            )
                        )
                        
                        // Also cache in memory for this session
                        VideoSharePreparationCache.set(
                            uri,
                            com.playerid.app.video.PreparedShareResult(
                                analysisResult = analysisResult,
                                preparedAtMs = System.currentTimeMillis(),
                                mode = com.playerid.app.video.VideoProcessingManager.DetectionMode.FAST
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("CameraScreen", "Background detection failed: ${e.message}", e)
                    }
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = "Clip saved",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
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
    // Track physical device orientation so VideoCapture gets correct rotation metadata
    // even when the activity is locked to portrait.
    DisposableEffect(Unit) {
        val orientationListener = object : android.view.OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when {
                    orientation <= 45 || orientation > 315 -> android.view.Surface.ROTATION_0
                    orientation in 46..134 -> android.view.Surface.ROTATION_270
                    orientation in 135..224 -> android.view.Surface.ROTATION_180
                    else -> android.view.Surface.ROTATION_90
                }
                recordingManager.setTargetRotation(rotation)
            }
        }
        orientationListener.enable()
        onDispose { orientationListener.disable() }
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
                    FloatingActionButton(
                            onClick = {
                                if (!isCameraReady || isCapturingPhoto) return@FloatingActionButton
                                when (selectedCameraFeature) {
                                    CameraFeature.PHOTO -> {
                                        val capture = imageCapture ?: return@FloatingActionButton
                                        isCapturingPhoto = true
                                        takeStillPhoto(
                                            context = context,
                                            imageCapture = capture,
                                            onSaved = {
                                                isCapturingPhoto = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Photo saved",
                                                        withDismissAction = true,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            },
                                            onError = { message ->
                                                isCapturingPhoto = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        withDismissAction = true,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    CameraFeature.VIDEO -> {
                                        if (recordingState == RecordingState.RECORDING) {
                                            recordingManager.stopRecording()
                                        } else if (recordingState == RecordingState.IDLE) {
                                            recordingManager.startRecording(onRecordingSaved)
                                        }
                                    }

                                    CameraFeature.CAPTURE_PAST -> {
                                        if (!capturePastMode) {
                                            viewModel.setCapturePastMode(true)
                                        } else if (recordingState == RecordingState.RECORDING) {
                                            lastManualStop = System.currentTimeMillis()
                                            recordingManager.stopRecording()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier,
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                        ) {
                            val recordAlpha = if (isCameraReady && !isCapturingPhoto) 1f else 0.45f
                            val innerColor = when (selectedCameraFeature) {
                                CameraFeature.PHOTO -> Color.White
                                CameraFeature.VIDEO -> Color.Red
                                CameraFeature.CAPTURE_PAST -> MaterialTheme.colorScheme.secondaryContainer
                            }
                            val innerSize by animateDpAsState(
                                targetValue = when {
                                    selectedCameraFeature == CameraFeature.PHOTO -> 30.dp
                                    isRecording -> 24.dp
                                    else -> 38.dp
                                },
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
                                        .background(innerColor, CircleShape)
                                )
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
                            ) { cam: Camera, imageCaptureUseCase: androidx.camera.core.ImageCapture ->
                                camera = cam
                                imageCapture = imageCaptureUseCase
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
                // Removed obsolete mic button and pulse logic
                if (arMode && !isStandby) {
                    PlayerBubblesOverlay(
                        trackedPlayers = trackedPlayersWithInfo,
                        processing = processing,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isStandby && !showSelectionSheet) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CameraFeatureActionButton(
                                icon = Icons.Default.PhotoCamera,
                                label = "Photo",
                                selected = selectedCameraFeature == CameraFeature.PHOTO,
                                enabled = !isLiveRecording,
                                selectedColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    if (capturePastMode) {
                                        viewModel.setCapturePastMode(false)
                                    }
                                    selectedCameraFeature = CameraFeature.PHOTO
                                }
                            )
                            CameraFeatureActionButton(
                                icon = Icons.Default.Videocam,
                                label = "Video",
                                selected = selectedCameraFeature == CameraFeature.VIDEO,
                                enabled = !isLiveRecording,
                                selectedColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    if (capturePastMode) {
                                        viewModel.setCapturePastMode(false)
                                    }
                                    selectedCameraFeature = CameraFeature.VIDEO
                                }
                            )
                            CameraFeatureActionButton(
                                icon = Icons.Default.History,
                                label = "Capture Past",
                                selected = selectedCameraFeature == CameraFeature.CAPTURE_PAST,
                                enabled = !isLiveRecording,
                                selectedColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    selectedCameraFeature = CameraFeature.CAPTURE_PAST
                                    if (!capturePastMode) {
                                        viewModel.setCapturePastMode(true)
                                    }
                                }
                            )
                        }
                    }
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    modifier = androidx.compose.ui.Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .padding(top = 16.dp, start = 16.dp)
                        .clickable { showSelectionSheet = true }
                ) {
                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Text(
                                text = selectedTeam ?: "Select Team",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (selectedTeam != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(teamJerseyColor, CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                                )
                            }
                        }
                        if (!selectedOpponent.isNullOrBlank()) {
                            Text(
                                text = "vs $selectedOpponent",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                postCapturePromptUri?.let { savedUri ->
                    if (!isPostCaptureShareDismissed) {
                    val clipThumbnail by rememberCameraClipThumbnail(context = context, videoUri = savedUri)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        tonalElevation = 6.dp,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 28.dp)
                            .offset { IntOffset(shareTileDragOffsetX.roundToInt(), 0) }
                            .pointerInput(savedUri) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount ->
                                        shareTileDragOffsetX += dragAmount
                                    },
                                    onDragEnd = {
                                        if (abs(shareTileDragOffsetX) > 80f) {
                                            isPostCaptureShareDismissed = true
                                            showPostCaptureShareDialog = false
                                        }
                                        shareTileDragOffsetX = 0f
                                    },
                                    onDragCancel = {
                                        shareTileDragOffsetX = 0f
                                    }
                                )
                            }
                            .clickable { showPostCaptureShareDialog = true }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                            IconButton(
                                onClick = {
                                    isPostCaptureShareDismissed = true
                                    showPostCaptureShareDialog = false
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (clipThumbnail != null) {
                                        Image(
                                            bitmap = clipThumbnail!!.asImageBitmap(),
                                            contentDescription = "Latest clip thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                                Text(
                                    text = "Share Last",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = teamPrimary
                                )
                            }
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
                                                if (selectedJerseyType == label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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

    if (showPostCaptureShareDialog) {
        val savedUri = postCapturePromptUri ?: return
        Dialog(
            onDismissRequest = { showPostCaptureShareDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Share clip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // ── Team Parents ────────────────────────────────────
                    Text(
                        "Team Parents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (shareRosterPlayers.isEmpty()) {
                        Text(
                            "No players on roster yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Collapsible player list header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSharePlayerList = !showSharePlayerList },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (shareSelectedPlayerIds.isEmpty()) "Choose players"
                                else "${shareSelectedPlayerIds.size} player${if (shareSelectedPlayerIds.size == 1) "" else "s"} selected",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                if (showSharePlayerList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (showSharePlayerList) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = {
                                shareSelectedPlayerIds = shareRosterPlayers
                                    .filter { it.addedBy.any(Char::isDigit) && it.addedBy.filter(Char::isDigit).length >= 10 }
                                    .map { it.id }
                                    .toSet()
                            }) {
                                Text("Select all")
                            }
                            if (shareSelectedPlayerIds.isNotEmpty()) {
                                TextButton(onClick = { shareSelectedPlayerIds = emptySet() }) {
                                    Text("Clear")
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            shareRosterPlayers.forEach { player ->
                                val hasParentContact = player.addedBy.any(Char::isDigit) &&
                                    player.addedBy.filter(Char::isDigit).length >= 10
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(if (hasParentContact) 1f else 0.4f)
                                        .clickable(enabled = hasParentContact) {
                                            shareSelectedPlayerIds = if (shareSelectedPlayerIds.contains(player.id)) {
                                                shareSelectedPlayerIds - player.id
                                            } else {
                                                shareSelectedPlayerIds + player.id
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasParentContact) {
                                        Checkbox(
                                            checked = shareSelectedPlayerIds.contains(player.id),
                                            onCheckedChange = { checked ->
                                                shareSelectedPlayerIds = if (checked) {
                                                    shareSelectedPlayerIds + player.id
                                                } else {
                                                    shareSelectedPlayerIds - player.id
                                                }
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(48.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "#${player.number} ${player.name}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                    }
                                }
                            }
                        }
                        } // end if showSharePlayerList
                        val selectedPlayers = shareRosterPlayers.filter { shareSelectedPlayerIds.contains(it.id) }
                        if (shareSelectedPlayerIds.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val recipients = buildTeamShareRecipients(selectedPlayers)
                                    if (recipients.isEmpty()) {
                                        launchPersonalShareChooser(context, savedUri, "Share clip")
                                    } else {
                                        shareVideoToTeamRecipients(
                                            context = context,
                                            videoUri = savedUri,
                                            recipients = recipients,
                                            players = selectedPlayers,
                                            highlightTag = null,
                                            customMessage = ""
                                        )
                                    }
                                    showPostCaptureShareDialog = false
                                    postCapturePromptUri = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Send to ${shareSelectedPlayerIds.size} parent${if (shareSelectedPlayerIds.size == 1) "" else "s"}"
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // ── My Contacts ──────────────────────────────────────
                    Text(
                        "My Contacts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { shareContactPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick contact from phone")
                    }

                    TextButton(
                        onClick = { showPostCaptureShareDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberCameraClipThumbnail(context: Context, videoUri: Uri) = produceState<android.graphics.Bitmap?>(
    initialValue = null,
    key1 = videoUri
) {
    value = withContext(Dispatchers.IO) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            retriever.getFrameAtTime(800_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime()
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
private fun CameraFeatureActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    selectedColor: Color,
    selectedContentColor: Color,
    onClick: () -> Unit
) {
    val containerColor = if (selected) selectedColor else Color.Transparent
    val contentColor = if (selected) selectedContentColor else Color.White
    val alpha = if (enabled) 1f else 0.45f

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.alpha(alpha)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        }
    }
}

private fun takeStillPhoto(
    context: Context,
    imageCapture: androidx.camera.core.ImageCapture,
    onSaved: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val name = "Spotr-Photo-" + java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", java.util.Locale.US)
        .format(System.currentTimeMillis()) + ".jpg"
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PlayerID")
    }
    val outputOptions = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: androidx.camera.core.ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                if (savedUri != null) {
                    onSaved(savedUri)
                } else {
                    onError("Photo saved, but location was unavailable")
                }
            }

            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                Log.e("CameraScreen", "Still photo capture failed", exception)
                onError("Photo failed: ${exception.message ?: "Unknown error"}")
            }
        }
    )
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

