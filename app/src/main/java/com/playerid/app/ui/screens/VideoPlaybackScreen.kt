package com.playerid.app.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
    startInShareFlow: Boolean = false,
    showPlaybackUi: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var videoDuration by remember { mutableLongStateOf(0L) }
    var currentVideoIndex by remember { mutableIntStateOf(0) }
    var playbackZoom by remember(videoUri, currentVideoIndex) { mutableFloatStateOf(1f) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    var wasPlayingBeforeSeek by remember { mutableStateOf(false) }
    val activeScrubUri = if (playlistUris.isNotEmpty() && currentVideoIndex in playlistUris.indices) {
        playlistUris[currentVideoIndex]
    } else {
        videoUri
    }
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
    
    val isPlaylistMode = playlistUris.isNotEmpty()
    val totalVideos = if (isPlaylistMode) playlistUris.size else 1
    
    var isAnalyzingPlayers by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var detectedSharePlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var selectedSharePlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showShareSuggestions by remember { mutableStateOf(false) }
    var showQuickSharePicker by remember { mutableStateOf(false) }
    var allContactsForShare by remember { mutableStateOf<List<SelectedContact>>(emptyList()) }
    var pendingPhoneSharePlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
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
    var selectedTeamRecipientKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedHighlightTag by remember { mutableStateOf<String?>(null) }
    var teamShareMessage by remember { mutableStateOf("") }
    var showHighlightTags by remember { mutableStateOf(false) }
    val detectionModeLabel = if (detectionMode == VideoProcessingManager.DetectionMode.FAST) "Fast" else "Slow"
    val selectedPlayersForShare = remember(detectedSharePlayers, selectedSharePlayerIds) {
        detectedSharePlayers.filter { selectedSharePlayerIds.contains(it.id) }
    }
    val teamShareRecipients = remember(selectedPlayersForShare) {
        buildTeamShareRecipients(selectedPlayersForShare)
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri == null) return@rememberLauncherForActivityResult

        val parsedContact = readSelectedContact(context, contactUri)
        val selectedContact = parsedContact
            ?: SelectedContact(
                displayName = "Selected contact",
                phoneNumber = null,
                email = null
            )

        shareVideoToPhoneContact(
            context = context,
            videoUri = videoUri,
            players = pendingPhoneSharePlayers.ifEmpty { selectedPlayersForShare },
            contact = selectedContact
        )
        if (parsedContact != null) {
            favoritePhoneContacts = addFavoritePhoneContact(context, parsedContact)
        }
        pendingPhoneSharePlayers = emptyList()
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
                Toast.makeText(context, "No roster matches found in this clip", Toast.LENGTH_SHORT).show()
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

    val exoPlayer = remember(showPlaybackUi, playlistUris, videoUri, isPlaylistMode) {
        if (!showPlaybackUi) {
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setSeekParameters(SeekParameters.EXACT)
                if (isPlaylistMode) {
                    // Add all videos to playlist
                    playlistUris.forEach { uri ->
                        addMediaItem(MediaItem.fromUri(uri))
                    }
                    repeatMode = Media3Player.REPEAT_MODE_OFF // Don't repeat in playlist mode
                } else {
                    setMediaItem(MediaItem.fromUri(videoUri))
                    repeatMode = Media3Player.REPEAT_MODE_ONE
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
        scrubPreviewFrameCount = if (prepared) {
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
        } else {
            player.pause()
        }
    }

    // Update current position periodically

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            if (videoDuration <= 0L && player.duration > 0L) {
                videoDuration = player.duration
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
                currentVideoIndex = player.currentMediaItemIndex
            }
            delay(if (isPlaying) 16 else 100)
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
                    }
                }
                override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                    isPlaying = isPlayingChanged
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    if (isPlaylistMode) {
                        currentVideoIndex = player.currentMediaItemIndex
                    }
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
        Scaffold(
        topBar = {
            if (!isPlaylistMode) {
                TopAppBar(
                    title = {
                        Text(
                            "Video Playback",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
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
                        .graphicsLayer {
                            scaleX = playbackZoom
                            scaleY = playbackZoom
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    detectionMode = if (detectionMode == VideoProcessingManager.DetectionMode.FAST) {
                                        VideoProcessingManager.DetectionMode.ACCURATE
                                    } else {
                                        VideoProcessingManager.DetectionMode.FAST
                                    }
                                },
                                enabled = !isAnalyzingPlayers,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Mode: $detectionModeLabel", fontSize = 10.sp)
                            }
                            Text(
                                "${currentVideoIndex + 1} / $totalVideos",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalButton(
                                onClick = { startTeamParentShareFlow() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Team Parents", fontSize = 11.sp)
                            }
                            if (detectedOverlays.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = { showOverlaySelectionDialog = true },
                                    enabled = !isAnalyzingPlayers,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Names (${selectedOverlayIds.size})", fontSize = 11.sp)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        if (selectedOverlayIds.isEmpty()) {
                                            Toast.makeText(context, "Select a player name first", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isLockAssistMode = !isLockAssistMode
                                        }
                                    },
                                    enabled = !isAnalyzingPlayers,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(if (isLockAssistMode) "Cancel Lock" else "Lock Here", fontSize = 11.sp)
                                }
                            }
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
                onNavigateBack()
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
                            launchPersonalShareChooser(
                                context = context,
                                videoUri = videoUri,
                                shareTitle = "Share to My Contacts"
                            )
                            showInitialShareDestinationDialog = false
                            onNavigateBack()
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
                                    shareVideoToTeamRecipients(
                                        context = context,
                                        videoUri = videoUri,
                                        recipients = recipients,
                                        players = selectedPlayersForShare,
                                        highlightTag = selectedHighlightTag,
                                        customMessage = teamShareMessage
                                    )
                                    showTeamShareAboutDialog = false
                                    onNavigateBack()
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
                            shareVideoToTeamRecipients(
                                context = context,
                                videoUri = videoUri,
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
            title = { Text("Share with") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Sending clip with ${detectedSharePlayers.size} identified player${if (detectedSharePlayers.size != 1) "s" else ""}",
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
                                            shareVideoToPhoneContact(
                                                context = context,
                                                videoUri = videoUri,
                                                players = selectedPlayersForShare,
                                                contact = contact
                                            )
                                            showQuickSharePicker = false
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
                                        shareVideoToPhoneContact(
                                            context = context,
                                            videoUri = videoUri,
                                            players = selectedPlayersForShare,
                                            contact = contact
                                        )
                                        showQuickSharePicker = false
                                        favoritePhoneContacts = addFavoritePhoneContact(context, contact)
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
            "Spotr share for ${player.addedBy}: original clip featuring #${player.number} ${player.name}."
        )
        putExtra(Intent.EXTRA_SUBJECT, "Spotr original highlight for ${player.name} (${player.addedBy})")
    }
    launchHighQualityShareChooser(context, shareIntent, "Share original clip")
}

private fun shareVideoForPlayers(context: android.content.Context, videoUri: Uri, players: List<Player>) {
    val names = players.joinToString(", ") { "#${it.number} ${it.name}" }
    val spotrContacts = players.map { it.addedBy }.distinct().joinToString(", ")
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_TEXT, "Spotr original clip share for contacts [$spotrContacts]. Clip featuring: $names")
        putExtra(Intent.EXTRA_SUBJECT, "Spotr original highlight")
    }
    launchHighQualityShareChooser(context, shareIntent, "Share original clip")
}

data class TeamShareRecipient(
    val key: String,
    val displayName: String,
    val detail: String,
    val rawContact: String,
    val phoneNumber: String?,
    val email: String?
)

fun launchPersonalShareChooser(context: Context, videoUri: Uri, shareTitle: String = "Share clip") {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        putExtra(Intent.EXTRA_TEXT, "Created with Spotr")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, shareTitle))
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
        putExtra(Intent.EXTRA_SUBJECT, "Spotr clip for $recipientNames")
        putExtra(Intent.EXTRA_TEXT, "$message\n\nFor: $recipientNames")
    }
    launchHighQualityShareChooser(context, shareIntent, "Share with team parents")
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
        "Hi ${contact.displayName}, sharing the original Spotr clip"
    } else {
        "Hi ${contact.displayName}, sharing the original clip featuring: $names"
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
        launchStandardShareChooser(context, shareIntent, "Share with ${contact.displayName}")
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
        Toast.makeText(context, "No compatible apps found to share this clip.", Toast.LENGTH_LONG).show()
        return
    }
    context.startActivity(Intent.createChooser(baseIntent, title))
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
    val pm = context.packageManager
    val resolved = pm.queryIntentActivities(baseIntent, 0)
    val highQualityTargets = resolved.filter { resolveInfo ->
        val pkg = resolveInfo.activityInfo?.packageName.orEmpty()
        HIGH_QUALITY_SHARE_PACKAGE_HINTS.any { hint -> pkg.contains(hint, ignoreCase = true) }
    }

    if (highQualityTargets.isEmpty()) {
        Toast.makeText(
            context,
            "No high-quality phone transfer option found. Enable Quick Share or Bluetooth.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val targetedIntents = highQualityTargets.map { resolveInfo ->
        Intent(baseIntent).apply {
            setPackage(resolveInfo.activityInfo.packageName)
            setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    val primaryIntent = targetedIntents.first()
    val extraIntents = targetedIntents.drop(1).toTypedArray()
    val chooser = Intent.createChooser(primaryIntent, title).apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)
    }
    context.startActivity(chooser)
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
