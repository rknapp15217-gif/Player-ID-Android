package com.playerid.app.ui.screens

import android.content.Intent
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.playerid.app.data.Player
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.data.VideoClip
import com.playerid.app.video.VideoProcessingManager
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val NO_OPPONENT_FILTER = "No opponent specified"
private const val REEL_SHARE_TAG = "ReelShare"
private val PlaysAccentColor = Color(0xFF1C8CFF)
private val PlaysBackgroundColor = Color(0xFFF4F6F8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreen(
    playerViewModel: PlayerViewModel,
    teamViewModel: TeamViewModel,
    cameraHandoffToken: Int = 0,
    onNavigateToTeams: () -> Unit
) {
    val cameraTeam by teamViewModel.selectedTeam.collectAsState()
    // Local state per screen; Camera only hands off on explicit camera navigation exits.
    var localTeamName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(cameraHandoffToken) {
        localTeamName = cameraTeam
    }
    val teamName = localTeamName
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeam = remember(subscribedTeams, teamName) {
        subscribedTeams.firstOrNull { it.name == teamName }
    }
    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var videos by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var allTeamVideos by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var cleanupInProgress by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoClip?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTeamMenu by remember { mutableStateOf(false) }
    var showKidMenu by remember { mutableStateOf(false) }
    var showFiltersMenu by remember { mutableStateOf(false) }
    var expandedTeamSection by remember { mutableStateOf(false) }
    var expandedKidSection by remember { mutableStateOf(false) }
    var expandedSeasonSection by remember { mutableStateOf(false) }
    var expandedOpponentSection by remember { mutableStateOf(false) }
    var expandedViewSection by remember { mutableStateOf(false) }
    var isSelectModeEnabled by remember { mutableStateOf(false) }
    var selectedShareVideoIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var libraryBrowseMode by remember { mutableStateOf(VideoLibraryBrowseMode.CLIPS) }
    var highlightPlaylistUris by remember { mutableStateOf<List<Uri>?>(null) }
    var activeHighlightReelFilter by remember { mutableStateOf<HighlightReelFilter?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var selectedVideoStartInShareFlow by remember { mutableStateOf(false) }
    var shareDialogUri by remember { mutableStateOf<Uri?>(null) }
    var shareDialogExtraUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var sharePreviewUri by remember { mutableStateOf<Uri?>(null) }
    var shareDialogTeam by remember { mutableStateOf<com.playerid.app.data.Team?>(null) }
    var shareDialogNeedsColorSelection by remember { mutableStateOf(false) }
    var shareSelectedJerseyColorHex by remember { mutableStateOf<String?>(null) }
    var shareSelectedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var suggestedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var isRunningManualDeepScan by remember { mutableStateOf(false) }
    var scanSuggestionsDone by remember { mutableStateOf(false) }
    var shareHasAccurateScan by remember { mutableStateOf(false) }
    var hallOfFameClipIds by remember(teamName) { mutableStateOf<Set<String>>(emptySet()) }
    var hallOfFameReelFilters by remember(teamName) { mutableStateOf<Set<HighlightReelFilter>>(emptySet()) }
    var savedGoatReels by remember(teamName) { mutableStateOf<List<SavedReel>>(emptyList()) }
    var showCreateReelDialog by remember { mutableStateOf(false) }
    var activeReelId by remember { mutableStateOf<String?>(null) }
    val kidOptions by teamViewModel.kidOptions.collectAsState()
    var selectedKid by remember(teamName) {
        mutableStateOf<String?>(null)
    }
    val shareContactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val primaryUri = shareDialogUri
            if (primaryUri != null) {
                val contact = readSelectedContact(context, contactUri)
                if (contact != null) {
                    val urisToShare = listOf(primaryUri) + shareDialogExtraUris
                    shareVideosToPhoneContact(context, urisToShare, emptyList(), contact)
                }
            }
        }
    }
    val uploadPlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val selectedTeamName = teamName
        if (uri != null && !selectedTeamName.isNullOrBlank()) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            val uriString = uri.toString()
            val now = System.currentTimeMillis()
            context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(uriString, selectedTeamName)
                .apply()
            context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
                .edit()
                .putLong(uriString, now)
                .apply()
            context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(uriString, selectedKid ?: teamViewModel.getSelectedKidForTeam(selectedTeamName))
                .apply()

            scope.launch {
                isLoading = true
                videos = loadTeamVideosForClips(context, selectedTeamName)
                allTeamVideos = loadAllVideosForClips(context)
                isLoading = false
            }

            // Precompute share suggestions in the background so share opens quickly later.
            scope.launch(Dispatchers.Default) {
                try {
                    val rosterForTeam = allPlayers
                        .filter { it.team == selectedTeamName }
                        .sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.number }, { it.name }))
                    if (rosterForTeam.isEmpty()) return@launch

                    val uriString = uri.toString()
                    val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                    val dao = database.videoDetectionResultDao()
                    val existing = dao.getDetectionResult(uriString)
                    if (existing != null) return@launch

                    val teamForClip = subscribedTeams.firstOrNull { it.name == selectedTeamName }
                    val precomputeColorHex = teamForClip?.homeJerseyColor
                    val manager = VideoProcessingManager(context)
                    val result = try {
                        manager.autoDetectPlayersWithTracksInVideo(
                            videoUri = uri,
                            roster = rosterForTeam,
                            mode = VideoProcessingManager.DetectionMode.FAST,
                            jerseyColorHex = precomputeColorHex,
                            maxScanDurationMs = 9_500L,
                            stopAfterUniqueDetections = 3
                        )
                    } finally {
                        manager.release()
                    }

                    val detectionJson = com.playerid.app.data.DetectionResultSerializer.serialize(result)
                    dao.insertDetectionResult(
                        com.playerid.app.data.VideoDetectionResultEntity(
                            videoUri = uriString,
                            detectionMode = "FAST",
                            detectionJson = detectionJson,
                            detectionTimestampMs = System.currentTimeMillis()
                        )
                    )
                    com.playerid.app.video.VideoSharePreparationCache.set(
                        uri,
                        com.playerid.app.video.PreparedShareResult(
                            analysisResult = result,
                            preparedAtMs = System.currentTimeMillis(),
                            mode = VideoProcessingManager.DetectionMode.FAST
                        )
                    )
                    com.playerid.app.video.DeferredDeepScanScheduler.schedule(
                        context = context,
                        videoUri = uri,
                        roster = rosterForTeam,
                        jerseyColorHex = precomputeColorHex
                    )
                } catch (_: Exception) {
                    // Background prep is best-effort and should never interrupt clip upload flow.
                }
            }
            Toast.makeText(context, "Play uploaded", Toast.LENGTH_SHORT).show()
        }
    }
    var selectedGameKey by remember(teamName) { mutableStateOf<String?>(null) }
    var selectedSeasonKey by remember(teamName) { mutableStateOf<String?>(null) }

    LaunchedEffect(teamName) {
        val selected = teamName
        if (selected.isNullOrBlank()) {
            videos = emptyList()
            allTeamVideos = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        videos = loadTeamVideosForClips(context, selected)
        allTeamVideos = loadAllVideosForClips(context)
        isLoading = false
    }

    if (teamName.isNullOrBlank()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select a team to view clips", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateToTeams) {
                    Text("Go to Teams")
                }
            }
        }
        return
    }

    val rosterPlayers = remember(allPlayers, teamName) {
        allPlayers.filter { it.team == teamName }
    }
    val shareRosterPlayers = remember(rosterPlayers) {
        rosterPlayers.sortedWith(
            compareBy<Player>(
                { it.number.toIntOrNull() ?: Int.MAX_VALUE },
                { it.number },
                { it.name }
            )
        )
    }
    val opponentLookup = remember(videos) {
        buildClipOpponentLookup(context, videos)
    }
    val kidLookup = remember(videos) {
        buildClipKidLookup(context, videos)
    }
    val availableKidOptions = remember(videos, kidLookup) {
        videos
            .mapNotNull { video -> kidLookup[video.id] }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val selectedKidName = selectedKid
    val kidScopedVideos = remember(videos, kidLookup, selectedKidName) {
        val kidFilter = selectedKidName?.trim().orEmpty()
        if (kidFilter.isEmpty()) {
            videos
        } else {
            videos.filter { video ->
                kidLookup[video.id]?.equals(kidFilter, ignoreCase = true) == true
            }
        }
    }
    val availableSeasons = remember(kidScopedVideos) {
        kidScopedVideos
            .mapNotNull { video -> parseSeasonKey(video.gameDate) }
            .distinctBy { it.key }
            .sortedByDescending { it.startYear }
    }
    val seasonScopedVideos = remember(kidScopedVideos, selectedSeasonKey) {
        if (selectedSeasonKey == null) {
            kidScopedVideos
        } else {
            kidScopedVideos.filter { video ->
                parseSeasonKey(video.gameDate)?.key == selectedSeasonKey
            }
        }
    }
    val gameSections = remember(seasonScopedVideos, opponentLookup) {
        buildClipGameSections(seasonScopedVideos, opponentLookup)
    }
    val selectedGameSection = remember(gameSections, selectedGameKey) {
        gameSections.firstOrNull { it.key == selectedGameKey }
    }
    // Season is top-level; opponent then narrows and list sections split by date.
    val visibleSections = remember(selectedGameSection, seasonScopedVideos, opponentLookup) {
        if (selectedGameSection != null) {
            buildClipDateSubSections(selectedGameSection.videos, opponentLookup)
        } else {
            buildClipListSectionsForAllGames(seasonScopedVideos, opponentLookup)
        }
    }
    val visibleVideos = remember(visibleSections) {
        visibleSections.flatMap { it.videos }
    }
    val selectedHeartVideos = remember(seasonScopedVideos, selectedGameSection, selectedKid) {
        val scopedVideos = selectedGameSection?.videos ?: seasonScopedVideos
        scopedVideos.filter { it.isHighlight }
    }
    val selectedBatchUris = remember(selectedShareVideoIds, visibleVideos) {
        visibleVideos.filter { selectedShareVideoIds.contains(it.id) }.map { Uri.parse(it.filePath) }
    }
    val selectedBatchVideos = remember(selectedShareVideoIds, visibleVideos) {
        visibleVideos.filter { selectedShareVideoIds.contains(it.id) }
    }
    val isClipBrowseMode = libraryBrowseMode == VideoLibraryBrowseMode.CLIPS
    val highlightCount = remember(selectedHeartVideos) { selectedHeartVideos.size }
    val heroOpponentLabel = remember(selectedHeartVideos, opponentLookup) {
        buildReelDraftOpponentLabel(selectedHeartVideos, opponentLookup)
    }
    val starReelHeroClip = remember(selectedHeartVideos) {
        pickStarReelHeroClip(selectedHeartVideos)
    }
    val starReelHeroFrame by rememberStarReelHeroFrame(
        context = context,
        clip = starReelHeroClip
    )
    val starReelGlow = rememberInfiniteTransition(label = "starReelGlow").animateFloat(
        initialValue = 0.22f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starReelGlowAlpha"
    )
    val starReelShimmerX = rememberInfiniteTransition(label = "starReelShimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "starReelShimmerX"
    )
    fun openBatchShareDialog() {
        val primary = selectedBatchUris.firstOrNull() ?: return
        shareSelectedPlayerIds = emptySet()
        shareDialogUri = primary
        shareDialogExtraUris = selectedBatchUris.drop(1)
        sharePreviewUri = primary
    }
    LaunchedEffect(libraryBrowseMode) {
        if (libraryBrowseMode != VideoLibraryBrowseMode.CLIPS) {
            isSelectModeEnabled = false
            selectedShareVideoIds = emptySet()
        }
    }
    LaunchedEffect(gameSections, selectedGameKey, selectedGameSection) {
        if (gameSections.isEmpty()) {
            selectedGameKey = null
        } else if (selectedGameSection == null) {
            selectedGameKey = null
        }
    }
    LaunchedEffect(availableKidOptions, selectedKidName) {
        if (selectedKidName != null && availableKidOptions.none { option -> option.equals(selectedKidName, ignoreCase = true) }) {
            selectedKid = null
        }
    }
    LaunchedEffect(availableSeasons, selectedSeasonKey) {
        if (selectedSeasonKey != null && availableSeasons.none { it.key == selectedSeasonKey }) {
            selectedSeasonKey = null
        }
    }

    fun runShareDetection(
        mode: VideoProcessingManager.DetectionMode,
        maxScanDurationMs: Long,
        stopAfterUniqueDetections: Int,
        isManualDeepScan: Boolean
    ) {
        val uri = sharePreviewUri ?: shareDialogUri ?: return
        if (shareRosterPlayers.isEmpty()) return

        shareSelectedPlayerIds = emptySet()
        suggestedPlayerIds = emptySet()
        scanSuggestionsDone = false
        isLoadingSuggestions = true
        isRunningManualDeepScan = isManualDeepScan

        scope.launch(Dispatchers.Default) {
            val applyDetectionResult: (com.playerid.app.video.VideoPlayerDetectionResult) -> Unit = { result ->
                val detected = result.bubbles.mapNotNull { bubble ->
                    shareRosterPlayers.find { it.number == bubble.jerseyNumber }?.id
                }.toSet()
                suggestedPlayerIds = detected
                if (detected.isNotEmpty()) shareSelectedPlayerIds = detected
            }

            try {
                if (!isManualDeepScan) {
                    com.playerid.app.video.VideoSharePreparationCache.get(uri)?.let { prepared ->
                        applyDetectionResult(prepared.analysisResult)
                        shareHasAccurateScan = prepared.mode == VideoProcessingManager.DetectionMode.ACCURATE
                        return@launch
                    }

                    val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                    val dao = database.videoDetectionResultDao()
                    val persisted = dao.getDetectionResult(uri.toString())
                    if (persisted != null) {
                        val deserialized = com.playerid.app.data.DetectionResultSerializer.deserialize(persisted.detectionJson)
                        if (deserialized != null) {
                            applyDetectionResult(deserialized)
                            val persistedMode = if (persisted.detectionMode == "ACCURATE") {
                                VideoProcessingManager.DetectionMode.ACCURATE
                            } else {
                                VideoProcessingManager.DetectionMode.FAST
                            }
                            shareHasAccurateScan = persistedMode == VideoProcessingManager.DetectionMode.ACCURATE
                            com.playerid.app.video.VideoSharePreparationCache.set(
                                uri,
                                com.playerid.app.video.PreparedShareResult(
                                    analysisResult = deserialized,
                                    preparedAtMs = persisted.detectionTimestampMs,
                                    mode = persistedMode
                                )
                            )
                            return@launch
                        }
                    }
                }

                val vpm = VideoProcessingManager(context)
                val result = try {
                    vpm.autoDetectPlayersWithTracksInVideo(
                        videoUri = uri,
                        roster = shareRosterPlayers,
                        mode = mode,
                        jerseyColorHex = shareSelectedJerseyColorHex ?: (shareDialogTeam ?: selectedTeam)?.homeJerseyColor,
                        maxScanDurationMs = maxScanDurationMs,
                        stopAfterUniqueDetections = stopAfterUniqueDetections
                    )
                } finally {
                    vpm.release()
                }

                applyDetectionResult(result)
                shareHasAccurateScan = mode == VideoProcessingManager.DetectionMode.ACCURATE

                val detectionModeName = if (mode == VideoProcessingManager.DetectionMode.ACCURATE) "ACCURATE" else "FAST"
                val database = com.playerid.app.data.PlayerDatabase.getDatabase(context)
                val dao = database.videoDetectionResultDao()
                val detectionJson = com.playerid.app.data.DetectionResultSerializer.serialize(result)
                dao.insertDetectionResult(
                    com.playerid.app.data.VideoDetectionResultEntity(
                        videoUri = uri.toString(),
                        detectionMode = detectionModeName,
                        detectionJson = detectionJson,
                        detectionTimestampMs = System.currentTimeMillis()
                    )
                )
                com.playerid.app.video.VideoSharePreparationCache.set(
                    uri,
                    com.playerid.app.video.PreparedShareResult(
                        analysisResult = result,
                        preparedAtMs = System.currentTimeMillis(),
                        mode = mode
                    )
                )
                if (mode == VideoProcessingManager.DetectionMode.FAST) {
                    com.playerid.app.video.DeferredDeepScanScheduler.schedule(
                        context = context,
                        videoUri = uri,
                        roster = shareRosterPlayers,
                        jerseyColorHex = shareSelectedJerseyColorHex ?: (shareDialogTeam ?: selectedTeam)?.homeJerseyColor
                    )
                }
            } catch (e: Exception) {
                android.util.Log.d("ClipsScreen", "Detection failed: ${e.message}")
            } finally {
                isLoadingSuggestions = false
                isRunningManualDeepScan = false
                scanSuggestionsDone = true
            }
        }
    }

    LaunchedEffect(teamName) {
        val selectedTeamName = teamName ?: return@LaunchedEffect
        hallOfFameClipIds = loadHallOfFameClipIds(context, selectedTeamName)
        hallOfFameReelFilters = loadHallOfFameReelFilters(context, selectedTeamName)
        savedGoatReels = loadSavedGoatReels(context, selectedTeamName)
    }

    LaunchedEffect(shareDialogUri) {
        sharePreviewUri = shareDialogUri
        if (shareDialogUri != null) {
            // Resolve team for this specific clip from stored metadata
            val clipTeamName = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
                .getString(shareDialogUri.toString(), null)
                ?.trim()
            shareDialogTeam = if (clipTeamName.isNullOrBlank()) {
                selectedTeam
            } else {
                subscribedTeams.firstOrNull { it.name.equals(clipTeamName, ignoreCase = true) } ?: selectedTeam
            }
            val inferredJerseyColorHex = (shareDialogTeam ?: selectedTeam)?.homeJerseyColor
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            shareDialogNeedsColorSelection = inferredJerseyColorHex.isNullOrBlank()
            shareSelectedJerseyColorHex = inferredJerseyColorHex
            shareSelectedPlayerIds = emptySet()
            suggestedPlayerIds = emptySet()
            scanSuggestionsDone = false
            shareHasAccurateScan = false
            isLoadingSuggestions = false
            isRunningManualDeepScan = false
        }
    }

    LaunchedEffect(sharePreviewUri) {
        if (sharePreviewUri != null) {
            shareSelectedPlayerIds = emptySet()
            suggestedPlayerIds = emptySet()
            scanSuggestionsDone = false
            shareHasAccurateScan = false
            isLoadingSuggestions = false
            isRunningManualDeepScan = false
        }
    }

    LaunchedEffect(sharePreviewUri, shareDialogUri, shareSelectedJerseyColorHex, shareDialogNeedsColorSelection, shareRosterPlayers) {
        val uri = sharePreviewUri ?: shareDialogUri ?: return@LaunchedEffect
        if (shareRosterPlayers.isEmpty()) return@LaunchedEffect
        if (shareDialogNeedsColorSelection && shareSelectedJerseyColorHex.isNullOrBlank()) return@LaunchedEffect
        if (scanSuggestionsDone) return@LaunchedEffect

        fun applyResult(result: com.playerid.app.video.VideoPlayerDetectionResult) {
            val detected = result.bubbles.mapNotNull { bubble ->
                shareRosterPlayers.find { it.number == bubble.jerseyNumber }?.id
            }.toSet()
            suggestedPlayerIds = detected
            if (detected.isNotEmpty()) shareSelectedPlayerIds = detected
            scanSuggestionsDone = true
        }

        // In-memory cache hit � instant, no loading spinner shown.
        val cached = com.playerid.app.video.VideoSharePreparationCache.get(uri)
        if (cached != null) {
            shareHasAccurateScan = cached.mode == VideoProcessingManager.DetectionMode.ACCURATE
            applyResult(cached.analysisResult)
            return@LaunchedEffect
        }

        // DB hit � fast, still no loading spinner.
        val persisted = withContext(Dispatchers.IO) {
            com.playerid.app.data.PlayerDatabase.getDatabase(context)
                .videoDetectionResultDao()
                .getDetectionResult(uri.toString())
        }
        if (persisted != null) {
            val deserialized = com.playerid.app.data.DetectionResultSerializer.deserialize(persisted.detectionJson)
            if (deserialized != null) {
                val mode = if (persisted.detectionMode == "ACCURATE")
                    VideoProcessingManager.DetectionMode.ACCURATE
                else
                    VideoProcessingManager.DetectionMode.FAST
                com.playerid.app.video.VideoSharePreparationCache.set(
                    uri,
                    com.playerid.app.video.PreparedShareResult(
                        analysisResult = deserialized,
                        preparedAtMs = persisted.detectionTimestampMs,
                        mode = mode
                    )
                )
                shareHasAccurateScan = mode == VideoProcessingManager.DetectionMode.ACCURATE
                applyResult(deserialized)
                return@LaunchedEffect
            }
        }

        // Nothing cached � run live scan (shows loading spinner).
        runShareDetection(
            mode = VideoProcessingManager.DetectionMode.FAST,
            maxScanDurationMs = 9_500L,
            stopAfterUniqueDetections = 3,
            isManualDeepScan = false
        )
    }

    if (showCreateReelDialog) {
        CreateReelDialog(
            allVideos = selectedHeartVideos,
            onDismiss = { showCreateReelDialog = false },
            onSave = { name, clipIds ->
                val selectedTeamName = teamName
                if (selectedTeamName != null) {
                    val scopeValidationError = validateReelScopeForClipIds(
                        context = context,
                        clipIds = clipIds,
                        allVideos = allTeamVideos
                    )
                    if (scopeValidationError != null) {
                        Toast.makeText(context, scopeValidationError, Toast.LENGTH_SHORT).show()
                        return@CreateReelDialog
                    }
                    val reel = SavedReel(name = name, clipIds = clipIds)
                    savedGoatReels = saveGoatReel(context, selectedTeamName, reel, savedGoatReels)

                    // Reels are created from temporary heart selections; clear hearts after save.
                    val highlightedIds = selectedHeartVideos.map { it.id }.toSet()
                    val prefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    highlightedIds.forEach { id -> editor.putBoolean(id, false) }
                    editor.apply()
                    videos = videos.map { if (highlightedIds.contains(it.id)) it.copy(isHighlight = false) else it }
                    allTeamVideos = allTeamVideos.map { if (highlightedIds.contains(it.id)) it.copy(isHighlight = false) else it }
                }
                showCreateReelDialog = false
            },
            onCreateAndShare = { name, clipIds ->
                val selectedTeamName = teamName
                if (selectedTeamName != null) {
                    val scopeValidationError = validateReelScopeForClipIds(
                        context = context,
                        clipIds = clipIds,
                        allVideos = allTeamVideos
                    )
                    if (scopeValidationError != null) {
                        Toast.makeText(context, scopeValidationError, Toast.LENGTH_SHORT).show()
                        return@CreateReelDialog
                    }
                    val reel = SavedReel(name = name, clipIds = clipIds)
                    savedGoatReels = saveGoatReel(context, selectedTeamName, reel, savedGoatReels)

                    val selectedReelVideos = selectedHeartVideos
                        .filter { clipIds.contains(it.id) }
                    val clipUris = selectedReelVideos.map { Uri.parse(it.filePath) }

                    if (clipUris.isNotEmpty()) {
                        scope.launch {
                            Toast.makeText(context, "Preparing reel for sharing...", Toast.LENGTH_SHORT).show()
                            val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
                            val reelOpponents = selectedReelVideos.mapNotNull { clip ->
                                opponentPrefs.getString(clip.id, null)?.trim()?.takeIf { it.isNotBlank() }
                            }.distinct()

                            val scenario = when {
                                name.contains("top plays", ignoreCase = true) || name.contains("top play", ignoreCase = true) -> "top_plays"
                                name.contains(" vs ", ignoreCase = true) || reelOpponents.size == 1 -> "opponent"
                                else -> "season"
                            }

                            val sharedReelUri = buildShareableReelWithIntro(
                                context = context,
                                clipUris = clipUris,
                                reelTitle = name,
                                teamName = selectedTeamName,
                                opponentName = reelOpponents.firstOrNull(),
                                scenario = scenario
                            )

                            if (sharedReelUri != null) {
                                shareSelectedPlayerIds = emptySet()
                                shareDialogUri = sharedReelUri
                                shareDialogExtraUris = emptyList()
                                sharePreviewUri = sharedReelUri
                            } else {
                                Toast.makeText(
                                    context,
                                    "Unable to build reel video. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "No clips found for this reel", Toast.LENGTH_SHORT).show()
                    }

                    // Reels are created from temporary heart selections; clear hearts after save.
                    val highlightedIds = selectedHeartVideos.map { it.id }.toSet()
                    val prefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    highlightedIds.forEach { id -> editor.putBoolean(id, false) }
                    editor.apply()
                    videos = videos.map { if (highlightedIds.contains(it.id)) it.copy(isHighlight = false) else it }
                    allTeamVideos = allTeamVideos.map { if (highlightedIds.contains(it.id)) it.copy(isHighlight = false) else it }
                }
                showCreateReelDialog = false
            }
        )
    }

    highlightPlaylistUris?.let { playlistUris ->
        val reelVideos = playlistUris.mapNotNull { uri ->
            allTeamVideos.firstOrNull { it.filePath == uri.toString() }
        }
        val reelOpponents = reelVideos
            .mapNotNull { clip ->
                opponentLookup[clip.id]
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
            .distinct()
        val reelSeasonLabels = reelVideos
            .mapNotNull { clip ->
                parseSeasonKey(clip.gameDate)?.label
            }
            .distinct()
        val reelSeasonLabel = when (reelSeasonLabels.size) {
            0 -> null
            1 -> reelSeasonLabels.first()
            else -> "${reelSeasonLabels.first()} +${reelSeasonLabels.size - 1}"
        }
        val resolvedReelTitle = savedGoatReels.firstOrNull { it.id == activeReelId }?.name
            ?: activeHighlightReelFilter?.let(::hallOfFameReelTitle)
        val titleLower = resolvedReelTitle?.lowercase().orEmpty()
        val reelScenario = when {
            titleLower.contains("top plays") || titleLower.contains("top play") -> "top_plays"
            titleLower.contains(" vs ") || reelOpponents.size == 1 -> "opponent"
            activeHighlightReelFilter == HighlightReelFilter.THIS_SEASON -> "season"
            else -> "season"
        }

        VideoPlaybackScreen(
            videoUri = playlistUris.first(),
            detectedPlayers = emptyList(),
            onNavigateBack = {
                highlightPlaylistUris = null
                activeHighlightReelFilter = null
                activeReelId = null
            },
            playlistUris = playlistUris,
            reelTitle = resolvedReelTitle,
            reelTeamName = teamName,
            reelSeasonLabel = reelSeasonLabel,
            reelOpponents = reelOpponents,
            reelScenario = reelScenario,
            activeReelId = activeReelId,
            onSaveAsGoatReel = { name ->
                val selectedTeamName = teamName ?: return@VideoPlaybackScreen
                val clipIds = playlistUris.mapNotNull { uri ->
                    allTeamVideos.firstOrNull { it.filePath == uri.toString() }?.id
                }
                val scopeValidationError = validateReelScopeForClipIds(
                    context = context,
                    clipIds = clipIds,
                    allVideos = allTeamVideos
                )
                if (scopeValidationError != null) {
                    Toast.makeText(context, scopeValidationError, Toast.LENGTH_SHORT).show()
                    return@VideoPlaybackScreen
                }
                val reelId = activeReelId ?: java.util.UUID.randomUUID().toString()
                val reel = SavedReel(id = reelId, name = name, clipIds = clipIds)
                savedGoatReels = saveGoatReel(context, selectedTeamName, reel, savedGoatReels)
                activeReelId = reelId
            }
        )
        return
    }

    selectedVideoUri?.let { uri ->
        VideoPlaybackScreen(
            videoUri = uri,
            detectedPlayers = selectedVideoPlayers,
            onNavigateBack = {
                selectedVideoUri = null
                selectedVideoPlayers = emptyList()
                selectedVideoStartInShareFlow = false
            },
            startInShareFlow = selectedVideoStartInShareFlow
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        color = Color.White,
                        tonalElevation = 2.dp,
                        shadowElevation = 3.dp,
                        onClick = { showTeamMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = PlaysAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = teamName ?: "Select team",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch team",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showTeamMenu,
                        onDismissRequest = { showTeamMenu = false }
                    ) {
                        subscribedTeams.forEach { team ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        team.name,
                                        fontWeight = if (team.name == teamName) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    localTeamName = team.name
                                    selectedKid = teamViewModel.getSelectedKidForTeam(team.name)
                                    showTeamMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = PlaysAccentColor.copy(alpha = 0.12f),
                    onClick = { uploadPlayLauncher.launch(arrayOf("video/*")) }
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Import clip from phone",
                            tint = PlaysAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { showKidMenu = true },
                        label = { Text("Kid: $selectedKid", fontWeight = FontWeight.SemiBold) },
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PlaysAccentColor.copy(alpha = 0.12f),
                            selectedLabelColor = PlaysAccentColor,
                            selectedTrailingIconColor = PlaysAccentColor
                        )
                    )
                    DropdownMenu(
                        expanded = showKidMenu,
                        onDismissRequest = { showKidMenu = false }
                    ) {
                        kidOptions.forEach { kidName ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        kidName,
                                        fontWeight = if (kidName == selectedKid) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (kidName == selectedKid) PlaysAccentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedKid = kidName
                                    teamViewModel.selectKidForTeam(teamName, kidName)
                                    showKidMenu = false
                                }
                            )
                        }
                    }
                }

                Box {
                    FilterChip(
                        selected = selectedSeasonKey != null || selectedGameKey != null || libraryBrowseMode != VideoLibraryBrowseMode.CLIPS,
                        onClick = { showFiltersMenu = true },
                        label = { Text("Filters", fontWeight = FontWeight.SemiBold) },
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PlaysAccentColor.copy(alpha = 0.12f),
                            selectedLabelColor = PlaysAccentColor,
                            selectedTrailingIconColor = PlaysAccentColor
                        )
                    )

                    if (showFiltersMenu) {
                        androidx.compose.material3.ModalBottomSheet(
                            onDismissRequest = { showFiltersMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    "Filters",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                if (availableSeasons.isNotEmpty()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp)),
                                        onClick = { expandedSeasonSection = !expandedSeasonSection }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Season",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PlaysAccentColor
                                            )
                                            Icon(
                                                if (expandedSeasonSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = PlaysAccentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (expandedSeasonSection) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp)),
                                                color = if (selectedSeasonKey == null) PlaysAccentColor.copy(alpha = 0.12f) else Color.Transparent,
                                                onClick = { selectedSeasonKey = null }
                                            ) {
                                                Text(
                                                    "All seasons",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selectedSeasonKey == null) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (selectedSeasonKey == null) PlaysAccentColor else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                            availableSeasons.forEach { season ->
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    color = if (selectedSeasonKey == season.key) PlaysAccentColor.copy(alpha = 0.12f) else Color.Transparent,
                                                    onClick = { selectedSeasonKey = season.key }
                                                ) {
                                                    Text(
                                                        season.label,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (selectedSeasonKey == season.key) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (selectedSeasonKey == season.key) PlaysAccentColor else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (gameSections.isNotEmpty()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp)),
                                        onClick = { expandedOpponentSection = !expandedOpponentSection }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Opponent",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PlaysAccentColor
                                            )
                                            Icon(
                                                if (expandedOpponentSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = PlaysAccentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (expandedOpponentSection) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp)),
                                                color = if (selectedGameKey == null) PlaysAccentColor.copy(alpha = 0.12f) else Color.Transparent,
                                                onClick = { selectedGameKey = null }
                                            ) {
                                                Text(
                                                    "All opponents",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selectedGameKey == null) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (selectedGameKey == null) PlaysAccentColor else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                            gameSections.forEach { section ->
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    color = if (selectedGameKey == section.key) PlaysAccentColor.copy(alpha = 0.12f) else Color.Transparent,
                                                    onClick = { selectedGameKey = section.key }
                                                ) {
                                                    Text(
                                                        section.title.ifBlank { "Unspecified" },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (selectedGameKey == section.key) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (selectedGameKey == section.key) PlaysAccentColor else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    onClick = { expandedViewSection = !expandedViewSection }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "View",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PlaysAccentColor
                                        )
                                        Icon(
                                            if (expandedViewSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = PlaysAccentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (expandedViewSection) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, bottom = 20.dp, top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            VideoLibraryBrowseMode.CLIPS to "Clips",
                                            VideoLibraryBrowseMode.REELS to "Reels",
                                            VideoLibraryBrowseMode.TOP_PLAYS to "Top Plays"
                                        ).forEach { (mode, label) ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp)),
                                                color = if (libraryBrowseMode == mode) PlaysAccentColor.copy(alpha = 0.12f) else Color.Transparent,
                                                onClick = { libraryBrowseMode = mode }
                                            ) {
                                                Text(
                                                    label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (libraryBrowseMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (libraryBrowseMode == mode) PlaysAccentColor else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PlaysBackgroundColor)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            if (isClipBrowseMode && selectedHeartVideos.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1C3A24),
                                        Color(0xFF122A18)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1200f, 700f)
                                )
                            )
                    ) {
                        if (starReelHeroFrame != null) {
                            Image(
                                bitmap = starReelHeroFrame!!.asImageBitmap(),
                                contentDescription = "Star Reel featured play",
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0.94f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xE6227C3B),
                                            Color(0x884FA45A),
                                            Color.Transparent
                                        ),
                                        startX = 0f,
                                        endX = 520f
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.22f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.14f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(900f, 540f)
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Reel in the Making",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        lineHeight = 24.sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )

                                Text(
                                    "$highlightCount moments from\n${heroOpponentLabel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 18.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }

                            if (selectedHeartVideos.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            val filtered = filterHighlightsForClips(selectedHeartVideos, HighlightReelFilter.ALL)
                                            if (filtered.isNotEmpty()) {
                                                activeHighlightReelFilter = HighlightReelFilter.ALL
                                                activeReelId = null
                                                highlightPlaylistUris = filtered.map { Uri.parse(it.filePath) }
                                            }
                                        },
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF30463A)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preview Reel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showCreateReelDialog = true },
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2E7D52),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Create Reel",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isClipBrowseMode && selectedBatchVideos.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { openBatchShareDialog() },
                            enabled = selectedBatchUris.isNotEmpty(),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlaysAccentColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Share selected")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (cleanupInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            VideoLibraryScreen(
                teamName = teamName!!,
                videos = visibleVideos,
                sections = visibleSections,
                rosterPlayers = rosterPlayers,
                isLoading = isLoading,
                lastRefreshedLabel = "",
                emptyStateTitle = if (selectedGameSection != null) "No clips in this game" else "No videos yet",
                emptyStateSubtitle = if (selectedGameSection != null) {
                    "Try another game or return to all games"
                } else {
                    "Record a video to get started"
                },
                onNavigateBack = {},
                onRefresh = {
                    scope.launch {
                        isLoading = true
                        videos = loadTeamVideosForClips(context, teamName!!)
                        allTeamVideos = loadAllVideosForClips(context)
                        isLoading = false
                    }
                },
                onVideoSelected = { uri, players ->
                    selectedVideoStartInShareFlow = false
                    selectedVideoPlayers = players
                    selectedVideoUri = uri
                },
                onVideoEdit = { uri ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        Toast.makeText(context, "No video editor found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onOpponentChanged = { video, opponent ->
                    val prefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString(video.id, opponent).apply()
                    // Refresh the video list to rebuild sections with new opponent
                    scope.launch {
                        videos = loadTeamVideosForClips(context, teamName!!)
                        allTeamVideos = loadAllVideosForClips(context)
                    }
                },
                onVideoShare = { uri ->
                    shareSelectedPlayerIds = emptySet()
                    shareDialogUri = uri
                    shareDialogExtraUris = emptyList()
                    sharePreviewUri = uri
                },
                isSelectionModeEnabled = isSelectModeEnabled && isClipBrowseMode,
                selectedShareVideoIds = selectedShareVideoIds,
                onSelectedShareVideoIdsChange = { selectedShareVideoIds = it },
                onVideoDelete = { video ->
                    videoToDelete = video
                    showDeleteDialog = true
                },
                onVideoNameChanged = { video, newName ->
                    val prefs = context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
                    if (newName.isBlank()) {
                        prefs.edit().remove(video.id).apply()
                    } else {
                        prefs.edit().putString(video.id, newName).apply()
                    }
                    videos = videos.map { if (it.id == video.id) it.copy(momentTag = newName.ifBlank { null }) else it }
                    allTeamVideos = allTeamVideos.map { if (it.id == video.id) it.copy(momentTag = newName.ifBlank { null }) else it }
                },
                onToggleHighlight = { video ->
                    val newStatus = !video.isHighlight
                    val prefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(video.id, newStatus).apply()
                    videos = videos.map { if (it.id == video.id) it.copy(isHighlight = newStatus) else it }
                    allTeamVideos = allTeamVideos.map { if (it.id == video.id) it.copy(isHighlight = newStatus) else it }
                },
                hallOfFameClipIds = hallOfFameClipIds,
                savedGoatReels = savedGoatReels,
                onToggleHallOfFameClip = { video ->
                    val selectedTeamName = teamName ?: return@VideoLibraryScreen
                    hallOfFameClipIds = toggleHallOfFameClip(
                        context = context,
                        teamName = selectedTeamName,
                        clipId = video.id,
                        existing = hallOfFameClipIds
                    )
                },
                onPlaySavedReel = { reel ->
                    val reelUris = reel.clipIds.mapNotNull { id ->
                        allTeamVideos.firstOrNull { it.id == id }?.filePath?.let { Uri.parse(it) }
                    }
                    if (reelUris.isNotEmpty()) {
                        activeReelId = reel.id
                        highlightPlaylistUris = reelUris
                    } else {
                        Toast.makeText(context, "No clips found for this reel", Toast.LENGTH_SHORT).show()
                    }
                },
                onShareSavedReel = { reel ->
                    val reelVideos = reel.clipIds.mapNotNull { id ->
                        allTeamVideos.firstOrNull { it.id == id }
                    }
                    val reelUris = reelVideos.map { Uri.parse(it.filePath) }
                    if (reelUris.isEmpty()) {
                        Toast.makeText(context, "No clips found for this reel", Toast.LENGTH_SHORT).show()
                        return@VideoLibraryScreen
                    }

                    scope.launch {
                        Toast.makeText(context, "Preparing reel for sharing...", Toast.LENGTH_SHORT).show()
                        val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
                        val reelOpponents = reelVideos.mapNotNull { clip ->
                            opponentPrefs.getString(clip.id, null)?.trim()?.takeIf { it.isNotBlank() }
                        }.distinct()
                        val scenario = when {
                            reel.name.contains("top plays", ignoreCase = true) || reel.name.contains("top play", ignoreCase = true) -> "top_plays"
                            reel.name.contains(" vs ", ignoreCase = true) || reelOpponents.size == 1 -> "opponent"
                            else -> "season"
                        }

                        val sharedReelUri = buildShareableReelWithIntro(
                            context = context,
                            clipUris = reelUris,
                            reelTitle = reel.name,
                            teamName = teamName,
                            opponentName = reelOpponents.firstOrNull(),
                            scenario = scenario
                        )

                        if (sharedReelUri != null) {
                            shareSelectedPlayerIds = emptySet()
                            shareDialogUri = sharedReelUri
                            shareDialogExtraUris = emptyList()
                            sharePreviewUri = sharedReelUri
                        } else {
                            Toast.makeText(
                                context,
                                "Unable to build reel video. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onToggleSavedReelTopPlay = { reel ->
                    val selectedTeamName = teamName ?: return@VideoLibraryScreen
                    hallOfFameClipIds = toggleHallOfFameReelClips(
                        context = context,
                        teamName = selectedTeamName,
                        clipIds = reel.clipIds,
                        existing = hallOfFameClipIds
                    )
                },
                onEditSavedReelDetails = { reel, newName ->
                    val selectedTeamName = teamName ?: return@VideoLibraryScreen
                    val trimmedName = newName.trim()
                    if (trimmedName.isBlank()) {
                        Toast.makeText(context, "Reel name cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        val updatedReel = reel.copy(name = trimmedName)
                        savedGoatReels = saveGoatReel(context, selectedTeamName, updatedReel, savedGoatReels)
                    }
                },
                onDeleteSavedReel = { reel ->
                    val selectedTeamName = teamName ?: return@VideoLibraryScreen
                    savedGoatReels = deleteGoatReel(context, selectedTeamName, reel.id, savedGoatReels)
                    if (activeReelId == reel.id) activeReelId = null
                },
                onCreateReel = {
                    if (selectedHeartVideos.isNotEmpty()) {
                        showCreateReelDialog = true
                    } else {
                        Toast.makeText(context, "Select hearts first, then create a reel", Toast.LENGTH_SHORT).show()
                    }
                },
                showTopBar = false,
                selectionModeLabel = if (isSelectModeEnabled) "Cancel" else "Select",
                onSelectionModeToggle = {
                    isSelectModeEnabled = !isSelectModeEnabled
                    if (!isSelectModeEnabled) {
                        selectedShareVideoIds = emptySet()
                    }
                },
                onUploadRequested = { uploadPlayLauncher.launch(arrayOf("video/*")) },
                showModeFilters = false,
                browseMode = libraryBrowseMode,
                goatSourceVideos = seasonScopedVideos
            )
        }
    } // end outer Column

    // Share flow: minimal layout optimized for video review + player selection.
    shareDialogUri?.let { savedUri ->
        val allShareUris = listOf(savedUri) + shareDialogExtraUris
        val previewUri = sharePreviewUri ?: savedUri
        val previewIndex = allShareUris.indexOf(previewUri).takeIf { it >= 0 } ?: 0
        val screenConfig = androidx.compose.ui.platform.LocalConfiguration.current
        val isWideReviewLayout = screenConfig.screenWidthDp >= 860
        val dialogMaxHeight = screenConfig.screenHeightDp.dp * 0.96f
        val compactVideoMaxHeight = screenConfig.screenHeightDp.dp * 0.38f
        val hasSuggestions = suggestedPlayerIds.isNotEmpty()
        val basePlayerList = shareRosterPlayers
        val selectedPlayers = shareRosterPlayers.filter { shareSelectedPlayerIds.contains(it.id) }

        Dialog(
            onDismissRequest = {
                shareDialogUri = null
                shareDialogExtraUris = emptyList()
                sharePreviewUri = null
            },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.985f)
                    .heightIn(max = dialogMaxHeight),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (allShareUris.size == 1) "Share Play" else "Share ${allShareUris.size} Plays",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = {
                                shareDialogUri = null
                                shareDialogExtraUris = emptyList()
                                sharePreviewUri = null
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    if (allShareUris.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val prevIndex = (previewIndex - 1 + allShareUris.size) % allShareUris.size
                                    sharePreviewUri = allShareUris[prevIndex]
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Previous")
                            }
                            Text(
                                "Play ${previewIndex + 1} of ${allShareUris.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedButton(
                                onClick = {
                                    val nextIndex = (previewIndex + 1) % allShareUris.size
                                    sharePreviewUri = allShareUris[nextIndex]
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Next")
                            }
                        }
                    }

                    if (shareDialogNeedsColorSelection) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            jerseyColorMap().forEach { (hex, label) ->
                                FilterChip(
                                    selected = shareSelectedJerseyColorHex == hex,
                                    onClick = { shareSelectedJerseyColorHex = hex },
                                    label = { Text(label.replaceFirstChar { c -> c.uppercase() }) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share with Players")
                        }
                        OutlinedButton(
                            onClick = { shareContactPickerLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share with Contacts")
                        }
                    }

                    @Composable
                    fun PlayerSelectionPanel(modifier: Modifier = Modifier) {
                        Column(
                            modifier = modifier,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLoadingSuggestions) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning players...", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasSuggestions) {
                                    Text(
                                        text = "${suggestedPlayerIds.size} in play",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                TextButton(onClick = {
                                    shareSelectedPlayerIds = shareRosterPlayers
                                        .filter { it.addedBy.any(Char::isDigit) && it.addedBy.filter(Char::isDigit).length >= 10 }
                                        .map { it.id }
                                        .toSet()
                                }) {
                                    Text("All")
                                }
                                if (shareSelectedPlayerIds.isNotEmpty()) {
                                    TextButton(onClick = { shareSelectedPlayerIds = emptySet() }) {
                                        Text("Clear")
                                    }
                                }
                            }

                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val inPlayPlayers = basePlayerList.filter { suggestedPlayerIds.contains(it.id) }
                                val otherPlayers = basePlayerList.filterNot { suggestedPlayerIds.contains(it.id) }

                                if (inPlayPlayers.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "In Play",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(inPlayPlayers, key = { it.id }) { player ->
                                        val isSuggested = true
                                        val hasParentContact = player.addedBy.any(Char::isDigit) &&
                                            player.addedBy.filter(Char::isDigit).length >= 10
                                        val isChecked = hasParentContact && shareSelectedPlayerIds.contains(player.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .alpha(if (hasParentContact) 1f else 0.35f)
                                                .clickable(enabled = hasParentContact) {
                                                    shareSelectedPlayerIds = if (shareSelectedPlayerIds.contains(player.id)) {
                                                        shareSelectedPlayerIds - player.id
                                                    } else {
                                                        shareSelectedPlayerIds + player.id
                                                    }
                                                }
                                                .padding(vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                enabled = hasParentContact,
                                                onCheckedChange = { checked ->
                                                    shareSelectedPlayerIds = if (checked) {
                                                        shareSelectedPlayerIds + player.id
                                                    } else {
                                                        shareSelectedPlayerIds - player.id
                                                    }
                                                }
                                            )
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "#${player.number} ${player.name}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (isSuggested) {
                                                    Text(
                                                        text = "In play",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (otherPlayers.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "All Others",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                        )
                                    }
                                    items(otherPlayers, key = { it.id }) { player ->
                                        val isSuggested = false
                                        val hasParentContact = player.addedBy.any(Char::isDigit) &&
                                            player.addedBy.filter(Char::isDigit).length >= 10
                                        val isChecked = hasParentContact && shareSelectedPlayerIds.contains(player.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .alpha(if (hasParentContact) 1f else 0.35f)
                                                .clickable(enabled = hasParentContact) {
                                                    shareSelectedPlayerIds = if (shareSelectedPlayerIds.contains(player.id)) {
                                                        shareSelectedPlayerIds - player.id
                                                    } else {
                                                        shareSelectedPlayerIds + player.id
                                                    }
                                                }
                                                .padding(vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                enabled = hasParentContact,
                                                onCheckedChange = { checked ->
                                                    shareSelectedPlayerIds = if (checked) {
                                                        shareSelectedPlayerIds + player.id
                                                    } else {
                                                        shareSelectedPlayerIds - player.id
                                                    }
                                                }
                                            )
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "#${player.number} ${player.name}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (isSuggested) {
                                                    Text(
                                                        text = "In play",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isWideReviewLayout) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1.35f)
                                    .fillMaxHeight(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black)
                            ) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { viewContext ->
                                        android.widget.VideoView(viewContext).apply {
                                            val uriTag = previewUri.toString()
                                            tag = uriTag
                                            setVideoURI(previewUri)
                                            setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                start()
                                            }
                                        }
                                    },
                                    update = { videoView ->
                                        val expectedTag = previewUri.toString()
                                        if (videoView.tag != expectedTag) {
                                            videoView.tag = expectedTag
                                            videoView.setVideoURI(previewUri)
                                            videoView.setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                videoView.start()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            PlayerSelectionPanel(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 220.dp, max = compactVideoMaxHeight),
                                colors = CardDefaults.cardColors(containerColor = Color.Black)
                            ) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { viewContext ->
                                        android.widget.VideoView(viewContext).apply {
                                            val uriTag = previewUri.toString()
                                            tag = uriTag
                                            setVideoURI(previewUri)
                                            setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                start()
                                            }
                                        }
                                    },
                                    update = { videoView ->
                                        val expectedTag = previewUri.toString()
                                        if (videoView.tag != expectedTag) {
                                            videoView.tag = expectedTag
                                            videoView.setVideoURI(previewUri)
                                            videoView.setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                videoView.start()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Always-visible roster section for player sharing.
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                            ) {
                                Text(
                                    text = "Choose Players",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                PlayerSelectionPanel(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val contactEligiblePlayers = selectedPlayers.filter {
                                it.addedBy.any(Char::isDigit) &&
                                    it.addedBy.filter(Char::isDigit).length >= 10
                            }
                            val recipients = buildTeamShareRecipients(contactEligiblePlayers)
                            if (recipients.isEmpty()) {
                                launchPersonalShareChooser(context, allShareUris, "Share Play")
                            } else {
                                shareVideosToTeamRecipients(
                                    context = context,
                                    videoUris = allShareUris,
                                    recipients = recipients,
                                    players = contactEligiblePlayers,
                                    highlightTag = null,
                                    customMessage = ""
                                )
                            }
                            shareDialogUri = null
                            shareDialogExtraUris = emptyList()
                            sharePreviewUri = null
                        },
                        enabled = selectedPlayers.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }

    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("Delete all clips?") },
            text = { Text("This will permanently delete all clips for this team from your phone.") },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanupDialog = false
                        cleanupInProgress = true
                        scope.launch {
                            cleanupTeamClipsForClips(context, teamName!!)
                            videos = loadTeamVideosForClips(context, teamName!!)
                            allTeamVideos = loadAllVideosForClips(context)
                            cleanupInProgress = false
                        }
                    }
                                ) {
                    Text("Delete")
                }
            }
        )
    }

    if (showDeleteDialog && videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete clip?") },
            text = { Text("This will permanently delete '${videoToDelete!!.gameTitle}' from your phone.") },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = videoToDelete
                        showDeleteDialog = false
                        if (target != null) {
                            scope.launch {
                                deleteClipForClips(context, target)
                                videos = loadTeamVideosForClips(context, teamName!!)
                                allTeamVideos = loadAllVideosForClips(context)
                                videoToDelete = null
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }

    }

}

}

private suspend fun deleteClipForClips(context: android.content.Context, video: VideoClip) {
    return withContext(Dispatchers.IO) {
        val videoUri = android.net.Uri.parse(video.filePath)
        val videoId = video.id

        try {
            if (videoUri.scheme == "content") {
                context.contentResolver.delete(videoUri, null, null)
            }
            if (videoUri.scheme == "file") {
                val file = java.io.File(videoUri.path ?: "")
                file.delete()
            }
        } catch (_: Exception) {
        }

        val prefs = listOf(
            context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE)
        )
        prefs.forEach { pref ->
            pref.edit()
                .remove(videoId)
                .remove(video.filePath)
                .apply()
        }
    }
}

private suspend fun loadTeamVideosForClips(context: android.content.Context, teamName: String): List<VideoClip> {
    return withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoClip>()
        val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        val startPrefs = context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
        val highlightPrefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
        val customNamePrefs = context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
        val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
        val moviesDirs = context.getExternalFilesDirs(android.os.Environment.DIRECTORY_MOVIES).filterNotNull()
        val seenIds = mutableSetOf<String>()

        for (moviesDir in moviesDirs) {
            val videoFiles = moviesDir.listFiles { file ->
                file.isFile && file.extension.equals("mp4", ignoreCase = true)
            } ?: emptyArray()

            for (file in videoFiles.sortedByDescending { it.lastModified() }) {
                val videoPath = file.absolutePath
                val fileUriString = android.net.Uri.fromFile(file).toString()
                if (seenIds.contains(videoPath) || seenIds.contains(fileUriString)) continue

                val storedTeamName = teamPrefs.getString(videoPath, null)
                    ?: teamPrefs.getString(fileUriString, null)
                if (storedTeamName != teamName) continue

                val storedStartTime = startPrefs.getLong(videoPath, 0L)
                    .takeIf { it > 0L }
                    ?: startPrefs.getLong(fileUriString, 0L).takeIf { it > 0L }
                val createdAt = storedStartTime ?: file.lastModified()
                val gameDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(createdAt))
                val customName = customNamePrefs.getString(videoPath, null)
                    ?: customNamePrefs.getString(fileUriString, null)
                val opponentName = opponentPrefs.getString(videoPath, null)
                    ?: opponentPrefs.getString(fileUriString, null)
                val isHighlight = highlightPrefs.getBoolean(videoPath, false)
                    || highlightPrefs.getBoolean(fileUriString, false)
                val gameTitle = buildClipTitle(
                    baseTitle = file.nameWithoutExtension,
                    opponentName = opponentName,
                    customName = customName
                )

                val duration = try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(videoPath)
                    val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    retriever.release()
                    durationMs
                } catch (_: Exception) {
                    0L
                }

                videos.add(
                    VideoClip(
                        id = videoPath,
                        filePath = android.net.Uri.fromFile(file).toString(),
                        duration = duration,
                        createdAt = createdAt,
                        gameDate = gameDate,
                        gameTitle = gameTitle,
                        isHighlight = isHighlight,
                        momentTag = customName
                    )
                )
                seenIds.add(videoPath)
                seenIds.add(fileUriString)
            }
        }

        val resolver = context.contentResolver
        val collection = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            android.provider.MediaStore.Video.Media._ID,
            android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
            android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
            android.provider.MediaStore.MediaColumns.DATE_TAKEN,
            android.provider.MediaStore.MediaColumns.DATE_ADDED,
            android.provider.MediaStore.Video.Media.DURATION
        )
        val selectionPaths = listOf(
            "Movies/PlayerID/",
            "Movies/PlayerID",
            "Movies/Spotr/",
            "Movies/Spotr"
        )
        val selection = selectionPaths.joinToString(" OR ") { "${android.provider.MediaStore.MediaColumns.RELATIVE_PATH}=?" }
        val selectionArgs = selectionPaths.toTypedArray()
        val sortOrder = "${android.provider.MediaStore.MediaColumns.DATE_ADDED} DESC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_ADDED)
            val durationIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val contentUri = android.content.ContentUris.withAppendedId(collection, id)
                val uriString = contentUri.toString()
                if (seenIds.contains(uriString)) continue

                val storedTeamName = teamPrefs.getString(uriString, null)
                if (storedTeamName != teamName) continue

                val displayName = cursor.getString(nameIndex) ?: "clip_$id"
                val dateTaken = cursor.getLong(dateTakenIndex)
                val dateAdded = cursor.getLong(dateAddedIndex)
                val storedStartTime = startPrefs.getLong(uriString, 0L).takeIf { it > 0L }
                val createdAt = storedStartTime ?: if (dateTaken > 0) dateTaken else dateAdded * 1000
                val gameDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(createdAt))
                val duration = cursor.getLong(durationIndex)
                val customName = customNamePrefs.getString(uriString, null)
                val opponentName = opponentPrefs.getString(uriString, null)
                val isHighlight = highlightPrefs.getBoolean(uriString, false)
                val baseTitle = displayName.substringBeforeLast(".", displayName)
                val gameTitle = buildClipTitle(
                    baseTitle = baseTitle,
                    opponentName = opponentName,
                    customName = customName
                )

                videos.add(
                    VideoClip(
                        id = uriString,
                        filePath = uriString,
                        duration = duration,
                        createdAt = createdAt,
                        gameDate = gameDate,
                        gameTitle = gameTitle,
                        isHighlight = isHighlight,
                        momentTag = customName
                    )
                )
                seenIds.add(uriString)
            }
        }

        val taggedContentUris = teamPrefs.all.keys.filter { key ->
            key.startsWith("content://") && teamPrefs.getString(key, null) == teamName
        }
        for (uriString in taggedContentUris) {
            if (seenIds.contains(uriString)) continue
            val contentUri = Uri.parse(uriString)

            val createdAt = startPrefs.getLong(uriString, 0L).takeIf { it > 0L }
                ?: System.currentTimeMillis()
            val gameDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(createdAt))
            val customName = customNamePrefs.getString(uriString, null)
            val opponentName = opponentPrefs.getString(uriString, null)
            val isHighlight = highlightPrefs.getBoolean(uriString, false)
            val baseTitle = contentUri.lastPathSegment?.substringAfterLast('/') ?: "uploaded_clip"
            val gameTitle = buildClipTitle(
                baseTitle = baseTitle,
                opponentName = opponentName,
                customName = customName
            )

            val duration = try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, contentUri)
                val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                retriever.release()
                durationMs
            } catch (_: Exception) {
                0L
            }

            videos.add(
                VideoClip(
                    id = uriString,
                    filePath = uriString,
                    duration = duration,
                    createdAt = createdAt,
                    gameDate = gameDate,
                    gameTitle = gameTitle,
                    isHighlight = isHighlight,
                    momentTag = customName
                )
            )
            seenIds.add(uriString)
        }

        videos
    }
}

