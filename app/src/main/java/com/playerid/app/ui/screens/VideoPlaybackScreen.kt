package com.playerid.app.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.content.Intent
import androidx.compose.ui.text.style.TextOverflow
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.activity.compose.BackHandler
import kotlin.math.abs
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as Media3Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import com.playerid.app.data.Player
import com.playerid.app.video.VideoProcessingManager
import com.playerid.app.video.VideoPlayerDetectionResult
import com.playerid.app.video.VideoSharePreparationCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs
import kotlin.math.min

/**
 * Video Playback Screen with selectable player overlays
 * 
 * Shows a list of detected players and allows user to toggle
 * which players should have name bubbles displayed during playback
 */
@Composable
private fun VideoSeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.35f),
    progressColor: Color = Color.White,
    thumbColor: Color = Color.White
) {
    var dragFraction by remember { mutableFloatStateOf(fraction) }
    var isDragging by remember { mutableStateOf(false) }

    // rememberUpdatedState ensures pointerInput always calls the latest lambdas
    // even though pointerInput(Unit) never restarts mid-gesture
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnSeekFinished by rememberUpdatedState(onSeekFinished)

    LaunchedEffect(fraction) {
        if (!isDragging) dragFraction = fraction
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isDragging = true
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val initFraction = (down.position.x / width).coerceIn(0f, 1f)
                    dragFraction = initFraction
                    currentOnSeek(initFraction)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        val newFraction = (change.position.x / width).coerceIn(0f, 1f)
                        if (newFraction != dragFraction) {
                            dragFraction = newFraction
                            currentOnSeek(newFraction)
                        }
                        if (!change.pressed) break
                    } while (true)

                    isDragging = false
                    currentOnSeekFinished()
                }
            }
    ) {
        val displayFraction = if (isDragging) dragFraction else fraction
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackY = size.height / 2f
            val trackHeight = 4.dp.toPx()
            val thumbRadius = 10.dp.toPx()
            drawLine(
                color = trackColor,
                start = Offset(0f, trackY),
                end = Offset(size.width, trackY),
                strokeWidth = trackHeight
            )
            drawLine(
                color = progressColor,
                start = Offset(0f, trackY),
                end = Offset(size.width * displayFraction, trackY),
                strokeWidth = trackHeight
            )
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(size.width * displayFraction, trackY)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VideoPlaybackScreen(
    videoUri: Uri,
    detectedPlayers: List<Player>,
    onNavigateBack: () -> Unit,
    playlistUris: List<Uri> = emptyList(), // For highlight reel mode
    initialIndex: Int = 0,
    startInShareFlow: Boolean = false,
    showPlaybackUi: Boolean = true,
    isReviewMode: Boolean = false,
    reelTitle: String? = null,
    reelTeamName: String? = null,
    reelSeasonLabel: String? = null,
    reelOpponents: List<String> = emptyList(),
    reelScenario: String? = null,
    activeReelId: String? = null,
    onSaveAsGoatReel: ((String) -> Unit)? = null,
    onEditReel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveReelDialog by remember { mutableStateOf(false) }
    var reelNameInput by remember(reelTitle) { mutableStateOf(reelTitle ?: "My Reel") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var videoDuration by remember { mutableLongStateOf(0L) }
    var currentVideoIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, maxOf(0, playlistUris.size - 1))) }
    var transitionPulse by remember { mutableIntStateOf(0) }
    var showClipTransitionOverlay by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(false) }
    var showReplayOverlay by remember { mutableStateOf(false) }
    val clipTransitionAlpha by animateFloatAsState(
        targetValue = if (playlistUris.isNotEmpty() && showClipTransitionOverlay) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "clipTransitionAlpha"
    )
    val clipTransitionScale by animateFloatAsState(
        targetValue = if (playlistUris.isNotEmpty() && showClipTransitionOverlay) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "clipTransitionScale"
    )
    var playbackZoom by remember(videoUri, currentVideoIndex) { mutableFloatStateOf(1f) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    var wasPlayingBeforeSeek by remember { mutableStateOf(false) }
    val activeScrubUri = if (playlistUris.isNotEmpty() && currentVideoIndex in playlistUris.indices) {
        playlistUris[currentVideoIndex]
    } else {
        videoUri
    }
    val activeVoiceMemoryKey = activeScrubUri.toString()
    var scrubRetriever by remember { mutableStateOf<MediaMetadataRetriever?>(null) }
    var scrubPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scrubPreviewRequestMs by remember { mutableLongStateOf(-1L) }
    var scrubPreviewRenderedKey by remember { mutableIntStateOf(-1) }
    var scrubPreviewFrameCount by remember { mutableIntStateOf(0) }
    val scrubFrameCache = remember(activeScrubUri) { LinkedHashMap<Int, Bitmap>(256, 0.75f, true) }
    val currentFraction = if (videoDuration > 0L) {
        (currentPosition.toFloat() / videoDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val rawSliderFraction = if (isSeeking) seekFraction else currentFraction
    val animatedSliderFraction by animateFloatAsState(
        targetValue = rawSliderFraction,
        animationSpec = if (isSeeking) tween(durationMillis = 0) else tween(durationMillis = 80, easing = LinearEasing),
        label = "seekBarPosition"
    )
    val sliderFraction = if (isSeeking) seekFraction else animatedSliderFraction
    val displayedPosition = if (isSeeking && videoDuration > 0L) {
        (rawSliderFraction * videoDuration.toFloat()).toLong().coerceIn(0L, videoDuration)
    } else {
        currentPosition
    }
    
    var composedReelPlaybackUri by remember(
        playlistUris,
        reelTitle,
        reelTeamName,
        reelOpponents,
        reelScenario
    ) {
        mutableStateOf<Uri?>(null)
    }
    val isPlaylistMode = playlistUris.isNotEmpty() && composedReelPlaybackUri == null
    val totalVideos = if (isPlaylistMode) playlistUris.size else 1
    val reelClipCountLabel = if (totalVideos == 1) "1 clip" else "$totalVideos clips"
    var showReelIntroOverlay by remember(isPlaylistMode, reelTitle, reelSeasonLabel, reelOpponents, totalVideos) {
        mutableStateOf(isPlaylistMode)
    }
    val reelIntroAlpha by animateFloatAsState(
        targetValue = if (showReelIntroOverlay && isPlaylistMode) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = LinearEasing),
        label = "reelIntroAlpha"
    )
    val reelOpponentsLabel = remember(reelOpponents) {
        when {
            reelOpponents.isEmpty() -> "Opponents unavailable"
            reelOpponents.size <= 3 -> reelOpponents.joinToString("  •  ")
            else -> reelOpponents.take(3).joinToString("  •  ") + "  +${reelOpponents.size - 3}"
        }
    }
    val resolvedReelScenario = remember(reelScenario, reelTitle, reelOpponents) {
        when {
            reelScenario == "top_plays" -> "top_plays"
            reelScenario == "opponent" -> "opponent"
            reelScenario == "season" -> "season"
            reelTitle?.contains("top play", ignoreCase = true) == true -> "top_plays"
            reelTitle?.contains(" vs ", ignoreCase = true) == true || reelOpponents.size == 1 -> "opponent"
            else -> "season"
        }
    }
    val reelScenarioSubheader = when (resolvedReelScenario) {
        "top_plays" -> "Highlights the best moments"
        "opponent" -> ""
        else -> "Overview of the full season"
    }
    val primaryOpponent = reelOpponents.firstOrNull()?.trim().orEmpty()
    val reelHeroLine = when (resolvedReelScenario) {
        "top_plays" -> "TOP PLAYS"
        "opponent" -> {
            val firstOpponent = primaryOpponent.uppercase()
            if (firstOpponent.isBlank()) "VS OPPONENT" else "VS $firstOpponent"
        }
        else -> {
            val seasonLine = reelSeasonLabel?.trim().orEmpty()
            if (seasonLine.isBlank()) "SEASON" else "$seasonLine  SEASON"
        }
    }
    val reelOverlayGradient = when (resolvedReelScenario) {
        "top_plays" -> listOf(Color(0xF00B0A07), Color(0xDDA67C08), Color(0xB312110F))
        "opponent" -> listOf(Color(0xF006120A), Color(0xDD1E3A28), Color(0xAA08120E))
        else -> listOf(Color(0xF0121418), Color(0xCC6F5B2B), Color(0xAA13161A))
    }
    val reelAccentColor = when (resolvedReelScenario) {
        "top_plays" -> Color(0xFFFFC447)
        "opponent" -> Color(0xFF80D38A)
        else -> Color(0xFFFFD97A)
    }
    var preparedReelShareUri by remember(
        playlistUris,
        reelTitle,
        reelTeamName,
        reelOpponents,
        reelScenario
    ) {
        mutableStateOf<Uri?>(null)
    }

    fun resolveShareUri(onResolved: (Uri) -> Unit) {
        composedReelPlaybackUri?.let {
            onResolved(it)
            return
        }

        if (!isPlaylistMode) {
            onResolved(videoUri)
            return
        }

        preparedReelShareUri?.let {
            onResolved(it)
            return
        }

        scope.launch {
            try {
                Toast.makeText(context, "Preparing reel for sharing...", Toast.LENGTH_SHORT).show()
                val scenario = when {
                    reelScenario == "top_plays" -> "top_plays"
                    reelScenario == "opponent" -> "opponent"
                    reelScenario == "season" -> "season"
                    reelTitle?.contains("top play", ignoreCase = true) == true -> "top_plays"
                    reelTitle?.contains(" vs ", ignoreCase = true) == true || reelOpponents.size == 1 -> "opponent"
                    else -> "season"
                }

                val sharedReelUri = buildShareableReelWithIntro(
                    context = context,
                    clipUris = playlistUris,
                    reelTitle = reelTitle ?: "My Reel",
                    teamName = reelTeamName,
                    opponentName = reelOpponents.firstOrNull(),
                    scenario = scenario
                )

                if (sharedReelUri != null) {
                    preparedReelShareUri = sharedReelUri
                    onResolved(sharedReelUri)
                } else {
                    Toast.makeText(
                        context,
                        "Unable to build reel video. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("VideoPlaybackScreen", "Failed to prepare reel share URI", e)
                Toast.makeText(
                    context,
                    "Unable to prepare sharing right now.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(isPlaylistMode, isPlaying) {
        if (!isPlaylistMode) {
            showReelIntroOverlay = false
            return@LaunchedEffect
        }
        if (isPlaying) {
            showReelIntroOverlay = false
        }
    }
    
    var isAnalyzingPlayers by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var detectedSharePlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var selectedSharePlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showShareSuggestions by remember { mutableStateOf(false) }
    var showQuickSharePicker by remember { mutableStateOf(false) }
    var allContactsForShare by remember { mutableStateOf<List<SelectedContact>>(emptyList()) }
    var favoritePhoneContacts by remember { mutableStateOf(loadFavoritePhoneContacts(context)) }
    var detectionMode by remember { mutableStateOf(VideoProcessingManager.DetectionMode.FAST) }
    var showDeeperScanPrompt by remember { mutableStateOf(false) }
    var detectedOverlays by remember { mutableStateOf<List<IdentifiedOverlayPlayer>>(emptyList()) }
    var selectedOverlayIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showOverlaySelectionDialog by remember { mutableStateOf(false) }
    var isLockAssistMode by remember { mutableStateOf(false) }
    var autoShareTriggered by remember(videoUri) { mutableStateOf(false) }
    var showInitialShareDestinationDialog by remember(videoUri) { mutableStateOf(false) }
    var showTeamShareAboutDialog by remember { mutableStateOf(false) }
    var isPreparingTeamShare by remember { mutableStateOf(false) }
    var selectedHighlightTag by remember { mutableStateOf<String?>(null) }
    var teamShareMessage by remember { mutableStateOf("") }
    var showHighlightTags by remember { mutableStateOf(false) }
    var showPreviewActionsMenu by remember { mutableStateOf(false) }
    var shareDialogUri by remember { mutableStateOf<Uri?>(null) }
    var showSharePlayerList by remember { mutableStateOf(false) }
    var suggestedSharePlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingSuggestedPlayers by remember { mutableStateOf(false) }
    var scanShareSuggestionsDone by remember { mutableStateOf(false) }
    var showVoiceMemoryDialog by remember { mutableStateOf(false) }
    var isRecordingVoiceMemory by remember { mutableStateOf(false) }
    var isPlayingVoiceMemory by remember { mutableStateOf(false) }
    var voiceMemoryStatus by remember { mutableStateOf<String?>(null) }
    var activeVoiceMemoryPath by remember(activeVoiceMemoryKey) {
        mutableStateOf(loadClipVoiceMemoryPath(context, activeVoiceMemoryKey))
    }
    val voiceMemoryActionLabel = if (activeVoiceMemoryPath.isNullOrBlank()) {
        "Add Memory"
    } else {
        "Edit Memory"
    }
    var voiceMemoryRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceMemoryPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val selectedPlayersForShare = remember(detectedSharePlayers, selectedSharePlayerIds) {
        detectedSharePlayers.filter { selectedSharePlayerIds.contains(it.id) }
    }
    val voiceMemoryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoiceMemoryDialog = true
            voiceMemoryStatus = null
        } else {
            voiceMemoryStatus = "Microphone permission is required to attach a voice memory."
        }
    }

    LaunchedEffect(activeVoiceMemoryKey) {
        if (consumeVoiceMemoryOpenRequest(context, activeVoiceMemoryKey)) {
            val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                voiceMemoryPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                showVoiceMemoryDialog = true
                voiceMemoryStatus = null
            }
        }
    }

    fun stopVoiceMemoryPlayback() {
        voiceMemoryPlayer?.runCatching {
            if (isPlaying) stop()
            reset()
            release()
        }
        voiceMemoryPlayer = null
        isPlayingVoiceMemory = false
    }

    fun refreshVoiceMemoryPath() {
        activeVoiceMemoryPath = loadClipVoiceMemoryPath(context, activeVoiceMemoryKey)
    }

    fun openVideoEditor(targetUri: Uri) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(targetUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No video editor found", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteVoiceMemory(path: String?) {
        val existingPath = path ?: return
        runCatching { File(existingPath).delete() }
        clearClipVoiceMemoryPath(context, activeVoiceMemoryKey)
        activeVoiceMemoryPath = null
    }

    fun stopVoiceMemoryRecording(saveRecording: Boolean) {
        val recorder = voiceMemoryRecorder ?: return
        val recordedPath = activeVoiceMemoryPath
        runCatching { recorder.stop() }
        runCatching { recorder.reset() }
        runCatching { recorder.release() }
        voiceMemoryRecorder = null
        isRecordingVoiceMemory = false
        if (!saveRecording) {
            deleteVoiceMemory(recordedPath)
            activeVoiceMemoryPath = null
            voiceMemoryStatus = "Voice memory discarded"
        } else if (!recordedPath.isNullOrBlank()) {
            saveClipVoiceMemoryPath(context, activeVoiceMemoryKey, recordedPath)
            activeVoiceMemoryPath = recordedPath
            voiceMemoryStatus = "Voice memory attached"
        }
    }

    fun startVoiceMemoryRecording() {
        val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            voiceMemoryPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        stopVoiceMemoryPlayback()
        deleteVoiceMemory(activeVoiceMemoryPath)
        val outputFile = createVoiceMemoryFile(context, activeVoiceMemoryKey)
        val recorder = buildVoiceMemoryRecorder(context, outputFile)
        try {
            recorder.prepare()
            recorder.start()
            voiceMemoryRecorder = recorder
            activeVoiceMemoryPath = outputFile.absolutePath
            isRecordingVoiceMemory = true
            voiceMemoryStatus = "Recording voice memory..."
        } catch (e: Exception) {
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
            activeVoiceMemoryPath = null
            voiceMemoryStatus = "Could not start voice memory recording"
        }
    }

    fun playVoiceMemory() {
        val path = activeVoiceMemoryPath ?: return
        stopVoiceMemoryPlayback()
        try {
            voiceMemoryPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stopVoiceMemoryPlayback()
                }
                prepare()
                start()
            }
            isPlayingVoiceMemory = true
            voiceMemoryStatus = "Playing voice memory"
        } catch (e: Exception) {
            stopVoiceMemoryPlayback()
            voiceMemoryStatus = "Could not play voice memory"
        }
    }
    val shareRosterPlayers = remember(detectedPlayers) {
        detectedPlayers.sortedWith(
            compareBy<Player>(
                { it.number.toIntOrNull() ?: Int.MAX_VALUE },
                { it.number },
                { it.name }
            )
        )
    }
    val shareContactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val savedUri = shareDialogUri
            if (savedUri != null) {
                val contact = readSelectedContact(context, contactUri)
                if (contact != null) {
                    shareVideoToPhoneContact(context, savedUri, emptyList(), contact)
                    shareDialogUri = null
                }
            }
        }
    }

    val videoProcessingManager = remember {
        VideoProcessingManager(context)
    }

    val buildOverlayForPlayer: (Player, NameBubble, com.playerid.app.video.VideoPlayerDetectionResult) -> IdentifiedOverlayPlayer = { player, bubble, analysisResult ->
        val frameW = analysisResult.frameWidth.takeIf { it > 0 } ?: 1
        val frameH = analysisResult.frameHeight.takeIf { it > 0 } ?: 1
        val track = analysisResult.tracks.firstOrNull { it.playerId == player.id }
        val samples = track?.samples?.map {
            OverlaySample(
                timestampMs = it.timestampMs,
                xFraction = (it.position.x / frameW.toFloat()).coerceIn(0f, 1f),
                yFraction = (it.position.y / frameH.toFloat()).coerceIn(0f, 1f)
            )
        } ?: emptyList()
        val smoothedSamples = smoothOverlaySamples(samples)

        val fallbackSample = OverlaySample(
            timestampMs = 0L,
            xFraction = (bubble.position.x / frameW.toFloat()).coerceIn(0f, 1f),
            yFraction = (bubble.position.y / frameH.toFloat()).coerceIn(0f, 1f)
        )

        IdentifiedOverlayPlayer(
            id = player.id,
            label = playerLastName(player.name),
            samples = if (smoothedSamples.isNotEmpty()) smoothedSamples else listOf(fallbackSample)
        )
    }

    val applyAnalysisResult: (VideoPlayerDetectionResult, Boolean) -> Unit = { analysisResult, openShareDialog ->
        val bubbles = analysisResult.bubbles

        val matchMap = linkedMapOf<String, Pair<Player, NameBubble>>()
        bubbles.forEach { bubble ->
            val player = detectedPlayers.find { it.number == bubble.jerseyNumber } ?: return@forEach
            if (!matchMap.containsKey(player.id)) {
                matchMap[player.id] = player to bubble
            }
        }

        val matchedPlayers = matchMap.values.map { it.first }
        val normalizedOverlays = matchMap.values.map { entry ->
            val player = entry.first
            val bubble = entry.second
            buildOverlayForPlayer(player, bubble, analysisResult)
        }

        if (matchedPlayers.isEmpty()) {
            detectedOverlays = emptyList()
            selectedOverlayIds = emptySet()
            selectedSharePlayerIds = emptySet()
            detectedSharePlayers = emptyList()
            if (openShareDialog) {
                scope.launch {
                    allContactsForShare = loadAllPhoneContactsForShare(context)
                    showQuickSharePicker = true
                }
            } else {
                Toast.makeText(context, "No roster matches found in this play", Toast.LENGTH_SHORT).show()
            }
        } else {
            detectedOverlays = normalizedOverlays
            val initiallySelectedPlayer = matchedPlayers.firstOrNull()
            selectedOverlayIds = initiallySelectedPlayer?.let { setOf(it.id) } ?: emptySet()
            detectedSharePlayers = matchedPlayers
            selectedSharePlayerIds = matchedPlayers.map { it.id }.toSet()
            showDeeperScanPrompt = openShareDialog &&
                detectionMode == VideoProcessingManager.DetectionMode.FAST &&
                matchedPlayers.size <= 3
            if (openShareDialog) {
                // Load all contacts for quick share picker
                scope.launch {
                    allContactsForShare = loadAllPhoneContactsForShare(context)
                    showQuickSharePicker = true
                }
            }
        }
    }

    val runIdentifyAndShare: () -> Unit = {
        if (detectedPlayers.isEmpty()) {
            Toast.makeText(context, "No roster players available for matching", Toast.LENGTH_SHORT).show()
        } else {
            val prepared = VideoSharePreparationCache.get(videoUri)
            if (prepared != null) {
                detectionMode = prepared.mode
                applyAnalysisResult(prepared.analysisResult, true)
            } else {
                scope.launch {
                    // Check persisted database for cached results first
                    val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                    val dao = database.videoDetectionResultDao()
                    val persisted = dao.getDetectionResult(videoUri.toString())
                    if (persisted != null) {
                        val deserialized = com.playerid.app.data.DetectionResultSerializer.deserialize(persisted.detectionJson)
                        if (deserialized != null) {
                            detectionMode = if (persisted.detectionMode == "FAST") 
                                VideoProcessingManager.DetectionMode.FAST 
                            else 
                                VideoProcessingManager.DetectionMode.ACCURATE
                            applyAnalysisResult(deserialized, true)
                            return@launch
                        }
                    }

                    // No persisted result found, run analysis
                    isAnalyzingPlayers = true
                    analysisProgress = 0f
                    val analysisResult = videoProcessingManager.autoDetectPlayersWithTracksInVideo(
                        videoUri = videoUri,
                        roster = detectedPlayers,
                        mode = detectionMode,
                        onProgress = { progress -> analysisProgress = progress.coerceIn(0f, 1f) }
                    )
                    isAnalyzingPlayers = false
                    applyAnalysisResult(analysisResult, true)
                }
            }
        }
    }

    fun refreshDetectionForShareInBackground() {
        if (detectedPlayers.isEmpty()) {
            isPreparingTeamShare = false
            return
        }

        val prepared = VideoSharePreparationCache.get(videoUri)
        if (prepared != null) {
            detectionMode = prepared.mode
            applyAnalysisResult(prepared.analysisResult, false)
            isPreparingTeamShare = false
            return
        }

        scope.launch {
            try {
                val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                val dao = database.videoDetectionResultDao()
                val persisted = dao.getDetectionResult(videoUri.toString())
                if (persisted != null) {
                    val deserialized = com.playerid.app.data.DetectionResultSerializer.deserialize(persisted.detectionJson)
                    if (deserialized != null) {
                        detectionMode = if (persisted.detectionMode == "FAST") {
                            VideoProcessingManager.DetectionMode.FAST
                        } else {
                            VideoProcessingManager.DetectionMode.ACCURATE
                        }
                        applyAnalysisResult(deserialized, false)
                        return@launch
                    }
                }

                // Last resort: run FAST in background without blocking the quick share picker.
                val analysisResult = videoProcessingManager.autoDetectPlayersWithTracksInVideo(
                    videoUri = videoUri,
                    roster = detectedPlayers,
                    mode = VideoProcessingManager.DetectionMode.FAST
                )
                detectionMode = VideoProcessingManager.DetectionMode.FAST
                applyAnalysisResult(analysisResult, false)
                com.playerid.app.video.DeferredDeepScanScheduler.schedule(
                    context = context,
                    videoUri = videoUri,
                    roster = detectedPlayers
                )
            } catch (e: Exception) {
                android.util.Log.d("VideoPlaybackScreen", "Background share detection skipped: ${e.message}")
            } finally {
                isPreparingTeamShare = false
            }
        }
    }

    fun startTeamParentShareFlow() {
        showTeamShareAboutDialog = true
        isPreparingTeamShare = false
    }

    LaunchedEffect(startInShareFlow, videoUri, detectedPlayers) {
        if (startInShareFlow && !autoShareTriggered) {
            autoShareTriggered = true
            showInitialShareDestinationDialog = true
        }
    }

    LaunchedEffect(videoUri, detectedPlayers) {
        // Automatically load persisted detection results on screen init
        if (detectedPlayers.isEmpty()) return@LaunchedEffect
        scope.launch {
            try {
                val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                val dao = database.videoDetectionResultDao()
                val persisted = dao.getDetectionResult(videoUri.toString())
                if (persisted != null) {
                    val deserialized = com.playerid.app.data.DetectionResultSerializer.deserialize(persisted.detectionJson)
                    if (deserialized != null) {
                        detectionMode = if (persisted.detectionMode == "FAST") 
                            VideoProcessingManager.DetectionMode.FAST 
                        else 
                            VideoProcessingManager.DetectionMode.ACCURATE
                        applyAnalysisResult(deserialized, false)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("VideoPlaybackScreen", "Could not load persisted detection: ${e.message}")
            }
        }
    }

    LaunchedEffect(activeVoiceMemoryKey) {
        refreshVoiceMemoryPath()
        voiceMemoryStatus = null
        stopVoiceMemoryPlayback()
        if (isRecordingVoiceMemory) {
            stopVoiceMemoryRecording(saveRecording = false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecordingVoiceMemory) {
                stopVoiceMemoryRecording(saveRecording = false)
            }
            stopVoiceMemoryPlayback()
        }
    }

    LaunchedEffect(shareDialogUri) {
        if (shareDialogUri != null && detectedPlayers.isNotEmpty()) {
            selectedSharePlayerIds = emptySet()
            suggestedSharePlayerIds = emptySet()
            showSharePlayerList = false
            scanShareSuggestionsDone = false
            
            // Trigger quick player detection with FAST mode
            isLoadingSuggestedPlayers = true
            scope.launch(Dispatchers.Default) {
                try {
                    val detectionResult = videoProcessingManager.autoDetectPlayersWithTracksInVideo(
                        videoUri = shareDialogUri!!,
                        roster = detectedPlayers,
                        mode = VideoProcessingManager.DetectionMode.FAST
                    )
                    
                    // Match detected jersey numbers with roster players
                    val detected = detectionResult.bubbles.mapNotNull { bubble ->
                        detectedPlayers.find { it.number == bubble.jerseyNumber }?.id
                    }.toSet()
                    
                    // Pre-select suggested players
                    suggestedSharePlayerIds = detected
                    if (detected.isNotEmpty()) {
                        selectedSharePlayerIds = detected
                    }
                    com.playerid.app.video.DeferredDeepScanScheduler.schedule(
                        context = context,
                        videoUri = shareDialogUri!!,
                        roster = detectedPlayers
                    )
                } catch (e: Exception) {
                    Log.d("VideoPlaybackScreen", "Player detection failed: ${e.message}")
                } finally {
                    isLoadingSuggestedPlayers = false
                          scanShareSuggestionsDone = true
                }
            }
        }
    }

    val applyManualLockTap: (Float, Float) -> Unit = { tapXFraction, tapYFraction ->
        val selectedId = selectedOverlayIds.firstOrNull()
        if (selectedId != null) {
            detectedOverlays = detectedOverlays.map { overlay ->
                if (overlay.id != selectedId) return@map overlay
                val adjusted = reanchorSamplesAtTimestamp(
                    samples = overlay.samples,
                    playbackPositionMs = currentPosition,
                    targetXFraction = tapXFraction,
                    targetYFraction = tapYFraction
                )
                overlay.copy(samples = adjusted)
            }
            isLockAssistMode = false
        }
    }

    val exoPlayer = remember(showPlaybackUi, playlistUris, videoUri, isPlaylistMode, composedReelPlaybackUri) {
        if (!showPlaybackUi) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setSeekParameters(SeekParameters.EXACT)
                if (isPlaylistMode) {
                    playlistUris.forEach { uri ->
                        addMediaItem(MediaItem.fromUri(uri))
                    }
                    repeatMode = Media3Player.REPEAT_MODE_OFF // Don't repeat in playlist mode
                } else {
                    setMediaItem(MediaItem.fromUri(composedReelPlaybackUri ?: videoUri))
                    repeatMode = if (composedReelPlaybackUri != null) {
                        Media3Player.REPEAT_MODE_OFF
                    } else {
                        Media3Player.REPEAT_MODE_ONE
                    }
                }
                prepare()
            }
        }
    }

    DisposableEffect(activeScrubUri) {
        scrubPreviewBitmap = null
        scrubPreviewRequestMs = -1L
        scrubPreviewRenderedKey = -1
        scrubFrameCache.clear()

        val retriever = MediaMetadataRetriever()
        val prepared = runCatching {
            retriever.setDataSource(context, activeScrubUri)
        }.isSuccess

        scrubRetriever = if (prepared) retriever else null
        scrubPreviewFrameCount = if (prepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                ?.toIntOrNull()
                ?: 0
        } else {
            0
        }

        if (!prepared) {
            runCatching { retriever.release() }
        }

        onDispose {
            scrubRetriever = null
            scrubPreviewFrameCount = 0
            scrubFrameCache.clear()
            runCatching { retriever.release() }
        }
    }

    LaunchedEffect(isSeeking, scrubRetriever, videoDuration, scrubPreviewFrameCount) {
        val retriever = scrubRetriever ?: return@LaunchedEffect
        if (!isSeeking) {
            scrubPreviewBitmap = null
            scrubPreviewRenderedKey = -1
            return@LaunchedEffect
        }

        val maxCachedFrames = 300
        while (isSeeking) {
            val requestedMs = scrubPreviewRequestMs
            if (requestedMs >= 0L) {
                val frameKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && scrubPreviewFrameCount > 0 && videoDuration > 0L) {
                    ((requestedMs.toDouble() / videoDuration.toDouble()) * (scrubPreviewFrameCount - 1).toDouble())
                        .toInt()
                        .coerceIn(0, scrubPreviewFrameCount - 1)
                } else {
                    (requestedMs / 33L).toInt()
                }

                if (frameKey != scrubPreviewRenderedKey) {
                    val cached = scrubFrameCache[frameKey]
                    val bitmap = cached ?: withContext(Dispatchers.Default) {
                        runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && scrubPreviewFrameCount > 0 && videoDuration > 0L) {
                                retriever.getFrameAtIndex(frameKey)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                retriever.getScaledFrameAtTime(
                                    requestedMs * 1000L,
                                    MediaMetadataRetriever.OPTION_CLOSEST,
                                    480,
                                    270
                                )
                            } else {
                                retriever.getFrameAtTime(
                                    requestedMs * 1000L,
                                    MediaMetadataRetriever.OPTION_CLOSEST
                                )
                            }
                        }.getOrNull()
                    }

                    if (bitmap != null) {
                        if (cached == null) {
                            scrubFrameCache[frameKey] = bitmap
                            if (scrubFrameCache.size > maxCachedFrames) {
                                val eldestKey = scrubFrameCache.keys.firstOrNull()
                                if (eldestKey != null) scrubFrameCache.remove(eldestKey)
                            }
                        }
                        scrubPreviewBitmap = bitmap
                        scrubPreviewRenderedKey = frameKey
                    }
                }
            }
            delay(12)
        }
    }


    // Track playback progress
    LaunchedEffect(isPlaying, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (isPlaying) {
            player.play()
        } else if (!isTransitioning) {
            player.pause()
        }
    }

    // Update current position periodically

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            val latestDuration = player.duration
            if (latestDuration > 0L && latestDuration != videoDuration) {
                videoDuration = latestDuration
            }
            if (!isSeeking) {
                currentPosition = player.currentPosition
                seekFraction = if (videoDuration > 0L) {
                    (currentPosition.toFloat() / videoDuration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
            if (isPlaylistMode) {
                currentVideoIndex = player.currentMediaItemIndex.coerceIn(0, maxOf(0, playlistUris.size - 1))
            }
            delay(if (isPlaying) 16 else 100)
        }
    }

    LaunchedEffect(transitionPulse) {
        if (transitionPulse == 0) return@LaunchedEffect
        showClipTransitionOverlay = true
        delay(300)
        showClipTransitionOverlay = false
    }

    LaunchedEffect(isPlaylistMode, isPlaying, currentPosition, videoDuration) {
        if (!isPlaylistMode || !isPlaying || videoDuration <= 0L || showClipTransitionOverlay) return@LaunchedEffect

        val remainingMs = videoDuration - currentPosition
        if (remainingMs in 1..300L) {
            showClipTransitionOverlay = true
        }
    }

    // Update playback state
    DisposableEffect(exoPlayer) {
        val player = exoPlayer
        val listener = if (player != null) {
            object : Media3Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Media3Player.STATE_READY) {
                        videoDuration = player.duration
                        if (!isSeeking) {
                            val playerPosition = player.currentPosition
                            seekFraction = if (player.duration > 0L) {
                                (playerPosition.toFloat() / player.duration.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        }
                    } else if (state == Media3Player.STATE_ENDED) {
                        // Only show replay overlay if on last clip of playlist
                        if (isPlaylistMode && player.currentMediaItemIndex == totalVideos - 1) {
                            showReplayOverlay = true
                        }
                        // Keep UI pinned to clip end so the final reel clip doesn't appear truncated.
                        if (videoDuration > 0L) {
                            currentPosition = videoDuration
                            seekFraction = 1f
                        }
                    }
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    if (isPlaylistMode) {
                        isTransitioning = true
                        currentVideoIndex = player.currentMediaItemIndex.coerceIn(0, maxOf(0, totalVideos - 1))
                        currentPosition = 0L
                        transitionPulse += 1
                        val newDuration = player.duration
                        if (newDuration > 0L) {
                            videoDuration = newDuration
                        }
                        isPlaying = true
                        player.play()
                    }
                }
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    if (isTransitioning && isPlayingNow) {
                        isTransitioning = false
                    }
                    isPlaying = isPlayingNow
                }
            }
        } else {
            null
        }

        if (player != null && listener != null) {
            player.addListener(listener)
        }

        onDispose {
            if (player != null && listener != null) {
                player.removeListener(listener)
                player.release()
            }
            videoProcessingManager.release()
        }
    }

    if (showPlaybackUi) {
    // System back button and swipe-left-on-first-clip closes the screen
    BackHandler(enabled = true) {
        onNavigateBack()
    }

        Scaffold(
        topBar = {
            if (!isPlaylistMode) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        Surface(
                            onClick = {
                                if (onEditReel != null) {
                                    onEditReel()
                                } else {
                                    val editUri = if (isPlaylistMode && currentVideoIndex in playlistUris.indices) {
                                        playlistUris[currentVideoIndex]
                                    } else {
                                        videoUri
                                    }
                                    openVideoEditor(editUri)
                                }
                            },
                            color = Color.Transparent,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = if (onEditReel != null) "Edit Reel" else "Edit Clip",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    if (onEditReel != null) "Edit Reel" else "Edit Clip",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (!permissionGranted) {
                                    voiceMemoryPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    showVoiceMemoryDialog = true
                                    voiceMemoryStatus = null
                                }
                            },
                            color = Color.Transparent,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = voiceMemoryActionLabel,
                                    tint = if (!activeVoiceMemoryPath.isNullOrBlank()) Color(0xFFFFD54F) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    voiceMemoryActionLabel,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Video player
            Box(
                modifier = if (isPlaylistMode) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RectangleShape)
                        .pointerInput(videoUri, currentVideoIndex) {
                            detectTransformGestures { _, _, zoomChange, _ ->
                                playbackZoom = (playbackZoom * zoomChange).coerceIn(1f, 4f)
                            }
                        }
                            .pointerInput(isPlaylistMode, exoPlayer) {
                                if (!isPlaylistMode) return@pointerInput
                                val swipeThreshold = 80.dp.toPx()
                                awaitEachGesture {
                                    var totalDragX = 0f
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var dragging = true
                                    while (dragging) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            dragging = false
                                        } else {
                                            totalDragX += (change.position.x - change.previousPosition.x)
                                        }
                                    }
                                    if (abs(totalDragX) > swipeThreshold) {
                                        if (totalDragX < 0) {
                                            exoPlayer?.seekToNextMediaItem()
                                        } else {
                                            exoPlayer?.seekToPreviousMediaItem()
                                        }
                                    }
                                }
                            }
                        .graphicsLayer {
                                alpha = if (showClipTransitionOverlay) 0.96f else 1f
                                scaleX = playbackZoom * clipTransitionScale
                                scaleY = playbackZoom * clipTransitionScale
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                runCatching {
                                    javaClass.getMethod("setSurfaceType", Int::class.javaPrimitiveType)
                                        .invoke(this, 2)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Only show transition overlay if not on last clip
                    if (clipTransitionAlpha > 0f && !(isPlaylistMode && currentVideoIndex == totalVideos - 1)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = clipTransitionAlpha))
                        )
                    }
                    // Centered replay overlay when playlist ends
                    if (showReplayOverlay) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable {
                                    // Hide overlay and restart playback
                                    showReplayOverlay = false
                                    isTransitioning = true
                                    exoPlayer?.seekToDefaultPosition(0)
                                    currentVideoIndex = 0
                                    currentPosition = 0L
                                    seekFraction = 0f
                                    isPlaying = true
                                    exoPlayer?.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.7f),
                                shadowElevation = 8.dp,
                                modifier = Modifier.size(96.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Replay",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp).padding(16.dp)
                                )
                            }
                        }
                    }

                    if (reelIntroAlpha > 0.01f && isPlaylistMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(reelIntroAlpha)
                                .background(
                                    Brush.verticalGradient(
                                        colors = reelOverlayGradient
                                    )
                                )
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = Color(0xD0121A1F),
                                tonalElevation = 0.dp,
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .border(
                                        width = 1.6.dp,
                                        color = reelAccentColor.copy(alpha = 0.58f),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 22.dp)
                                ) {
                                    if (resolvedReelScenario != "opponent") {
                                        if (reelScenarioSubheader.isNotBlank()) {
                                            Text(
                                                text = reelScenarioSubheader,
                                                color = Color.White.copy(alpha = 0.82f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    if (resolvedReelScenario == "opponent") {
                                        val opponentTeamName = reelTeamName?.trim().orEmpty()
                                        Text(
                                            text = opponentTeamName.uppercase(),
                                            color = Color.White.copy(alpha = 0.93f),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "VS",
                                            color = reelAccentColor,
                                            style = MaterialTheme.typography.headlineLarge,
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = if (primaryOpponent.isBlank()) "OPPONENT" else primaryOpponent.uppercase(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = reelTitle ?: "Reel",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = reelHeroLine,
                                            color = reelAccentColor,
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    HorizontalDivider(
                                        color = reelAccentColor.copy(alpha = 0.45f),
                                        thickness = 1.2.dp,
                                        modifier = Modifier
                                            .fillMaxWidth(0.72f)
                                            .padding(top = 2.dp, bottom = 2.dp)
                                    )
                                    Text(
                                        text = reelClipCountLabel,
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!reelSeasonLabel.isNullOrBlank() && resolvedReelScenario != "season") {
                                        Text(
                                            text = reelSeasonLabel,
                                            color = Color.White.copy(alpha = 0.82f),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (resolvedReelScenario != "opponent") {
                                        Text(
                                            text = reelOpponentsLabel,
                                            color = Color.White.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val previewBitmap = scrubPreviewBitmap
                    if (isSeeking && previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }



                    if (detectedOverlays.isNotEmpty()) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isLockAssistMode, selectedOverlayIds, currentPosition, detectedOverlays) {
                                    if (!isLockAssistMode) return@pointerInput
                                    detectTapGestures { tapOffset ->
                                        val width = size.width.toFloat().coerceAtLeast(1f)
                                        val height = size.height.toFloat().coerceAtLeast(1f)
                                        val tapXFraction = (tapOffset.x / width).coerceIn(0f, 1f)
                                        val tapYFraction = (tapOffset.y / height).coerceIn(0f, 1f)
                                        applyManualLockTap(tapXFraction, tapYFraction)
                                    }
                                }
                        ) {
                            val density = LocalDensity.current
                            val widthPx = with(density) { maxWidth.toPx() }
                            val heightPx = with(density) { maxHeight.toPx() }

                            detectedOverlays.filter { selectedOverlayIds.contains(it.id) }.forEach { overlay ->
                                val lockSample = nearestTrackedSample(overlay.samples, currentPosition)
                                val sample = interpolatedOverlaySample(overlay.samples, currentPosition)
                                TrackingLockBox(
                                    anchorX = (widthPx * lockSample.xFraction).coerceIn(0f, widthPx),
                                    anchorY = (heightPx * lockSample.yFraction).coerceIn(0f, heightPx),
                                    containerWidthPx = widthPx,
                                    containerHeightPx = heightPx
                                )
                                AnchoredPlayerLabelBubble(
                                    label = overlay.label,
                                    anchorX = (widthPx * sample.xFraction).coerceIn(0f, widthPx),
                                    anchorY = (heightPx * sample.yFraction).coerceIn(0f, heightPx),
                                    containerWidthPx = widthPx,
                                    containerHeightPx = heightPx
                                )
                            }

                            if (isLockAssistMode) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp),
                                    color = Color.Black.copy(alpha = 0.72f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "Tap player to lock bubble",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isPlaylistMode) {
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (onSaveAsGoatReel != null && activeReelId == null) {
                            FilledTonalButton(
                                onClick = {
                                    reelNameInput = reelTitle ?: "My Reel"
                                    showSaveReelDialog = true
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFFD54F),
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isReviewMode && activeReelId != null) {
                            FilledTonalButton(
                                onClick = {
                                    selectedSharePlayerIds = emptySet()
                                    showSharePlayerList = false
                                    resolveShareUri { uri ->
                                        shareDialogUri = uri
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF4FC3F7),
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box {
                            IconButton(onClick = { showPreviewActionsMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More actions",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showPreviewActionsMenu,
                                onDismissRequest = { showPreviewActionsMenu = false }
                            ) {
                                if (!isReviewMode) {
                                    DropdownMenuItem(
                                        text = { Text(voiceMemoryActionLabel) },
                                        onClick = {
                                            showPreviewActionsMenu = false
                                            val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (!permissionGranted) {
                                                voiceMemoryPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                showVoiceMemoryDialog = true
                                                voiceMemoryStatus = null
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Mic, contentDescription = null)
                                        }
                                    )
                                }

                                if (detectedOverlays.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Names (${selectedOverlayIds.size})") },
                                        onClick = {
                                            showPreviewActionsMenu = false
                                            if (!isAnalyzingPlayers) {
                                                showOverlaySelectionDialog = true
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Badge, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isLockAssistMode) "Cancel Lock" else "Lock Here") },
                                        onClick = {
                                            showPreviewActionsMenu = false
                                            if (selectedOverlayIds.isEmpty()) {
                                                Toast.makeText(context, "Select a player name first", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isLockAssistMode = !isLockAssistMode
                                            }
                                        },
                                        enabled = !isAnalyzingPlayers,
                                        leadingIcon = {
                                            Icon(Icons.Default.CenterFocusStrong, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Prev / Next clip navigation arrows (Box scope overlay)
                    if (currentVideoIndex > 0) {
                        IconButton(
                            onClick = { exoPlayer?.seekToPreviousMediaItem() },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 12.dp)
                                .background(Color.Black.copy(alpha = 0.40f), CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous clip",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    if (currentVideoIndex < totalVideos - 1) {
                        IconButton(
                            onClick = { exoPlayer?.seekToNextMediaItem() },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp)
                                .background(Color.Black.copy(alpha = 0.40f), CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next clip",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (videoDuration > 0) {
                            VideoSeekBar(
                                fraction = sliderFraction,
                                onSeek = { fraction ->
                                    if (!isSeeking) {
                                        wasPlayingBeforeSeek = isPlaying
                                        isPlaying = false
                                    }
                                    isSeeking = true
                                    seekFraction = fraction
                                    if (videoDuration > 0L) {
                                        val pos = (fraction * videoDuration.toFloat()).toLong().coerceIn(0L, videoDuration)
                                        currentPosition = pos
                                        scrubPreviewRequestMs = pos
                                    }
                                },
                                onSeekFinished = {
                                    val player = exoPlayer
                                    if (player != null && videoDuration > 0L) {
                                        player.setSeekParameters(SeekParameters.EXACT)
                                        val seekPosition = (seekFraction * videoDuration.toFloat()).toLong().coerceIn(0L, videoDuration)
                                        player.seekTo(seekPosition)
                                        currentPosition = seekPosition
                                    }
                                    scrubPreviewRequestMs = -1L
                                    scrubPreviewRenderedKey = -1
                                    scrubPreviewBitmap = null
                                    isSeeking = false
                                    if (wasPlayingBeforeSeek) {
                                        isPlaying = true
                                        wasPlayingBeforeSeek = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        IconButton(onClick = { isPlaying = !isPlaying }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            if (showSaveReelDialog && onSaveAsGoatReel != null) {
                AlertDialog(
                    onDismissRequest = { showSaveReelDialog = false },
                    title = { Text("Create Reel") },
                    text = {
                        OutlinedTextField(
                            value = reelNameInput,
                            onValueChange = { reelNameInput = it },
                            label = { Text("Reel name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val name = reelNameInput.trim()
                                if (name.isNotBlank()) {
                                    onSaveAsGoatReel(name)
                                    showSaveReelDialog = false
                                }
                            }
                        ) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveReelDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showVoiceMemoryDialog) {
                Dialog(
                    onDismissRequest = {
                        if (!isRecordingVoiceMemory) {
                            stopVoiceMemoryPlayback()
                            showVoiceMemoryDialog = false
                        }
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Title
                            Text(
                                "Voice Memory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Big mic / status button
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    onClick = {
                                        when {
                                            isRecordingVoiceMemory -> stopVoiceMemoryRecording(saveRecording = true)
                                            isPlayingVoiceMemory -> stopVoiceMemoryPlayback()
                                            !activeVoiceMemoryPath.isNullOrBlank() -> playVoiceMemory()
                                            else -> startVoiceMemoryRecording()
                                        }
                                    },
                                    shape = CircleShape,
                                    color = when {
                                        isRecordingVoiceMemory -> Color(0xFFD32F2F)
                                        isPlayingVoiceMemory -> MaterialTheme.colorScheme.primaryContainer
                                        !activeVoiceMemoryPath.isNullOrBlank() -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = when {
                                                isRecordingVoiceMemory -> Icons.Default.Stop
                                                isPlayingVoiceMemory -> Icons.Default.Stop
                                                !activeVoiceMemoryPath.isNullOrBlank() -> Icons.Default.PlayArrow
                                                else -> Icons.Default.Mic
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                isRecordingVoiceMemory -> Color.White
                                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                                            },
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // Single-line status label
                            Text(
                                text = when {
                                    isRecordingVoiceMemory -> "Recording — tap to save"
                                    isPlayingVoiceMemory -> "Playing — tap to stop"
                                    !activeVoiceMemoryPath.isNullOrBlank() -> "Tap to play"
                                    else -> "Tap to record"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Secondary actions row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!isRecordingVoiceMemory && !activeVoiceMemoryPath.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { startVoiceMemoryRecording() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Re-record", fontSize = 13.sp)
                                    }
                                }
                                if (isRecordingVoiceMemory) {
                                    OutlinedButton(
                                        onClick = { stopVoiceMemoryRecording(saveRecording = false) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Discard", fontSize = 13.sp)
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        if (!isRecordingVoiceMemory) {
                                            stopVoiceMemoryPlayback()
                                            showVoiceMemoryDialog = false
                                        }
                                    },
                                    enabled = !isRecordingVoiceMemory,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (!isPlaylistMode) {
                // Playback progress bar
                if (videoDuration > 0) {
                    VideoSeekBar(
                        fraction = sliderFraction,
                        onSeek = { fraction ->
                            if (!isSeeking) {
                                wasPlayingBeforeSeek = isPlaying
                                isPlaying = false
                            }
                            isSeeking = true
                            seekFraction = fraction
                            if (videoDuration > 0L) {
                                val pos = (fraction * videoDuration.toFloat()).toLong().coerceIn(0L, videoDuration)
                                currentPosition = pos
                                scrubPreviewRequestMs = pos
                            }
                        },
                        onSeekFinished = {
                            val player = exoPlayer
                            if (player != null && videoDuration > 0L) {
                                player.setSeekParameters(SeekParameters.EXACT)
                                val seekPosition = (seekFraction * videoDuration.toFloat()).toLong().coerceIn(0L, videoDuration)
                                player.seekTo(seekPosition)
                                currentPosition = seekPosition
                            }
                            scrubPreviewRequestMs = -1L
                            scrubPreviewRenderedKey = -1
                            scrubPreviewBitmap = null
                            isSeeking = false
                            if (wasPlayingBeforeSeek) {
                                isPlaying = true
                                wasPlayingBeforeSeek = false
                            }
                        },
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        progressColor = MaterialTheme.colorScheme.primary,
                        thumbColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Playback controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying }
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        "${(displayedPosition / 1000 / 60) % 60}:${(displayedPosition / 1000) % 60} / " +
                        "${(videoDuration / 1000 / 60) % 60}:${(videoDuration / 1000) % 60}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

        }
        }
    }

    if (showDeeperScanPrompt) {
        AlertDialog(
            onDismissRequest = { showDeeperScanPrompt = false },
            title = { Text("Run Deeper Scan?") },
            text = {
                Text("Fast mode found ${detectedSharePlayers.size} players. Slow mode may find more, but takes longer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeeperScanPrompt = false
                        detectionMode = VideoProcessingManager.DetectionMode.ACCURATE
                        runIdentifyAndShare()
                    }
                ) {
                    Text("Run Slow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeeperScanPrompt = false }) {
                    Text("Keep Fast")
                }
            }
        )
    }

    if (showInitialShareDestinationDialog) {
        AlertDialog(
            onDismissRequest = {
                showInitialShareDestinationDialog = false
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Share",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            showInitialShareDestinationDialog = false
                            startTeamParentShareFlow()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Team Parents")
                    }
                    OutlinedButton(
                        onClick = {
                            resolveShareUri { uri ->
                                launchPersonalShareChooser(
                                    context = context,
                                    videoUri = uri,
                                    shareTitle = "Share to My Contacts"
                                )
                                showInitialShareDestinationDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("My Contacts")
                    }
                }
            },
            confirmButton = {}
        )
    }

    shareDialogUri?.let { savedUri ->
        Dialog(
            onDismissRequest = { shareDialogUri = null },
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
                        "Share this moment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // ── Suggested Players ────────────────────────────────
                    if (isLoadingSuggestedPlayers) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Scanning for players...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (suggestedSharePlayerIds.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Suggested Players",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                suggestedSharePlayerIds.forEach { playerId ->
                                    detectedPlayers.find { it.id == playerId }?.let { player ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedSharePlayerIds = if (selectedSharePlayerIds.contains(player.id)) {
                                                        selectedSharePlayerIds - player.id
                                                    } else {
                                                        selectedSharePlayerIds + player.id
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedSharePlayerIds.contains(player.id),
                                                onCheckedChange = { checked ->
                                                    selectedSharePlayerIds = if (checked) {
                                                        selectedSharePlayerIds + player.id
                                                    } else {
                                                        selectedSharePlayerIds - player.id
                                                    }
                                                }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "#${player.number} ${player.name}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (scanShareSuggestionsDone) {
                        Text(
                            "No players detected in this clip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        "Team Parents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (detectedPlayers.isEmpty()) {
                        Text(
                            "No players on roster yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSharePlayerList = !showSharePlayerList },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (selectedSharePlayerIds.isEmpty()) "Choose players"
                                else "${selectedSharePlayerIds.size} player${if (selectedSharePlayerIds.size == 1) "" else "s"} selected",
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
                                    selectedSharePlayerIds = shareRosterPlayers
                                        .filter { it.addedBy.any(Char::isDigit) && it.addedBy.filter(Char::isDigit).length >= 10 }
                                        .map { it.id }
                                        .toSet()
                                }) {
                                    Text("Select all")
                                }
                                if (selectedSharePlayerIds.isNotEmpty()) {
                                    TextButton(onClick = { selectedSharePlayerIds = emptySet() }) {
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
                                                selectedSharePlayerIds = if (selectedSharePlayerIds.contains(player.id)) {
                                                    selectedSharePlayerIds - player.id
                                                } else {
                                                    selectedSharePlayerIds + player.id
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (hasParentContact) {
                                            Checkbox(
                                                checked = selectedSharePlayerIds.contains(player.id),
                                                onCheckedChange = { checked ->
                                                    selectedSharePlayerIds = if (checked) {
                                                        selectedSharePlayerIds + player.id
                                                    } else {
                                                        selectedSharePlayerIds - player.id
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
                        }
                        val selectedPlayers = shareRosterPlayers.filter { selectedSharePlayerIds.contains(it.id) }
                        if (selectedSharePlayerIds.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val recipients = buildTeamShareRecipients(selectedPlayers)
                                    if (recipients.isEmpty()) {
                                        launchPersonalShareChooser(context, savedUri, "Share this moment")
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
                                    shareDialogUri = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Send to ${selectedSharePlayerIds.size} parent${if (selectedSharePlayerIds.size == 1) "" else "s"}"
                                )
                            }
                        }
                    }

                    HorizontalDivider()

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
                        onClick = { shareDialogUri = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    if (showTeamShareAboutDialog) {
        AlertDialog(
            onDismissRequest = { showTeamShareAboutDialog = false },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Choose Players",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedSharePlayerIds = emptySet()
                                teamShareMessage = ""
                                showHighlightTags = false
                                showShareSuggestions = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Select Manually")
                        }
                        Button(
                            onClick = {
                                if (!isPreparingTeamShare) {
                                    isPreparingTeamShare = true
                                    refreshDetectionForShareInBackground()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isPreparingTeamShare
                        ) {
                            Text(if (isPreparingTeamShare) "Scanning..." else "Scan Players")
                        }
                    }

                    if (selectedPlayersForShare.isNotEmpty()) {
                        Button(
                            onClick = {
                                val recipients = buildTeamShareRecipients(selectedPlayersForShare)
                                if (recipients.isEmpty()) {
                                    Toast.makeText(context, "No recipients available", Toast.LENGTH_SHORT).show()
                                } else {
                                    resolveShareUri { uri ->
                                        shareVideoToTeamRecipients(
                                            context = context,
                                            videoUri = uri,
                                            recipients = recipients,
                                            players = selectedPlayersForShare,
                                            highlightTag = selectedHighlightTag,
                                            customMessage = teamShareMessage
                                        )
                                        showTeamShareAboutDialog = false
                                        onNavigateBack()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue")
                        }
                    }

                    if (selectedPlayersForShare.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(selectedPlayersForShare, key = { it.id }) { player ->
                                FilterChip(
                                    selected = selectedSharePlayerIds.contains(player.id),
                                    onClick = {
                                        selectedSharePlayerIds = if (selectedSharePlayerIds.contains(player.id)) {
                                            selectedSharePlayerIds - player.id
                                        } else {
                                            selectedSharePlayerIds + player.id
                                        }
                                    },
                                    label = { Text(player.name) }
                                )
                            }
                        }
                    } else if (isPreparingTeamShare) {
                        Text(
                            "Scanning jersey numbers...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (showShareSuggestions) {
        Dialog(
            onDismissRequest = { showShareSuggestions = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "Choose Players to Share",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(detectedPlayers, key = { it.id }) { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSharePlayerIds = if (selectedSharePlayerIds.contains(player.id)) {
                                            selectedSharePlayerIds - player.id
                                        } else {
                                            selectedSharePlayerIds + player.id
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedSharePlayerIds.contains(player.id),
                                    onCheckedChange = { isChecked ->
                                        selectedSharePlayerIds = if (isChecked) {
                                            selectedSharePlayerIds + player.id
                                        } else {
                                            selectedSharePlayerIds - player.id
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "#${player.number} ${player.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showHighlightTags = !showHighlightTags },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showHighlightTags) "Hide Tag" else "Add Tag")
                    }

                    if (showHighlightTags) {
                        val keyboardController = LocalSoftwareKeyboardController.current
                        OutlinedTextField(
                            value = teamShareMessage,
                            onValueChange = { teamShareMessage = it },
                            label = { Text("Custom Tag") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { keyboardController?.hide() }
                            )
                        )
                    }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showShareSuggestions = false; showTeamShareAboutDialog = false }) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        detectedSharePlayers = detectedPlayers.filter { selectedSharePlayerIds.contains(it.id) }
                        val recipients = buildTeamShareRecipients(detectedSharePlayers)
                        if (recipients.isEmpty()) {
                            Toast.makeText(context, "No recipients available", Toast.LENGTH_SHORT).show()
                        } else {
                            resolveShareUri { uri ->
                                shareVideoToTeamRecipients(
                                    context = context,
                                    videoUri = uri,
                                    recipients = recipients,
                                    players = detectedSharePlayers,
                                    highlightTag = selectedHighlightTag,
                                    customMessage = teamShareMessage
                                )
                                showShareSuggestions = false
                                showTeamShareAboutDialog = false
                                onNavigateBack()
                            }
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        } // end Column
        } // end Surface
        } // end Dialog
    }


    if (showQuickSharePicker) {
        AlertDialog(
            onDismissRequest = { showQuickSharePicker = false },
            title = { Text("Share this moment with") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Sending this moment with ${detectedSharePlayers.size} identified player${if (detectedSharePlayers.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (favoritePhoneContacts.isNotEmpty()) {
                        Text(
                            "Favorites",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(favoritePhoneContacts, key = { it.cacheKey }) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            resolveShareUri { uri ->
                                                shareVideoToPhoneContact(
                                                    context = context,
                                                    videoUri = uri,
                                                    players = selectedPlayersForShare,
                                                    contact = contact
                                                )
                                                showQuickSharePicker = false
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(contact.phoneNumber ?: contact.email ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        "All Contacts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allContactsForShare, key = { it.cacheKey }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        resolveShareUri { uri ->
                                            shareVideoToPhoneContact(
                                                context = context,
                                                videoUri = uri,
                                                players = selectedPlayersForShare,
                                                contact = contact
                                            )
                                            showQuickSharePicker = false
                                            favoritePhoneContacts = addFavoritePhoneContact(context, contact)
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(contact.phoneNumber ?: contact.email ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQuickSharePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showOverlaySelectionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlaySelectionDialog = false },
            title = { Text("Select Names to Show") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Choose which player last names appear on video overlays.")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedOverlayIds = detectedOverlays.firstOrNull()?.let { setOf(it.id) } ?: emptySet()
                        }) {
                            Text("Select First")
                        }
                        TextButton(onClick = { selectedOverlayIds = emptySet() }) {
                            Text("Clear")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(detectedOverlays, key = { it.id }) { overlay ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    overlay.label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                RadioButton(
                                    selected = selectedOverlayIds.contains(overlay.id),
                                    onClick = {
                                        selectedOverlayIds = if (selectedOverlayIds.contains(overlay.id)) {
                                            emptySet()
                                        } else {
                                            setOf(overlay.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOverlaySelectionDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

private fun voiceMemoryPrefs(context: Context) =
    context.getSharedPreferences("clip_voice_memories", Context.MODE_PRIVATE)

private fun loadClipVoiceMemoryPath(context: Context, clipKey: String): String? =
    voiceMemoryPrefs(context).getString(clipKey, null)?.takeIf { it.isNotBlank() }

private fun saveClipVoiceMemoryPath(context: Context, clipKey: String, filePath: String) {
    voiceMemoryPrefs(context).edit().putString(clipKey, filePath).apply()
}

private fun clearClipVoiceMemoryPath(context: Context, clipKey: String) {
    voiceMemoryPrefs(context).edit().remove(clipKey).apply()
}

private fun consumeVoiceMemoryOpenRequest(context: Context, clipKey: String): Boolean {
    val prefs = context.getSharedPreferences("clip_voice_memory_open_requests", Context.MODE_PRIVATE)
    val shouldOpen = prefs.getBoolean(clipKey, false)
    if (shouldOpen) {
        prefs.edit().remove(clipKey).apply()
    }
    return shouldOpen
}

private fun createVoiceMemoryFile(context: Context, clipKey: String): File {
    val directory = File(context.filesDir, "voice_memories").apply { mkdirs() }
    return File(directory, "voice_${clipKey.hashCode()}_${System.currentTimeMillis()}.m4a")
}

private fun buildVoiceMemoryRecorder(context: Context, outputFile: File): MediaRecorder {
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    recorder.setAudioEncodingBitRate(128000)
    recorder.setAudioSamplingRate(44100)
    recorder.setOutputFile(outputFile.absolutePath)
    return recorder
}

private data class IdentifiedOverlayPlayer(
    val id: String,
    val label: String,
    val samples: List<OverlaySample>
)

private data class OverlaySample(
    val timestampMs: Long,
    val xFraction: Float,
    val yFraction: Float
)

// Fast mode samples around every 1500ms, so keep this above that to avoid pinned bubbles.
private const val MAX_INTERPOLATION_GAP_MS = 2200L
private const val TRACK_SMOOTHING_ALPHA = 0.8f
private const val MAX_TRACK_STEP_FRACTION = 0.09f

private fun nearestTrackedSample(samples: List<OverlaySample>, playbackPositionMs: Long): OverlaySample {
    if (samples.isEmpty()) return OverlaySample(0L, 0.5f, 0.5f)
    if (samples.size == 1) return samples.first()

    var best = samples.first()
    var bestDelta = abs(best.timestampMs - playbackPositionMs)
    for (i in 1 until samples.size) {
        val candidate = samples[i]
        val delta = abs(candidate.timestampMs - playbackPositionMs)
        if (delta < bestDelta) {
            best = candidate
            bestDelta = delta
        }
    }
    return best
}

private fun reanchorSamplesAtTimestamp(
    samples: List<OverlaySample>,
    playbackPositionMs: Long,
    targetXFraction: Float,
    targetYFraction: Float
): List<OverlaySample> {
    if (samples.isEmpty()) return samples
    if (samples.size == 1) {
        return listOf(
            samples.first().copy(
                xFraction = targetXFraction.coerceIn(0f, 1f),
                yFraction = targetYFraction.coerceIn(0f, 1f)
            )
        )
    }

    val sorted = samples.sortedBy { it.timestampMs }
    var anchorIndex = 0
    var bestDelta = abs(sorted.first().timestampMs - playbackPositionMs)
    for (i in 1 until sorted.size) {
        val delta = abs(sorted[i].timestampMs - playbackPositionMs)
        if (delta < bestDelta) {
            bestDelta = delta
            anchorIndex = i
        }
    }

    val anchor = sorted[anchorIndex]
    val dx = targetXFraction - anchor.xFraction
    val dy = targetYFraction - anchor.yFraction
    if (abs(dx) < 0.0001f && abs(dy) < 0.0001f) return sorted

    val shifted = sorted.mapIndexed { index, sample ->
        if (index < anchorIndex) {
            sample
        } else {
            sample.copy(
                xFraction = (sample.xFraction + dx).coerceIn(0f, 1f),
                yFraction = (sample.yFraction + dy).coerceIn(0f, 1f)
            )
        }
    }.toMutableList()

    val anchorShifted = shifted[anchorIndex]
    var plateauEnd = anchorIndex
    val stationaryThreshold = 0.012f
    for (i in (anchorIndex + 1) until shifted.size) {
        val sample = shifted[i]
        val sampleDx = sample.xFraction - anchorShifted.xFraction
        val sampleDy = sample.yFraction - anchorShifted.yFraction
        val sampleDistance = kotlin.math.sqrt((sampleDx * sampleDx + sampleDy * sampleDy).toDouble()).toFloat()
        if (sampleDistance <= stationaryThreshold) {
            plateauEnd = i
        } else {
            break
        }
    }

    val nextMovingIndex = plateauEnd + 1
    if (plateauEnd > anchorIndex && nextMovingIndex < shifted.size) {
        val movingSample = shifted[nextMovingIndex]
        val span = (nextMovingIndex - anchorIndex).coerceAtLeast(1)
        for (i in (anchorIndex + 1)..plateauEnd) {
            val t = (i - anchorIndex).toFloat() / span.toFloat()
            shifted[i] = shifted[i].copy(
                xFraction = lerp(anchorShifted.xFraction, movingSample.xFraction, t).coerceIn(0f, 1f),
                yFraction = lerp(anchorShifted.yFraction, movingSample.yFraction, t).coerceIn(0f, 1f)
            )
        }
    }

    return shifted
}

private fun interpolatedOverlaySample(samples: List<OverlaySample>, playbackPositionMs: Long): OverlaySample {
    if (samples.isEmpty()) return OverlaySample(0L, 0.5f, 0.5f)
    if (samples.size == 1) return samples.first()

    val sorted = samples.sortedBy { it.timestampMs }
    if (playbackPositionMs <= sorted.first().timestampMs) return sorted.first()
    if (playbackPositionMs >= sorted.last().timestampMs) return sorted.last()

    var left = sorted.first()
    var right = sorted.last()
    for (i in 1 until sorted.size) {
        val candidate = sorted[i]
        if (candidate.timestampMs >= playbackPositionMs) {
            left = sorted[i - 1]
            right = candidate
            break
        }
    }

    val span = (right.timestampMs - left.timestampMs).coerceAtLeast(1L)
    // If there is a large gap between detections, avoid over-interpolating and use nearest anchor.
    if (span > MAX_INTERPOLATION_GAP_MS) {
        return if (abs(playbackPositionMs - left.timestampMs) <= abs(right.timestampMs - playbackPositionMs)) {
            left
        } else {
            right
        }
    }

    val t = ((playbackPositionMs - left.timestampMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val easedT = t * t * (3f - 2f * t)
    return OverlaySample(
        timestampMs = playbackPositionMs,
        xFraction = lerp(left.xFraction, right.xFraction, easedT),
        yFraction = lerp(left.yFraction, right.yFraction, easedT)
    )
}

private fun smoothOverlaySamples(samples: List<OverlaySample>): List<OverlaySample> {
    if (samples.size < 3) return samples.sortedBy { it.timestampMs }

    val sorted = samples.sortedBy { it.timestampMs }
    val output = ArrayList<OverlaySample>(sorted.size)
    var prevX = sorted.first().xFraction
    var prevY = sorted.first().yFraction
    output.add(sorted.first())

    for (i in 1 until sorted.size) {
        val current = sorted[i]
        val rawDx = current.xFraction - prevX
        val rawDy = current.yFraction - prevY
        val distance = kotlin.math.sqrt((rawDx * rawDx + rawDy * rawDy).toDouble()).toFloat()
        val stepScale = if (distance > MAX_TRACK_STEP_FRACTION) {
            MAX_TRACK_STEP_FRACTION / distance
        } else {
            1f
        }

        val boundedX = prevX + rawDx * stepScale
        val boundedY = prevY + rawDy * stepScale
        val smoothedX = lerp(prevX, boundedX, TRACK_SMOOTHING_ALPHA).coerceIn(0f, 1f)
        val smoothedY = lerp(prevY, boundedY, TRACK_SMOOTHING_ALPHA).coerceIn(0f, 1f)
        output.add(
            OverlaySample(
                timestampMs = current.timestampMs,
                xFraction = smoothedX,
                yFraction = smoothedY
            )
        )
        prevX = smoothedX
        prevY = smoothedY
    }

    return output
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

private fun playerLastName(name: String): String {
    val cleaned = name.trim()
    if (cleaned.isBlank()) return "Player"
    return cleaned.substringAfterLast(" ")
}

@Composable
private fun TrackingLockBox(
    anchorX: Float,
    anchorY: Float,
    containerWidthPx: Float,
    containerHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val boxWidthPx = (containerWidthPx * 0.12f).coerceIn(with(density) { 42.dp.toPx() }, with(density) { 92.dp.toPx() })
    val boxHeightPx = (containerHeightPx * 0.2f).coerceIn(with(density) { 64.dp.toPx() }, with(density) { 140.dp.toPx() })
    val left = (anchorX - boxWidthPx / 2f).coerceIn(0f, (containerWidthPx - boxWidthPx).coerceAtLeast(0f))
    val top = (anchorY - boxHeightPx / 2f).coerceIn(0f, (containerHeightPx - boxHeightPx).coerceAtLeast(0f))
    val right = left + boxWidthPx
    val bottom = top + boxHeightPx
    val cornerLen = min(boxWidthPx, boxHeightPx) * 0.22f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lockColor = Color(0xFFFFF176).copy(alpha = 0.95f)
            val softColor = Color(0xFFFFF176).copy(alpha = 0.35f)
            val strokeWidth = with(density) { 2.dp.toPx() }

            // Soft body for readability.
            drawRect(
                color = softColor,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(boxWidthPx, boxHeightPx),
                style = Stroke(width = with(density) { 1.dp.toPx() })
            )

            // Corner brackets for "locked target" look.
            drawLine(lockColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth)
            drawLine(lockColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth)

            drawLine(lockColor, Offset(right, top), Offset(right - cornerLen, top), strokeWidth)
            drawLine(lockColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth)

            drawLine(lockColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth)
            drawLine(lockColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth)

            drawLine(lockColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth)
            drawLine(lockColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth)
        }
    }
}

@Composable
private fun AnchoredPlayerLabelBubble(
    label: String,
    anchorX: Float,
    anchorY: Float,
    containerWidthPx: Float,
    containerHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val bubbleWidthPx = with(density) { 92.dp.toPx() }
    val bubbleHeightPx = with(density) { 30.dp.toPx() }
    val verticalGapPx = with(density) { 10.dp.toPx() }
    val edgePaddingPx = with(density) { 8.dp.toPx() }
    val endpointRadiusPx = with(density) { 4.dp.toPx() }
    val strokeWidthPx = with(density) { 2.dp.toPx() }

    val showAbove = anchorY > bubbleHeightPx + verticalGapPx + edgePaddingPx
    val bubbleLeft = (anchorX - bubbleWidthPx / 2f)
        .coerceIn(edgePaddingPx, (containerWidthPx - bubbleWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx))
    val bubbleTop = if (showAbove) {
        (anchorY - bubbleHeightPx - verticalGapPx).coerceAtLeast(edgePaddingPx)
    } else {
        (anchorY + verticalGapPx).coerceAtMost((containerHeightPx - bubbleHeightPx - edgePaddingPx).coerceAtLeast(edgePaddingPx))
    }
    val bubbleCenterX = bubbleLeft + bubbleWidthPx / 2f
    val leaderStartY = if (showAbove) bubbleTop + bubbleHeightPx else bubbleTop

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(bubbleCenterX, leaderStartY),
                end = Offset(anchorX, anchorY),
                strokeWidth = strokeWidthPx
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.72f),
                radius = endpointRadiusPx + strokeWidthPx * 0.35f,
                center = Offset(anchorX, anchorY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = endpointRadiusPx,
                center = Offset(anchorX, anchorY),
                style = Stroke(width = strokeWidthPx * 0.7f)
            )
        }

        Surface(
            modifier = Modifier.offset {
                IntOffset(
                    x = bubbleLeft.toInt(),
                    y = bubbleTop.toInt()
                )
            },
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.78f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                maxLines = 1
            )
        }
    }
}

private fun shareVideoToSpotrContact(context: android.content.Context, videoUri: Uri, player: Player) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(
            Intent.EXTRA_TEXT,
            "Spotr share for ${player.addedBy}: original play featuring #${player.number} ${player.name}."
        )
        putExtra(Intent.EXTRA_SUBJECT, "Spotr original highlight for ${player.name} (${player.addedBy})")
    }
    launchHighQualityShareChooser(context, shareIntent, "Share original play")
}

private fun shareVideoForPlayers(context: android.content.Context, videoUri: Uri, players: List<Player>) {
    val names = players.joinToString(", ") { "#${it.number} ${it.name}" }
    val spotrContacts = players.map { it.addedBy }.distinct().joinToString(", ")
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_TEXT, "Spotr original play share for contacts [$spotrContacts]. Play featuring: $names")
        putExtra(Intent.EXTRA_SUBJECT, "Spotr original highlight")
    }
    launchHighQualityShareChooser(context, shareIntent, "Share original play")
}

data class TeamShareRecipient(
    val key: String,
    val displayName: String,
    val detail: String,
    val rawContact: String,
    val phoneNumber: String?,
    val email: String?
)

fun launchPersonalShareChooser(context: Context, videoUri: Uri, shareTitle: String = "Share this moment") {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        putExtra(Intent.EXTRA_TEXT, "Created with Spotr")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, shareTitle))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No compatible app found to share this moment.", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Unable to open share dialog.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open share dialog.", Toast.LENGTH_SHORT).show()
    }
}

internal fun buildTeamShareRecipients(players: List<Player>): List<TeamShareRecipient> {
    return players
        .groupBy { it.addedBy.trim().ifBlank { "Roster Contact" } }
        .map { (contact, groupedPlayers) ->
            val digitsOnly = contact.filter { it.isDigit() || it == '+' }.takeIf { it.any { ch -> ch.isDigit() } }
            val email = contact.takeIf { it.contains("@") }
            TeamShareRecipient(
                key = contact,
                displayName = contact.replace('_', ' '),
                detail = groupedPlayers.joinToString(", ") { it.name },
                rawContact = contact,
                phoneNumber = digitsOnly,
                email = email
            )
        }
        .sortedBy { it.displayName.lowercase() }
}

internal fun buildDefaultTeamShareMessage(players: List<Player>, highlightTag: String?): String {
    val playerText = when {
        players.isEmpty() -> ""
        players.size == 1 -> players.first().name
        else -> players.joinToString(", ") { it.name }
    }
    return if (highlightTag.isNullOrBlank()) {
        playerText
    } else {
        "$highlightTag - $playerText"
    }
}

internal fun shareVideoToTeamRecipients(
    context: Context,
    videoUri: Uri,
    recipients: List<TeamShareRecipient>,
    players: List<Player>,
    highlightTag: String?,
    customMessage: String
) {
    if (recipients.size == 1 && (!recipients[0].phoneNumber.isNullOrBlank() || !recipients[0].email.isNullOrBlank())) {
        shareVideoToPhoneContact(
            context = context,
            videoUri = videoUri,
            players = players,
            contact = SelectedContact(
                displayName = recipients[0].displayName,
                phoneNumber = recipients[0].phoneNumber,
                email = recipients[0].email
            )
        )
        return
    }

    val recipientNames = recipients.joinToString(", ") { it.displayName }
    val message = customMessage.ifBlank { buildDefaultTeamShareMessage(players, highlightTag) }
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_SUBJECT, "Spotr play for $recipientNames")
        putExtra(Intent.EXTRA_TEXT, "$message\n\nFor: $recipientNames")
    }
    launchHighQualityShareChooser(context, shareIntent, "Share")
}

private fun playerHasDirectContactInfo(player: Player): Boolean {
    val source = player.addedBy.trim()
    if (source.contains("@")) return true
    val digitsOnly = source.filter { it.isDigit() }
    return digitsOnly.length >= 10
}

internal data class SelectedContact(
    val displayName: String,
    val phoneNumber: String?,
    val email: String?
) {
    val cacheKey: String = "${displayName.trim()}|${phoneNumber.orEmpty().trim()}|${email.orEmpty().trim()}"
}

private const val PHONE_FAVORITES_PREF = "video_share_phone_favorites"
private const val PHONE_FAVORITES_KEY = "favorites"

private fun loadFavoritePhoneContacts(context: Context): List<SelectedContact> {
    val prefs = context.getSharedPreferences(PHONE_FAVORITES_PREF, Context.MODE_PRIVATE)
    val raw = prefs.getString(PHONE_FAVORITES_KEY, "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split("\n")
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.isEmpty() || parts[0].isBlank()) return@mapNotNull null
            SelectedContact(
                displayName = parts[0],
                phoneNumber = parts.getOrNull(1)?.ifBlank { null },
                email = parts.getOrNull(2)?.ifBlank { null }
            )
        }
}

private fun addFavoritePhoneContact(context: Context, contact: SelectedContact): List<SelectedContact> {
    val updated = (loadFavoritePhoneContacts(context) + contact)
        .distinctBy { it.cacheKey }
        .takeLast(12)
    saveFavoritePhoneContacts(context, updated)
    return updated
}

private fun removeFavoritePhoneContact(context: Context, contact: SelectedContact): List<SelectedContact> {
    val updated = loadFavoritePhoneContacts(context).filterNot { it.cacheKey == contact.cacheKey }
    saveFavoritePhoneContacts(context, updated)
    return updated
}

private fun saveFavoritePhoneContacts(context: Context, contacts: List<SelectedContact>) {
    val encoded = contacts.joinToString("\n") {
        listOf(it.displayName, it.phoneNumber.orEmpty(), it.email.orEmpty()).joinToString("||")
    }
    context.getSharedPreferences(PHONE_FAVORITES_PREF, Context.MODE_PRIVATE)
        .edit()
        .putString(PHONE_FAVORITES_KEY, encoded)
        .apply()
}

private suspend fun loadAllPhoneContactsForShare(context: Context): List<SelectedContact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<SelectedContact>()
    val resolver = context.contentResolver
    
    try {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )
        
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val id = if (idIndex >= 0) cursor.getString(idIndex) else continue
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Contact"
                
                val phoneNumber = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                    arrayOf(id),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        val numIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIndex >= 0) phoneCursor.getString(numIndex) else null
                    } else null
                }
                
                val email = resolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
                    arrayOf(id),
                    null
                )?.use { emailCursor ->
                    if (emailCursor.moveToFirst()) {
                        val emailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        if (emailIndex >= 0) emailCursor.getString(emailIndex) else null
                    } else null
                }
                
                if (phoneNumber != null || email != null) {
                    contacts.add(SelectedContact(displayName = name, phoneNumber = phoneNumber, email = email))
                }
            }
        }
    } catch (e: Exception) {
        Log.e("VideoPlaybackScreen", "Error loading all contacts: ${e.message}")
    }
    
    contacts
}

internal fun readSelectedContact(context: android.content.Context, contactUri: Uri): SelectedContact? {
    val resolver = context.contentResolver

    return try {
        fun readFirstString(uri: Uri, projection: Array<String>, selection: String? = null, selectionArgs: Array<String>? = null): String? {
            return resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(projection.first())
                if (idx >= 0) cursor.getString(idx) else null
            }
        }

        var displayName: String? = null
        var contactId: String? = null
        resolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                contactId = if (idIndex >= 0) cursor.getString(idIndex) else null
                displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        }

        val id = contactId
        val name = displayName ?: "Selected contact"

        val hasReadContacts = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val phoneNumber = if (hasReadContacts && !id.isNullOrBlank()) {
            readFirstString(
                uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                selectionArgs = arrayOf(id)
            )
        } else {
            null
        }

        val email = if (hasReadContacts && !id.isNullOrBlank()) {
            readFirstString(
                uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
                selectionArgs = arrayOf(id)
            )
        } else {
            null
        }

        SelectedContact(
            displayName = name,
            phoneNumber = phoneNumber,
            email = email
        )
    } catch (e: Exception) {
        android.util.Log.e("VideoPlaybackScreen", "Error reading contact: ${e.message}", e)
        null
    }
}

internal fun shareVideoToPhoneContact(
    context: android.content.Context,
    videoUri: Uri,
    players: List<Player>,
    contact: SelectedContact
) {
    val names = players.joinToString(", ") { "#${it.number} ${it.name}" }
    val message = if (players.isEmpty()) {
        "Hi ${contact.displayName}, sharing the original Spotr play"
    } else {
        "Hi ${contact.displayName}, sharing the original play featuring: $names"
    }
    val phoneTarget = contact.phoneNumber
        ?.filter { it.isDigit() || it == '+' }
        ?.takeIf { it.isNotBlank() }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_SUBJECT, "Spotr original highlight for ${contact.displayName}")
        putExtra(Intent.EXTRA_TEXT, message)
        if (phoneTarget != null) {
            // Many SMS/MMS apps honor these extras when handling ACTION_SEND.
            putExtra("address", phoneTarget)
            putExtra("sms_body", message)
        }
        if (!contact.email.isNullOrBlank()) {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
        }
    }

    val openedSms = launchDirectSmsShare(
        context = context,
        videoUri = videoUri,
        phoneNumber = phoneTarget,
        message = message
    )
    if (!openedSms) {
        launchStandardShareChooser(context, shareIntent, "Share this moment with ${contact.displayName}")
    }
}

private fun launchDirectSmsShare(
    context: Context,
    videoUri: Uri,
    phoneNumber: String?,
    message: String
): Boolean {
    if (phoneNumber.isNullOrBlank()) return false

    val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    val hasCellularHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    val simReady = telephonyManager?.simState == TelephonyManager.SIM_STATE_READY

    // Skip SMS-first flow on Wi-Fi-only / no-SIM devices and fall back to regular share chooser.
    if (!hasCellularHardware || !simReady) {
        Toast.makeText(context, "No SIM detected. Showing share apps instead.", Toast.LENGTH_SHORT).show()
        return false
    }

    val smsIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        putExtra("address", phoneNumber)
        putExtra("sms_body", message)
        putExtra(Intent.EXTRA_TEXT, message)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val smsPackage = Telephony.Sms.getDefaultSmsPackage(context)
    if (!smsPackage.isNullOrBlank()) {
        smsIntent.setPackage(smsPackage)
    }

    return try {
        if (smsIntent.resolveActivity(context.packageManager) == null) {
            false
        } else {
            context.startActivity(smsIntent)
            true
        }
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun launchStandardShareChooser(context: Context, baseIntent: Intent, title: String) {
    val pm = context.packageManager
    val resolved = pm.queryIntentActivities(baseIntent, 0)
    if (resolved.isEmpty()) {
        Toast.makeText(context, "No compatible apps found to share this moment.", Toast.LENGTH_LONG).show()
        return
    }
    try {
        context.startActivity(Intent.createChooser(baseIntent, title))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No compatible app found to share this moment.", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Unable to open share dialog.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open share dialog.", Toast.LENGTH_SHORT).show()
    }
}

private val HIGH_QUALITY_SHARE_PACKAGE_HINTS = listOf(
    "com.google.android.gms",           // Nearby Share / Quick Share on many Android builds
    "com.samsung.android.app.sharelive", // Samsung Quick Share
    "com.android.bluetooth",            // Bluetooth file transfer
    "com.google.android.apps.nbu.files", // Files by Google (can hand off file without re-encode)
    "com.miui.mishare.connectivity",   // Xiaomi Mi Share
    "com.coloros.oshare",              // OPPO/realme OShare
    "com.vivo.easyshare",              // vivo EasyShare
    "com.oneplus.filemanager"          // OnePlus file manager share targets
)

internal fun launchHighQualityShareChooser(context: Context, baseIntent: Intent, title: String) {
    try {
        context.startActivity(Intent.createChooser(baseIntent, title))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open share dialog.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Renders a circular name bubble for a player with jersey number
 */
@Composable
private fun PlayerNameBubble(
    playerName: String,
    jerseyNumber: String,
    offset: Offset,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .padding(start = offset.x.dp, top = offset.y.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.8f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "#$jerseyNumber",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                playerName.split(" ").lastOrNull() ?: playerName,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 2
            )
        }
    }
}
