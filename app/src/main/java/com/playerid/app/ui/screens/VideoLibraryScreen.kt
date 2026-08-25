package com.playerid.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.playerid.app.data.Player
import com.playerid.app.data.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Video Library Screen
 * 
 * Displays all recorded videos for a team.
 * User can select a video to play with player overlay options.
 */
@ExperimentalMaterial3Api
data class VideoLibrarySection(
    val key: String,
    val title: String,
    val subtitle: String,
    val videos: List<VideoClip>
)

enum class VideoLibraryBrowseMode {
    CLIPS,
    REELS,
    TOP_PLAYS
}

private val PlaysAccentColor = Color(0xFF1C8CFF)
private val PlaysBackgroundColor = Color(0xFFFFFFFF)

@ExperimentalMaterial3Api
@Suppress("UNUSED_PARAMETER")
@Composable
fun VideoLibraryScreen(
    teamName: String,
    availableTeams: List<String> = emptyList(),
    videos: List<VideoClip>,
    sections: List<VideoLibrarySection> = emptyList(),
    rosterPlayers: List<Player>,
    isLoading: Boolean,
    lastRefreshedLabel: String,
    emptyStateTitle: String = "No videos yet",
    emptyStateSubtitle: String = "Record a video to get started",
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onVideoSelected: (Uri, List<Player>) -> Unit,
    onVideoEdit: (Uri) -> Unit,
    onVideoShare: (Uri) -> Unit,
    isSelectionModeEnabled: Boolean = false,
    selectedShareVideoIds: Set<String> = emptySet(),
    onSelectedShareVideoIdsChange: (Set<String>) -> Unit = {},
    onVideoDelete: (VideoClip) -> Unit,
    onVideoNameChanged: (VideoClip, String) -> Unit = { _, _ -> },
    onOpponentChanged: (VideoClip, String) -> Unit = { _, _ -> },
    onTeamChanged: (VideoClip, String) -> Unit = { _, _ -> },
    onToggleHighlight: (VideoClip) -> Unit = {},
    hallOfFameClipIds: Set<String> = emptySet(),
    savedGoatReels: List<SavedReel> = emptyList(),
    onToggleHallOfFameClip: (VideoClip) -> Unit = {},
    onPlaySavedReel: (SavedReel) -> Unit = {},
    onShareSavedReel: (SavedReel) -> Unit = {},
    onToggleSavedReelTopPlay: (SavedReel) -> Unit = {},
    onEditSavedReelDetails: (SavedReel, String) -> Unit = { _, _ -> },
    onDeleteSavedReel: (SavedReel) -> Unit = {},
    onCreateReel: () -> Unit = {},
    showTopBar: Boolean = true,
    selectionModeLabel: String? = null,
    onSelectionModeToggle: (() -> Unit)? = null,
    onUploadRequested: (() -> Unit)? = null,
    showModeFilters: Boolean = true,
    browseMode: VideoLibraryBrowseMode? = null,
    onBrowseModeChanged: (showReelsOnly: Boolean, showHallOfFameOnly: Boolean) -> Unit = { _, _ -> },
    goatSourceVideos: List<VideoClip> = videos
) {
    var showReelsOnly by remember { mutableStateOf(false) }
    var showHallOfFameOnly by remember { mutableStateOf(false) }
    var showHeaderActionsMenu by remember { mutableStateOf(false) }
    val effectiveMode = browseMode ?: when {
        showModeFilters && showReelsOnly -> VideoLibraryBrowseMode.REELS
        showModeFilters && showHallOfFameOnly -> VideoLibraryBrowseMode.TOP_PLAYS
        else -> VideoLibraryBrowseMode.CLIPS
    }
    val effectiveShowReelsOnly = effectiveMode == VideoLibraryBrowseMode.REELS
    val effectiveShowHallOfFameOnly = effectiveMode == VideoLibraryBrowseMode.TOP_PLAYS

    LaunchedEffect(effectiveShowReelsOnly, effectiveShowHallOfFameOnly) {
        onBrowseModeChanged(effectiveShowReelsOnly, effectiveShowHallOfFameOnly)
    }

    val seasonClipIds = remember(goatSourceVideos) { goatSourceVideos.map { it.id }.toSet() }
    val reelsForSeason = remember(savedGoatReels, seasonClipIds) {
        savedGoatReels.filter { reel -> reel.clipIds.any { seasonClipIds.contains(it) } }
    }
    val hasReelContent = reelsForSeason.isNotEmpty()
    val showHallOfFameChip = goatSourceVideos.isNotEmpty() || hallOfFameClipIds.isNotEmpty()
    val displaySections = remember(videos, goatSourceVideos, sections, effectiveShowReelsOnly, effectiveShowHallOfFameOnly, hallOfFameClipIds) {
        if (effectiveShowReelsOnly) return@remember emptyList()

        val sourceVideos = if (effectiveShowHallOfFameOnly) goatSourceVideos else videos
        val contentVideos = sourceVideos.filter { video ->
            (!effectiveShowHallOfFameOnly || hallOfFameClipIds.contains(video.id))
        }
        if (effectiveShowHallOfFameOnly) {
            if (contentVideos.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    VideoLibrarySection(
                        key = "global-goats",
                        title = "Top Plays",
                        subtitle = "${contentVideos.size} plays",
                        videos = contentVideos.sortedByDescending { it.createdAt }
                    )
                )
            }
        } else if (sections.isNotEmpty()) {
            sections.mapNotNull { section ->
                val filtered = section.videos.filter { video ->
                    (!effectiveShowHallOfFameOnly || hallOfFameClipIds.contains(video.id))
                }
                if (filtered.isEmpty()) null else section.copy(videos = filtered)
            }
        } else if (contentVideos.isNotEmpty()) {
            val listTitle = when {
                effectiveShowHallOfFameOnly -> "Top Plays"
                else -> "All Plays"
            }
            listOf(VideoLibrarySection(key = "all", title = listTitle, subtitle = "${contentVideos.size} plays", videos = contentVideos))
        } else {
            emptyList()
        }
    }
    val hasVisibleContent = if (effectiveShowReelsOnly) reelsForSeason.isNotEmpty() else displaySections.isNotEmpty()
    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Game Moments", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(teamName, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            if (lastRefreshedLabel.isNotBlank()) {
                                Text(
                                    lastRefreshedLabel,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (showModeFilters && browseMode == null && hasReelContent) {
                            IconButton(onClick = {
                                showReelsOnly = !showReelsOnly
                                if (showReelsOnly) showHallOfFameOnly = false
                            }) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = if (showReelsOnly) "Show all plays" else "Show reels"
                                )
                            }
                        }
                        IconButton(onClick = onRefresh, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, "Refresh")
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
                .background(PlaysBackgroundColor)
        ) {
            if (showModeFilters && browseMode == null && (hasReelContent || showHallOfFameChip)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasReelContent) {
                            FilterChip(
                                selected = showReelsOnly,
                                onClick = {
                                    showReelsOnly = !showReelsOnly
                                    if (showReelsOnly) showHallOfFameOnly = false
                                },
                                leadingIcon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                                label = { Text("Season Reels") }
                            )
                        }
                        if (showHallOfFameChip) {
                            FilterChip(
                                selected = showHallOfFameOnly,
                                onClick = {
                                    showHallOfFameOnly = !showHallOfFameOnly
                                    if (showHallOfFameOnly) showReelsOnly = false
                                },
                                leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                                label = { Text("Season Top Plays") }
                            )
                        }
                    }

                    onUploadRequested?.let { uploadRequested ->
                        Box {
                            IconButton(onClick = { showHeaderActionsMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                            }
                            DropdownMenu(
                                expanded = showHeaderActionsMenu,
                                onDismissRequest = { showHeaderActionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Upload") },
                                    leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                                    onClick = {
                                        uploadRequested()
                                        showHeaderActionsMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Loading your game moments...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!hasVisibleContent) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        when {
                            effectiveShowReelsOnly -> "No reels in this season"
                            effectiveShowHallOfFameOnly -> "Top Play is empty"
                            else -> emptyStateTitle
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        when {
                            effectiveShowReelsOnly -> "Create a reel from selected plays to see it here."
                            effectiveShowHallOfFameOnly -> "Save standout plays to build your Top Play collection"
                            else -> emptyStateSubtitle
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (effectiveShowReelsOnly) {
                        item(key = "section-reels") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Reels",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        items(items = reelsForSeason, key = { "reel-${it.id}" }) { reel ->
                            val reelPreviewVideo = reel.clipIds
                                .asSequence()
                                .mapNotNull { id -> goatSourceVideos.firstOrNull { it.id == id } }
                                .firstOrNull()
                            val isReelInTopPlay = reel.clipIds.isNotEmpty() && reel.clipIds.all { id -> hallOfFameClipIds.contains(id) }
                            val reelClipDatesLabel = remember(reel.clipIds, goatSourceVideos) {
                                reel.clipIds
                                    .mapNotNull { id -> goatSourceVideos.firstOrNull { it.id == id }?.gameDate }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .sorted()
                                    .map { formatDate(it) }
                                    .let { dates ->
                                        when {
                                            dates.isEmpty() -> null
                                            dates.size <= 3 -> dates.joinToString(", ")
                                            else -> dates.take(3).joinToString(", ") + " +${dates.size - 3}"
                                        }
                                    }
                            }
                            SavedReelCard(
                                reel = reel,
                                thumbnailPath = reelPreviewVideo?.filePath,
                                durationMillis = reelPreviewVideo?.duration,
                                isInTopPlay = isReelInTopPlay,
                                clipDatesLabel = reelClipDatesLabel,
                                onPlayClick = { onPlaySavedReel(reel) },
                                onShareClick = { onShareSavedReel(reel) },
                                onToggleTopPlay = { onToggleSavedReelTopPlay(reel) },
                                onEditDetailsClick = { newName -> onEditSavedReelDetails(reel, newName) },
                                onDeleteClick = { onDeleteSavedReel(reel) }
                            )
                        }
                    }

                    val singleOpponentSelected = displaySections.size > 1 &&
                        displaySections
                            .map { it.title.trim().lowercase() }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .size == 1

                    displaySections.forEach { section ->
                        item(key = "section-${section.key}") {
                            val sectionTitle = section.title.ifBlank { "Unspecified" }
                            val showDateOnlyHeader = singleOpponentSelected && section.subtitle.isNotBlank()
                            val opponentHeader = when {
                                showDateOnlyHeader -> ""
                                section.title.isBlank() -> ""
                                else -> "vs $sectionTitle"
                            }
                            val dateHeader = section.subtitle
                            val seasonLabel = section.videos
                                .firstOrNull()
                                ?.gameDate
                                ?.let(::formatSeasonLabel)
                                .orEmpty()
                            val dateWithSeason = when {
                                dateHeader.isBlank() -> ""
                                seasonLabel.isBlank() -> dateHeader
                                else -> "$dateHeader  •  $seasonLabel"
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (showDateOnlyHeader) {
                                        Text(
                                            dateHeader,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else if (opponentHeader.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                opponentHeader,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (dateWithSeason.isNotBlank()) {
                                                Text(
                                                    dateWithSeason,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        items(items = section.videos, key = { it.id }) { video ->
                            VideoClipCard(
                                video = video,
                                onPlayClick = {
                                    onVideoSelected(Uri.parse(video.filePath), rosterPlayers)
                                },
                                onEditClick = {
                                    onVideoEdit(Uri.parse(video.filePath))
                                },
                                onShareClick = {
                                    onVideoShare(Uri.parse(video.filePath))
                                },
                                onDeleteClick = {
                                    onVideoDelete(video)
                                },
                                isSelectionModeEnabled = isSelectionModeEnabled,
                                isSelectedForShare = selectedShareVideoIds.contains(video.id),
                                onToggleShareSelection = {
                                    if (isSelectionModeEnabled) {
                                        onSelectedShareVideoIdsChange(
                                            if (selectedShareVideoIds.contains(video.id)) {
                                                selectedShareVideoIds - video.id
                                            } else {
                                                selectedShareVideoIds + video.id
                                            }
                                        )
                                    }
                                },
                                availableTeams = availableTeams,
                                onNameChanged = { newName ->
                                    onVideoNameChanged(video, newName)
                                },
                                onOpponentChanged = { opponent ->
                                    onOpponentChanged(video, opponent)
                                },
                                onTeamChanged = { team ->
                                    onTeamChanged(video, team)
                                },
                                onToggleHighlight = {
                                    onToggleHighlight(video)
                                },
                                isInReel = video.isHighlight,
                                isInHallOfFame = hallOfFameClipIds.contains(video.id),
                                onToggleHallOfFame = {
                                    onToggleHallOfFameClip(video)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Suppress("UNUSED_PARAMETER")
@Composable
private fun VideoClipCard(
    video: VideoClip,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isSelectionModeEnabled: Boolean = false,
    isSelectedForShare: Boolean = false,
    onToggleShareSelection: () -> Unit = {},
    availableTeams: List<String> = emptyList(),
    onNameChanged: (String) -> Unit = {},
    onOpponentChanged: (String) -> Unit = {},
    onTeamChanged: (String) -> Unit = {},
    onToggleHighlight: () -> Unit = {},
    isInReel: Boolean = false,
    isInHallOfFame: Boolean = false,
    onToggleHallOfFame: () -> Unit = {}
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var editedName by remember(video.id, video.momentTag) { mutableStateOf(video.momentTag ?: "") }
    val context = LocalContext.current
    val thumbnail by rememberVideoThumbnail(context, video.filePath)
    val detectedJerseyNumbers = remember(video.bubbleMetadata) {
        extractDetectedJerseyNumbers(video.bubbleMetadata)
    }
    var voiceMemoryPath by remember(video.filePath) { mutableStateOf(getClipVoiceMemoryPath(context, video.filePath)) }
    var voiceMemoryPlayer by remember(video.filePath) { mutableStateOf<MediaPlayer?>(null) }
    var isVoiceMemoryPlaying by remember(video.filePath) { mutableStateOf(false) }
    var showVoiceMemoryDialog by remember(video.filePath) { mutableStateOf(false) }
    var isRecordingVoiceMemory by remember(video.filePath) { mutableStateOf(false) }
    var voiceMemoryRecorder by remember(video.filePath) { mutableStateOf<MediaRecorder?>(null) }
    var pendingVoiceMemoryPath by remember(video.filePath) { mutableStateOf<String?>(null) }
    val hasVoiceMemoryAttachment = !voiceMemoryPath.isNullOrBlank()
    val voiceMemoryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoiceMemoryDialog = true
        }
    }

    fun stopVoiceMemoryPlayback() {
        voiceMemoryPlayer?.runCatching {
            if (isPlaying) stop()
            reset()
            release()
        }
        voiceMemoryPlayer = null
        isVoiceMemoryPlaying = false
    }

    fun toggleVoiceMemoryPlayback() {
        if (!hasVoiceMemoryAttachment) return
        if (isVoiceMemoryPlaying) {
            stopVoiceMemoryPlayback()
            return
        }

        val path = voiceMemoryPath ?: return
        stopVoiceMemoryPlayback()
        try {
            voiceMemoryPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stopVoiceMemoryPlayback()
                    voiceMemoryPath = getClipVoiceMemoryPath(context, video.filePath)
                }
                prepare()
                start()
            }
            isVoiceMemoryPlaying = true
        } catch (_: Exception) {
            stopVoiceMemoryPlayback()
            voiceMemoryPath = getClipVoiceMemoryPath(context, video.filePath)
        }
    }

    fun stopVoiceMemoryRecording(saveRecording: Boolean) {
        val recordingPath = pendingVoiceMemoryPath
        runCatching {
            voiceMemoryRecorder?.apply {
                stop()
                reset()
                release()
            }
        }
        voiceMemoryRecorder = null
        isRecordingVoiceMemory = false

        if (saveRecording && !recordingPath.isNullOrBlank()) {
            saveClipVoiceMemoryPathForLibrary(context, video.filePath, recordingPath)
            voiceMemoryPath = recordingPath
        } else if (!recordingPath.isNullOrBlank()) {
            runCatching { File(recordingPath).delete() }
        }
        pendingVoiceMemoryPath = null
    }

    fun startVoiceMemoryRecording() {
        stopVoiceMemoryPlayback()
        val outputFile = createVoiceMemoryFileForLibrary(context, video.filePath)
        runCatching {
            val recorder = buildVoiceMemoryRecorderForLibrary(context, outputFile)
            recorder.prepare()
            recorder.start()
            voiceMemoryRecorder = recorder
            pendingVoiceMemoryPath = outputFile.absolutePath
            isRecordingVoiceMemory = true
        }.onFailure {
            voiceMemoryRecorder = null
            pendingVoiceMemoryPath = null
            isRecordingVoiceMemory = false
            runCatching { outputFile.delete() }
        }
    }

    DisposableEffect(video.filePath) {
        onDispose {
            stopVoiceMemoryPlayback()
            runCatching {
                voiceMemoryRecorder?.run {
                    reset()
                    release()
                }
            }
            voiceMemoryRecorder = null
        }
    }
    val cardElevation by animateDpAsState(
        targetValue = if (isSelectedForShare) 12.dp else 10.dp,
        animationSpec = tween(durationMillis = 220),
        label = "cardElevation"
    )

    if (showDetailsDialog) {
        val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
        val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        val currentOpponent = opponentPrefs.getString(video.id, null) ?: ""
        val currentTeam = teamPrefs.getString(video.id, null)
            ?: teamPrefs.getString(video.filePath, null)
            ?: ""
        val allOpponents = remember {
            opponentPrefs.all.values
                .filterIsInstance<String>()
                .filter { it.isNotBlank() }
                .map { it.trim() }
                .distinct()
                .sortedBy { it.lowercase() }
        }
        val selectableTeams = remember(availableTeams) {
            availableTeams
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sortedBy { it.lowercase() }
        }
        val focusRequester = remember { FocusRequester() }
        var opponentField by remember {
            mutableStateOf(
                TextFieldValue(
                    text = currentOpponent,
                    selection = TextRange(0, currentOpponent.length)
                )
            )
        }
        var titleField by remember {
            mutableStateOf(
                TextFieldValue(
                    text = editedName,
                    selection = TextRange(0, editedName.length)
                )
            )
        }
        var selectedTeamName by remember { mutableStateOf(currentTeam) }
        var opponentDropdownExpanded by remember { mutableStateOf(false) }
        var teamDropdownExpanded by remember { mutableStateOf(false) }
        val filteredOpponents = remember(opponentField.text) {
            val query = opponentField.text.trim().lowercase()
            if (query.isEmpty()) allOpponents
            else allOpponents.filter { it.lowercase().contains(query) }
        }
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Edit Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = titleField,
                        onValueChange = { titleField = it },
                        label = { Text("Play title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = teamDropdownExpanded && selectableTeams.isNotEmpty(),
                        onExpandedChange = {
                            if (selectableTeams.isNotEmpty()) {
                                teamDropdownExpanded = !teamDropdownExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedTeamName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Team") },
                            placeholder = { Text("Select team") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = teamDropdownExpanded && selectableTeams.isNotEmpty(),
                            onDismissRequest = { teamDropdownExpanded = false }
                        ) {
                            selectableTeams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text(team) },
                                    onClick = {
                                        selectedTeamName = team
                                        teamDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = opponentDropdownExpanded && filteredOpponents.isNotEmpty(),
                        onExpandedChange = { opponentDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = opponentField,
                            onValueChange = {
                                opponentField = it
                                opponentDropdownExpanded = true
                            },
                            label = { Text("Opponent") },
                            placeholder = { Text("e.g. Lions FC") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = opponentDropdownExpanded && filteredOpponents.isNotEmpty(),
                            onDismissRequest = { opponentDropdownExpanded = false }
                        ) {
                            filteredOpponents.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        opponentField = TextFieldValue(
                                            text = suggestion,
                                            selection = TextRange(suggestion.length)
                                        )
                                        opponentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            },
            dismissButton = {
                TextButton(onClick = { showDetailsDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTitle = titleField.text.trim()
                        editedName = newTitle
                        onNameChanged(newTitle)
                        if (selectedTeamName.isNotBlank()) {
                            onTeamChanged(selectedTeamName.trim())
                        }
                        onOpponentChanged(opponentField.text.trim())
                        showDetailsDialog = false
                    }
                ) { Text("Save") }
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
                    Text(
                        "Voice Memory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            onClick = {
                                when {
                                    isRecordingVoiceMemory -> stopVoiceMemoryRecording(saveRecording = true)
                                    isVoiceMemoryPlaying -> stopVoiceMemoryPlayback()
                                    !voiceMemoryPath.isNullOrBlank() -> toggleVoiceMemoryPlayback()
                                    else -> startVoiceMemoryRecording()
                                }
                            },
                            shape = CircleShape,
                            color = when {
                                isRecordingVoiceMemory -> Color(0xFFD32F2F)
                                isVoiceMemoryPlaying -> MaterialTheme.colorScheme.primaryContainer
                                !voiceMemoryPath.isNullOrBlank() -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = when {
                                        isRecordingVoiceMemory -> Icons.Default.Stop
                                        isVoiceMemoryPlaying -> Icons.Default.Stop
                                        !voiceMemoryPath.isNullOrBlank() -> Icons.Default.PlayArrow
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

                    Text(
                        text = when {
                            isRecordingVoiceMemory -> "Recording - tap to save"
                            isVoiceMemoryPlaying -> "Playing - tap to stop"
                            !voiceMemoryPath.isNullOrBlank() -> "Tap to play"
                            else -> "Tap to record"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isRecordingVoiceMemory && !voiceMemoryPath.isNullOrBlank()) {
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

    val momentColor = when (video.momentTag) {
        "Goal"     -> Color(0xFF2E7D32)
        "Assist"   -> Color(0xFF1565C0)
        "Save"     -> Color(0xFF6A1B9A)
        "Big Play" -> Color(0xFFE65100)
        else       -> Color(0xFF37474F)
    }
    val momentIcon = when (video.momentTag) {
        "Goal"     -> Icons.Default.Favorite
        "Assist"   -> Icons.Default.Share
        "Save"     -> Icons.Default.Favorite
        "Big Play" -> Icons.Default.PlayArrow
        else       -> Icons.Default.VideoLibrary
    }

    val actionButtonWidth = 86.dp
    val actionRailWidth = 172.dp
    val actionRailWidthPx = with(LocalDensity.current) { actionRailWidth.toPx() }
    var horizontalOffsetPx by remember(video.id) { mutableStateOf(0f) }
    val revealThresholdPx = actionRailWidthPx * 0.35f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFCFDFF))
    ) {
        val actionsRevealed = horizontalOffsetPx <= -revealThresholdPx

        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp)),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .width(actionButtonWidth)
                    .fillMaxHeight()
                    .background(if (actionsRevealed) Color(0xFF2E7D52) else Color.Transparent)
                    .clickable(enabled = actionsRevealed && !isSelectionModeEnabled) {
                        onShareClick()
                        horizontalOffsetPx = 0f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = "Share",
                    tint = if (actionsRevealed) Color.White else Color.Transparent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Share",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (actionsRevealed) Color.White else Color.Transparent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .width(actionButtonWidth)
                    .fillMaxHeight()
                    .background(if (actionsRevealed) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                    .clickable(enabled = actionsRevealed && !isSelectionModeEnabled) {
                        onDeleteClick()
                        horizontalOffsetPx = 0f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (actionsRevealed) MaterialTheme.colorScheme.onErrorContainer else Color.Transparent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = if (actionsRevealed) MaterialTheme.colorScheme.onErrorContainer else Color.Transparent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .offset { IntOffset(horizontalOffsetPx.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta: Float ->
                        if (!isSelectionModeEnabled) {
                            val candidateOffset = horizontalOffsetPx + delta
                            horizontalOffsetPx = candidateOffset.coerceIn(-actionRailWidthPx, 0f)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    enabled = !isSelectionModeEnabled,
                    onDragStopped = {
                        horizontalOffsetPx = if (horizontalOffsetPx <= -revealThresholdPx) -actionRailWidthPx else 0f
                    }
                )
                .border(
                    width = if (isSelectedForShare) 2.dp else 1.dp,
                    color = if (isSelectedForShare) PlaysAccentColor else Color(0xFFD2D8DE),
                    shape = RoundedCornerShape(14.dp)
                ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            val customName = video.momentTag?.takeIf { it.isNotBlank() }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = "Play preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(momentColor, momentColor.copy(alpha = 0.72f))
                                    )
                                )
                        )
                        Icon(
                            momentIcon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(52.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                                .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            formatDuration(video.duration),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }

                    Surface(
                        modifier = Modifier
                                .align(Alignment.Center)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (isSelectionModeEnabled) onToggleShareSelection() else onPlayClick()
                            },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(28.dp)
                        )
                    }

                    if (isSelectedForShare) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .size(24.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxSize()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.50f))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onToggleHighlight() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = "Add to reel",
                                tint = if (isInReel) Color(0xFF1C8CFF) else Color.White,
                                    modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Add to Reel",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                    fontSize = 11.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable {
                                        if (hasVoiceMemoryAttachment) {
                                            toggleVoiceMemoryPlayback()
                                        } else {
                                            val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (!permissionGranted) {
                                                voiceMemoryPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                showVoiceMemoryDialog = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Add or edit memory",
                                tint = if (hasVoiceMemoryAttachment) Color(0xFF2E7D32) else Color.White,
                                    modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (hasVoiceMemoryAttachment) "Memory" else "Add Memory",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                    fontSize = 11.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onToggleHallOfFame() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (isInHallOfFame) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isInHallOfFame) "In top plays" else "Add to top plays",
                                tint = if (isInHallOfFame) Color(0xFFE0B13F) else Color.White,
                                    modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Top Play",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                    fontSize = 11.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = customName ?: "[Edit Title, Team, Opponent]",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (customName != null) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { showDetailsDialog = true }
                    )

                    if (detectedJerseyNumbers.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Detected players",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = detectedJerseyNumbers.joinToString("  "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getClipVoiceMemoryPath(context: Context, clipKey: String): String? {
    val path = context
        .getSharedPreferences("clip_voice_memories", Context.MODE_PRIVATE)
        .getString(clipKey, null)
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return runCatching { if (File(path).exists()) path else null }.getOrNull()
}

private fun saveClipVoiceMemoryPathForLibrary(context: Context, clipKey: String, filePath: String) {
    context.getSharedPreferences("clip_voice_memories", Context.MODE_PRIVATE)
        .edit()
        .putString(clipKey, filePath)
        .apply()
}

private fun createVoiceMemoryFileForLibrary(context: Context, clipKey: String): File {
    val directory = File(context.filesDir, "voice_memories").apply { mkdirs() }
    return File(directory, "voice_${clipKey.hashCode()}_${System.currentTimeMillis()}.m4a")
}

private fun buildVoiceMemoryRecorderForLibrary(context: Context, outputFile: File): MediaRecorder {
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

private fun getSavedReelVoiceMemoryPath(context: Context, reelId: String): String? {
    val path = context
        .getSharedPreferences("reel_voice_memories", Context.MODE_PRIVATE)
        .getString(reelId, null)
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return runCatching { if (File(path).exists()) path else null }.getOrNull()
}

private fun saveSavedReelVoiceMemoryPath(context: Context, reelId: String, filePath: String) {
    context.getSharedPreferences("reel_voice_memories", Context.MODE_PRIVATE)
        .edit()
        .putString(reelId, filePath)
        .apply()
}

@Composable
private fun SavedReelCard(
    reel: SavedReel,
    thumbnailPath: String?,
    durationMillis: Long?,
    isInTopPlay: Boolean,
    clipDatesLabel: String?,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleTopPlay: () -> Unit,
    onEditDetailsClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    var showEditDetailsDialog by remember(reel.id) { mutableStateOf(false) }
    var editedName by remember(reel.id, reel.name) { mutableStateOf(reel.name) }
    val context = LocalContext.current
    val reelThumbnail = if (thumbnailPath.isNullOrBlank()) null else rememberVideoThumbnail(context, thumbnailPath).value
    var voiceMemoryPath by remember(reel.id) { mutableStateOf(getSavedReelVoiceMemoryPath(context, reel.id)) }
    var voiceMemoryPlayer by remember(reel.id) { mutableStateOf<MediaPlayer?>(null) }
    var isVoiceMemoryPlaying by remember(reel.id) { mutableStateOf(false) }
    var showVoiceMemoryDialog by remember(reel.id) { mutableStateOf(false) }
    var isRecordingVoiceMemory by remember(reel.id) { mutableStateOf(false) }
    var voiceMemoryRecorder by remember(reel.id) { mutableStateOf<MediaRecorder?>(null) }
    var pendingVoiceMemoryPath by remember(reel.id) { mutableStateOf<String?>(null) }
    val hasVoiceMemoryAttachment = !voiceMemoryPath.isNullOrBlank()
    val voiceMemoryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoiceMemoryDialog = true
        }
    }

    fun stopVoiceMemoryPlayback() {
        voiceMemoryPlayer?.runCatching {
            if (isPlaying) stop()
            reset()
            release()
        }
        voiceMemoryPlayer = null
        isVoiceMemoryPlaying = false
    }

    fun toggleVoiceMemoryPlayback() {
        if (!hasVoiceMemoryAttachment) return
        if (isVoiceMemoryPlaying) {
            stopVoiceMemoryPlayback()
            return
        }

        val path = voiceMemoryPath ?: return
        stopVoiceMemoryPlayback()
        try {
            voiceMemoryPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    stopVoiceMemoryPlayback()
                    voiceMemoryPath = getSavedReelVoiceMemoryPath(context, reel.id)
                }
                prepare()
                start()
            }
            isVoiceMemoryPlaying = true
        } catch (_: Exception) {
            stopVoiceMemoryPlayback()
            voiceMemoryPath = getSavedReelVoiceMemoryPath(context, reel.id)
        }
    }

    fun stopVoiceMemoryRecording(saveRecording: Boolean) {
        val recordingPath = pendingVoiceMemoryPath
        runCatching {
            voiceMemoryRecorder?.apply {
                stop()
                reset()
                release()
            }
        }
        voiceMemoryRecorder = null
        isRecordingVoiceMemory = false

        if (saveRecording && !recordingPath.isNullOrBlank()) {
            saveSavedReelVoiceMemoryPath(context, reel.id, recordingPath)
            voiceMemoryPath = recordingPath
        } else if (!recordingPath.isNullOrBlank()) {
            runCatching { File(recordingPath).delete() }
        }
        pendingVoiceMemoryPath = null
    }

    fun startVoiceMemoryRecording() {
        stopVoiceMemoryPlayback()
        val outputFile = createVoiceMemoryFileForLibrary(context, "reel_${reel.id}")
        runCatching {
            val recorder = buildVoiceMemoryRecorderForLibrary(context, outputFile)
            recorder.prepare()
            recorder.start()
            voiceMemoryRecorder = recorder
            pendingVoiceMemoryPath = outputFile.absolutePath
            isRecordingVoiceMemory = true
        }.onFailure {
            voiceMemoryRecorder = null
            pendingVoiceMemoryPath = null
            isRecordingVoiceMemory = false
            runCatching { outputFile.delete() }
        }
    }

    DisposableEffect(reel.id) {
        onDispose {
            stopVoiceMemoryPlayback()
            runCatching {
                voiceMemoryRecorder?.run {
                    reset()
                    release()
                }
            }
            voiceMemoryRecorder = null
        }
    }

    if (showEditDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showEditDetailsDialog = false },
            title = { Text("Edit Reel Details") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Reel name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditDetailsDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = editedName.trim()
                        if (newName.isNotEmpty()) {
                            onEditDetailsClick(newName)
                        }
                        showEditDetailsDialog = false
                    }
                ) {
                    Text("Save")
                }
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
                    Text(
                        "Voice Memory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            onClick = {
                                when {
                                    isRecordingVoiceMemory -> stopVoiceMemoryRecording(saveRecording = true)
                                    isVoiceMemoryPlaying -> stopVoiceMemoryPlayback()
                                    !voiceMemoryPath.isNullOrBlank() -> toggleVoiceMemoryPlayback()
                                    else -> startVoiceMemoryRecording()
                                }
                            },
                            shape = CircleShape,
                            color = when {
                                isRecordingVoiceMemory -> Color(0xFFD32F2F)
                                isVoiceMemoryPlaying -> MaterialTheme.colorScheme.primaryContainer
                                !voiceMemoryPath.isNullOrBlank() -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = when {
                                        isRecordingVoiceMemory -> Icons.Default.Stop
                                        isVoiceMemoryPlaying -> Icons.Default.Stop
                                        !voiceMemoryPath.isNullOrBlank() -> Icons.Default.PlayArrow
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

                    Text(
                        text = when {
                            isRecordingVoiceMemory -> "Recording - tap to save"
                            isVoiceMemoryPlaying -> "Playing - tap to stop"
                            !voiceMemoryPath.isNullOrBlank() -> "Tap to play"
                            else -> "Tap to record"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isRecordingVoiceMemory && !voiceMemoryPath.isNullOrBlank()) {
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

    val actionButtonWidth = 86.dp
    val actionRailWidth = 172.dp
    val actionRailWidthPx = with(LocalDensity.current) { actionRailWidth.toPx() }
    var horizontalOffsetPx by remember(reel.id) { mutableStateOf(0f) }
    val revealThresholdPx = actionRailWidthPx * 0.35f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFCFDFF))
    ) {
        val actionsRevealed = horizontalOffsetPx <= -revealThresholdPx

        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp)),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .width(actionButtonWidth)
                    .fillMaxHeight()
                    .background(if (actionsRevealed) Color(0xFF2E7D52) else Color.Transparent)
                    .clickable(enabled = actionsRevealed) {
                        onShareClick()
                        horizontalOffsetPx = 0f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = "Share",
                    tint = if (actionsRevealed) Color.White else Color.Transparent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Share",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (actionsRevealed) Color.White else Color.Transparent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .width(actionButtonWidth)
                    .fillMaxHeight()
                    .background(if (actionsRevealed) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                    .clickable(enabled = actionsRevealed) {
                        onDeleteClick()
                        horizontalOffsetPx = 0f
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (actionsRevealed) MaterialTheme.colorScheme.onErrorContainer else Color.Transparent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = if (actionsRevealed) MaterialTheme.colorScheme.onErrorContainer else Color.Transparent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .offset { IntOffset(horizontalOffsetPx.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta: Float ->
                        val candidateOffset = horizontalOffsetPx + delta
                        horizontalOffsetPx = candidateOffset.coerceIn(-actionRailWidthPx, 0f)
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        horizontalOffsetPx = if (horizontalOffsetPx <= -revealThresholdPx) -actionRailWidthPx else 0f
                    }
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFD2D8DE),
                    shape = RoundedCornerShape(14.dp)
                ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            ) {
                if (!thumbnailPath.isNullOrBlank() && reelThumbnail != null) {
                    Image(
                        bitmap = reelThumbnail.asImageBitmap(),
                        contentDescription = "Reel preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF2E6BB4), Color(0xFF1C8CFF))
                                )
                            )
                    )
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(52.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.34f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        formatDuration(durationMillis ?: 0L),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onPlayClick() },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play Reel",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = reel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        editedName = reel.name
                        showEditDetailsDialog = true
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                if (hasVoiceMemoryAttachment) {
                                    toggleVoiceMemoryPlayback()
                                } else {
                                    val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (!permissionGranted) {
                                        voiceMemoryPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        showVoiceMemoryDialog = true
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Add or edit memory",
                            tint = if (hasVoiceMemoryAttachment) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasVoiceMemoryAttachment) "Memory" else "Add Memory",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onToggleTopPlay() }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (isInTopPlay) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isInTopPlay) "In top plays" else "Add to top plays",
                            tint = if (isInTopPlay) Color(0xFFE0B13F) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Top Play",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = buildString {
                        append("${reel.clipIds.size} clip${if (reel.clipIds.size == 1) "" else "s"}")
                        if (!clipDatesLabel.isNullOrBlank()) {
                            append(" • ")
                            append(clipDatesLabel)
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }
        }
    }
    }
}

@Composable
private fun rememberVideoThumbnail(context: Context, filePath: String) = produceState<Bitmap?>(
    initialValue = null,
    key1 = filePath
) {
    value = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val sourceUri = Uri.parse(filePath)
            if (sourceUri.scheme == "content") {
                retriever.setDataSource(context, sourceUri)
            } else {
                val directPath = if (sourceUri.scheme == "file") sourceUri.path else filePath
                if (directPath.isNullOrBlank()) return@withContext null
                retriever.setDataSource(directPath)
            }
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime()
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDateTime(timestampMillis: Long): String {
    return try {
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(timestampMillis))
    } catch (_: Exception) {
        ""
    }
}

private fun formatSeasonLabel(gameDate: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(gameDate) ?: return ""
        val calendar = Calendar.getInstance().apply { time = parsed }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        if (month >= 8) {
            val next = (year + 1) % 100
            "${year}-${String.format(Locale.getDefault(), "%02d", next)}"
        } else {
            val previous = year - 1
            val current = year % 100
            "${previous}-${String.format(Locale.getDefault(), "%02d", current)}"
        }
    } catch (_: Exception) {
        ""
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun extractDetectedJerseyNumbers(bubbleMetadata: String, maxNumbers: Int = 4): List<String> {
    if (bubbleMetadata.isBlank()) return emptyList()
    val jerseyNumberRegex = "\"jerseyNumber\"\\s*:\\s*\"?([^\",}]+)\"?".toRegex()
    val allNumbers = jerseyNumberRegex
        .findAll(bubbleMetadata)
        .map { it.groupValues.getOrNull(1)?.trim().orEmpty() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
    if (allNumbers.isEmpty()) return emptyList()
    return if (allNumbers.size <= maxNumbers) {
        allNumbers
    } else {
        allNumbers.take(maxNumbers) + "+${allNumbers.size - maxNumbers}"
    }
}

data class SavedReel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val clipIds: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)

enum class HighlightReelFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_SEASON("This Season")
}

private fun filterHighlightsByDate(videos: List<VideoClip>, filter: HighlightReelFilter): List<VideoClip> {
    val now = System.currentTimeMillis()
    val oneDayMillis = 24L * 60L * 60L * 1000L
    val oneWeekMillis = 7L * oneDayMillis
    val oneMonthMillis = 30L * oneDayMillis
    val oneSeasonMillis = 120L * oneDayMillis // ~4 months
    
    return when (filter) {
        HighlightReelFilter.ALL -> videos
        HighlightReelFilter.TODAY -> videos.filter { now - it.createdAt < oneDayMillis }
        HighlightReelFilter.THIS_WEEK -> videos.filter { now - it.createdAt < oneWeekMillis }
        HighlightReelFilter.THIS_MONTH -> videos.filter { now - it.createdAt < oneMonthMillis }
        HighlightReelFilter.THIS_SEASON -> videos.filter { now - it.createdAt < oneSeasonMillis }
    }
}