private suspend fun loadAllVideosForClips(context: android.content.Context): List<VideoClip> {
    return withContext(Dispatchers.IO) {
        val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        val teamNames = teamPrefs.all.values
            .mapNotNull { it as? String }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        if (teamNames.isEmpty()) {
            emptyList()
        } else {
            teamNames
                .flatMap { team -> loadTeamVideosForClips(context, team) }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
        }
    }
}

private suspend fun cleanupTeamClipsForClips(context: android.content.Context, teamName: String) {
    withContext(Dispatchers.IO) {
        val teamVideos = loadTeamVideosForClips(context, teamName)
        for (video in teamVideos) {
            try {
                deleteClipForClips(context, video)
            } catch (_: Exception) {
            }
        }
    }
}

private fun launchPersonalShareChooser(context: android.content.Context, videoUris: List<Uri>, shareTitle: String = "Share this moment") {
    if (videoUris.isEmpty()) return
    if (videoUris.size == 1) {
        launchPersonalShareChooser(context, videoUris.first(), shareTitle)
        return
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = "video/mp4"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(videoUris))
        putExtra(Intent.EXTRA_TEXT, "Created with Spotr")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, shareTitle))
}

private fun shareVideosToTeamRecipients(
    context: android.content.Context,
    videoUris: List<Uri>,
    recipients: List<TeamShareRecipient>,
    players: List<Player>,
    highlightTag: String?,
    customMessage: String
) {
    if (videoUris.isEmpty()) return
    if (videoUris.size == 1) {
        shareVideoToTeamRecipients(
            context = context,
            videoUri = videoUris.first(),
            recipients = recipients,
            players = players,
            highlightTag = highlightTag,
            customMessage = customMessage
        )
        return
    }

    val recipientNames = recipients.joinToString(", ") { it.displayName }
    val message = customMessage.ifBlank { buildDefaultTeamShareMessage(players, highlightTag) }
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = "video/mp4"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(videoUris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_SUBJECT, "Spotr plays for $recipientNames")
        putExtra(Intent.EXTRA_TEXT, "$message\n\nFor: $recipientNames")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share"))
}

