@file:OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.camera.core.ExperimentalGetImage::class
)
package com.playerid.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

// Only keep unambiguous, non-conflicting imports. Use fully qualified names for ambiguous types in code.
import android.Manifest
import android.app.Activity
import android.content.Context
import android.provider.MediaStore
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.playerid.app.R
import com.playerid.app.ar.JerseyDetectionManager
import com.playerid.app.ui.composables.PlayerBubblesOverlay
import com.playerid.app.ui.theme.ErrorRed
import com.playerid.app.ui.theme.SpotrPrimaryBlue
import com.playerid.app.ui.theme.SpotrSuccessGreen
import com.playerid.app.utils.performRecordButtonPressHaptic
import com.playerid.app.utils.performRecordingCapturedDoubleHaptic
import com.playerid.app.utils.RecordingManager
import com.playerid.app.utils.RecordingState
import com.playerid.app.video.VideoProcessingManager
import com.playerid.app.video.VideoSharePreparationCache
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.VoiceAction
import com.playerid.app.viewmodels.VoiceAssistantResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import java.util.concurrent.Executors
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    onNavigateToClips: () -> Unit = {},
    onNavigateToTeams: () -> Unit = {}
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
    var opponentManuallyEdited by remember { mutableStateOf(false) }
    var opponentTeamInitialized by remember { mutableStateOf<String?>(null) }
    var opponentSuggestionsExpanded by remember { mutableStateOf(false) }
    val scheduledGames by remember(selectedTeam) {
        teamViewModel.getGamesForTeam(selectedTeam.orEmpty())
    }.collectAsState(initial = emptyList())
    val knownOpponents = remember(context) {
        context.getSharedPreferences("video_opponent_names", Context.MODE_PRIVATE)
            .all
            .values
            .filterIsInstance<String>()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }
    val matchingOpponents = remember(selectedOpponent, knownOpponents) {
        val query = selectedOpponent.trim()
        if (query.isBlank()) emptyList() else knownOpponents.filter { opponent ->
            opponent.equals(query, ignoreCase = true) || opponent.startsWith(query, ignoreCase = true)
        }.sortedBy { it.length }.take(5)
    }
    LaunchedEffect(selectedOpponent, knownOpponents) {
        val query = selectedOpponent.trim()
        if (query.isBlank()) {
            opponentSuggestionsExpanded = false
            return@LaunchedEffect
        }
        val exactPrefixMatches = knownOpponents.filter { it.startsWith(query, ignoreCase = true) }
        if (exactPrefixMatches.size == 1) {
            val match = exactPrefixMatches.first()
            if (match != query) {
                selectedOpponent = match
            }
            opponentSuggestionsExpanded = true
        } else {
            opponentSuggestionsExpanded = matchingOpponents.isNotEmpty()
        }
    }
    val kidOptions by teamViewModel.kidOptions.collectAsState()
    var selectedKid by rememberSaveable { mutableStateOf("Tyson") }
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
    val teamPrimary = parseCameraScreenColor(selectedTeamMeta?.color, SpotrPrimaryBlue)
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
    var postCapturePromptUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showPostCaptureShareDialog by rememberSaveable { mutableStateOf(false) }
    var isPostCaptureShareDismissed by rememberSaveable { mutableStateOf(false) }
    var shareTileDragOffsetX by remember { mutableStateOf(0f) }
    var selectedMomentTag by remember { mutableStateOf<MomentTag?>(null) }
    var shareSelectedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var suggestedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var scanSuggestionsDone by remember { mutableStateOf(false) }
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

    // Roster Drawer
    val voiceResult by viewModel.voiceResult.collectAsState()
    var rosterDrawerOpen by remember { mutableStateOf(false) }
    var rosterDrawerFull by remember { mutableStateOf(false) }
    var rosterSearchQuery by remember { mutableStateOf("") }
    var rosterHighlightedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var rosterVoiceBubble by remember { mutableStateOf<String?>(null) }
    var rosterSelectedPlayer by remember { mutableStateOf<com.playerid.app.data.Player?>(null) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val rosterPeekHeight = screenHeight * 0.35f
    val rosterFullHeight = screenHeight * 0.87f
    val cameraViewportBottomPadding by animateDpAsState(
        targetValue = when {
            !rosterDrawerOpen -> 0.dp
            rosterDrawerFull -> rosterFullHeight
            else -> rosterPeekHeight
        },
        animationSpec = tween(durationMillis = 220),
        label = "cameraViewportBottomPadding"
    )
    LaunchedEffect(voiceResult) {
        val r = voiceResult ?: return@LaunchedEffect
        rosterHighlightedIds = emptySet()
        when (r) {
            is VoiceAssistantResult.Success -> {
                val ids = buildSet<String> {
                    r.player?.let { add(it.id) }
                    r.players.forEach { add(it.id) }
                }
                rosterHighlightedIds = ids
                rosterSelectedPlayer = r.player ?: r.players.firstOrNull()
                rosterVoiceBubble = r.message
            }
            is VoiceAssistantResult.Error -> {
                rosterVoiceBubble = r.message
            }
        }
        delay(4000)
        rosterVoiceBubble = null
        rosterHighlightedIds = emptySet()
    }

    fun capturePhoto() {
        if (!isCameraReady || isCapturingPhoto) return
        val capture = imageCapture ?: return
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

    LaunchedEffect(showPostCaptureShareDialog) {
        if (showPostCaptureShareDialog) {
            shareSelectedPlayerIds = emptySet()
            suggestedPlayerIds = emptySet()
            showSharePlayerList = false
            showManualShareOptions = false
            scanSuggestionsDone = false
            
            // Trigger player detection with FAST mode
            if (postCapturePromptUri != null && shareRosterPlayers.isNotEmpty()) {
                isLoadingSuggestions = true
                val detectionJob = scope.launch(Dispatchers.Default) {
                    try {
                        val videoProcessingManager = VideoProcessingManager(context)
                        val detectionResult = videoProcessingManager.autoDetectPlayersWithTracksInVideo(
                            videoUri = postCapturePromptUri!!,
                            roster = shareRosterPlayers,
                            mode = VideoProcessingManager.DetectionMode.FAST
                        )
                        
                        // Match detected jersey numbers with roster players
                        val detected = detectionResult.bubbles.mapNotNull { bubble ->
                            shareRosterPlayers.find { it.number == bubble.jerseyNumber }?.id
                        }.toSet()
                        
                        // Pre-select suggested players
                        suggestedPlayerIds = detected
                        if (detected.isNotEmpty()) {
                            shareSelectedPlayerIds = detected
                        }
                        
                        videoProcessingManager.release()
                    } catch (e: Exception) {
                        Log.d("CameraScreen", "Player detection failed: ${e.message}")
                    } finally {
                        isLoadingSuggestions = false
                                scanSuggestionsDone = true
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { isCameraReady = false }
    LaunchedEffect(selectedTeam) {
        viewModel.setSelectedTeam(selectedTeam)
        val teamKey = selectedTeam?.trim().orEmpty().ifEmpty { "__no_team__" }
        opponentManuallyEdited = false
        opponentTeamInitialized = selectedTeam
        val savedOpponent = cameraPrefs.getString("last_opponent_$teamKey", "").orEmpty()
        val savedAtMs = cameraPrefs.getLong("last_opponent_updated_$teamKey", 0L)
        selectedOpponent = if (shouldRestoreSavedOpponent(savedOpponent, savedAtMs, System.currentTimeMillis())) {
            savedOpponent
        } else {
            ""
        }
        selectedKid = teamViewModel.getSelectedKidForTeam(selectedTeam)
    }
    LaunchedEffect(selectedTeam, scheduledGames, opponentTeamInitialized) {
        if (selectedTeam != null && opponentTeamInitialized == selectedTeam && !opponentManuallyEdited) {
            findCurrentScheduledGame(scheduledGames, System.currentTimeMillis())?.let { game ->
                selectedOpponent = game.opponentName
            }
        }
    }
    LaunchedEffect(selectedTeam, selectedOpponent) {
        val teamKey = selectedTeam?.trim().orEmpty().ifEmpty { "__no_team__" }
        val opponent = selectedOpponent.trim()
        cameraPrefs.edit().apply {
            if (opponent.isEmpty()) {
                remove("last_opponent_$teamKey")
                remove("last_opponent_updated_$teamKey")
            } else {
                putString("last_opponent_$teamKey", opponent)
                putLong("last_opponent_updated_$teamKey", System.currentTimeMillis())
            }
        }.apply()
    }
    LaunchedEffect(selectedTeam, selectedKid) {
        teamViewModel.selectKidForTeam(selectedTeam, selectedKid)
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
            var resolvedUri = uri?.takeIf { isRecordedVideoUriReadable(context, it) }
            if (resolvedUri == null) {
                resolvedUri = resolveRecentRecordedVideoUri(context, clipStartTime)
                if (resolvedUri != null) {
                    Log.w("CameraScreen", "Recovered missing/unreadable finalize URI via MediaStore lookup: $resolvedUri")
                }
            }

            if (resolvedUri != null) {
                performRecordingCapturedDoubleHaptic(context)
            }
            if (resolvedUri != null && !clipTeam.isNullOrBlank()) {
                persistClipTeamMetadata(
                    context = context,
                    videoUri = resolvedUri,
                    teamName = clipTeam,
                    startedAtMs = clipStartTime,
                    opponentName = selectedOpponent,
                    kidName = selectedKid
                )
            }

            if (resolvedUri != null) {
                Log.d("CameraScreen", "OPENING_POST_CAPTURE_PROMPT uri=$resolvedUri")
                postCapturePromptUri = resolvedUri
                selectedMomentTag = null
                showPostCaptureShareDialog = true
                Log.d("CameraScreen", "POST_CAPTURE_STATE_SET showDialog=${showPostCaptureShareDialog} uri=${postCapturePromptUri}")
                isPostCaptureShareDismissed = false
                shareTileDragOffsetX = 0f
                launch {
                    try {
                        // Run background FAST detection
                        val videoProcessingManager = com.playerid.app.video.VideoProcessingManager(context)
                        val analysisResult = videoProcessingManager.autoDetectPlayersWithTracksInVideo(
                            videoUri = resolvedUri,
                            roster = currentRoster,
                            mode = com.playerid.app.video.VideoProcessingManager.DetectionMode.FAST
                        )
                        
                        // Persist result to database for future plays
                        val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                        val dao = database.videoDetectionResultDao()
                        val detectionJson = com.playerid.app.data.DetectionResultSerializer.serialize(analysisResult)
                        dao.insertDetectionResult(
                            com.playerid.app.data.VideoDetectionResultEntity(
                                videoUri = resolvedUri.toString(),
                                detectionMode = "FAST",
                                detectionJson = detectionJson,
                                detectionTimestampMs = System.currentTimeMillis()
                            )
                        )
                        
                        // Also cache in memory for this session
                        VideoSharePreparationCache.set(
                            resolvedUri,
                            com.playerid.app.video.PreparedShareResult(
                                analysisResult = analysisResult,
                                preparedAtMs = System.currentTimeMillis(),
                                mode = com.playerid.app.video.VideoProcessingManager.DetectionMode.FAST
                            )
                        )
                        com.playerid.app.video.DeferredDeepScanScheduler.schedule(
                            context = context,
                            videoUri = resolvedUri,
                            roster = currentRoster,
                            jerseyColorHex = selectedTeamMeta?.homeJerseyColor
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("CameraScreen", "Background detection failed: ${e.message}", e)
                    }
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = "Couldn't open moment prompt. Please try again.",
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

    // isVoiceListening suppresses auto-restart of capture-past while mic is held by SpeechRecognizer
    var isVoiceListening by remember { mutableStateOf(false) }

    // Capture Past should keep a rolling recording active so a tap saves prior moments.
    LaunchedEffect(capturePastMode, isCameraReady, recordingState, isVoiceListening) {
        if (capturePastMode && isCameraReady && recordingState == RecordingState.IDLE && !isVoiceListening) {
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
    DisposableEffect(Unit) {
        onDispose {
            if (recordingManager.recordingState.value == RecordingState.RECORDING) {
                recordingManager.stopRecording()
            }
        }
    }

    // Build a RecognitionListener wired to current Compose state
    fun makeRecognitionListener(): RecognitionListener = object : RecognitionListener {
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
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                SpeechRecognizer.ERROR_AUDIO -> "Audio error — try again"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error"
                else -> "Error ($error) — try again"
            }
            Log.e("CameraScreen", "SpeechRecognizer error: $error")
            recognitionError = errorMsg
            rosterVoiceBubble = errorMsg
            isSpeechActive = false
            isVoiceListening = false
            if (capturePastMode) recordingManager.startRecording(onRecordingSaved)
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spoken = matches?.firstOrNull()?.trim() ?: ""
            Log.d("CameraScreen", "SpeechRecognizer: onResults: $spoken")
            lastSpokenText = spoken
            if (spoken.isNotBlank()) {
                rosterVoiceBubble = "Heard: \"$spoken\""
                viewModel.processVoiceCommandHypotheses(matches?.map { it.trim() } ?: listOf(spoken))
            } else {
                rosterVoiceBubble = "Didn't catch that"
            }
            isSpeechActive = false
            isVoiceListening = false
            if (capturePastMode) recordingManager.startRecording(onRecordingSaved)
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // Create the initial recognizer
    LaunchedEffect(Unit) {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(makeRecognitionListener())
            }
        }
    }

    // Clean up when the recognizer instance is replaced or screen leaves composition
    DisposableEffect(speechRecognizer) {
        val captured = speechRecognizer  // capture current instance; onDispose must NOT read the state var
        onDispose {
            captured?.setRecognitionListener(null)
            captured?.destroy()
        }
    }

    fun startListening() {
        rosterVoiceBubble = "Listening..."
        isVoiceListening = true  // prevent auto-restart of capture-past recording
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        scope.launch {
            // Stop recording and wait for the StateFlow to confirm IDLE (mic truly released)
            if (recordingManager.recordingState.value == RecordingState.RECORDING) {
                recordingManager.stopAndDiscardRecording()
                // Wait directly on the StateFlow — not Compose state
                recordingManager.recordingState.filter { it == RecordingState.IDLE }.first()
                delay(300) // give OS time to release hardware mic
            }
            // Recreate recognizer fresh and attach listener immediately
            speechRecognizer?.destroy()
            speechRecognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context).also {
                    it.setRecognitionListener(makeRecognitionListener())
                }
            } else null
            try {
                speechRecognizer?.startListening(intent)
                Log.d("CameraScreen", "SpeechRecognizer: startListening() called")
                recognitionError = null
            } catch (e: Exception) {
                recognitionError = "Failed: ${e.message}"
                rosterVoiceBubble = "Mic unavailable"
                isVoiceListening = false
                Log.e("CameraScreen", "SpeechRecognizer: Exception: ${e.message}")
                if (capturePastMode) recordingManager.startRecording(onRecordingSaved)
            }
        }
    }

    if (cameraPermissionsState.allPermissionsGranted) {
        Scaffold(
            containerColor = Color.Black,
            contentColor = Color.White,
            floatingActionButton = {
                if (!isStandby && !showSelectionSheet) {
                    val recordAlpha = if (isCameraReady && !isCapturingPhoto) 1f else 0.45f
                    val recordButtonSize = 64.dp
                    val innerColor = when (selectedCameraFeature) {
                        CameraFeature.PHOTO -> Color.White
                        CameraFeature.VIDEO -> Color.Red
                        CameraFeature.CAPTURE_PAST -> MaterialTheme.colorScheme.secondaryContainer
                    }
                    val innerSize by animateDpAsState(
                        targetValue = when {
                            selectedCameraFeature == CameraFeature.PHOTO -> 34.dp
                            isRecording -> 28.dp
                            else -> 42.dp
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "recordInnerSize"
                    )
                    val rollingSpin by rememberInfiniteTransition(label = "rollingCaptureSpin").animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1800, easing = LinearEasing)),
                        label = "rollingCaptureSpinAngle"
                    )
                    Box(modifier = Modifier.padding(bottom = cameraViewportBottomPadding)) {
                        FloatingActionButton(
                            modifier = Modifier.size(recordButtonSize),
                            onClick = {
                                if (!isCameraReady || isCapturingPhoto) return@FloatingActionButton
                                when (selectedCameraFeature) {
                                    CameraFeature.PHOTO -> capturePhoto()
                                    CameraFeature.VIDEO -> {
                                        performRecordButtonPressHaptic(context)
                                        if (recordingState == RecordingState.RECORDING) {
                                            lastManualStop = System.currentTimeMillis()
                                            recordingManager.stopRecording()
                                        } else if (recordingState == RecordingState.IDLE) {
                                            recordingManager.startRecording(onRecordingSaved)
                                        }
                                    }
                                    CameraFeature.CAPTURE_PAST -> {
                                        performRecordButtonPressHaptic(context)
                                        if (recordingState == RecordingState.RECORDING) {
                                            lastManualStop = System.currentTimeMillis()
                                            recordingManager.stopRecording()
                                        }
                                    }
                                }
                            },
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(recordButtonSize).alpha(recordAlpha),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(recordButtonSize)
                                        .background(Color.Transparent, CircleShape)
                                        .border(width = 2.dp, color = Color.White, shape = CircleShape)
                                )
                                if (selectedCameraFeature == CameraFeature.CAPTURE_PAST) {
                                    Canvas(modifier = Modifier.size(recordButtonSize + 4.dp)) {
                                        val strokeWidth = 5.0.dp.toPx()
                                        val radius = (size.minDimension / 2f) - (strokeWidth / 2f)
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val cometColor = Color(0xFFB8FF6A)

                                        for (i in 0..5) {
                                            val alpha = 1f - (i * 0.16f)
                                            val start = rollingSpin - (i * 11f)
                                            drawArc(
                                                color = cometColor.copy(alpha = alpha.coerceAtLeast(0.08f)),
                                                startAngle = start,
                                                sweepAngle = 9f,
                                                useCenter = false,
                                                topLeft = Offset(center.x - radius, center.y - radius),
                                                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                            )
                                        }

                                        val angleRad = Math.toRadians(rollingSpin.toDouble())
                                        val headX = center.x + (radius * cos(angleRad)).toFloat()
                                        val headY = center.y + (radius * sin(angleRad)).toFloat()
                                        drawCircle(
                                            color = cometColor,
                                            radius = 3.2.dp.toPx(),
                                            center = Offset(headX, headY)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(innerSize)
                                        .background(innerColor, CircleShape)
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
            ) {
                // Removed obsolete listening window with pulsing mic
                if (!isCameraReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = cameraViewportBottomPadding)
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
                    }, Modifier
                        .fillMaxSize()
                        .padding(bottom = cameraViewportBottomPadding)
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
                )
                if (showCameraOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = cameraViewportBottomPadding)
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = cameraViewportBottomPadding)
                    )
                }

                // Recording timer
                if (isLiveRecording) {
                    val elapsedSeconds = (liveElapsedMs / 1000).toInt()
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Text(
                            text = "%02d:%02d".format(minutes, seconds),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                if (isStandby) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = cameraViewportBottomPadding)
                            .background(Color.Black.copy(alpha = 0.98f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = stringResource(R.string.capture_past_icon),
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.capture_past),
                                color = Color.White.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.capture_past_recording_hint),
                                color = Color.White.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .padding(top = 14.dp, start = 10.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .clickable { showSelectionSheet = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTeam != null) {
                        Box(modifier = Modifier.size(26.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_jersey),
                                contentDescription = "Jersey color",
                                tint = teamJerseyColor,
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_jersey_outline),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = if (selectedTeam != null) {
                                if (!selectedOpponent.isNullOrBlank()) "$selectedTeam · vs $selectedOpponent"
                                else selectedTeam
                            } else "Select Team",
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        if (selectedTeam != null && selectedOpponent.isNotBlank()) {
                            Text(
                                text = "Player: $selectedKid",
                                color = Color.White.copy(alpha = 0.78f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Choose team, opponent, and player",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (!isStandby && !showSelectionSheet) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 104.dp + cameraViewportBottomPadding)
                            .zIndex(3f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val modes = listOf(
                            CameraFeature.PHOTO to "Photo",
                            CameraFeature.CAPTURE_PAST to "Rolling",
                            CameraFeature.VIDEO to "Video"
                        )
                        modes.forEach { (mode, label) ->
                            val selected = selectedCameraFeature == mode
                            val enabled = !isLiveRecording || selected
                            Button(
                                onClick = {
                                    if (!enabled) return@Button
                                    if (mode == CameraFeature.CAPTURE_PAST) {
                                        selectedCameraFeature = CameraFeature.CAPTURE_PAST
                                        if (!capturePastMode) {
                                            viewModel.setCapturePastMode(true)
                                            val rollingHintShown = cameraPrefs.getBoolean("rolling_capture_hint_shown", false)
                                            if (!rollingHintShown) {
                                                cameraPrefs.edit().putBoolean("rolling_capture_hint_shown", true).apply()
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Capture last 30 seconds even if you weren't recording",
                                                        withDismissAction = true,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        if (capturePastMode) viewModel.setCapturePastMode(false)
                                        selectedCameraFeature = mode
                                    }
                                },
                                enabled = enabled,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.14f),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Black.copy(alpha = 0.14f),
                                    disabledContentColor = Color.White.copy(alpha = 0.55f)
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .zIndex(10f)
                                    .alpha(if (enabled) 1f else 0.35f)
                                    .defaultMinSize(minWidth = 136.dp, minHeight = 80.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 20.sp
                                    )
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 5.dp)
                                                .size(6.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(11.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Roster swipe-up handle strip (visible when drawer is closed)
                if (!isStandby && !rosterDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 196.dp)
                            .width(96.dp)
                            .height(40.dp)
                            .zIndex(1f)
                            .pointerInput(Unit) {
                                var totalDrag = 0f
                                detectDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag < -25f) {
                                            rosterDrawerOpen = true
                                            rosterDrawerFull = false
                                        }
                                    },
                                    onDrag = { _, dragAmount -> totalDrag += dragAmount.y }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Swipe up to view roster",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(5.dp)
                                    .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                // Roster Drawer
                if (rosterDrawerOpen && !isStandby) {
                    RosterDrawerSheet(
                        players = shareRosterPlayers,
                        isFull = rosterDrawerFull,
                        searchQuery = rosterSearchQuery,
                        highlightedIds = rosterHighlightedIds,
                        selectedPlayer = rosterSelectedPlayer,
                        voiceBubble = rosterVoiceBubble,
                        isSpeechActive = isSpeechActive,
                        onSearchChange = { rosterSearchQuery = it },
                        onPlayerSelect = { rosterSelectedPlayer = it },
                        onMicClick = { startListening() },
                        onExpandChange = { rosterDrawerFull = it },
                        onNavigateToTeams = onNavigateToTeams,
                        onDismiss = {
                            rosterDrawerOpen = false
                            rosterDrawerFull = false
                            rosterSearchQuery = ""
                            rosterSelectedPlayer = null
                            viewModel.dismissVoiceResult()
                        }
                    )
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
                                .imePadding()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val opponentBringIntoViewRequester = remember { BringIntoViewRequester() }
                                val fieldScope = rememberCoroutineScope()
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
                                ExposedDropdownMenuBox(
                                    expanded = opponentSuggestionsExpanded && matchingOpponents.isNotEmpty(),
                                    onExpandedChange = { if (matchingOpponents.isNotEmpty()) opponentSuggestionsExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedOpponent,
                                        onValueChange = {
                                            opponentManuallyEdited = true
                                            selectedOpponent = it
                                            if (it.isBlank()) {
                                                fieldScope.launch {
                                                    delay(75)
                                                    opponentBringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        },
                                        label = { Text("Enter opponent name") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = teamPrimary,
                                            unfocusedBorderColor = teamPrimary.copy(alpha = 0.45f),
                                            focusedLabelColor = teamPrimary,
                                            unfocusedLabelColor = teamPrimary.copy(alpha = 0.75f),
                                            cursorColor = teamPrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(opponentBringIntoViewRequester)
                                            .onFocusEvent { focusState ->
                                                if (focusState.isFocused) {
                                                    fieldScope.launch {
                                                        delay(100)
                                                        opponentBringIntoViewRequester.bringIntoView()
                                                    }
                                                }
                                            }
                                            .menuAnchor(),
                                        trailingIcon = {
                                            IconButton(onClick = { keyboardController?.hide() }) {
                                                Icon(Icons.Default.Done, contentDescription = "Done", tint = teamPrimary)
                                            }
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = opponentSuggestionsExpanded && matchingOpponents.isNotEmpty(),
                                        onDismissRequest = { opponentSuggestionsExpanded = false }
                                    ) {
                                        matchingOpponents.forEach { suggestion ->
                                            DropdownMenuItem(
                                                text = { Text(suggestion) },
                                                onClick = {
                                                    opponentManuallyEdited = true
                                                    selectedOpponent = suggestion
                                                    opponentSuggestionsExpanded = false
                                                    keyboardController?.hide()
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (selectedOpponent.isNotBlank()) {
                                    var kidExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = kidExpanded,
                                        onExpandedChange = { kidExpanded = !kidExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedKid,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Select Player") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = kidExpanded)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = kidExpanded,
                                            onDismissRequest = { kidExpanded = false }
                                        ) {
                                            kidOptions.forEach { kidName ->
                                                DropdownMenuItem(
                                                    text = { Text(kidName) },
                                                    onClick = {
                                                        selectedKid = kidName
                                                        teamViewModel.selectKidForTeam(selectedTeam, kidName)
                                                        kidExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Enter opponent to choose player",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

    if (showPostCaptureShareDialog && postCapturePromptUri != null) {
        val savedUri = postCapturePromptUri!!
        Log.d("CameraScreen", "RENDERING_POST_CAPTURE_DIALOG uri=$savedUri")
        var showCustomTagInput by rememberSaveable { mutableStateOf(false) }
        var customTagText by rememberSaveable { mutableStateOf("") }
        var customMomentButtons by rememberSaveable { mutableStateOf(loadCustomMomentButtonLabels(context)) }
        var isEditingCustomButtons by rememberSaveable { mutableStateOf(false) }
        var customTagBeingEdited by rememberSaveable { mutableStateOf<String?>(null) }
        var renameCustomTagText by rememberSaveable { mutableStateOf("") }
        val dismissAfterSelection: (MomentTag) -> Unit = { tag ->
            persistClipMomentTag(context = context, videoUri = savedUri, tagLabel = tag.displayName)
            selectedMomentTag = tag
            showPostCaptureShareDialog = false
            postCapturePromptUri = null
        }
        Dialog(
            onDismissRequest = {
                showPostCaptureShareDialog = false
                postCapturePromptUri = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .padding(bottom = 18.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "What happened?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MomentTagButton(
                                tag = MomentTag.GOAL,
                                isSelected = selectedMomentTag == MomentTag.GOAL,
                                onClick = { dismissAfterSelection(MomentTag.GOAL) },
                                modifier = Modifier.weight(1f)
                            )
                            MomentTagButton(
                                tag = MomentTag.SAVE,
                                isSelected = selectedMomentTag == MomentTag.SAVE,
                                onClick = { dismissAfterSelection(MomentTag.SAVE) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MomentTagButton(
                                tag = MomentTag.FACEOFF_WIN,
                                isSelected = selectedMomentTag == MomentTag.FACEOFF_WIN,
                                onClick = { dismissAfterSelection(MomentTag.FACEOFF_WIN) },
                                modifier = Modifier.weight(1f)
                            )
                            MomentTagButton(
                                tag = MomentTag.ASSIST,
                                isSelected = selectedMomentTag == MomentTag.ASSIST,
                                onClick = { dismissAfterSelection(MomentTag.ASSIST) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MomentTagButton(
                                tag = MomentTag.DEFENSIVE_STOP,
                                isSelected = selectedMomentTag == MomentTag.DEFENSIVE_STOP,
                                onClick = { dismissAfterSelection(MomentTag.DEFENSIVE_STOP) },
                                modifier = Modifier.weight(1f)
                            )
                            MomentActionButton(
                                label = "Custom",
                                icon = Icons.Default.Add,
                                onClick = { showCustomTagInput = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        customMomentButtons.chunked(2).forEach { rowLabels ->
                            if (rowLabels == customMomentButtons.chunked(2).first()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Custom tags",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = { isEditingCustomButtons = !isEditingCustomButtons }
                                    ) {
                                        Text(if (isEditingCustomButtons) "Done" else "Edit")
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowLabels.forEach { label ->
                                    if (isEditingCustomButtons) {
                                        CustomMomentTagEditCard(
                                            label = label,
                                            onRename = {
                                                customTagBeingEdited = label
                                                renameCustomTagText = label
                                            },
                                            onDelete = {
                                                removeCustomMomentButtonLabel(context, label)
                                                customMomentButtons = loadCustomMomentButtonLabels(context)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        MomentActionButton(
                                            label = label,
                                            icon = Icons.Default.Label,
                                            onClick = {
                                                persistClipMomentTag(context = context, videoUri = savedUri, tagLabel = label)
                                                selectedMomentTag = null
                                                showPostCaptureShareDialog = false
                                                postCapturePromptUri = null
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (rowLabels.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (showCustomTagInput) {
                            OutlinedTextField(
                                value = customTagText,
                                onValueChange = { customTagText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                label = { Text("Enter custom tag") },
                                placeholder = { Text("Ex: assist, turnover, hustle") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    showCustomTagInput = false
                                    customTagText = ""
                                }) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val normalized = customTagText.trim()
                                        if (normalized.isNotEmpty()) {
                                            persistClipMomentTag(
                                                context = context,
                                                videoUri = savedUri,
                                                tagLabel = normalized
                                            )
                                            persistCustomMomentButtonLabel(context, normalized)
                                            customMomentButtons = loadCustomMomentButtonLabels(context)
                                            selectedMomentTag = null
                                            showCustomTagInput = false
                                            customTagText = ""
                                            showPostCaptureShareDialog = false
                                            postCapturePromptUri = null
                                        }
                                    },
                                    enabled = customTagText.trim().isNotEmpty()
                                ) {
                                    Text("Save Tag")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MomentActionButton(
                                label = "Share Clip",
                                icon = Icons.Default.Share,
                                onClick = {
                                    launchPersonalShareChooser(context, savedUri, "Share this moment")
                                    showPostCaptureShareDialog = false
                                    postCapturePromptUri = null
                                },
                                modifier = Modifier.weight(1f)
                            )

                            MomentActionButton(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                onClick = {
                                    runCatching {
                                        context.contentResolver.delete(savedUri, null, null)
                                    }
                                    showPostCaptureShareDialog = false
                                    postCapturePromptUri = null
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )

                            MomentActionButton(
                                label = "Skip",
                                icon = Icons.Default.SkipNext,
                                onClick = {
                                    showPostCaptureShareDialog = false
                                    postCapturePromptUri = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (customTagBeingEdited != null) {
            AlertDialog(
                onDismissRequest = {
                    customTagBeingEdited = null
                    renameCustomTagText = ""
                },
                title = { Text("Rename custom tag") },
                text = {
                    OutlinedTextField(
                        value = renameCustomTagText,
                        onValueChange = { renameCustomTagText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                dismissButton = {
                    TextButton(onClick = {
                        customTagBeingEdited = null
                        renameCustomTagText = ""
                    }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val original = customTagBeingEdited ?: ""
                            val renamed = renameCustomTagText.trim()
                            if (original.isNotBlank() && renamed.isNotBlank()) {
                                renameCustomMomentButtonLabel(context, original, renamed)
                                customMomentButtons = loadCustomMomentButtonLabels(context)
                            }
                            customTagBeingEdited = null
                            renameCustomTagText = ""
                        },
                        enabled = renameCustomTagText.trim().isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    }
}

@Composable
private fun RosterDrawerSheet(
    players: List<com.playerid.app.data.Player>,
    isFull: Boolean,
    searchQuery: String,
    highlightedIds: Set<String>,
    selectedPlayer: com.playerid.app.data.Player?,
    voiceBubble: String?,
    isSpeechActive: Boolean,
    onSearchChange: (String) -> Unit,
    onPlayerSelect: (com.playerid.app.data.Player?) -> Unit,
    onMicClick: () -> Unit,
    onExpandChange: (Boolean) -> Unit,
    onNavigateToTeams: () -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val peekHeight = screenHeightDp * 0.35f
    val fullHeight = screenHeightDp * 0.87f
    val targetHeight = if (isFull) fullHeight else peekHeight
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(300),
        label = "drawerHeight"
    )

    val displayedPlayers = if (isFull && searchQuery.isNotEmpty()) {
        players.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.number.contains(searchQuery, ignoreCase = true)
        }
    } else {
        players
    }
    val rosterListState = rememberLazyListState()
    var detailsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPlayer?.id) {
        detailsExpanded = selectedPlayer != null
    }

    LaunchedEffect(selectedPlayer?.id, displayedPlayers) {
        val selectedId = selectedPlayer?.id ?: return@LaunchedEffect
        val selectedIndex = displayedPlayers.indexOfFirst { it.id == selectedId }
        if (selectedIndex >= 0) {
            rosterListState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Dim scrim in full mode
        if (isFull) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onExpandChange(false) }
            )
        }

        // Floating voice response bubble above the drawer
        voiceBubble?.let { bubble ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = animatedHeight + 10.dp)
                    .background(Color(0xFF1A1E3C).copy(alpha = 0.96f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = bubble,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        // Drawer panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .pointerInput(isFull) {
                    var totalDrag = 0f
                    detectDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            when {
                                totalDrag > 80f && isFull -> onExpandChange(false)
                                totalDrag > 80f && !isFull -> onDismiss()
                                totalDrag < -80f && !isFull -> onExpandChange(true)
                            }
                        },
                        onDrag = { _, dragAmount -> totalDrag += dragAmount.y }
                    )
                },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF10101E).copy(alpha = 0.96f),
            tonalElevation = 16.dp,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle row with mic always in top-right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            .align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice search",
                            tint = if (isSpeechActive) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Search bar (full state only)
                if (isFull) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = {
                            Text(
                                "Search name or number",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.35f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            cursorColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                selectedPlayer?.let { player ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A223D).copy(alpha = 0.95f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${player.number}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = player.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Position: ${player.position.ifBlank { "Unknown" }}",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Grad Year: ${player.academicYear.ifBlank { "Unknown" }}",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 12.sp
                                )
                                if (detailsExpanded) {
                                    Text(
                                        text = "Team: ${player.team.ifBlank { "Unknown" }}",
                                        color = Color.White.copy(alpha = 0.78f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (detailsExpanded) onPlayerSelect(null)
                                else detailsExpanded = true
                            }) {
                                Icon(
                                    imageVector = if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (detailsExpanded) "Collapse details" else "Expand details",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                if (players.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_roster_loaded),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.load_roster_prefix),
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.load_roster_link),
                                color = Color(0xFF7CC6FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onNavigateToTeams() }
                            )
                            Text(
                                text = stringResource(R.string.load_roster_suffix),
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (displayedPlayers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_players_found),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = stringResource(R.string.try_adjusting_search),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = rosterListState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(displayedPlayers, key = { it.id }) { player ->
                            val isHighlighted = highlightedIds.contains(player.id)
                            val isSelected = selectedPlayer?.id == player.id
                            val rowBg by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFF2E7D32).copy(alpha = 0.42f)
                                    else if (isHighlighted) Color(0xFF1565C0).copy(alpha = 0.40f)
                                    else Color.Transparent,
                                animationSpec = tween(300),
                                label = "rowBg_${player.id}"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg, RoundedCornerShape(8.dp))
                                    .clickable { onPlayerSelect(player) }
                                    .padding(horizontal = 8.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${player.number}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.width(52.dp)
                                )
                                Text(
                                    text = player.name,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 16.sp
                                )
                            }
                            if (isFull) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.06f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
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
    // Keep the selected mode fully visible during recording so the user always sees which mode is active
    val alpha = if (enabled || selected) 1f else 0.35f

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.alpha(alpha)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
            Text(label, color = contentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
        Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text(stringResource(R.string.grant_permissions)) }
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
    opponentName: String?,
    kidName: String?
) {
    val teamPrefs = context.getSharedPreferences("video_team_names", Context.MODE_PRIVATE)
    val startPrefs = context.getSharedPreferences("video_start_times", Context.MODE_PRIVATE)
    val opponentPrefs = context.getSharedPreferences("video_opponent_names", Context.MODE_PRIVATE)
    val kidPrefs = context.getSharedPreferences("video_kid_names", Context.MODE_PRIVATE)
    val cleanedOpponent = opponentName?.trim().orEmpty()
    val cleanedKid = kidName?.trim().orEmpty()

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
    if (cleanedKid.isNotEmpty()) {
        kidPrefs.edit().putString(uriKey, cleanedKid).apply()
    } else {
        kidPrefs.edit().remove(uriKey).apply()
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
            if (cleanedKid.isNotEmpty()) {
                kidPrefs.edit().putString(pathKey, cleanedKid).apply()
            } else {
                kidPrefs.edit().remove(pathKey).apply()
            }
        }
    }
}

private fun persistClipMomentTag(
    context: Context,
    videoUri: Uri,
    tagLabel: String
) {
    val normalized = tagLabel.trim()
    if (normalized.isEmpty()) return

    val tagPrefs = context.getSharedPreferences("video_custom_names", Context.MODE_PRIVATE)
    val keys = linkedSetOf<String>()
    val uriKey = videoUri.toString()
    if (uriKey.isNotBlank()) keys.add(uriKey)

    val pathKey = videoUri.path
    if (!pathKey.isNullOrBlank()) {
        keys.add(pathKey)
        keys.add(Uri.fromFile(java.io.File(pathKey)).toString())
    }

    if (videoUri.scheme == "content") {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        runCatching {
            context.contentResolver.query(videoUri, projection, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        context.getExternalFilesDirs(android.os.Environment.DIRECTORY_MOVIES)
                            .filterNotNull()
                            .forEach { moviesDir ->
                                val candidate = java.io.File(moviesDir, displayName)
                                if (candidate.exists()) {
                                    keys.add(candidate.absolutePath)
                                    keys.add(Uri.fromFile(candidate).toString())
                                }
                            }
                    }
                }
            }
        }
    }

    if (keys.isNotEmpty()) {
        val editor = tagPrefs.edit()
        keys.forEach { key -> editor.putString(key, normalized) }
        editor.apply()
    }
}

private suspend fun resolveRecentRecordedVideoUri(
    context: Context,
    clipStartTimeMs: Long
): Uri? {
    val minimumDateAddedSeconds = (clipStartTimeMs / 1000L) - 120L
    val minimumDateTakenMs = clipStartTimeMs - 120_000L
    repeat(5) {
        val candidate = withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.RELATIVE_PATH,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_TAKEN
            )

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val pathIndex = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                val nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val addedIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val takenIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)

                var scanned = 0
                while (cursor.moveToNext() && scanned < 20) {
                    scanned += 1
                    if (idIndex < 0) continue

                    val id = cursor.getLong(idIndex)
                    val relativePath = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else ""
                    val displayName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                    val dateAddedSec = if (addedIndex >= 0) cursor.getLong(addedIndex) else 0L
                    val dateTakenMs = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L

                    val isPlayerIdFolder = relativePath.contains("Movies/PlayerID", ignoreCase = true)
                    val isSpotrName = displayName.startsWith("Spotr-Clip-", ignoreCase = true)
                    val isRecent = dateAddedSec >= minimumDateAddedSeconds || dateTakenMs >= minimumDateTakenMs
                    if (isRecent && (isPlayerIdFolder || isSpotrName)) {
                        return@withContext Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                    }
                }
            }
            null
        }

        if (candidate != null) return candidate
        delay(180)
    }
    return null
}

private fun isRecordedVideoUriReadable(context: Context, uri: Uri): Boolean {
    if (uri == Uri.EMPTY) return false
    return try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            afd.length != 0L
        } ?: false
    } catch (_: Exception) {
        false
    }
}

private fun loadCustomMomentButtonLabels(context: Context): List<String> {
    val prefs = context.getSharedPreferences("camera_custom_moment_tags", Context.MODE_PRIVATE)
    return prefs.getStringSet("labels", emptySet())
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.sortedBy { it.lowercase() }
        ?: emptyList()
}

private fun persistCustomMomentButtonLabel(context: Context, label: String) {
    val normalized = label.trim()
    if (normalized.isEmpty()) return
    val prefs = context.getSharedPreferences("camera_custom_moment_tags", Context.MODE_PRIVATE)
    val current = prefs.getStringSet("labels", emptySet())?.toMutableSet() ?: mutableSetOf()
    val alreadyExists = current.any { it.equals(normalized, ignoreCase = true) }
    if (!alreadyExists) {
        current.add(normalized)
        prefs.edit().putStringSet("labels", current).apply()
    }
}

private fun removeCustomMomentButtonLabel(context: Context, label: String) {
    val normalized = label.trim()
    if (normalized.isEmpty()) return
    val prefs = context.getSharedPreferences("camera_custom_moment_tags", Context.MODE_PRIVATE)
    val current = prefs.getStringSet("labels", emptySet())?.toMutableSet() ?: mutableSetOf()
    val updated = current.filterNot { it.equals(normalized, ignoreCase = true) }.toSet()
    prefs.edit().putStringSet("labels", updated).apply()
}

private fun renameCustomMomentButtonLabel(context: Context, oldLabel: String, newLabel: String) {
    val normalizedOld = oldLabel.trim()
    val normalizedNew = newLabel.trim()
    if (normalizedOld.isEmpty() || normalizedNew.isEmpty()) return
    val prefs = context.getSharedPreferences("camera_custom_moment_tags", Context.MODE_PRIVATE)
    val current = prefs.getStringSet("labels", emptySet())?.toMutableSet() ?: mutableSetOf()
    val withoutOld = current.filterNot { it.equals(normalizedOld, ignoreCase = true) }.toMutableSet()
    val hasNew = withoutOld.any { it.equals(normalizedNew, ignoreCase = true) }
    if (!hasNew) {
        withoutOld.add(normalizedNew)
    }
    prefs.edit().putStringSet("labels", withoutOld).apply()
}

@Composable
private fun CustomMomentTagEditCard(
    label: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename custom tag",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete custom tag",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