private fun shareVideosToPhoneContact(
    context: android.content.Context,
    videoUris: List<Uri>,
    players: List<Player>,
    contact: SelectedContact
) {
    if (videoUris.isEmpty()) return
    if (videoUris.size == 1) {
        shareVideoToPhoneContact(context, videoUris.first(), players, contact)
        return
    }

    val names = players.joinToString(", ") { "#${it.number} ${it.name}" }
    val message = if (players.isEmpty()) {
        "Hi ${contact.displayName}, sharing Spotr plays"
    } else {
        "Hi ${contact.displayName}, sharing plays featuring: $names"
    }
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = "video/mp4"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(videoUris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_SUBJECT, "Spotr highlights for ${contact.displayName}")
        putExtra(Intent.EXTRA_TEXT, message)
        if (!contact.email.isNullOrBlank()) {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
        }
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share plays with ${contact.displayName}"))
}

internal suspend fun buildShareableReelWithIntro(
    context: android.content.Context,
    clipUris: List<Uri>,
    reelTitle: String,
    teamName: String?,
    opponentName: String?,
    scenario: String
): Uri? {
    if (clipUris.isEmpty()) return null
    return withContext(Dispatchers.IO) {
        try {
            val exportDir = java.io.File(context.cacheDir, "reel_share_exports").apply { mkdirs() }
            val introImage = java.io.File(exportDir, "intro_${System.currentTimeMillis()}.png")
            val introBitmap = createReelIntroBitmap(
                reelTitle = reelTitle,
                teamName = teamName,
                opponentName = opponentName,
                scenario = scenario
            )
            java.io.FileOutputStream(introImage).use { out ->
                introBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            introBitmap.recycle()

            val exportedFile = java.io.File(exportDir, "reel_${System.currentTimeMillis()}.mp4")
            val didExport = exportReelComposition(context, introImage, clipUris, exportedFile)
            if (!didExport || !exportedFile.exists()) {
                Log.w(REEL_SHARE_TAG, "Reel export failed or file missing for $reelTitle")
                return@withContext null
            }

            persistVideoToMediaStore(context, exportedFile)
        } catch (t: Throwable) {
            Log.e(REEL_SHARE_TAG, "Reel share export failed for $reelTitle", t)
            null
        }
    }
}

internal fun createReelIntroBitmap(
    reelTitle: String,
    teamName: String?,
    opponentName: String?,
    scenario: String
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf(
                AndroidColor.parseColor("#060C10"),
                AndroidColor.parseColor("#0D1B14"),
                AndroidColor.parseColor("#05080A")
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val streakPaintA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#2CFF8A")
        alpha = 44
        strokeWidth = 22f
    }
    val streakPaintB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#45C8FF")
        alpha = 34
        strokeWidth = 16f
    }
    canvas.drawLine(60f, 240f, 640f, 10f, streakPaintA)
    canvas.drawLine(420f, 240f, 1040f, 20f, streakPaintB)
    canvas.drawLine(90f, 1660f, 1040f, 1320f, streakPaintA)

    val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#B2FFD0")
        alpha = 170
    }
    listOf(
        Triple(132f, 178f, 6f),
        Triple(206f, 144f, 4.5f),
        Triple(936f, 162f, 5f),
        Triple(994f, 214f, 3.5f),
        Triple(142f, 1704f, 5.5f),
        Triple(988f, 1668f, 4.8f)
    ).forEach { (x, y, r) ->
        canvas.drawCircle(x, y, r, sparkPaint)
    }

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            80f,
            height * 0.45f,
            width.toFloat(),
            height * 0.72f,
            intArrayOf(
                AndroidColor.parseColor("#0F2018"),
                AndroidColor.parseColor("#1E4E33"),
                AndroidColor.parseColor("#0A1511")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        alpha = 165
    }
    canvas.drawRoundRect(64f, 520f, width - 64f, 1220f, 54f, 54f, glowPaint)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#12191F")
        alpha = 232
    }
    canvas.drawRoundRect(88f, 300f, width - 88f, 1320f, 42f, 42f, cardPaint)

    val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = AndroidColor.parseColor("#3F6E56")
        alpha = 210
    }
    canvas.drawRoundRect(88f, 300f, width - 88f, 1320f, 42f, 42f, cardStroke)

    val topLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#6AC98A")
        strokeWidth = 6f
        alpha = 195
    }
    canvas.drawLine(172f, 350f, width - 172f, 350f, topLinePaint)

    val teamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#E8EDF2")
        textSize = 54f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.075f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val resolvedTeam = teamName?.trim().orEmpty().ifBlank { "NORTH ALLEGHENY LACROSSE" }
    drawCenteredMultilineText(
        canvas = canvas,
        text = resolvedTeam.uppercase(),
        paint = teamPaint,
        centerX = width / 2f,
        top = 410f,
        maxWidth = width - 260f,
        maxLines = 2,
        lineHeightMultiplier = 1.1f
    )

    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#C8D2DA")
        textSize = 46f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    val sectionLabel = when (scenario) {
        "top_plays" -> "TOP PLAYS"
        "season" -> "SEASON REEL"
        else -> "MATCHUP REEL"
    }
    canvas.drawText(sectionLabel, width / 2f, 610f, sectionPaint)

    val vsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#79D98F")
        textSize = 112f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    }
    val rivalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#F7FAFC")
        textSize = 108f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    }
    val headline = when (scenario) {
        "top_plays" -> "TOP PLAYS"
        "season" -> "SEASON"
        else -> opponentName?.trim().orEmpty().ifBlank { "OPPONENT" }.uppercase()
    }

    if (scenario == "opponent") {
        canvas.drawText("VS", width / 2f, 790f, vsPaint)
        drawCenteredMultilineText(
            canvas = canvas,
            text = headline,
            paint = rivalPaint,
            centerX = width / 2f,
            top = 860f,
            maxWidth = width - 220f,
            maxLines = 2,
            lineHeightMultiplier = 1.02f
        )
    } else {
        drawCenteredMultilineText(
            canvas = canvas,
            text = headline,
            paint = rivalPaint,
            centerX = width / 2f,
            top = 810f,
            maxWidth = width - 220f,
            maxLines = 2,
            lineHeightMultiplier = 1.02f
        )
    }

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#73CC8C")
        strokeWidth = 3f
        alpha = 185
    }
    canvas.drawLine(190f, 1090f, width - 190f, 1090f, dividerPaint)

    val reelNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#D2DBE2")
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    drawCenteredMultilineText(
        canvas = canvas,
        text = reelTitle.uppercase(),
        paint = reelNamePaint,
        centerX = width / 2f,
        top = 1135f,
        maxWidth = width - 220f,
        maxLines = 2,
        lineHeightMultiplier = 1.1f
    )

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#9FB2A3")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    canvas.drawText("SPOTR HIGHLIGHTS", width / 2f, 1420f, footerPaint)

    return bitmap
}

private fun drawCenteredMultilineText(
    canvas: Canvas,
    text: String,
    paint: Paint,
    centerX: Float,
    top: Float,
    maxWidth: Float,
    maxLines: Int,
    lineHeightMultiplier: Float
): Float {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return top

    val words = trimmed.split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = ""

    fun commitLine(value: String) {
        if (value.isNotBlank() && lines.size < maxLines) {
            lines += value
        }
    }

    for (word in words) {
        val candidate = if (current.isBlank()) word else "$current $word"
        val candidateWidth = paint.measureText(candidate)
        if (candidateWidth <= maxWidth || current.isBlank()) {
            current = candidate
        } else {
            commitLine(current)
            current = word
            if (lines.size == maxLines - 1) break
        }
    }
    commitLine(current)

    if (lines.isEmpty()) return top

    val overflowed = words.joinToString(" ") != lines.joinToString(" ")
    if (overflowed && lines.isNotEmpty()) {
        val lastIndex = lines.lastIndex
        var last = lines[lastIndex]
        while (last.isNotBlank() && paint.measureText("$last...") > maxWidth) {
            last = last.dropLast(1)
        }
        lines[lastIndex] = if (last.isBlank()) "..." else "$last..."
    }

    val lineHeight = paint.textSize * lineHeightMultiplier
    lines.forEachIndexed { index, line ->
        canvas.drawText(line, centerX, top + (index * lineHeight), paint)
    }
    return top + ((lines.size - 1) * lineHeight)
}

private suspend fun exportReelComposition(
    context: android.content.Context,
    introImage: java.io.File,
    clipUris: List<Uri>,
    outputFile: java.io.File
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val exportDir = outputFile.parentFile ?: return@withContext false
            val introVideo = java.io.File(exportDir, "intro_video_${System.currentTimeMillis()}.mp4")
            val concatList = java.io.File(exportDir, "concat_${System.currentTimeMillis()}.txt")

            val introCommand = arrayOf(
                "-y",
                "-loop", "1",
                "-i", introImage.absolutePath,
                "-t", "2.2",
                "-vf", "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black,format=yuv420p",
                "-c:v", "mpeg4",
                "-q:v", "4",
                "-r", "30",
                "-an",
                introVideo.absolutePath
            )
            val introSession = FFmpegKit.executeWithArguments(introCommand)
            val introSuccess = ReturnCode.isSuccess(introSession.returnCode)
            if (!introSuccess || !introVideo.exists()) {
                Log.w(REEL_SHARE_TAG, "FFmpeg intro render failed for ${introImage.name}: ${introSession.allLogsAsString}")
                return@withContext false
            }

            val normalizedClips = mutableListOf<java.io.File>()
            clipUris.forEachIndexed { index, uri ->
                val stagedClip = stageUriToLocalFile(context, uri, exportDir, index) ?: run {
                    Log.w(REEL_SHARE_TAG, "Could not stage clip uri for export: $uri")
                    return@withContext false
                }
                val normalizedClip = java.io.File(exportDir, "clip_norm_${index}_${System.currentTimeMillis()}.mp4")
                val normalizeCommand = arrayOf(
                    "-y",
                    "-i", stagedClip.absolutePath,
                    "-vf", "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black,format=yuv420p",
                    "-c:v", "mpeg4",
                    "-q:v", "4",
                    "-r", "30",
                    "-an",
                    normalizedClip.absolutePath
                )
                val normalizeSession = FFmpegKit.executeWithArguments(normalizeCommand)
                val normalizeSuccess = ReturnCode.isSuccess(normalizeSession.returnCode)
                if (!normalizeSuccess || !normalizedClip.exists()) {
                    Log.w(REEL_SHARE_TAG, "FFmpeg normalize failed for ${stagedClip.name}: ${normalizeSession.allLogsAsString}")
                    return@withContext false
                }
                normalizedClips += normalizedClip
            }

            val concatLines = buildString {
                appendLine("file '${introVideo.absolutePath.replace("'", "'\\''")}'")
                normalizedClips.forEach { clipFile ->
                    appendLine("file '${clipFile.absolutePath.replace("'", "'\\''")}'")
                }
            }
            concatList.writeText(concatLines)

            val concatCommand = arrayOf(
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.absolutePath,
                "-c:v", "mpeg4",
                "-q:v", "4",
                "-r", "30",
                "-an",
                "-movflags", "+faststart",
                outputFile.absolutePath
            )
            val concatSession = FFmpegKit.executeWithArguments(concatCommand)
            val concatSuccess = ReturnCode.isSuccess(concatSession.returnCode)
            if (!concatSuccess) {
                Log.w(REEL_SHARE_TAG, "FFmpeg concat failed for ${outputFile.name}: ${concatSession.allLogsAsString}")
            }
            concatSuccess && outputFile.exists()
        } catch (t: Throwable) {
            Log.e(REEL_SHARE_TAG, "FFmpeg reel export crashed", t)
            false
        }
    }
}

private fun resolveUriToPath(context: android.content.Context, uri: Uri): String? {
    if (uri.scheme == "file") return uri.path
    if (uri.scheme == "content") {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                if (index >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(index)
                }
            }
        }
    }
    return null
}

private fun stageUriToLocalFile(
    context: android.content.Context,
    uri: Uri,
    exportDir: java.io.File,
    index: Int
): java.io.File? {
    if (uri.scheme == "file") {
        val existing = uri.path?.let { java.io.File(it) }
        if (existing != null && existing.exists()) return existing
    }

    val resolvedPath = resolveUriToPath(context, uri)
    if (!resolvedPath.isNullOrBlank()) {
        val file = java.io.File(resolvedPath)
        if (file.exists()) return file
    }

    return runCatching {
        val staged = java.io.File(exportDir, "clip_src_${index}_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(staged).use { output ->
                input.copyTo(output)
            }
        } ?: return null
        staged
    }.getOrNull()
}

private fun persistVideoToMediaStore(context: android.content.Context, file: java.io.File): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Spotr")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openOutputStream(uri)?.use { output ->
        java.io.FileInputStream(file).use { input ->
            input.copyTo(output)
        }
    } ?: return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val finalizeValues = ContentValues().apply {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }
        resolver.update(uri, finalizeValues, null, null)
    }
    return uri
}

private fun jerseyColorMap(): Map<String, String> = mapOf(
    "#0B3D91" to "navy",
    "#1976D2" to "royal blue",
    "#E53E3E" to "red",
    "#7A0019" to "maroon",
    "#059669" to "green",
    "#111827" to "black",
    "#FFFFFF" to "white",
    "#9CA3AF" to "gray",
    "#D4AF37" to "gold",
    "#EA580C" to "orange",
    "#7C3AED" to "purple",
    "#0D9488" to "teal"
)

private fun hexToJerseyColorLabel(hex: String?): String? {
    if (hex.isNullOrBlank()) return null
    val normalized = hex.trim().uppercase()
    return jerseyColorMap()[normalized]
}

private fun parseScreenTeamColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

private fun pickStarReelHeroClip(videos: List<VideoClip>): VideoClip? {
    val highlights = videos.filter { it.isHighlight }
    if (highlights.isEmpty()) return null

    // Prefer tagged big moments and clips with stronger team-action signals.
    val bigPlayTags = setOf("goal", "assist", "save", "amazing", "big play", "winning")
    return highlights.maxWithOrNull(
        compareBy<VideoClip> { clip ->
            val tag = clip.momentTag?.lowercase().orEmpty()
            if (bigPlayTags.any { tag.contains(it) }) 1 else 0
        }
            .thenBy { estimateClipActionScore(it) }
            .thenBy { it.duration }
            .thenBy { it.createdAt }
    )
}

private fun buildReelDraftOpponentLabel(
    selectedVideos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): String {
    val opponents = selectedVideos
        .mapNotNull { clip ->
            opponentLookup[clip.id]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.split(" ")
                ?.joinToString(" ") { token -> token.replaceFirstChar(Char::uppercase) }
        }
        .distinctBy { it.lowercase() }

    return when (opponents.size) {
        0 -> "all opponents"
        1 -> opponents.first()
        2 -> "${opponents[0]} + ${opponents[1]}"
        else -> "${opponents.first()} + ${opponents.size - 1} more"
    }
}

@Composable
private fun rememberStarReelHeroFrame(context: android.content.Context, clip: VideoClip?) = produceState<Bitmap?>(
    initialValue = null,
    key1 = clip?.id,
    key2 = clip?.filePath,
    key3 = clip?.duration
) {
    value = withContext(Dispatchers.IO) {
        val target = clip ?: return@withContext null
        val retriever = MediaMetadataRetriever()
        try {
            val sourceUri = Uri.parse(target.filePath)
            if (sourceUri.scheme == "content") {
                retriever.setDataSource(context, sourceUri)
            } else {
                val directPath = if (sourceUri.scheme == "file") sourceUri.path else target.filePath
                if (directPath.isNullOrBlank()) return@withContext null
                retriever.setDataSource(directPath)
            }

            val candidateUs = chooseStarReelCandidateFrameUs(target)
            var bestBitmap: Bitmap? = null
            var bestScore = Double.NEGATIVE_INFINITY
            candidateUs.forEach { frameUs ->
                val bitmap = retriever.getFrameAtTime(frameUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val correctedBitmap = applyVideoRotationIfNeeded(bitmap, retriever)
                    val score = scoreFrameForHeader(correctedBitmap)
                    if (score > bestScore) {
                        bestScore = score
                        bestBitmap = correctedBitmap
                    }
                }
            }

            bestBitmap
                ?: retriever.getFrameAtTime(chooseStarReelActionFrameUs(target), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                    applyVideoRotationIfNeeded(it, retriever)
                }
                ?: retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                    applyVideoRotationIfNeeded(it, retriever)
                }
                ?: retriever.getFrameAtTime()?.let {
                    applyVideoRotationIfNeeded(it, retriever)
                }
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }
}

private fun estimateClipActionScore(clip: VideoClip): Long {
    val intervals = extractBubbleIntervals(clip.bubbleMetadata, clip.duration)
    if (intervals.isEmpty()) return 0L
    val maxOverlap = findMaxOverlap(intervals)
    return (maxOverlap * 1_000L) + intervals.size
}

private fun chooseStarReelActionFrameUs(clip: VideoClip): Long {
    val durationMs = clip.duration.coerceAtLeast(1_000L)
    val intervals = extractBubbleIntervals(clip.bubbleMetadata, durationMs)

    val chosenMs = if (intervals.isNotEmpty()) {
        val mid = durationMs / 2L
        val candidates = linkedSetOf<Long>()
        intervals.forEach { (start, end) ->
            val clampedStart = start.coerceIn(0L, durationMs)
            val clampedEnd = end.coerceIn(clampedStart, durationMs)
            val center = ((clampedStart + clampedEnd) / 2L).coerceIn(0L, durationMs)
            candidates.add(clampedStart)
            candidates.add(center)
            candidates.add(clampedEnd)
        }

        var bestMs = (durationMs * 35L) / 100L
        var bestScore = Long.MIN_VALUE
        for (candidate in candidates) {
            val overlapCount = intervals.count { candidate >= it.first && candidate <= it.second }
            val distancePenalty = kotlin.math.abs(candidate - mid)
            val score = overlapCount * 100_000L - distancePenalty
            if (score > bestScore) {
                bestScore = score
                bestMs = candidate
            }
        }
        bestMs
    } else {
        (durationMs * 35L) / 100L
    }

    val minUs = 500_000L
    val maxUs = (durationMs * 1_000L).coerceAtLeast(minUs + 100_000L) - 100_000L
    return (chosenMs * 1_000L).coerceIn(minUs, maxUs)
}

private fun chooseStarReelCandidateFrameUs(clip: VideoClip): List<Long> {
    val durationMs = clip.duration.coerceAtLeast(1_000L)
    val intervals = extractBubbleIntervals(clip.bubbleMetadata, durationMs)
    val candidatesMs = linkedSetOf<Long>()

    if (intervals.isNotEmpty()) {
        intervals.forEach { (start, end) ->
            val center = ((start + end) / 2L).coerceIn(0L, durationMs)
            candidatesMs.add(start.coerceIn(0L, durationMs))
            candidatesMs.add(center)
            candidatesMs.add(end.coerceIn(0L, durationMs))
        }
    }

    // Add robust fallback points to avoid unlucky keyframes.
    candidatesMs.add((durationMs * 22L) / 100L)
    candidatesMs.add((durationMs * 35L) / 100L)
    candidatesMs.add((durationMs * 50L) / 100L)
    candidatesMs.add((durationMs * 65L) / 100L)

    val minUs = 500_000L
    val maxUs = (durationMs * 1_000L).coerceAtLeast(minUs + 100_000L) - 100_000L
    return candidatesMs
        .map { (it * 1_000L).coerceIn(minUs, maxUs) }
        .toList()
}

private fun scoreFrameForHeader(bitmap: Bitmap): Double {
    // Prefer sharper frames with enough detail/contrast for a hero image.
    val width = bitmap.width
    val height = bitmap.height
    if (width < 3 || height < 3) return Double.NEGATIVE_INFINITY

    val stepX = (width / 28).coerceAtLeast(1)
    val stepY = (height / 18).coerceAtLeast(1)

    var edgeEnergy = 0.0
    var sampleCount = 0

    var y = stepY
    while (y < height - stepY) {
        var x = stepX
        while (x < width - stepX) {
            val c = bitmap.getPixel(x, y)
            val cx = bitmap.getPixel(x + stepX, y)
            val cy = bitmap.getPixel(x, y + stepY)

            val l = luminance(c)
            val lx = luminance(cx)
            val ly = luminance(cy)

            val gx = lx - l
            val gy = ly - l
            edgeEnergy += kotlin.math.sqrt((gx * gx) + (gy * gy))
            sampleCount += 1

            x += stepX
        }
        y += stepY
    }

    if (sampleCount == 0) return Double.NEGATIVE_INFINITY
    return edgeEnergy / sampleCount
}

private fun applyVideoRotationIfNeeded(bitmap: Bitmap, retriever: MediaMetadataRetriever): Bitmap {
    return pickBestRotationForFrame(bitmap, retriever)
}

private fun pickBestRotationForFrame(bitmap: Bitmap, retriever: MediaMetadataRetriever): Bitmap {
    val rawRotation = retriever
        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        ?.toIntOrNull()
        ?: 0
    val rotation = ((rawRotation % 360) + 360) % 360

    val candidates = LinkedHashSet<Int>()

    // Primary rule: most of these clips are landscape, so fix portrait first.
    if (bitmap.height > bitmap.width) {
        candidates.add(270)
        candidates.add(90)
        candidates.add(180)
    }

    // Secondary hint: metadata can still help in some files.
    if (rotation != 0) {
        candidates.add((360 - rotation) % 360)
        candidates.add(rotation)
    }

    candidates.add(0)

    var bestBitmap = bitmap
    var bestScore = scoreFrameWithOrientationBias(bitmap, 0)

    candidates.forEach { candidateRotation ->
        if (candidateRotation == 0) return@forEach
        try {
            val matrix = Matrix().apply { postRotate(candidateRotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val score = scoreFrameWithOrientationBias(rotated, candidateRotation)
            if (score > bestScore) {
                if (bestBitmap !== bitmap) {
                    try { bestBitmap.recycle() } catch (_: Exception) { }
                }
                bestBitmap = rotated
                bestScore = score
            } else {
                try { rotated.recycle() } catch (_: Exception) { }
            }
        } catch (_: Exception) {
        }
    }

    return bestBitmap
}

private fun scoreFrameWithOrientationBias(bitmap: Bitmap, appliedRotation: Int): Double {
    val edgeScore = scoreFrameForHeader(bitmap)

    // Strongly prefer landscape orientation (width > height) for sports video.
    val width = bitmap.width
    val height = bitmap.height
    val aspectRatio = width.toDouble() / height.toDouble()

    // Landscape gets huge bonus, portrait gets severe penalty.
    val orientationBonus = when {
        aspectRatio > 1.3 -> 500.0
        aspectRatio > 1.0 -> 100.0
        aspectRatio < 0.77 -> -1000.0
        else -> -200.0
    }

    // Tie-break toward 270-degree correction, matching current device symptom.
    val rotationBias = when (appliedRotation) {
        270 -> 8.0
        0 -> 4.0
        180 -> 2.0
        else -> 0.0
    }

    return edgeScore + orientationBonus + rotationBias
}

private fun luminance(colorInt: Int): Double {
    val r = android.graphics.Color.red(colorInt).toDouble()
    val g = android.graphics.Color.green(colorInt).toDouble()
    val b = android.graphics.Color.blue(colorInt).toDouble()
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun extractBubbleIntervals(bubbleMetadata: String, durationMs: Long): List<Pair<Long, Long>> {
    if (bubbleMetadata.isBlank()) return emptyList()

    val startTimes = "\"startTime\"\\s*:\\s*(\\d+)".toRegex()
        .findAll(bubbleMetadata)
        .mapNotNull { it.groupValues.getOrNull(1)?.toLongOrNull() }
        .toList()
    if (startTimes.isEmpty()) return emptyList()

    val endTimes = "\"endTime\"\\s*:\\s*(\\d+)".toRegex()
        .findAll(bubbleMetadata)
        .mapNotNull { it.groupValues.getOrNull(1)?.toLongOrNull() }
        .toList()

    return startTimes.mapIndexed { index, rawStart ->
        val start = rawStart.coerceIn(0L, durationMs)
        val fallbackEnd = (start + 1_200L).coerceAtMost(durationMs)
        val rawEnd = endTimes.getOrNull(index) ?: fallbackEnd
        val end = if (rawEnd <= start) {
            (start + 600L).coerceAtMost(durationMs)
        } else {
            rawEnd.coerceIn(start, durationMs)
        }
        start to end
    }
}

private fun findMaxOverlap(intervals: List<Pair<Long, Long>>): Long {
    if (intervals.isEmpty()) return 0L

    val events = mutableListOf<Pair<Long, Int>>()
    intervals.forEach { (start, end) ->
        events += start to 1
        events += (end + 1L) to -1
    }
    events.sortWith(compareBy<Pair<Long, Int>> { it.first }.thenByDescending { it.second })

    var current = 0L
    var maxOverlap = 0L
    for ((_, delta) in events) {
        current += delta
        if (current > maxOverlap) maxOverlap = current
    }
    return maxOverlap
}

private fun filterHighlightsForClips(videos: List<VideoClip>, filter: HighlightReelFilter): List<VideoClip> {
    val now = System.currentTimeMillis()
    val oneDayMillis = 24L * 60L * 60L * 1000L
    val oneWeekMillis = 7L * oneDayMillis
    val oneMonthMillis = 30L * oneDayMillis
    val oneSeasonMillis = 120L * oneDayMillis

    val filtered = when (filter) {
        HighlightReelFilter.ALL -> videos
        HighlightReelFilter.TODAY -> videos.filter { now - it.createdAt < oneDayMillis }
        HighlightReelFilter.THIS_WEEK -> videos.filter { now - it.createdAt < oneWeekMillis }
        HighlightReelFilter.THIS_MONTH -> videos.filter { now - it.createdAt < oneMonthMillis }
        HighlightReelFilter.THIS_SEASON -> videos.filter { now - it.createdAt < oneSeasonMillis }
    }

    // Highlight reels should play in timeline order from oldest to newest.
    return filtered.sortedBy { it.createdAt }
}

private const val HALL_OF_FAME_PREFS = "hall_of_fame"
private const val GOAT_SCHEMA_VERSION_KEY = "goat_schema_version"
private const val GOAT_SCHEMA_VERSION = 1
private const val GOAT_CLIPS_GLOBAL_KEY = "goat_clips_global"
private const val GOAT_REEL_FILTERS_GLOBAL_KEY = "goat_reel_filters_global"
private const val GOAT_REELS_GLOBAL_KEY = "goat_reels_global"

private fun parseSavedReelsJson(json: String?): List<SavedReel> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val clipIdsArr = obj.getJSONArray("clipIds")
            SavedReel(
                id = obj.getString("id"),
                name = obj.getString("name"),
                clipIds = (0 until clipIdsArr.length()).map { j -> clipIdsArr.getString(j) },
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun reelsToJson(reels: List<SavedReel>): String {
    val arr = org.json.JSONArray()
    reels.forEach { reel ->
        val obj = org.json.JSONObject().apply {
            put("id", reel.id)
            put("name", reel.name)
            put("clipIds", org.json.JSONArray().also { ids -> reel.clipIds.forEach { ids.put(it) } })
            put("createdAt", reel.createdAt)
        }
        arr.put(obj)
    }
    return arr.toString()
}

private fun migrateLegacyTeamScopedGoatDataIfNeeded(context: android.content.Context) {
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    if (prefs.getInt(GOAT_SCHEMA_VERSION_KEY, 0) >= GOAT_SCHEMA_VERSION) return

    val mergedClipIds = prefs.getStringSet(GOAT_CLIPS_GLOBAL_KEY, emptySet()).orEmpty().toMutableSet()
    val mergedReelFilters = prefs.getStringSet(GOAT_REEL_FILTERS_GLOBAL_KEY, emptySet()).orEmpty().toMutableSet()
    val mergedReelsById = linkedMapOf<String, SavedReel>()
    parseSavedReelsJson(prefs.getString(GOAT_REELS_GLOBAL_KEY, null)).forEach { mergedReelsById[it.id] = it }

    prefs.all.forEach { (key, value) ->
        when {
            key.startsWith("clips_") -> {
                (value as? Set<*>)
                    ?.mapNotNull { it as? String }
                    ?.forEach { mergedClipIds.add(it) }
            }
            key.startsWith("reels_") -> {
                (value as? Set<*>)
                    ?.mapNotNull { it as? String }
                    ?.forEach { mergedReelFilters.add(it) }
            }
            key.startsWith("saved_reels_") -> {
                parseSavedReelsJson(value as? String).forEach { mergedReelsById[it.id] = it }
            }
        }
    }

    prefs.edit()
        .putStringSet(GOAT_CLIPS_GLOBAL_KEY, mergedClipIds)
        .putStringSet(GOAT_REEL_FILTERS_GLOBAL_KEY, mergedReelFilters)
        .putString(GOAT_REELS_GLOBAL_KEY, reelsToJson(mergedReelsById.values.toList()))
        .putInt(GOAT_SCHEMA_VERSION_KEY, GOAT_SCHEMA_VERSION)
        .apply()
}

private fun hallOfFameClipKey(teamName: String): String = "clips_$teamName"

private fun hallOfFameReelKey(teamName: String): String = "reels_$teamName"

private fun loadHallOfFameClipIds(context: android.content.Context, teamName: String): Set<String> {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    return prefs.getStringSet(GOAT_CLIPS_GLOBAL_KEY, emptySet()).orEmpty().toSet()
}

private fun loadHallOfFameReelFilters(
    context: android.content.Context,
    teamName: String
): Set<HighlightReelFilter> {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    return prefs.getStringSet(GOAT_REEL_FILTERS_GLOBAL_KEY, emptySet())
        .orEmpty()
        .mapNotNull { name -> HighlightReelFilter.entries.firstOrNull { it.name == name } }
        .toSet()
}

private fun toggleHallOfFameClip(
    context: android.content.Context,
    teamName: String,
    clipId: String,
    existing: Set<String>
): Set<String> {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    val updated = if (existing.contains(clipId)) existing - clipId else existing + clipId
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    prefs.edit().putStringSet(GOAT_CLIPS_GLOBAL_KEY, updated).apply()
    return updated
}

private fun toggleHallOfFameReelClips(
    context: android.content.Context,
    teamName: String,
    clipIds: List<String>,
    existing: Set<String>
): Set<String> {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    val ids = clipIds.toSet()
    if (ids.isEmpty()) return existing

    val shouldRemove = ids.all { existing.contains(it) }
    val updated = if (shouldRemove) existing - ids else existing + ids
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    prefs.edit().putStringSet(GOAT_CLIPS_GLOBAL_KEY, updated).apply()
    return updated
}

private fun toggleHallOfFameReelFilter(
    context: android.content.Context,
    teamName: String,
    filter: HighlightReelFilter,
    existing: Set<HighlightReelFilter>
): Set<HighlightReelFilter> {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    val updated = if (existing.contains(filter)) existing - filter else existing + filter
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    prefs.edit().putStringSet(GOAT_REEL_FILTERS_GLOBAL_KEY, updated.map { it.name }.toSet()).apply()
    return updated
}

private fun removeHallOfFameReelFilter(
    context: android.content.Context,
    teamName: String,
    filter: HighlightReelFilter,
    existing: Set<HighlightReelFilter>
): Set<HighlightReelFilter> {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    val updated = existing - filter
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    prefs.edit().putStringSet(GOAT_REEL_FILTERS_GLOBAL_KEY, updated.map { it.name }.toSet()).apply()
    return updated
}

private fun hallOfFameReelTitle(filter: HighlightReelFilter): String {
    return if (filter == HighlightReelFilter.ALL) "All Reel" else "${filter.label} Reel"
}

private fun buildClipTitle(baseTitle: String, opponentName: String?, customName: String?): String {
    val trimmedCustom = customName?.trim().orEmpty()
    if (trimmedCustom.isNotEmpty()) return trimmedCustom

    val trimmedOpponent = opponentName?.trim().orEmpty()
    if (trimmedOpponent.isEmpty()) return baseTitle

    return if (baseTitle.contains("vs", ignoreCase = true)) {
        baseTitle
    } else {
        "$baseTitle vs $trimmedOpponent"
    }
}

private fun buildClipOpponentLookup(
    context: android.content.Context,
    videos: List<VideoClip>
): Map<String, String?> {
    val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
    return videos.associate { video ->
        val fileUri = android.net.Uri.parse(video.filePath)
        val filePath = fileUri.path
        video.id to (
            opponentPrefs.getString(video.id, null)
                ?: opponentPrefs.getString(video.filePath, null)
                ?: filePath?.let { opponentPrefs.getString(it, null) }
        )?.trim().takeIf { !it.isNullOrBlank() }
    }
}

private fun buildClipKidLookup(
    context: android.content.Context,
    videos: List<VideoClip>
): Map<String, String?> {
    val kidPrefs = context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE)
    return videos.associate { video ->
        val fileUri = android.net.Uri.parse(video.filePath)
        val filePath = fileUri.path
        video.id to (
            kidPrefs.getString(video.id, null)
                ?: kidPrefs.getString(video.filePath, null)
                ?: filePath?.let { kidPrefs.getString(it, null) }
        )?.trim().takeIf { !it.isNullOrBlank() }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
private fun buildClipGameSections(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    return videos
        .groupBy { video ->
            opponentLookup[video.id]?.takeIf(String::isNotBlank)?.lowercase() ?: NO_OPPONENT_FILTER
        }
        .map { (key, groupedVideos) ->
            val sample = groupedVideos.maxByOrNull { it.createdAt } ?: groupedVideos.first()
            val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)
            val displayTitle = opponent?.split(" ")?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) } ?: ""
            // Collect unique dates for subtitle
            val dates = groupedVideos.map { it.gameDate }.distinct().sorted()
            val dateLabel = dates.joinToString(", ") { formatClipFilterDate(it) }
            VideoLibrarySection(
                key = key,
                title = displayTitle,
                subtitle = dateLabel,
                videos = groupedVideos.sortedByDescending { it.createdAt }
            )
        }
        .sortedByDescending { section ->
            section.videos.maxOfOrNull { it.createdAt } ?: 0L
        }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun buildClipDateSubSections(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    val sample = videos.maxByOrNull { it.createdAt } ?: return emptyList()
    val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)

    // Keep unspecified as one section without date headers.
    if (opponent == null) {
        return listOf(
            VideoLibrarySection(
                key = NO_OPPONENT_FILTER,
                title = "",
                subtitle = "",
                videos = videos.sortedByDescending { it.createdAt }
            )
        )
    }

    val displayTitle = opponent.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    return videos
        .groupBy { it.gameDate }
        .map { (date, groupedVideos) ->
            VideoLibrarySection(
                key = "${opponent.lowercase()}|$date",
                title = displayTitle,
                subtitle = formatClipFilterDate(date),
                videos = groupedVideos.sortedByDescending { it.createdAt }
            )
        }
        .sortedByDescending { section ->
            section.videos.maxOfOrNull { it.createdAt } ?: 0L
        }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun buildClipListSectionsForAllGames(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    val groupedByOpponent = videos
        .groupBy { video ->
            opponentLookup[video.id]?.takeIf(String::isNotBlank)?.lowercase() ?: NO_OPPONENT_FILTER
        }
        .entries
        .sortedByDescending { (_, opponentVideos) ->
            opponentVideos.maxOfOrNull { it.createdAt } ?: 0L
        }

    return groupedByOpponent
        .flatMap { (opponentKey, opponentVideos) ->
            val sample = opponentVideos.maxByOrNull { it.createdAt } ?: opponentVideos.first()
            val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)

            if (opponent == null) {
                listOf(
                    VideoLibrarySection(
                        key = NO_OPPONENT_FILTER,
                        title = "",
                        subtitle = "",
                        videos = opponentVideos.sortedByDescending { it.createdAt }
                    )
                )
            } else {
                val displayTitle = opponent.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                opponentVideos
                    .groupBy { it.gameDate }
                    .map { (date, groupedVideos) ->
                        VideoLibrarySection(
                            key = "$opponentKey|$date",
                            title = displayTitle,
                            subtitle = formatClipFilterDate(date),
                            videos = groupedVideos.sortedByDescending { it.createdAt }
                        )
                    }
                    .sortedByDescending { section ->
                        section.videos.maxOfOrNull { it.createdAt } ?: 0L
                    }
            }
        }
}

private fun formatClipFilterDate(dateString: String): String {
    return try {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateString)
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(date ?: java.util.Date())
    } catch (_: Exception) {
        dateString
    }
}

private data class SeasonOption(
    val key: String,
    val label: String,
    val startYear: Int
)

private fun resolveVideoTeamName(context: android.content.Context, video: VideoClip): String? {
    val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
    val uri = android.net.Uri.parse(video.filePath)
    return (
        teamPrefs.getString(video.id, null)
            ?: teamPrefs.getString(video.filePath, null)
            ?: uri.path?.let { teamPrefs.getString(it, null) }
    )?.trim()?.takeIf { it.isNotBlank() }
}

private fun validateReelScopeForClipIds(
    context: android.content.Context,
    clipIds: List<String>,
    allVideos: List<VideoClip>
): String? {
    if (clipIds.isEmpty()) return "Select at least one clip for this reel"

    val selected = clipIds.mapNotNull { id -> allVideos.firstOrNull { it.id == id } }
    if (selected.isEmpty()) return "Unable to resolve selected clips"

    val teamNames = selected.mapNotNull { video ->
        resolveVideoTeamName(context, video)
    }.toSet()
    if (teamNames.size > 1) {
        return "A reel must stay within one team"
    }

    val seasonKeys = selected.mapNotNull { video ->
        parseSeasonKey(video.gameDate)?.key
    }.toSet()
    if (seasonKeys.size > 1) {
        return "A reel must stay within one season"
    }

    return null
}

private fun parseSeasonKey(gameDate: String): SeasonOption? {
    return try {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(gameDate) ?: return null
        val calendar = java.util.Calendar.getInstance().apply { time = parsed }
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val startYear = if (month >= 8) year else year - 1
        val endYear = startYear + 1
        val key = "$startYear-${endYear}"
        val label = "$startYear-${String.format(java.util.Locale.getDefault(), "%02d", endYear % 100)}"
        SeasonOption(key = key, label = label, startYear = startYear)
    } catch (_: Exception) {
        null
    }
}

// --- SavedReel persistence ---------------------------------------------------

private fun loadSavedGoatReels(
    context: android.content.Context,
    teamName: String
): List<SavedReel> {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    val prefs = context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
    return parseSavedReelsJson(prefs.getString(GOAT_REELS_GLOBAL_KEY, null))
}

private fun saveGoatReel(
    context: android.content.Context,
    teamName: String,
    reel: SavedReel,
    existing: List<SavedReel>
): List<SavedReel> {
    val updated = existing.filter { it.id != reel.id } + reel
    persistSavedGoatReels(context, teamName, updated)
    return updated
}

private fun deleteGoatReel(
    context: android.content.Context,
    teamName: String,
    reelId: String,
    existing: List<SavedReel>
): List<SavedReel> {
    val updated = existing.filter { it.id != reelId }
    persistSavedGoatReels(context, teamName, updated)
    return updated
}

private fun persistSavedGoatReels(
    context: android.content.Context,
    teamName: String,
    reels: List<SavedReel>
) {
    migrateLegacyTeamScopedGoatDataIfNeeded(context)
    context.getSharedPreferences(HALL_OF_FAME_PREFS, android.content.Context.MODE_PRIVATE)
        .edit().putString(GOAT_REELS_GLOBAL_KEY, reelsToJson(reels)).apply()
}

// --- Create Reel dialog -------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReelDialog(
    allVideos: List<VideoClip>,
    onDismiss: () -> Unit,
    onSave: (name: String, clipIds: List<String>) -> Unit,
    onCreateAndShare: (name: String, clipIds: List<String>) -> Unit
) {
    var reelName by remember { mutableStateOf("") }
    val selectedIds = remember(allVideos) { allVideos.map { it.id }.toSet() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Reel") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = reelName,
                    onValueChange = { reelName = it },
                    label = { Text("Reel name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Selected clips: ${selectedIds.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        val name = reelName.trim()
                        if (name.isNotBlank() && selectedIds.isNotEmpty()) {
                            onSave(name, selectedIds.toList())
                        }
                    },
                    enabled = reelName.isNotBlank() && selectedIds.isNotEmpty()
                ) { Text("Create") }
                Button(
                    onClick = {
                        val name = reelName.trim()
                        if (name.isNotBlank() && selectedIds.isNotEmpty()) {
                            onCreateAndShare(name, selectedIds.toList())
                        }
                    },
                    enabled = reelName.isNotBlank() && selectedIds.isNotEmpty()
                ) { Text("Create & Share") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}



