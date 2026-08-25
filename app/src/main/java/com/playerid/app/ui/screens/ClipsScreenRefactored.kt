@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.playerid.app.ui.screens

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.playerid.app.data.VideoClip
import com.playerid.app.reelVideoRelativePathsForRestoredClips
import com.playerid.app.ui.icons.CrossedLacrosseSticksIcon
import com.playerid.app.ui.icons.GroundBallIcon
import com.playerid.app.ui.icons.StopSignIcon
import com.playerid.app.viewmodels.TeamViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private const val NO_OPPONENT_FILTER_RESTORED = "No opponent specified"
private val PlaysAccentColor = com.playerid.app.ui.theme.SpotrHighlightOrange
private val OpponentGroupColor = Color(0xFF3676B8)
private val RestoredSwipeActionWidth = 118.dp
private val PrimaryClipsControlHeight = 48.dp
private val DefaultMomentTags = listOf(
    "Score",
    "Assist",
    "Save",
    "Defensive Stop",
    "Faceoff Win",
    "Ground Ball"
)
private const val CLIP_COMMENTARY_PREFS = "video_clip_commentary"
private const val CLIP_CUSTOM_TAGS_PREFS = "video_clip_custom_tags"
private const val REEL_BUILD_TIMEOUT_MS = 180_000L
private const val REEL_OUTPUT_WIDTH = 720
private const val REEL_OUTPUT_HEIGHT = 1280
private const val REEL_OUTPUT_FPS = 24
private const val REEL_INTRO_SECONDS = "1.6"
private const val REEL_COLLECTION_PREFS = "reel_filter_collections"
private const val REEL_METADATA_PREFS = "reel_metadata_v1"

private data class ReelFilterCollection(
    val name: String,
    val kid: String?,
    val seasonKey: String?,
    val opponentKey: String?,
    val reelsOnly: Boolean
)

private data class OpponentFilterOption(
    val key: String,
    val label: String
)

private fun isReelTaggedClip(clip: VideoClip): Boolean {
    return clip.momentTag?.startsWith("Reel:", ignoreCase = true) == true ||
        clip.gameTitle.startsWith("Spotr-Reel", ignoreCase = true) ||
        clip.gameTitle.startsWith("Reel:", ignoreCase = true)
}

private fun reelDisplayTitleForClip(clip: VideoClip): String {
    val raw = (clip.momentTag?.takeIf { it.isNotBlank() } ?: clip.gameTitle).trim()
    return if (raw.startsWith("Reel:", ignoreCase = true)) raw.substring(5).trim() else raw
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreenRefactored(
    teamViewModel: TeamViewModel,
    cameraHandoffToken: Int = 0,
    onNavigateToTeams: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cameraTeam by teamViewModel.selectedTeam.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val scope = rememberCoroutineScope()

    var localTeamName by remember { mutableStateOf<String?>(null) }
    var clips by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAllTeams by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTeamFilterMenu by remember { mutableStateOf(false) }
    var showPlayerFilterMenu by remember { mutableStateOf(false) }
    var showSeasonFilterMenu by remember { mutableStateOf(false) }
    var showOpponentFilterMenu by remember { mutableStateOf(false) }
    var selectedKid by remember(localTeamName) { mutableStateOf<String?>(null) }
    var selectedSeasonKey by remember(localTeamName) { mutableStateOf<String?>(null) }
    var selectedOpponentKey by remember(localTeamName) { mutableStateOf<String?>(null) }
    var showReelsOnly by remember(localTeamName) { mutableStateOf(false) }
    var useGridView by rememberSaveable { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var editingClip by remember { mutableStateOf<VideoClip?>(null) }
    var editingClipAllowsTrim by remember { mutableStateOf(true) }
    var isTrimmingClip by remember { mutableStateOf(false) }
    var editingReelClip by remember { mutableStateOf<VideoClip?>(null) }
    var isRebuildingReel by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoClip?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isBuildingReel by remember { mutableStateOf(false) }
    var showReelPickerDialog by remember { mutableStateOf(false) }
    var completedReelUri by remember { mutableStateOf<Uri?>(null) }
    var reelSelectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reelNameInput by remember { mutableStateOf("") }
    var draftReelIds by remember { mutableStateOf<List<String>?>(null) }
    var reelEditorSelectionIdsOverride by remember { mutableStateOf<List<String>?>(null) }
    var reelSelectionEditTarget by remember { mutableStateOf<VideoClip?>(null) }
    val toggleReelClipSelection: (String) -> Unit = { clipId ->
        reelSelectionIds = if (reelSelectionIds.contains(clipId)) {
            reelSelectionIds - clipId
        } else {
            reelSelectionIds + clipId
        }
    }

    LaunchedEffect(cameraHandoffToken) {
        localTeamName = cameraTeam
        selectedKid = cameraTeam?.let { teamViewModel.getSelectedKidForTeam(it) }
    }

    LaunchedEffect(subscribedTeams, cameraTeam) {
        if (localTeamName.isNullOrBlank()) {
            localTeamName = cameraTeam ?: subscribedTeams.firstOrNull()?.name
            if (!localTeamName.isNullOrBlank()) {
                selectedKid = teamViewModel.getSelectedKidForTeam(localTeamName!!)
            }
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val selectedTeamName = localTeamName
        if (uri != null && !selectedTeamName.isNullOrBlank()) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            val uriString = uri.toString()
            context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(uriString, selectedTeamName)
                .apply()
            context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
                .edit()
                .putLong(uriString, System.currentTimeMillis())
                .apply()
            context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(uriString, selectedKid ?: teamViewModel.getSelectedKidForTeam(selectedTeamName))
                .apply()

            scope.launch {
                isLoading = true
                clips = loadRestoredClips(context, selectedTeamName)
                isLoading = false
            }

            Toast.makeText(context, "Play uploaded", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(localTeamName, showAllTeams, subscribedTeams) {
        if (showAllTeams) {
            isLoading = true
            clips = subscribedTeams
                .flatMap { team -> loadRestoredClips(context, team.name) }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            isLoading = false
            return@LaunchedEffect
        }
        val teamName = localTeamName
        if (teamName.isNullOrBlank()) {
            clips = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        clips = loadRestoredClips(context, teamName)
        selectedSeasonKey = null
        selectedOpponentKey = null
        showReelsOnly = false
        if (selectedKid == null) {
            selectedKid = teamViewModel.getSelectedKidForTeam(teamName)
        }
        isLoading = false
    }

    val opponentLookup = remember(clips) {
        buildRestoredClipOpponentLookup(context, clips)
    }
    val kidLookup = remember(clips) {
        buildRestoredClipKidLookup(context, clips)
    }
    val clipsById = remember(clips) { clips.associateBy { it.id } }
    val reelIncludedClipIds = remember(clips) {
        clips.filter(::isReelTaggedClip).associate { reelClip ->
            val ids = resolveClipUriForReel(reelClip.filePath)?.let { uri -> loadReelClipIds(context, uri) }.orEmpty()
            reelClip.id to ids
        }
    }
    val kidFilter = selectedKid?.trim().orEmpty()
    fun opponentKeyOfId(id: String): String =
        opponentLookup[id]?.trim()?.takeIf(String::isNotEmpty)?.lowercase() ?: NO_OPPONENT_FILTER_RESTORED
    fun opponentKeyOf(clip: VideoClip): String = opponentKeyOfId(clip.id)
    // Reels don't carry their own kid/season/opponent tags, so a reel matches a facet if ANY of its
    // included source clips does (e.g. selecting "Brooklyn" surfaces every reel containing her clips).
    fun clipMatchesKid(clip: VideoClip): Boolean {
        if (kidFilter.isEmpty()) return true
        if (isReelTaggedClip(clip)) {
            return reelIncludedClipIds[clip.id].orEmpty().any { id -> kidLookup[id]?.equals(kidFilter, ignoreCase = true) == true }
        }
        return kidLookup[clip.id]?.equals(kidFilter, ignoreCase = true) == true
    }
    fun clipMatchesSeason(clip: VideoClip): Boolean {
        if (selectedSeasonKey == null) return true
        if (isReelTaggedClip(clip)) {
            return reelIncludedClipIds[clip.id].orEmpty().any { id ->
                clipsById[id]?.gameDate?.let { parseRestoredSeasonKey(it)?.key == selectedSeasonKey } == true
            }
        }
        return parseRestoredSeasonKey(clip.gameDate)?.key == selectedSeasonKey
    }
    fun clipMatchesOpponent(clip: VideoClip): Boolean {
        if (selectedOpponentKey == null) return true
        if (isReelTaggedClip(clip)) {
            return reelIncludedClipIds[clip.id].orEmpty().any { id -> opponentKeyOfId(id) == selectedOpponentKey }
        }
        return opponentKeyOf(clip) == selectedOpponentKey
    }

    // Each facet's options only reflect clips that satisfy the OTHER active facets, so unavailable
    // combinations (e.g. a player with no clips against the currently selected opponent) drop out.
    val availableKidOptions = remember(clips, kidLookup, selectedSeasonKey, selectedOpponentKey, opponentLookup) {
        clips
            .filterNot(::isReelTaggedClip)
            .filter { clipMatchesSeason(it) && clipMatchesOpponent(it) }
            .mapNotNull { kidLookup[it.id]?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val availableSeasons = remember(clips, kidLookup, kidFilter, selectedOpponentKey, opponentLookup) {
        clips
            .filterNot(::isReelTaggedClip)
            .filter { clipMatchesKid(it) && clipMatchesOpponent(it) }
            .mapNotNull { parseRestoredSeasonKey(it.gameDate) }
            .distinctBy { it.key }
            .sortedByDescending { it.startYear }
    }
    val availableOpponents = remember(clips, kidLookup, kidFilter, selectedSeasonKey, opponentLookup) {
        clips
            .filterNot(::isReelTaggedClip)
            .filter { clipMatchesKid(it) && clipMatchesSeason(it) }
            .groupBy { clip -> opponentKeyOf(clip) }
            .map { (key, videosForOpponent) ->
                val label = videosForOpponent
                    .firstNotNullOfOrNull { clip -> opponentLookup[clip.id]?.trim()?.takeIf(String::isNotEmpty) }
                    ?.split(" ")
                    ?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                    ?: NO_OPPONENT_FILTER_RESTORED
                OpponentFilterOption(key = key, label = label)
            }
            .sortedBy { it.label.lowercase() }
    }
    val opponentScopedVideos = remember(clips, kidLookup, kidFilter, selectedSeasonKey, selectedOpponentKey, opponentLookup, reelIncludedClipIds, clipsById) {
        clips.filter { clipMatchesKid(it) && clipMatchesSeason(it) && clipMatchesOpponent(it) }
    }
    val reelScopedVideos = remember(clips, opponentScopedVideos, showReelsOnly, showReelPickerDialog) {
        when {
            showReelsOnly -> opponentScopedVideos.filter(::isReelTaggedClip)
            else -> opponentScopedVideos.filterNot(::isReelTaggedClip)
        }
    }
    val searchedVideos = remember(reelScopedVideos, searchQuery, kidLookup, opponentLookup) {
        filterRestoredClipsBySearchQuery(
            videos = reelScopedVideos,
            query = searchQuery,
            kidLookup = kidLookup,
            opponentLookup = opponentLookup
        )
    }
    // Fraction of a reel's included clips that satisfy the active filters, so a reel that's entirely
    // one opponent/player/season ranks above one with only a single matching clip.
    fun reelMatchScore(clip: VideoClip): Double {
        val includedIds = reelIncludedClipIds[clip.id].orEmpty()
        if (includedIds.isEmpty()) return 0.0
        val matchingCount = includedIds.count { id ->
            val matchesKid = kidFilter.isEmpty() || kidLookup[id]?.equals(kidFilter, ignoreCase = true) == true
            val matchesSeason = selectedSeasonKey == null ||
                clipsById[id]?.gameDate?.let { parseRestoredSeasonKey(it)?.key == selectedSeasonKey } == true
            val matchesOpponent = selectedOpponentKey == null || opponentKeyOfId(id) == selectedOpponentKey
            matchesKid && matchesSeason && matchesOpponent
        }
        return matchingCount.toDouble() / includedIds.size.toDouble()
    }
    val visibleSections = remember(
        searchedVideos,
        opponentLookup,
        showReelsOnly,
        kidFilter,
        selectedSeasonKey,
        selectedOpponentKey,
        reelIncludedClipIds,
        clipsById,
        kidLookup
    ) {
        val reelFiltersActive = showReelsOnly && (kidFilter.isNotEmpty() || selectedSeasonKey != null || selectedOpponentKey != null)
        buildRestoredClipListSectionsForAllGames(
            searchedVideos,
            opponentLookup,
            groupByOpponent = !showReelsOnly,
            reelSortOverride = if (reelFiltersActive) { videos ->
                videos.sortedWith(compareByDescending<VideoClip> { reelMatchScore(it) }.thenByDescending { it.createdAt })
            } else null
        )
    }
    val visibleVideos = remember(visibleSections) {
        visibleSections.flatMap { it.videos }
    }
    LaunchedEffect(availableKidOptions, selectedKid) {
        if (selectedKid != null && availableKidOptions.none { it.equals(selectedKid, ignoreCase = true) }) {
            selectedKid = null
        }
    }
    LaunchedEffect(availableSeasons, selectedSeasonKey) {
        if (selectedSeasonKey != null && availableSeasons.none { it.key == selectedSeasonKey }) {
            selectedSeasonKey = null
        }
    }
    LaunchedEffect(availableOpponents, selectedOpponentKey) {
        if (selectedOpponentKey != null && availableOpponents.none { it.key == selectedOpponentKey }) {
            selectedOpponentKey = null
        }
    }

    selectedVideoUri?.let { videoUri ->
        val matchingReelClip = clips.firstOrNull { clip ->
            isReelTaggedClip(clip) && resolveClipUriForReel(clip.filePath) == videoUri
        }
        VideoPlaybackScreen(
            videoUri = videoUri,
            detectedPlayers = emptyList(),
            onNavigateBack = { selectedVideoUri = null },
            onEditReel = matchingReelClip?.let { reelClip ->
                {
                    selectedVideoUri = null
                    editingReelClip = reelClip
                }
            }
        )
        return
    }

    val reloadClipsForCurrentScope: suspend () -> Unit = {
        if (showAllTeams) {
            clips = subscribedTeams
                .flatMap { team -> loadRestoredClips(context, team.name) }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
        } else {
            localTeamName?.let { teamName -> clips = loadRestoredClips(context, teamName) }
        }
    }

    editingClip?.let { pending ->
        val target = clips.firstOrNull { it.id == pending.id } ?: pending
        ClipEditorScreen(
            clip = target,
            isTrimming = isTrimmingClip,
            allowTrim = editingClipAllowsTrim,
            onClose = { editingClip = null },
            onSaveTrim = { startMs, endMs ->
                scope.launch {
                    isTrimmingClip = true
                    try {
                        Toast.makeText(context, "Trimming clip...", Toast.LENGTH_SHORT).show()
                        val trimmedUri = trimClipToNewVideo(context, target, startMs, endMs)
                        if (trimmedUri != null) {
                            copyClipMetadataForTrim(context, target, trimmedUri, localTeamName)
                            reloadClipsForCurrentScope()
                            Toast.makeText(context, "Trimmed clip saved", Toast.LENGTH_SHORT).show()
                            editingClip = null
                        } else {
                            Toast.makeText(context, "Could not trim this clip", Toast.LENGTH_SHORT).show()
                        }
                    } catch (t: Throwable) {
                        Log.e("ClipsScreenRefactored", "Saving trimmed clip failed", t)
                        Toast.makeText(context, "Could not save trimmed clip", Toast.LENGTH_SHORT).show()
                    } finally {
                        isTrimmingClip = false
                    }
                }
            }
        )
        return
    }

    draftReelIds?.let { selectedIds ->
        val availableClipsForReel = clips.filterNot(::isReelTaggedClip)
        val defaultTitle = reelNameInput.trim().ifBlank {
            localTeamName?.let { "$it Highlight Reel" } ?: "Highlight Reel"
        }
        EditReelScreen(
            reelClip = null,
            initialSelectedIds = selectedIds,
            initialTitle = defaultTitle,
            availableClips = availableClipsForReel,
            isSaving = isBuildingReel,
            onAddClips = { orderedIds, title ->
                reelEditorSelectionIdsOverride = orderedIds
                reelSelectionIds = orderedIds.toSet()
                reelNameInput = title
                draftReelIds = null
                showReelPickerDialog = true
            },
            onClose = {
                draftReelIds = null
                showReelPickerDialog = true
            },
            onSave = { orderedIds, newTitle ->
                val selectedTeamName = localTeamName
                if (selectedTeamName.isNullOrBlank()) {
                    Toast.makeText(context, "Select a team first", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        isBuildingReel = true
                        try {
                            val orderedClips = orderedIds.mapNotNull { id -> clips.firstOrNull { it.id == id } }
                            val clipUris = orderedClips.mapNotNull { resolveClipUriForReel(it.filePath) }
                            if (clipUris.size != orderedClips.size || clipUris.isEmpty()) {
                                Toast.makeText(context, "Some selected clips are unavailable", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val reelUri = buildShareableReelForRefactored(
                                context = context,
                                clipUris = clipUris,
                                reelTitle = newTitle,
                                teamName = selectedTeamName,
                                opponentName = null
                            )
                            if (reelUri != null) {
                                persistReelClipSummary(context, reelUri, orderedClips)
                                persistReelClipIds(context, reelUri, orderedIds)
                                reloadClipsForCurrentScope()
                                completedReelUri = reelUri
                                draftReelIds = null
                                reelSelectionIds = emptySet()
                                Toast.makeText(context, "Reel saved to Movies/Spotr", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Unable to build reel video", Toast.LENGTH_SHORT).show()
                            }
                        } catch (t: Throwable) {
                            Log.e("ClipsScreenRefactored", "Make Reel failed", t)
                            Toast.makeText(context, "Reel creation failed", Toast.LENGTH_SHORT).show()
                        } finally {
                            isBuildingReel = false
                        }
                    }
                }
            }
        )
        return
    }

    editingReelClip?.let { reelTarget ->
        val availableClipsForReel = clips.filterNot(::isReelTaggedClip)
        val initialSelectedIds = remember(reelTarget.id, reelTarget.filePath) {
            reelEditorSelectionIdsOverride
                ?: resolveClipUriForReel(reelTarget.filePath)?.let { uri -> loadReelClipIds(context, uri) }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(reelTarget.id)
        }
        EditReelScreen(
            reelClip = reelTarget,
            initialSelectedIds = initialSelectedIds,
            initialTitle = reelNameInput.takeIf { reelEditorSelectionIdsOverride != null && it.isNotBlank() }
                ?: reelDisplayTitleForClip(reelTarget),
            availableClips = availableClipsForReel,
            isSaving = isRebuildingReel,
            onAddClips = { orderedIds, title ->
                reelEditorSelectionIdsOverride = orderedIds
                reelSelectionIds = orderedIds.toSet()
                reelNameInput = title
                reelSelectionEditTarget = reelTarget
                editingReelClip = null
                showReelsOnly = false
                showReelPickerDialog = true
            },
            onClose = { editingReelClip = null },
            onSave = { orderedIds, newTitle ->
                scope.launch {
                    isRebuildingReel = true
                    try {
                        val orderedClips = orderedIds.mapNotNull { id -> clips.firstOrNull { it.id == id } }
                        val clipUris = orderedClips.mapNotNull { resolveClipUriForReel(it.filePath) }
                        if (clipUris.isEmpty()) {
                            Toast.makeText(context, "Select at least one clip", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val newReelUri = buildShareableReelForRefactored(
                            context = context,
                            clipUris = clipUris,
                            reelTitle = newTitle,
                            teamName = localTeamName,
                            opponentName = null
                        )
                        if (newReelUri != null) {
                            persistReelClipSummary(context, newReelUri, orderedClips)
                            persistReelClipIds(context, newReelUri, orderedIds)
                            resolveClipUriForReel(reelTarget.filePath)?.let { oldUri ->
                                runCatching { context.contentResolver.delete(oldUri, null, null) }
                                context.getSharedPreferences(CLIP_COMMENTARY_PREFS, android.content.Context.MODE_PRIVATE).edit().remove(oldUri.toString()).apply()
                            }
                            reloadClipsForCurrentScope()
                            Toast.makeText(context, "Reel updated", Toast.LENGTH_SHORT).show()
                            reelEditorSelectionIdsOverride = null
                            editingReelClip = null
                        } else {
                            Toast.makeText(context, "Unable to update reel", Toast.LENGTH_SHORT).show()
                        }
                    } catch (t: Throwable) {
                        Log.e("ClipsScreenRefactored", "Reel update failed", t)
                        Toast.makeText(context, "Unable to update reel", Toast.LENGTH_SHORT).show()
                    } finally {
                        isRebuildingReel = false
                    }
                }
            }
        )
        return
    }

    if (subscribedTeams.isEmpty()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(48.dp))
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

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(PrimaryClipsControlHeight),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Find a memory",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .width(PrimaryClipsControlHeight)
                        .height(PrimaryClipsControlHeight),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { useGridView = !useGridView }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (useGridView) {
                                Icons.AutoMirrored.Filled.ViewList
                            } else {
                                Icons.Default.GridView
                            },
                            contentDescription = if (useGridView) "List view" else "Grid view",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (!showReelPickerDialog) {
                    Surface(
                        modifier = Modifier.height(PrimaryClipsControlHeight),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { uploadLauncher.launch(arrayOf("video/*")) }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(PrimaryClipsControlHeight)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Import clip from phone",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    FilterMenuLabel(
                        label = selectedKid ?: "Player",
                        isActive = selectedKid != null,
                        onClick = { showPlayerFilterMenu = true }
                    )
                    DropdownMenu(expanded = showPlayerFilterMenu, onDismissRequest = { showPlayerFilterMenu = false }) {
                        DropdownMenuItem(text = { Text("All players") }, onClick = { selectedKid = null; showPlayerFilterMenu = false })
                        availableKidOptions.forEach { kidName ->
                            DropdownMenuItem(
                                text = { Text(kidName) },
                                onClick = {
                                    selectedKid = kidName
                                    localTeamName?.let { teamViewModel.selectKidForTeam(it, kidName) }
                                    showPlayerFilterMenu = false
                                }
                            )
                        }
                    }
                }
                Box {
                    FilterMenuLabel(
                        label = availableSeasons.firstOrNull { it.key == selectedSeasonKey }?.label ?: "Season",
                        isActive = selectedSeasonKey != null,
                        onClick = { showSeasonFilterMenu = true }
                    )
                    DropdownMenu(expanded = showSeasonFilterMenu, onDismissRequest = { showSeasonFilterMenu = false }) {
                        DropdownMenuItem(text = { Text("All seasons") }, onClick = { selectedSeasonKey = null; showSeasonFilterMenu = false })
                        availableSeasons.forEach { season ->
                            DropdownMenuItem(
                                text = { Text(season.label) },
                                onClick = { selectedSeasonKey = season.key; showSeasonFilterMenu = false }
                            )
                        }
                    }
                }
                if (subscribedTeams.size > 1) {
                    Box {
                        FilterMenuLabel(
                            label = if (showAllTeams) "All Clubs" else localTeamName ?: "Club",
                            isActive = !showAllTeams,
                            onClick = { showTeamFilterMenu = true }
                        )
                        DropdownMenu(expanded = showTeamFilterMenu, onDismissRequest = { showTeamFilterMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("All clubs") },
                                onClick = {
                                    showAllTeams = true
                                    showTeamFilterMenu = false
                                }
                            )
                            subscribedTeams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text(team.name) },
                                    onClick = {
                                        localTeamName = team.name
                                        showAllTeams = false
                                        selectedKid = teamViewModel.getSelectedKidForTeam(team.name)
                                        showTeamFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                Box {
                    FilterMenuLabel(
                        label = availableOpponents.firstOrNull { it.key == selectedOpponentKey }?.label ?: "Opponent",
                        isActive = selectedOpponentKey != null,
                        onClick = { showOpponentFilterMenu = true }
                    )
                    DropdownMenu(expanded = showOpponentFilterMenu, onDismissRequest = { showOpponentFilterMenu = false }) {
                        DropdownMenuItem(text = { Text("All opponents") }, onClick = { selectedOpponentKey = null; showOpponentFilterMenu = false })
                        availableOpponents.forEach { opponent ->
                            DropdownMenuItem(
                                text = { Text(opponent.label) },
                                onClick = { selectedOpponentKey = opponent.key; showOpponentFilterMenu = false }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                        .padding(top = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!showReelPickerDialog) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClipsFileTab(
                            label = "Clips",
                            highlighted = !showReelsOnly,
                            underlined = !showReelsOnly && !showReelPickerDialog,
                            enabled = !isBuildingReel && !showReelPickerDialog,
                            onClick = {
                                showReelsOnly = false
                                showReelPickerDialog = false
                            }
                        )
                        ClipsFileTab(
                            label = "Reels",
                            highlighted = showReelsOnly && !showReelPickerDialog,
                            underlined = showReelsOnly && !showReelPickerDialog,
                            enabled = !isBuildingReel && !showReelPickerDialog,
                            onClick = {
                                showReelsOnly = true
                                showReelPickerDialog = false
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(124.dp)
                        .height(PrimaryClipsControlHeight)
                        .clickable(enabled = !showReelPickerDialog) {
                        when {
                            opponentScopedVideos.none { !isReelTaggedClip(it) } -> Toast.makeText(
                                context,
                                "No clips available to build a reel",
                                Toast.LENGTH_SHORT
                            ).show()
                            else -> {
                                reelSelectionIds = emptySet()
                                reelSelectionEditTarget = null
                                reelEditorSelectionIdsOverride = null
                                reelNameInput = buildCollectionName(
                                    selectedKid = selectedKid,
                                    selectedSeasonKey = selectedSeasonKey,
                                    selectedOpponentKey = selectedOpponentKey,
                                    reelsOnly = false
                                )
                                showReelsOnly = false
                                useGridView = true
                                showReelPickerDialog = true
                            }
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Create Reel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

        }

        if (showReelPickerDialog) {
            val selectedClips = (visibleVideos + clips)
                .distinctBy { it.id }
                .filter { it.id in reelSelectionIds }
            val selectedDurationMs = selectedClips.sumOf { it.duration }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select clips",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedClips.size} clip${if (selectedClips.size == 1) "" else "s"} selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total ${formatRestoredDuration(selectedDurationMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            showReelPickerDialog = false
                            reelSelectionIds = emptySet()
                            reelSelectionEditTarget = null
                            reelEditorSelectionIdsOverride = null
                            draftReelIds = null
                        }
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (selectedClips.isNotEmpty()) {
                                val selectedIdSet = selectedClips.mapTo(linkedSetOf()) { it.id }
                                val orderedSelectedIds = reelEditorSelectionIdsOverride
                                    .orEmpty()
                                    .filter { it in selectedIdSet } +
                                    selectedClips.map { it.id }.filterNot { it in reelEditorSelectionIdsOverride.orEmpty() }
                                reelEditorSelectionIdsOverride = orderedSelectedIds
                                val editTarget = reelSelectionEditTarget
                                if (editTarget != null) {
                                    reelSelectionEditTarget = null
                                    editingReelClip = editTarget
                                } else {
                                    draftReelIds = orderedSelectedIds
                                }
                                showReelPickerDialog = false
                            }
                        },
                        enabled = selectedClips.isNotEmpty()
                    ) {
                        Text("Next")
                    }
                }
            }
        }

        completedReelUri?.let { finishedReelUri ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { completedReelUri = null },
                title = { Text("Reel ready") },
                text = { Text("Saved to Movies/Spotr. Watch it now or share it with your team.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            completedReelUri = null
                            selectedVideoUri = finishedReelUri
                        }
                    ) {
                        Text("View")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            completedReelUri = null
                            launchPersonalShareChooser(
                                context = context,
                                videoUri = finishedReelUri,
                                shareTitle = "Share highlight reel"
                            )
                        }
                    ) {
                        Text("Share")
                    }
                }
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
            )
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading clips...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            localTeamName.isNullOrBlank() && !showAllTeams -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a team to view clips", style = MaterialTheme.typography.bodyMedium)
                }
            }
            visibleVideos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No videos yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            useGridView -> {
                val gridColumnCount = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumnCount),
                    modifier = Modifier
                        .then(
                            if (showReelPickerDialog) Modifier.weight(1f) else Modifier.fillMaxSize()
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(visibleVideos, key = { it.id }) { clip ->
                        RestoredClipGridTile(
                            clip = clip,
                            reelMode = showReelPickerDialog,
                            isSelectedForReel = reelSelectionIds.contains(clip.id),
                            onToggleReelSelection = { toggleReelClipSelection(clip.id) },
                            onPlay = { selectedVideoUri = Uri.parse(clip.filePath) },
                            onToggleHighlight = {
                                val newStatus = !clip.isHighlight
                                context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(clip.id, newStatus)
                                    .apply()
                                clips = clips.map { current ->
                                    if (current.id == clip.id) current.copy(isHighlight = newStatus) else current
                                }
                            }
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .then(
                            if (showReelPickerDialog) Modifier.weight(1f) else Modifier.fillMaxSize()
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    visibleSections.forEach { section ->
                        item(key = "header_${section.key}") {
                            if (section.title.isNotBlank() || section.subtitle.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OpponentGroupColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(OpponentGroupColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        if (section.title.isNotBlank()) {
                                            Text(
                                                text = "vs ${section.title}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        val clipCountLabel = if (section.videos.size == 1) {
                                            "1 clip"
                                        } else {
                                            "${section.videos.size} clips"
                                        }
                                        Text(
                                            text = if (section.subtitle.isNotBlank()) {
                                                "${section.subtitle}  •  $clipCountLabel"
                                            } else {
                                                clipCountLabel
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        items(section.videos, key = { it.id }) { clip ->
                            val reelModeEnabled = showReelPickerDialog
                            Box(modifier = Modifier.fillMaxWidth()) {
                                RestoredClipRow(
                                    clip = clip,
                                    reelMode = reelModeEnabled,
                                    isSelectedForReel = reelSelectionIds.contains(clip.id),
                                    onToggleReelSelection = { toggleReelClipSelection(clip.id) },
                                    onEdit = { editingClipAllowsTrim = true; editingClip = clip },
                                    onExpand = { editingClipAllowsTrim = false; editingClip = clip },
                                    onShare = {
                                        launchPersonalShareChooser(
                                            context = context,
                                            videoUri = Uri.parse(clip.filePath),
                                            shareTitle = "Share this play"
                                        )
                                    },
                                    onDelete = {
                                        videoToDelete = clip
                                        showDeleteDialog = true
                                    },
                                    onToggleHighlight = {
                                        val newStatus = !clip.isHighlight
                                        val prefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
                                        prefs.edit().putBoolean(clip.id, newStatus).apply()
                                        clips = clips.map { current ->
                                            if (current.id == clip.id) current.copy(isHighlight = newStatus) else current
                                        }
                                    },
                                    onTagChanged = { newTag ->
                                        val prefs = context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
                                        if (newTag == null) {
                                            prefs.edit().remove(clip.id).remove(clip.filePath).apply()
                                        } else {
                                            prefs.edit().putString(clip.id, newTag).putString(clip.filePath, newTag).apply()
                                        }
                                        clips = clips.map { current ->
                                            if (current.id == clip.id) current.copy(momentTag = newTag) else current
                                        }
                                    },
                                    onTitleChanged = { newTitle ->
                                        updateReelTitle(context, clip, newTitle)
                                        localTeamName?.let { teamName ->
                                            scope.launch { clips = loadRestoredClips(context, teamName) }
                                        }
                                    },
                                    onEditReel = { editingReelClip = clip }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog && videoToDelete != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete clip?") },
                text = { Text("This clip will be permanently deleted from your phone.") },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val target = videoToDelete
                            showDeleteDialog = false
                            if (target != null) {
                                scope.launch {
                                    deleteRestoredClip(context, target)
                                    localTeamName?.let { teamName ->
                                        clips = loadRestoredClips(context, teamName)
                                    }
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

@Composable
private fun RestoredClipGridTile(
    clip: VideoClip,
    reelMode: Boolean,
    isSelectedForReel: Boolean,
    onToggleReelSelection: () -> Unit,
    onPlay: () -> Unit,
    onToggleHighlight: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val previewBitmap by produceState<Bitmap?>(initialValue = null, clip.id, clip.filePath) {
        value = withContext(Dispatchers.IO) {
            loadRestoredClipThumbnail(context, clip.filePath)
        }
    }
    val isReel = remember(clip.momentTag, clip.gameTitle) { isReelTaggedClip(clip) }
    val label = if (isReel) {
        reelDisplayTitleForClip(clip)
    } else {
        clip.momentTag?.takeIf { it.isNotBlank() } ?: "Clip"
    }
    val labelColor = if (isReel) {
        Color(0xFF2B2F33)
    } else {
        momentTagConfig(clip.momentTag).second
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clickable(enabled = reelMode) { onToggleReelSelection() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .widthIn(max = 96.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(labelColor.copy(alpha = 0.88f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )

            IconButton(
                onClick = onToggleHighlight,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = if (clip.isHighlight) "Remove favorite" else "Add favorite",
                    tint = if (clip.isHighlight) PlaysAccentColor else Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Surface(
                onClick = onPlay,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.58f),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play full screen",
                    modifier = Modifier.padding(8.dp)
                )
            }

            Text(
                text = formatRestoredDuration(clip.duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )

            if (reelMode) {
                Surface(
                    onClick = onToggleReelSelection,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp),
                    shape = CircleShape,
                    color = if (isSelectedForReel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                    contentColor = if (isSelectedForReel) Color.White else MaterialTheme.colorScheme.outline
                ) {
                    if (isSelectedForReel) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected for reel",
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun loadClipCommentary(context: android.content.Context, clip: VideoClip): String {
    val prefs = context.getSharedPreferences(CLIP_COMMENTARY_PREFS, android.content.Context.MODE_PRIVATE)
    return prefs.getString(clip.id, null)
        ?: prefs.getString(clip.filePath, null)
        ?: ""
}

private fun persistClipCommentary(context: android.content.Context, clip: VideoClip, commentary: String) {
    val prefs = context.getSharedPreferences(CLIP_COMMENTARY_PREFS, android.content.Context.MODE_PRIVATE)
    val trimmed = commentary.trim()
    val editor = prefs.edit()
    if (trimmed.isEmpty()) {
        editor.remove(clip.id)
        editor.remove(clip.filePath)
    } else {
        editor.putString(clip.id, trimmed)
        editor.putString(clip.filePath, trimmed)
    }
    editor.apply()
}

private fun loadClipCustomTags(context: android.content.Context, clip: VideoClip): List<String> {
    val prefs = context.getSharedPreferences(CLIP_CUSTOM_TAGS_PREFS, android.content.Context.MODE_PRIVATE)
    val raw = prefs.getString(clip.id, null) ?: prefs.getString(clip.filePath, null) ?: return emptyList()
    return runCatching {
        val stored = JSONArray(raw)
        (0 until stored.length()).mapNotNull { index ->
            stored.optString(index).trim().takeIf { it.isNotEmpty() }
        }
    }.getOrDefault(emptyList())
}

private fun persistClipCustomTags(context: android.content.Context, clip: VideoClip, tags: List<String>) {
    val stored = JSONArray()
    tags.forEach { stored.put(it) }
    val payload = stored.toString()
    context.getSharedPreferences(CLIP_CUSTOM_TAGS_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(clip.id, payload)
        .putString(clip.filePath, payload)
        .apply()
}

private fun persistReelClipSummary(
    context: android.content.Context,
    reelUri: Uri,
    includedClips: List<VideoClip>
) {
    if (includedClips.isEmpty()) return
    val lines = includedClips.joinToString("\n") { clip ->
        val moment = clip.momentTag?.trim()?.takeIf { it.isNotEmpty() } ?: "Clip"
        val date = clip.gameDate.takeIf { it.isNotBlank() }?.let { formatRestoredClipDate(it) }
        if (date != null) "• $moment - $date" else "• $moment"
    }
    val summary = "Includes ${includedClips.size} memories:\n$lines"
    context.getSharedPreferences(CLIP_COMMENTARY_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(reelUri.toString(), summary)
        .apply()
}

private const val REEL_CLIP_IDS_PREFS = "reel_clip_ids_v1"

private fun persistReelClipIds(context: android.content.Context, reelUri: Uri, clipIds: List<String>) {
    val payload = JSONArray().apply { clipIds.forEach { put(it) } }.toString()
    context.getSharedPreferences(REEL_CLIP_IDS_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(reelUri.toString(), payload)
        .apply()
}

private fun loadReelClipIds(context: android.content.Context, reelUri: Uri): List<String> {
    val raw = context.getSharedPreferences(REEL_CLIP_IDS_PREFS, android.content.Context.MODE_PRIVATE)
        .getString(reelUri.toString(), null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
}

private fun updateReelTitle(context: android.content.Context, clip: VideoClip, newTitle: String) {
    val trimmed = newTitle.trim()
    if (trimmed.isEmpty()) return
    val label = "Reel: $trimmed"
    context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(clip.id, label)
        .putString(clip.filePath, label)
        .apply()

    val metadataPrefs = context.getSharedPreferences(REEL_METADATA_PREFS, android.content.Context.MODE_PRIVATE)
    metadataPrefs.all.forEach { (key, value) ->
        if (value !is String) return@forEach
        val payload = runCatching { JSONObject(value) }.getOrNull() ?: return@forEach
        if (payload.optString("reelUri", "").trim() != clip.filePath) return@forEach
        payload.put("title", trimmed)
        metadataPrefs.edit().putString(key, payload.toString()).apply()
    }
}

private fun loadRestoredClipThumbnail(context: android.content.Context, filePath: String): Bitmap? {
    val uri = Uri.parse(filePath)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
        try {
            return context.contentResolver.loadThumbnail(uri, Size(1280, 720), null)
        } catch (_: Exception) {
        }
    }

    return try {
        val retriever = MediaMetadataRetriever()
        if (uri.scheme == "content") {
            retriever.setDataSource(context, uri)
        } else {
            val localPath = if (uri.scheme == "file") uri.path else filePath
            if (localPath.isNullOrBlank()) return null
            retriever.setDataSource(localPath)
        }

        val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                500_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                1280,
                720
            )
        } else {
            null
        } ?: retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime()
        retriever.release()
        frame
    } catch (_: Exception) {
        null
    }
}

private suspend fun deleteRestoredClip(context: android.content.Context, video: VideoClip) {
    withContext(Dispatchers.IO) {
        val videoUri = Uri.parse(video.filePath)

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
                .remove(video.id)
                .remove(video.filePath)
                .apply()
        }

        val reelMetadataPrefs = context.getSharedPreferences(REEL_METADATA_PREFS, android.content.Context.MODE_PRIVATE)
        val metadataEditor = reelMetadataPrefs.edit()
            .remove(video.id)
            .remove(video.filePath)

        reelMetadataPrefs.all.forEach { (key, value) ->
            if (value !is String) return@forEach
            val payload = runCatching { JSONObject(value) }.getOrNull() ?: return@forEach
            val reelUri = payload.optString("reelUri", "").trim()
            if (reelUri == video.filePath || reelUri == video.id || key == video.filePath || key == video.id) {
                metadataEditor.remove(key)
            }
        }
        metadataEditor.apply()
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ClipEditorScreen(
    clip: VideoClip,
    isTrimming: Boolean,
    allowTrim: Boolean,
    onClose: () -> Unit,
    onSaveTrim: (Long, Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val player = remember(clip.filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(clip.filePath)))
            prepare()
            playWhenReady = false
        }
    }
    BackHandler(enabled = !isTrimming) {
        onClose()
    }

    DisposableEffect(clip.filePath) {
        onDispose { player.release() }
    }

    var durationMs by remember(clip.id) { mutableStateOf(clip.duration.coerceAtLeast(0L)) }
    var startMs by remember(clip.id) { mutableStateOf(0L) }
    var endMs by remember(clip.id) { mutableStateOf(clip.duration.coerceAtLeast(0L)) }
    var playbackPositionMs by remember(clip.id) { mutableStateOf(0L) }

    LaunchedEffect(clip.filePath) {
        while (durationMs <= 0L) {
            val reported = player.duration
            if (reported > 0L) {
                durationMs = reported
                if (endMs <= 0L) endMs = reported
            }
            delay(200)
        }
    }

    LaunchedEffect(player) {
        while (true) {
            playbackPositionMs = player.currentPosition.coerceAtLeast(0L)
            delay(50)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, enabled = !isTrimming) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (allowTrim) "Trim clip" else "View clip",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose, enabled = !isTrimming) {
                    Text("Done")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = {
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (player.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (allowTrim) {
                if (durationMs > 0L) {
                    TrimTimelineBar(
                        durationMs = durationMs,
                        startMs = startMs,
                        endMs = endMs,
                        progressMs = playbackPositionMs,
                        enabled = !isTrimming,
                        onStartChanged = { value -> startMs = value.coerceIn(0L, durationMs) },
                        onEndChanged = { value -> endMs = value.coerceIn(0L, durationMs) },
                        onPreviewAtStart = { player.seekTo(startMs) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatRestoredDuration(startMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatRestoredDuration((endMs - startMs).coerceAtLeast(0L))}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PlaysAccentColor
                        )
                        Text(
                            text = "${formatRestoredDuration(endMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Loading clip...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (durationMs > 0L) {
                        TextButton(
                            onClick = {
                                startMs = 0L
                                endMs = durationMs
                                player.seekTo(0L)
                            },
                            enabled = !isTrimming,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Full clip")
                        }
                    }

                    Button(
                        onClick = { onSaveTrim(startMs, endMs) },
                        enabled = !isTrimming && durationMs > 0L && (endMs - startMs) >= 500L,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTrimming) "Saving..." else "Save")
                    }
                }
                }
            }
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun EditReelScreen(
    reelClip: VideoClip?,
    initialSelectedIds: List<String>,
    initialTitle: String,
    availableClips: List<VideoClip>,
    isSaving: Boolean,
    onAddClips: (List<String>, String) -> Unit,
    onClose: () -> Unit,
    onSave: (List<String>, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var orderedIds by remember { mutableStateOf(initialSelectedIds) }
    val clipsById = remember(availableClips, reelClip) {
        (availableClips + listOfNotNull(reelClip)).associateBy { it.id }
    }

    val previewClip = orderedIds.firstNotNullOfOrNull { clipsById[it] }
        ?: availableClips.firstOrNull()
        ?: return
    var editedTitle by remember(reelClip?.id, initialTitle) { mutableStateOf(initialTitle) }
    val orderedClips = remember(orderedIds, clipsById) { orderedIds.mapNotNull { clipsById[it] } }
    val totalDurationMs = remember(orderedClips) { orderedClips.sumOf { it.duration } }
    var isPlayingPreview by remember(previewClip.filePath) { mutableStateOf(false) }
    var showFullScreenPreview by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, enabled = !isSaving) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Edit Reel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onSave(orderedIds, editedTitle.trim().ifBlank { initialTitle }) },
                    enabled = !isSaving && orderedIds.isNotEmpty(),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "thumbnail") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(158.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable { isPlayingPreview = !isPlayingPreview },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlayingPreview) {
                            val inlinePlayer = remember(previewClip.filePath) {
                                ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(MediaItem.fromUri(Uri.parse(previewClip.filePath)))
                                    prepare()
                                    playWhenReady = true
                                }
                            }
                            DisposableEffect(previewClip.filePath) {
                                onDispose { inlinePlayer.release() }
                            }
                            AndroidView(
                                factory = { viewContext ->
                                    PlayerView(viewContext).apply {
                                        player = inlinePlayer
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val previewThumbnail by produceState<Bitmap?>(initialValue = null, key1 = previewClip.filePath) {
                                value = withContext(Dispatchers.IO) { loadRestoredClipThumbnail(context, previewClip.filePath) }
                            }
                            if (previewThumbnail != null) {
                                Image(
                                    bitmap = previewThumbnail!!.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
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
                                formatRestoredDuration(totalDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { showFullScreenPreview = true },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            contentColor = Color.White
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "View full screen",
                                modifier = Modifier
                                    .padding(5.dp)
                                    .fillMaxSize()
                            )
                        }
                    }
                }

                item(key = "title") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reel title") },
                        singleLine = true,
                        enabled = !isSaving,
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                item(key = "clips_header") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Clips in this reel (${orderedIds.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                itemsIndexed(orderedIds, key = { _, id -> id }) { index, id ->
                    ReelOrderClipRow(
                        clip = clipsById[id],
                        canMoveUp = index > 0,
                        canMoveDown = index < orderedIds.size - 1,
                        onMoveUp = {
                            orderedIds = orderedIds.toMutableList().apply { add(index - 1, removeAt(index)) }
                        },
                        onMoveDown = {
                            orderedIds = orderedIds.toMutableList().apply { add(index + 1, removeAt(index)) }
                        },
                        onRemove = { orderedIds = orderedIds - id }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                item(key = "add_clips_button") {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            onAddClips(
                                orderedIds,
                                editedTitle.trim().ifBlank { initialTitle }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Clips")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showFullScreenPreview) {
        Dialog(
            onDismissRequest = { showFullScreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                val fullScreenPlayer = remember(previewClip.filePath) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(previewClip.filePath)))
                        prepare()
                        playWhenReady = true
                    }
                }
                DisposableEffect(previewClip.filePath) {
                    onDispose { fullScreenPlayer.release() }
                }
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            player = fullScreenPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { showFullScreenPreview = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ReelOrderClipRow(
    clip: VideoClip?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = clip?.filePath) {
        value = clip?.filePath?.let { path -> withContext(Dispatchers.IO) { loadRestoredClipThumbnail(context, path) } }
    }
    val currentTag = clip?.momentTag?.takeIf { it.isNotBlank() }
    val tagConfig = momentTagConfig(currentTag)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(tagConfig.second.copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = tagConfig.third,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = clip?.momentTag?.takeIf { it.isNotBlank() } ?: clip?.gameTitle ?: "Clip",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatRestoredDuration(clip?.duration ?: 0L),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove from reel")
            }
        }
    }
}

@Composable
private fun AddClipsToReelScreen(
    availableClips: List<VideoClip>,
    selectedIds: Set<String>,
    kidLookup: Map<String, String?>,
    opponentLookup: Map<String, String?>,
    onBack: () -> Unit,
    onAddClip: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedKid by remember { mutableStateOf<String?>(null) }
    var selectedSeasonKey by remember { mutableStateOf<String?>(null) }
    var selectedOpponentKey by remember { mutableStateOf<String?>(null) }
    var showKidMenu by remember { mutableStateOf(false) }
    var showSeasonMenu by remember { mutableStateOf(false) }
    var showOpponentMenu by remember { mutableStateOf(false) }

    val kidOptions = remember(availableClips, kidLookup) {
        availableClips.mapNotNull { kidLookup[it.id]?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val seasonOptions = remember(availableClips) {
        availableClips.mapNotNull { parseRestoredSeasonKey(it.gameDate) }
            .distinctBy { it.key }
            .sortedByDescending { it.startYear }
    }
    val opponentOptions = remember(availableClips, opponentLookup) {
        availableClips.mapNotNull { opponentLookup[it.id]?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    val filteredClips = remember(availableClips, query, selectedKid, selectedSeasonKey, selectedOpponentKey, kidLookup, opponentLookup) {
        availableClips.filter { clip ->
            val matchesKid = selectedKid == null || kidLookup[clip.id]?.equals(selectedKid, ignoreCase = true) == true
            val matchesSeason = selectedSeasonKey == null || parseRestoredSeasonKey(clip.gameDate)?.key == selectedSeasonKey
            val matchesOpponent = selectedOpponentKey == null || opponentLookup[clip.id]?.equals(selectedOpponentKey, ignoreCase = true) == true
            val matchesQuery = query.isBlank() ||
                clip.momentTag.orEmpty().contains(query, ignoreCase = true) ||
                clip.gameTitle.contains(query, ignoreCase = true)
            matchesKid && matchesSeason && matchesOpponent && matchesQuery
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Add Clips",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search by tag or game") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (kidOptions.isNotEmpty()) {
                    Box {
                        FilterChip(
                            selected = selectedKid != null,
                            onClick = { showKidMenu = true },
                            label = { Text(selectedKid ?: "Player") }
                        )
                        DropdownMenu(expanded = showKidMenu, onDismissRequest = { showKidMenu = false }) {
                            DropdownMenuItem(text = { Text("All players") }, onClick = { selectedKid = null; showKidMenu = false })
                            kidOptions.forEach { kid ->
                                DropdownMenuItem(text = { Text(kid) }, onClick = { selectedKid = kid; showKidMenu = false })
                            }
                        }
                    }
                }
                if (seasonOptions.isNotEmpty()) {
                    Box {
                        FilterChip(
                            selected = selectedSeasonKey != null,
                            onClick = { showSeasonMenu = true },
                            label = { Text(seasonOptions.firstOrNull { it.key == selectedSeasonKey }?.label ?: "Season") }
                        )
                        DropdownMenu(expanded = showSeasonMenu, onDismissRequest = { showSeasonMenu = false }) {
                            DropdownMenuItem(text = { Text("All seasons") }, onClick = { selectedSeasonKey = null; showSeasonMenu = false })
                            seasonOptions.forEach { season ->
                                DropdownMenuItem(text = { Text(season.label) }, onClick = { selectedSeasonKey = season.key; showSeasonMenu = false })
                            }
                        }
                    }
                }
                if (opponentOptions.isNotEmpty()) {
                    Box {
                        FilterChip(
                            selected = selectedOpponentKey != null,
                            onClick = { showOpponentMenu = true },
                            label = { Text(selectedOpponentKey ?: "Opponent") }
                        )
                        DropdownMenu(expanded = showOpponentMenu, onDismissRequest = { showOpponentMenu = false }) {
                            DropdownMenuItem(text = { Text("All opponents") }, onClick = { selectedOpponentKey = null; showOpponentMenu = false })
                            opponentOptions.forEach { opponent ->
                                DropdownMenuItem(text = { Text(opponent) }, onClick = { selectedOpponentKey = opponent; showOpponentMenu = false })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredClips.isEmpty()) {
                    item {
                        Text(
                            text = "No clips match your filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                items(filteredClips, key = { it.id }) { clip ->
                    val included = selectedIds.contains(clip.id)
                    AddClipToReelRow(
                        clip = clip,
                        included = included,
                        onClick = { if (!included) onAddClip(clip.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AddClipToReelRow(
    clip: VideoClip,
    included: Boolean,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = clip.filePath) {
        value = withContext(Dispatchers.IO) { loadRestoredClipThumbnail(context, clip.filePath) }
    }
    val currentTag = clip.momentTag?.takeIf { it.isNotBlank() }
    val tagConfig = momentTagConfig(currentTag)
    val rowAlpha = if (included) 0.4f else 1f

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        onClick = onClick,
        modifier = Modifier.alpha(rowAlpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(tagConfig.second.copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = tagConfig.third,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clip.momentTag?.takeIf { it.isNotBlank() } ?: clip.gameTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (clip.gameDate.isNotBlank()) {
                    Text(
                        text = formatRestoredClipDate(clip.gameDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = formatRestoredDuration(clip.duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Icon(
                if (included) Icons.Default.Check else Icons.Default.Add,
                contentDescription = if (included) "Already in reel" else "Add to reel",
                tint = if (included) MaterialTheme.colorScheme.onSurfaceVariant else PlaysAccentColor
            )
        }
    }
}

@Composable
private fun TrimTimelineBar(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    progressMs: Long,
    enabled: Boolean,
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
    onPreviewAtStart: () -> Unit
) {
    var activeHandle by remember(durationMs) { mutableStateOf<TrimHandle?>(null) }
    var barWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(vertical = 6.dp)
            .pointerInput(enabled, durationMs, startMs, endMs) {
                if (!enabled || durationMs <= 0L) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val x = change.position.x.coerceIn(0f, barWidthPx.coerceAtLeast(1f))
                        val msForX = ((x / barWidthPx.coerceAtLeast(1f)) * durationMs).toLong()
                        val startDistance = kotlin.math.abs(x - ((startMs.toFloat() / durationMs.toFloat()) * barWidthPx))
                        val endDistance = kotlin.math.abs(x - ((endMs.toFloat() / durationMs.toFloat()) * barWidthPx))

                        if (change.pressed && activeHandle == null) {
                            activeHandle = if (startDistance <= endDistance) TrimHandle.START else TrimHandle.END
                        }

                        if (change.pressed) {
                            when (activeHandle) {
                                TrimHandle.START -> {
                                    val nextStart = msForX.coerceIn(0L, (endMs - 1L).coerceAtLeast(0L))
                                    onStartChanged(nextStart)
                                    onPreviewAtStart()
                                }
                                TrimHandle.END -> {
                                    val nextEnd = msForX.coerceIn((startMs + 1L).coerceAtLeast(0L), durationMs)
                                    onEndChanged(nextEnd)
                                }
                                null -> Unit
                            }
                        } else {
                            activeHandle = null
                        }
                    }
                }
            }
            .onSizeChanged { barWidthPx = it.width.toFloat() }
    ) {
        val handleRadius = 14f
        val maxTrackWidth = (barWidthPx - (handleRadius * 2f)).coerceAtLeast(0f)
        val selectionStartPx = if (durationMs <= 0L) 0f else (startMs.toFloat() / durationMs.toFloat()) * maxTrackWidth
        val selectionEndPx = if (durationMs <= 0L) 0f else (endMs.toFloat() / durationMs.toFloat()) * maxTrackWidth
        val progressPx = if (durationMs <= 0L) 0f else {
            (progressMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()) * maxTrackWidth
        }
        val minimumSelectionWidth = minOf(12f, maxTrackWidth)
        val clampedStart = selectionStartPx.coerceIn(
            0f,
            (maxTrackWidth - minimumSelectionWidth).coerceAtLeast(0f)
        )
        val clampedEnd = selectionEndPx.coerceIn(
            clampedStart + minimumSelectionWidth,
            maxTrackWidth
        )

        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}

        Surface(
            modifier = Modifier
                .height(4.dp)
                .width(with(density) { progressPx.toDp() })
                .align(Alignment.CenterStart),
            shape = RoundedCornerShape(2.dp),
            color = PlaysAccentColor
        ) {}

        Surface(
            modifier = Modifier
                .height(32.dp)
                .width(with(density) { (clampedEnd - clampedStart).toDp() })
                .offset(x = with(density) { clampedStart.toDp() }),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
        ) {}

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { (clampedStart - handleRadius).toDp() })
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(3.dp, Color.White, CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { (clampedEnd - handleRadius).toDp() })
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(3.dp, Color.White, CircleShape)
        )
    }
}

private enum class TrimHandle { START, END }

@Composable
private fun FilterMenuLabel(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Open $label filter",
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ClipsFileTab(
    label: String,
    highlighted: Boolean,
    underlined: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(PrimaryClipsControlHeight)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (underlined) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(48.dp)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
            )
        }
    }
}

@Composable
private fun ClipActionButton(
    icon: ImageVector?,
    label: String,
    modifier: Modifier = Modifier,
    labelFontSize: TextUnit = 14.sp,
    maxLabelWidth: Dp = 96.dp,
    tint: Color,
    labelColor: Color = tint,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = labelFontSize,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = maxLabelWidth)
        )
    }
}

@Composable
private fun momentTagConfig(tag: String?): Triple<ImageVector, Color, String> {
    val normalized = tag?.trim().orEmpty()
    return when {
        normalized.equals("Score", ignoreCase = true) ||
            normalized.equals("Goal", ignoreCase = true) ||
            normalized.equals("Winning Goal", ignoreCase = true) -> Triple(Icons.Default.GpsFixed, Color(0xFFE87522), "Score")
        normalized.equals("Assist", ignoreCase = true) -> Triple(Icons.Default.Hub, Color(0xFF168C8C), "Assist")
        normalized.equals("Save", ignoreCase = true) -> Triple(Icons.Default.Shield, Color(0xFF7950BC), "Save")
        normalized.equals("Defensive Stop", ignoreCase = true) -> Triple(StopSignIcon, Color(0xFFD9534F), "Defensive Stop")
        normalized.equals("Faceoff Win", ignoreCase = true) || normalized.equals("Win", ignoreCase = true) -> Triple(CrossedLacrosseSticksIcon, Color(0xFF399875), "Faceoff Win")
        normalized.equals("Ground Ball", ignoreCase = true) -> Triple(GroundBallIcon, Color(0xFF3676B8), "Ground Ball")
        normalized.equals("Big Play", ignoreCase = true) -> Triple(StopSignIcon, Color(0xFFD9534F), "Defensive Stop")
        normalized.isNotEmpty() -> Triple(Icons.Default.PlayArrow, MaterialTheme.colorScheme.onSurfaceVariant, normalized)
        else -> Triple(Icons.Default.PlayArrow, MaterialTheme.colorScheme.onSurfaceVariant, "Tag")
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun RestoredClipRow(
    clip: VideoClip,
    reelMode: Boolean,
    isSelectedForReel: Boolean,
    onToggleReelSelection: () -> Unit,
    onEdit: () -> Unit,
    onExpand: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleHighlight: () -> Unit,
    onTagChanged: (String?) -> Unit,
    onTitleChanged: (String) -> Unit,
    onEditReel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var commentary by remember(clip.id, clip.filePath) {
        mutableStateOf(loadClipCommentary(context, clip))
    }
    var isCommentaryExpanded by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    val previewBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = clip.id,
        key2 = clip.filePath
    ) {
        value = withContext(Dispatchers.IO) {
            loadRestoredClipThumbnail(context, clip.filePath)
        }
    }

    val actionWidthPx = with(LocalDensity.current) { RestoredSwipeActionWidth.toPx() }
    var swipeOffsetTarget by remember(clip.id) { mutableStateOf(0f) }
    val swipeOffset by animateFloatAsState(
        targetValue = swipeOffsetTarget,
        animationSpec = tween(durationMillis = 180),
        label = "restored_clip_swipe"
    )
    val dragState = rememberDraggableState { delta ->
        swipeOffsetTarget = (swipeOffsetTarget + delta).coerceIn(-actionWidthPx, actionWidthPx)
    }
    val rowScope = rememberCoroutineScope()
    var showTagPicker by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    var customTags by remember(clip.id, clip.filePath) { mutableStateOf(loadClipCustomTags(context, clip)) }
    var showTitleEditor by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    var isPlayingInline by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    val tagOptions = DefaultMomentTags
    val isReel = remember(clip.momentTag, clip.gameTitle) {
        clip.momentTag?.startsWith("Reel:", ignoreCase = true) == true ||
            clip.gameTitle.startsWith("Reel:", ignoreCase = true) ||
            clip.gameTitle.startsWith("Spotr-Reel", ignoreCase = true)
    }
    val reelDisplayTitle = remember(clip.momentTag, clip.gameTitle) {
        val raw = (clip.momentTag?.takeIf { it.isNotBlank() } ?: clip.gameTitle).trim()
        if (raw.startsWith("Reel:", ignoreCase = true)) raw.substring(5).trim() else raw
    }

    LaunchedEffect(reelMode) {
        if (reelMode) {
            swipeOffsetTarget = 0f
            isPlayingInline = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(158.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        ) {
            when {
                swipeOffset > 0f -> {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(RestoredSwipeActionWidth)
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            if (swipeOffsetTarget >= actionWidthPx * 0.9f) {
                                onShare()
                                rowScope.launch { swipeOffsetTarget = 0f }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                swipeOffset < 0f -> {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(RestoredSwipeActionWidth)
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            if (swipeOffsetTarget <= -actionWidthPx * 0.9f) {
                                onDelete()
                                rowScope.launch { swipeOffsetTarget = 0f }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = !reelMode,
                    onDragStopped = {
                        val settleThreshold = actionWidthPx * 0.5f
                        swipeOffsetTarget = when {
                            swipeOffsetTarget > settleThreshold -> actionWidthPx
                            swipeOffsetTarget < -settleThreshold -> -actionWidthPx
                            else -> 0f
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .clickable { isPlayingInline = !isPlayingInline },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlayingInline) {
                        val inlinePlayer = remember(clip.filePath) {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(Uri.parse(clip.filePath)))
                                prepare()
                                playWhenReady = true
                            }
                        }
                        DisposableEffect(clip.filePath) {
                            onDispose { inlinePlayer.release() }
                        }
                        AndroidView(
                            factory = { viewContext ->
                                PlayerView(viewContext).apply {
                                    player = inlinePlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
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
                            formatRestoredDuration(clip.duration),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                    val tagStartPadding = 6.dp
                    if (isReel) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = tagStartPadding, top = 6.dp)
                                .widthIn(max = 190.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2B2F33).copy(alpha = 0.82f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { showTitleEditor = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reelDisplayTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE8EAED),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        run {
                            val clipCount = Regex("""Includes (\d+)""").find(commentary)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = tagStartPadding, top = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable(enabled = commentary.isNotBlank()) { isCommentaryExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = if (clipCount != null) "$clipCount clip${if (clipCount == 1) "" else "s"}" else "View clips",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val currentTag = clip.momentTag?.takeIf { it.isNotBlank() }
                        val tagConfig = if (currentTag != null) momentTagConfig(currentTag) else null
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = tagStartPadding, top = 6.dp)
                                .widthIn(max = 168.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(tagConfig?.second ?: Color.Black.copy(alpha = 0.6f))
                                .clickable(enabled = !reelMode) { showTagPicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tagConfig != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = tagConfig.first,
                                        contentDescription = tagConfig.third,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = tagConfig.third,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "+Tag",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (!isPlayingInline) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (!reelMode) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { onExpand() },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            contentColor = Color.White
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "View full screen",
                                modifier = Modifier
                                    .padding(5.dp)
                                    .fillMaxSize()
                            )
                        }
                    }
                    if (reelMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelectedForReel) MaterialTheme.colorScheme.primary else Color.White)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelectedForReel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onToggleReelSelection() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelectedForReel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected for reel",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isReel && (!reelMode || commentary.isNotBlank())) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !reelMode) { isCommentaryExpanded = !isCommentaryExpanded }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (commentary.isBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = commentary.takeIf { it.isNotBlank() } ?: "What Happened",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                softWrap = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!reelMode || clip.isHighlight) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        if (!reelMode) {
                            if (isReel) {
                                ClipActionButton(
                                    icon = Icons.Default.Edit,
                                    label = "Edit Reel",
                                    modifier = Modifier.weight(1f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = onEditReel
                                )
                            } else {
                                ClipActionButton(
                                    icon = Icons.Default.Edit,
                                    label = "Trim",
                                    modifier = Modifier.weight(1f),
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = onEdit
                                )
                            }
                            ClipActionButton(
                                icon = Icons.Default.Share,
                                label = "Share",
                                modifier = Modifier.weight(1f),
                                tint = MaterialTheme.colorScheme.tertiary,
                                onClick = onShare
                            )
                        }

                        ClipActionButton(
                            icon = Icons.Default.Star,
                            label = "Favorite",
                            modifier = Modifier.weight(1f),
                            tint = if (clip.isHighlight) {
                                PlaysAccentColor
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            onClick = onToggleHighlight
                        )
                        }
                    }
                }

                if (isReel && isCommentaryExpanded) {
                    val summaryLines = commentary.split("\n")
                    val headline = summaryLines.firstOrNull().orEmpty()
                    val memoryLines = summaryLines.drop(1).filter { it.isNotBlank() }
                    Dialog(onDismissRequest = { isCommentaryExpanded = false }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                    width = 1.4.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PlaysAccentColor, Color(0xFF1C8CFF))
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF0A1118)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = PlaysAccentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = headline.uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp,
                                        color = Color.White
                                    )
                                }
                                memoryLines.forEach { line ->
                                    Text(
                                        text = line.removePrefix("•").trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF9FC9FF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { isCommentaryExpanded = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }

                if (!isReel && isCommentaryExpanded) {
                    val keyboardController = LocalSoftwareKeyboardController.current
                    AlertDialog(
                        onDismissRequest = { isCommentaryExpanded = false },
                        title = {
                            Text(if (commentary.isBlank()) "What Happened" else "Edit Comment")
                        },
                        text = {
                            OutlinedTextField(
                                value = commentary,
                                onValueChange = { updated -> commentary = updated },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Add notes about this clip") },
                                minLines = 2,
                                maxLines = 4,
                                singleLine = false,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    persistClipCommentary(context, clip, commentary)
                                    isCommentaryExpanded = false
                                }
                            ) {
                                Text(if (commentary.isBlank()) "Add" else "Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { isCommentaryExpanded = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }

        if (showTagPicker) {
            var customTag by remember { mutableStateOf("") }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTagPicker = false },
                title = { Text("Edit tag") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val dialogKeyboard = LocalSoftwareKeyboardController.current
                        val dialogFocus = LocalFocusManager.current
                        val commitCustomTag = {
                            val trimmed = customTag.trim()
                            if (trimmed.isNotEmpty()) {
                                val alreadyKnown = (tagOptions + customTags).any { it.equals(trimmed, ignoreCase = true) }
                                if (!alreadyKnown) {
                                    val updated = customTags + trimmed
                                    customTags = updated
                                    persistClipCustomTags(context, clip, updated)
                                }
                                onTagChanged(trimmed)
                                customTag = ""
                            }
                            dialogFocus.clearFocus(force = true)
                            dialogKeyboard?.hide()
                        }
                        val selectableTags = remember(clip.momentTag, customTags) {
                            val current = clip.momentTag?.trim()?.takeIf { it.isNotEmpty() }
                            (tagOptions + customTags + listOfNotNull(current)).distinctBy { it.lowercase() }
                        }
                        selectableTags.forEach { label ->
                            val isSelected = clip.momentTag?.equals(label, ignoreCase = true) == true
                            TextButton(
                                onClick = { onTagChanged(if (isSelected) null else label) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label)
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = customTag,
                            onValueChange = { customTag = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Custom tag") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { commitCustomTag() }
                            )
                        )
                        TextButton(
                            onClick = { commitCustomTag() },
                            enabled = customTag.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Add tag")
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showTagPicker = false }) {
                        Text("Done")
                    }
                }
            )
        }

        if (showTitleEditor) {
            var titleInput by remember { mutableStateOf(reelDisplayTitle) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTitleEditor = false },
                title = { Text("Rename reel") },
                text = {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reel title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val trimmed = titleInput.trim()
                            if (trimmed.isNotEmpty()) {
                                onTitleChanged(trimmed)
                            }
                            showTitleEditor = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showTitleEditor = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private suspend fun loadRestoredClips(context: android.content.Context, teamName: String): List<VideoClip> {
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
                val fileUriString = Uri.fromFile(file).toString()
                if (seenIds.contains(videoPath) || seenIds.contains(fileUriString)) continue

                val storedTeamName = teamPrefs.getString(videoPath, null)
                    ?: teamPrefs.getString(fileUriString, null)
                if (storedTeamName != teamName) continue

                val storedStartTime = startPrefs.getLong(videoPath, 0L)
                    .takeIf { it > 0L }
                    ?: startPrefs.getLong(fileUriString, 0L).takeIf { it > 0L }
                val createdAt = storedStartTime ?: file.lastModified()
                val gameDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt))
                val customName = customNamePrefs.getString(videoPath, null)
                    ?: customNamePrefs.getString(fileUriString, null)
                val opponentName = opponentPrefs.getString(videoPath, null)
                    ?: opponentPrefs.getString(fileUriString, null)
                val isHighlight = highlightPrefs.getBoolean(videoPath, false)
                    || highlightPrefs.getBoolean(fileUriString, false)
                val gameTitle = buildRestoredClipTitle(file.nameWithoutExtension, opponentName, customName)

                val duration = try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(videoPath)
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    retriever.release()
                    durationMs
                } catch (_: Exception) {
                    0L
                }

                videos.add(
                    VideoClip(
                        id = videoPath,
                        filePath = Uri.fromFile(file).toString(),
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
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )
        val relativePaths = reelVideoRelativePathsForRestoredClips()
        val selection = relativePaths.joinToString(" OR ") { "${MediaStore.MediaColumns.RELATIVE_PATH}=?" }
        val selectionArgs = relativePaths.toTypedArray()
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val uriString = contentUri.toString()
                if (seenIds.contains(uriString)) continue

                val storedTeamName = teamPrefs.getString(uriString, null)
                if (storedTeamName != teamName) continue

                val displayName = cursor.getString(nameIndex) ?: "clip_$id"
                val dateTaken = cursor.getLong(dateTakenIndex)
                val dateAdded = cursor.getLong(dateAddedIndex)
                val storedStartTime = startPrefs.getLong(uriString, 0L).takeIf { it > 0L }
                val createdAt = storedStartTime ?: if (dateTaken > 0) dateTaken else dateAdded * 1000
                val gameDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt))
                val duration = cursor.getLong(durationIndex)
                val customName = customNamePrefs.getString(uriString, null)
                val opponentName = opponentPrefs.getString(uriString, null)
                val isHighlight = highlightPrefs.getBoolean(uriString, false)
                val baseTitle = displayName.substringBeforeLast(".", displayName)
                val gameTitle = buildRestoredClipTitle(baseTitle, opponentName, customName)

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
            val gameDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt))
            val customName = customNamePrefs.getString(uriString, null)
            val opponentName = opponentPrefs.getString(uriString, null)
            val isHighlight = highlightPrefs.getBoolean(uriString, false)
            val baseTitle = contentUri.lastPathSegment?.substringAfterLast('/') ?: "uploaded_clip"
            val gameTitle = buildRestoredClipTitle(baseTitle, opponentName, customName)

            val duration = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, contentUri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
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

        val reelMetadataPrefs = context.getSharedPreferences(REEL_METADATA_PREFS, android.content.Context.MODE_PRIVATE)
        reelMetadataPrefs.all.forEach { (key, value) ->
            if (value !is String) return@forEach
            val payload = runCatching { JSONObject(value) }.getOrNull() ?: return@forEach
            val team = payload.optString("team", "").trim()
            if (team != teamName) return@forEach
            val reelUriString = payload.optString("reelUri", "").trim()
            if (reelUriString.isBlank() || seenIds.contains(reelUriString)) return@forEach
            val reelUri = Uri.parse(reelUriString)
            val reelExists = runCatching {
                when (reelUri.scheme?.lowercase(Locale.getDefault())) {
                    "content" -> context.contentResolver.openFileDescriptor(reelUri, "r")?.use { true } == true
                    "file" -> {
                        val path = reelUri.path
                        !path.isNullOrBlank() && File(path).exists()
                    }
                    else -> false
                }
            }.getOrDefault(false)
            if (!reelExists) {
                reelMetadataPrefs.edit().remove(key).remove(reelUriString).apply()
                return@forEach
            }

            val title = payload.optString("title", "Spotr Reel").trim().ifBlank { "Spotr Reel" }
            val createdAt = payload.optLong("createdAt", System.currentTimeMillis())
            val gameDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt))
            val reelName = "Reel: $title"
            val duration = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, reelUri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                retriever.release()
                durationMs
            } catch (_: Exception) {
                0L
            }

            videos.add(
                VideoClip(
                    id = reelUriString,
                    filePath = reelUriString,
                    duration = duration,
                    createdAt = createdAt,
                    gameDate = gameDate,
                    gameTitle = reelName,
                    isHighlight = false,
                    momentTag = reelName
                )
            )
            seenIds.add(reelUriString)
        }

        videos.sortedByDescending { it.createdAt }
    }
}

private fun buildRestoredClipTitle(baseTitle: String, opponentName: String?, customName: String?): String {
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

private fun buildRestoredClipOpponentLookup(
    context: android.content.Context,
    videos: List<VideoClip>
): Map<String, String?> {
    val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
    return videos.associate { video ->
        val fileUri = Uri.parse(video.filePath)
        val filePath = fileUri.path
        video.id to (
            opponentPrefs.getString(video.id, null)
                ?: opponentPrefs.getString(video.filePath, null)
                ?: filePath?.let { opponentPrefs.getString(it, null) }
        )?.trim().takeIf { !it.isNullOrBlank() }
    }
}

private fun filterRestoredClipsBySearchQuery(
    videos: List<VideoClip>,
    query: String,
    kidLookup: Map<String, String?>,
    opponentLookup: Map<String, String?>
): List<VideoClip> {
    val normalizedQuery = query
        .lowercase(Locale.getDefault())
        .replace("’s", " ")
        .replace("'s", " ")
        .replace(Regex("[^a-z0-9#]+"), " ")
        .trim()

    if (normalizedQuery.isBlank()) return videos

    val queryWords = normalizedQuery.split(Regex("\\s+")).filter(String::isNotBlank)
    val hasSeasonIntent = "season" in queryWords
    val currentSeasonStartYear = currentRestoredSeasonStartYear()
    val requestedSeasonStartYear = when {
        !hasSeasonIntent -> null
        "last" in queryWords || "previous" in queryWords -> currentSeasonStartYear - 1
        "this" in queryWords || "current" in queryWords -> currentSeasonStartYear
        else -> null
    }
    val requestedTag = detectMomentTagIntent(queryWords)
    val intentWords = buildSet {
        if (hasSeasonIntent) addAll(setOf("season", "last", "previous", "this", "current"))
        if (requestedTag != null) addAll(momentTagIntentWords(requestedTag))
    }
    val wantsFirst = !hasSeasonIntent && "first" in queryWords
    val wantsLast = !hasSeasonIntent && "last" in queryWords
    val searchTerms = queryWords
        .filter { it !in setOf("the", "a", "an", "and", "of", "my", "first", "last") && it !in intentWords }

    val matches = videos.filter { video ->
        val matchesSeason = requestedSeasonStartYear == null ||
            parseRestoredSeasonKey(video.gameDate)?.startYear == requestedSeasonStartYear
        val matchesTag = requestedTag == null || canonicalMomentTag(video.momentTag) == requestedTag
        if (!matchesSeason || !matchesTag) return@filter false

        val bubbleNames = try {
            val bubbles = JSONArray(video.bubbleMetadata)
            (0 until bubbles.length()).flatMap { index ->
                val bubble = bubbles.optJSONObject(index) ?: return@flatMap emptyList()
                listOf(
                    bubble.optString("playerName"),
                    bubble.optString("team"),
                    bubble.optString("playerNumber")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        val searchableText = listOf(
            video.gameTitle,
            video.gameDate,
            video.momentTag.orEmpty(),
            kidLookup[video.id].orEmpty(),
            opponentLookup[video.id].orEmpty()
        ) + bubbleNames

        val searchable = searchableText.joinToString(" ")
            .lowercase(Locale.getDefault())
            .replace("’s", " ")
            .replace("'s", " ")

        searchTerms.all { term ->
            searchable.contains(term) ||
                (term.endsWith("s") && term.length > 3 && searchable.contains(term.dropLast(1)))
        }
    }

    return when {
        wantsFirst -> matches.minWithOrNull(compareBy<VideoClip> { it.gameDate }.thenBy { it.createdAt })?.let(::listOf).orEmpty()
        wantsLast -> matches.maxWithOrNull(compareBy<VideoClip> { it.gameDate }.thenBy { it.createdAt })?.let(::listOf).orEmpty()
        else -> matches
    }
}

private fun currentRestoredSeasonStartYear(): Int {
    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    return if (month >= 8) year else year - 1
}

private fun detectMomentTagIntent(words: List<String>): String? = when {
    words.any { it in setOf("goal", "goals", "score", "scores", "scored", "scoring") } -> "Score"
    words.any { it in setOf("assist", "assists") } -> "Assist"
    words.any { it in setOf("save", "saves") } -> "Save"
    words.containsAll(listOf("defensive", "stop")) || words.containsAll(listOf("defensive", "stops")) -> "Defensive Stop"
    words.any { it in setOf("faceoff", "faceoffs") } && words.any { it in setOf("win", "wins", "won") } -> "Faceoff Win"
    words.containsAll(listOf("ground", "ball")) || words.containsAll(listOf("ground", "balls")) -> "Ground Ball"
    else -> null
}

private fun momentTagIntentWords(tag: String): Set<String> = when (tag) {
    "Score" -> setOf("goal", "goals", "score", "scores", "scored", "scoring")
    "Assist" -> setOf("assist", "assists")
    "Save" -> setOf("save", "saves")
    "Defensive Stop" -> setOf("defensive", "stop", "stops")
    "Faceoff Win" -> setOf("faceoff", "faceoffs", "win", "wins", "won")
    "Ground Ball" -> setOf("ground", "ball", "balls")
    else -> emptySet()
}

private fun canonicalMomentTag(tag: String?): String? = when (tag?.trim()?.lowercase(Locale.getDefault())) {
    "score", "goal", "winning goal" -> "Score"
    "assist" -> "Assist"
    "save" -> "Save"
    "defensive stop", "big play" -> "Defensive Stop"
    "faceoff win", "win" -> "Faceoff Win"
    "ground ball" -> "Ground Ball"
    else -> null
}

private fun buildRestoredClipKidLookup(
    context: android.content.Context,
    videos: List<VideoClip>
): Map<String, String?> {
    val kidPrefs = context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE)
    return videos.associate { video ->
        val fileUri = Uri.parse(video.filePath)
        val filePath = fileUri.path
        video.id to (
            kidPrefs.getString(video.id, null)
                ?: kidPrefs.getString(video.filePath, null)
                ?: filePath?.let { kidPrefs.getString(it, null) }
        )?.trim().takeIf { !it.isNullOrBlank() }
    }
}

private fun buildRestoredClipGameSections(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    return videos
        .groupBy { clip -> opponentLookup[clip.id]?.takeIf(String::isNotBlank)?.lowercase() ?: NO_OPPONENT_FILTER_RESTORED }
        .map { (key, groupedVideos) ->
            val sample = groupedVideos.maxByOrNull { it.createdAt } ?: groupedVideos.first()
            val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)
            val displayTitle = opponent?.split(" ")?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) } ?: ""
            val dates = groupedVideos.map { it.gameDate }.distinct().sorted()
            val dateLabel = dates.joinToString(", ") { formatRestoredClipFilterDate(it) }
            VideoLibrarySection(
                key = key,
                title = displayTitle,
                subtitle = dateLabel,
                videos = groupedVideos.sortedByDescending { it.createdAt }
            )
        }
        .sortedByDescending { section -> section.videos.maxOfOrNull { it.createdAt } ?: 0L }
}

private fun buildRestoredClipDateSubSections(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    val sample = videos.maxByOrNull { it.createdAt } ?: return emptyList()
    val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)
    if (opponent == null) {
        return listOf(
            VideoLibrarySection(
                key = NO_OPPONENT_FILTER_RESTORED,
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
                subtitle = formatRestoredClipFilterDate(date),
                videos = groupedVideos.sortedByDescending { it.createdAt }
            )
        }
        .sortedByDescending { section -> section.videos.maxOfOrNull { it.createdAt } ?: 0L }
}

private fun buildRestoredClipListSectionsForAllGames(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>,
    groupByOpponent: Boolean = true,
    reelSortOverride: ((List<VideoClip>) -> List<VideoClip>)? = null
): List<VideoLibrarySection> {
    if (!groupByOpponent) {
        return listOf(
            VideoLibrarySection(
                key = "reels_flat",
                title = "",
                subtitle = "",
                videos = reelSortOverride?.invoke(videos) ?: videos.sortedByDescending { it.createdAt }
            )
        )
    }
    val groupedByOpponent = videos
        .groupBy { clip -> opponentLookup[clip.id]?.takeIf(String::isNotBlank)?.lowercase() ?: NO_OPPONENT_FILTER_RESTORED }
        .entries
        .sortedByDescending { (_, opponentVideos) -> opponentVideos.maxOfOrNull { it.createdAt } ?: 0L }

    return groupedByOpponent.flatMap { (opponentKey, opponentVideos) ->
        val sample = opponentVideos.maxByOrNull { it.createdAt } ?: opponentVideos.first()
        val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)

        if (opponent == null) {
            listOf(
                VideoLibrarySection(
                    key = NO_OPPONENT_FILTER_RESTORED,
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
                        subtitle = formatRestoredClipFilterDate(date),
                        videos = groupedVideos.sortedByDescending { it.createdAt }
                    )
                }
                .sortedByDescending { section -> section.videos.maxOfOrNull { it.createdAt } ?: 0L }
        }
    }
}

private fun formatRestoredClipFilterDate(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date ?: Date())
    } catch (_: Exception) {
        dateString
    }
}

private data class RestoredSeasonOption(
    val key: String,
    val label: String,
    val startYear: Int
)

private fun parseRestoredSeasonKey(gameDate: String): RestoredSeasonOption? {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(gameDate) ?: return null
        val calendar = java.util.Calendar.getInstance().apply { time = parsed }
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val startYear = if (month >= 8) year else year - 1
        val endYear = startYear + 1
        val key = "$startYear-$endYear"
        val label = "$startYear-${String.format(Locale.getDefault(), "%02d", endYear % 100)}"
        RestoredSeasonOption(key = key, label = label, startYear = startYear)
    } catch (_: Exception) {
        null
    }
}

private fun formatRestoredDate(timestamp: Long): String {
    return try {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        "Unknown date"
    }
}

private fun formatRestoredDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

private fun formatRestoredClipDate(gameDate: String): String {
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(gameDate) ?: return gameDate
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed)
    }.getOrDefault(gameDate)
}

private fun resolveClipUriForReel(filePath: String): Uri? {
    val raw = filePath.trim()
    if (raw.isBlank()) return null
    val parsed = Uri.parse(raw)
    return when (parsed.scheme?.lowercase(Locale.getDefault())) {
        "content", "file" -> parsed
        null -> {
            if (raw.startsWith("/")) {
                Uri.fromFile(File(raw))
            } else {
                null
            }
        }
        else -> null
    }
}

private suspend fun buildShareableReelForRefactored(
    context: android.content.Context,
    clipUris: List<Uri>,
    reelTitle: String,
    teamName: String?,
    opponentName: String?
): Uri? {
    if (clipUris.isEmpty()) return null
    return withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "reel_share_exports").apply { mkdirs() }
            val safeTitle = reelTitle
                .replace(Regex("[^a-zA-Z0-9]+"), "_")
                .trim('_')
                .ifBlank { "Spotr_Reel" }
                .take(40)
            val exportedFile = File(exportDir, "Spotr-Reel-$safeTitle-${System.currentTimeMillis()}.mp4")
            val didExport = exportSimpleReelComposition(
                context = context,
                clipUris = clipUris,
                outputFile = exportedFile,
                reelTitle = reelTitle
            )
            if (!didExport || !exportedFile.exists()) {
                Log.w("ClipsScreenRefactored", "Reel export failed for $reelTitle")
                return@withContext null
            }
            val sharedUri = persistReelVideoToMediaStore(context, exportedFile)
            if (sharedUri != null && !teamName.isNullOrBlank()) {
                persistReelMetadataForApp(
                    context = context,
                    reelUri = sharedUri,
                    teamName = teamName,
                    startedAtMs = System.currentTimeMillis(),
                    title = reelTitle,
                    opponentName = opponentName,
                    sourceClipUris = clipUris
                )
            }
            sharedUri
        } catch (t: Throwable) {
            Log.e("ClipsScreenRefactored", "Reel export failed for $reelTitle", t)
            null
        }
    }
}

private suspend fun exportSimpleReelComposition(
    context: android.content.Context,
    clipUris: List<Uri>,
    outputFile: File,
    reelTitle: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val exportDir = outputFile.parentFile ?: return@withContext false
            val introImage = File(exportDir, "intro_${System.currentTimeMillis()}.jpg")
            val startedAt = System.currentTimeMillis()
            val stagedClips = mutableListOf<File>()

            val introBitmap = createRefactoredReelIntroBitmap(reelTitle)
            java.io.FileOutputStream(introImage).use { out ->
                introBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            introBitmap.recycle()

            clipUris.forEachIndexed { index, uri ->
                val stagedClip = stageReelUriToLocalFile(context, uri, exportDir, index) ?: run {
                    Log.w("ClipsScreenRefactored", "Could not stage clip uri: $uri")
                    return@withContext false
                }
                stagedClips += stagedClip
            }

            val ffmpegArgs = mutableListOf<String>()
            ffmpegArgs += listOf(
                "-y",
                "-loop", "1",
                "-t", REEL_INTRO_SECONDS,
                "-i", introImage.absolutePath
            )
            stagedClips.forEach { clipFile ->
                ffmpegArgs += listOf("-i", clipFile.absolutePath)
            }

            val filterParts = mutableListOf<String>()
            filterParts += "[0:v]scale=${REEL_OUTPUT_WIDTH}:${REEL_OUTPUT_HEIGHT}:force_original_aspect_ratio=decrease,pad=${REEL_OUTPUT_WIDTH}:${REEL_OUTPUT_HEIGHT}:(ow-iw)/2:(oh-ih)/2:black,fps=${REEL_OUTPUT_FPS},format=yuv420p[v0]"
            stagedClips.indices.forEach { idx ->
                val inputIndex = idx + 1
                filterParts += "[$inputIndex:v]scale=${REEL_OUTPUT_WIDTH}:${REEL_OUTPUT_HEIGHT}:force_original_aspect_ratio=decrease,pad=${REEL_OUTPUT_WIDTH}:${REEL_OUTPUT_HEIGHT}:(ow-iw)/2:(oh-ih)/2:black,fps=${REEL_OUTPUT_FPS},format=yuv420p[v$inputIndex]"
            }
            val concatInputs = (0..stagedClips.size).joinToString(separator = "") { "[v$it]" }
            filterParts += "$concatInputs concat=n=${stagedClips.size + 1}:v=1:a=0[vout]"

            ffmpegArgs += listOf(
                "-filter_complex", filterParts.joinToString(";"),
                "-map", "[vout]"
            )
            ffmpegArgs += listOf(
                "-c:v", "mpeg4",
                "-q:v", "10",
                "-threads", "2",
                "-movflags", "+faststart",
                outputFile.absolutePath
            )

            val session = FFmpegKit.executeWithArguments(ffmpegArgs.toTypedArray())
            val success = ReturnCode.isSuccess(session.returnCode)
            val elapsed = System.currentTimeMillis() - startedAt
            Log.d("ClipsScreenRefactored", "MAKE_REEL_FFMPEG_DONE success=$success elapsedMs=$elapsed")
            if (!success) {
                Log.w("ClipsScreenRefactored", "FFmpeg reel render failed: ${session.allLogsAsString}")
            }
            success && outputFile.exists()
        } catch (t: Throwable) {
            Log.e("ClipsScreenRefactored", "FFmpeg reel export crashed", t)
            false
        }
    }
}

private fun createRefactoredReelIntroBitmap(reelTitle: String): Bitmap {
    val width = REEL_OUTPUT_WIDTH
    val height = REEL_OUTPUT_HEIGHT
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#0A1118") }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#1C8CFF") }
    canvas.drawRect(0f, 0f, width.toFloat(), 18f, stripePaint)
    canvas.drawRect(0f, height - 18f, width.toFloat(), height.toFloat(), stripePaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#9FC9FF")
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    val safeTitle = reelTitle.trim().ifBlank { "Spotr Reel" }
    canvas.drawText("HIGHLIGHT REEL", width / 2f, (height / 2f) - 56f, subtitlePaint)
    drawCenteredMultiline(canvas, safeTitle, width / 2f, height / 2f + 10f, titlePaint, maxCharsPerLine = 18)
    canvas.drawText("Created with Spotr", width / 2f, height - 84f, subtitlePaint)

    return bitmap
}

private fun drawCenteredMultiline(
    canvas: Canvas,
    text: String,
    centerX: Float,
    startY: Float,
    paint: Paint,
    maxCharsPerLine: Int
) {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isBlank()) word else "$current $word"
        if (candidate.length <= maxCharsPerLine) {
            current = candidate
        } else {
            if (current.isNotBlank()) lines += current
            current = word
        }
    }
    if (current.isNotBlank()) lines += current
    val finalLines = lines.take(3)
    val lineHeight = paint.textSize * 1.2f
    finalLines.forEachIndexed { idx, line ->
        canvas.drawText(line, centerX, startY + idx * lineHeight, paint)
    }
}

private fun persistReelMetadataForApp(
    context: android.content.Context,
    reelUri: Uri,
    teamName: String,
    startedAtMs: Long,
    title: String,
    opponentName: String?,
    sourceClipUris: List<Uri>
) {
    val uriString = reelUri.toString()
    val startPrefs = context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
    val kidPrefs = context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE)

    val sourceStartTimes = sourceClipUris.mapNotNull { sourceUri ->
        val sourceUriString = sourceUri.toString()
        val sourceFilePath = sourceUri.path
        listOfNotNull(
            startPrefs.getLong(sourceUriString, 0L).takeIf { it > 0L },
            sourceFilePath?.let { path -> startPrefs.getLong(path, 0L).takeIf { it > 0L } }
        ).firstOrNull()
    }
    val resolvedStartedAtMs = sourceStartTimes.minOrNull() ?: startedAtMs

    val resolvedKidName = sourceClipUris
        .asSequence()
        .mapNotNull { sourceUri ->
            val sourceUriString = sourceUri.toString()
            val sourceFilePath = sourceUri.path
            kidPrefs.getString(sourceUriString, null)
                ?: sourceFilePath?.let { path -> kidPrefs.getString(path, null) }
        }
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }

    context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(uriString, teamName)
        .apply()
    startPrefs.edit().putLong(uriString, resolvedStartedAtMs).apply()
    context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(uriString, "Reel: $title")
        .apply()
    if (!resolvedKidName.isNullOrBlank()) {
        kidPrefs.edit().putString(uriString, resolvedKidName).apply()
    }
    if (!opponentName.isNullOrBlank()) {
        context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(uriString, opponentName)
            .apply()
    }

    val metadataPrefs = context.getSharedPreferences(REEL_METADATA_PREFS, android.content.Context.MODE_PRIVATE)
    val sourceArray = JSONArray()
    sourceClipUris.forEach { sourceArray.put(it.toString()) }
    val payload = JSONObject()
        .put("reelUri", uriString)
        .put("team", teamName)
        .put("title", title)
        .put("opponent", opponentName ?: "")
        .put("createdAt", resolvedStartedAtMs)
        .put("sourceClipUris", sourceArray)
    metadataPrefs.edit().putString(uriString, payload.toString()).apply()
}

private fun buildCollectionName(
    selectedKid: String?,
    selectedSeasonKey: String?,
    selectedOpponentKey: String?,
    reelsOnly: Boolean
): String {
    val parts = mutableListOf<String>()
    if (reelsOnly) parts += "Reels"
    selectedKid?.takeIf { it.isNotBlank() }?.let { parts += it }
    selectedSeasonKey?.takeIf { it.isNotBlank() }?.let { parts += it }
    selectedOpponentKey?.takeIf { it.isNotBlank() }?.let { parts += it }
    return if (parts.isEmpty()) "My View" else parts.joinToString(" · ")
}

private fun loadReelFilterCollections(
    context: android.content.Context,
    teamName: String
): List<ReelFilterCollection> {
    val prefs = context.getSharedPreferences(REEL_COLLECTION_PREFS, android.content.Context.MODE_PRIVATE)
    val raw = prefs.getString(teamName, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    ReelFilterCollection(
                        name = item.optString("name", "My View"),
                        kid = item.optString("kid", "").ifBlank { null },
                        seasonKey = item.optString("seasonKey", "").ifBlank { null },
                        opponentKey = item.optString("opponentKey", "").ifBlank { null },
                        reelsOnly = item.optBoolean("reelsOnly", false)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun upsertReelFilterCollection(
    context: android.content.Context,
    teamName: String,
    collection: ReelFilterCollection
): List<ReelFilterCollection> {
    val existing = loadReelFilterCollections(context, teamName).toMutableList()
    val index = existing.indexOfFirst { it.name.equals(collection.name, ignoreCase = true) }
    if (index >= 0) {
        existing[index] = collection
    } else {
        existing += collection
    }
    val payload = JSONArray()
    existing.forEach { item ->
        payload.put(
            JSONObject()
                .put("name", item.name)
                .put("kid", item.kid ?: "")
                .put("seasonKey", item.seasonKey ?: "")
                .put("opponentKey", item.opponentKey ?: "")
                .put("reelsOnly", item.reelsOnly)
        )
    }
    context.getSharedPreferences(REEL_COLLECTION_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(teamName, payload.toString())
        .apply()
    return existing
}

private fun resolveReelUriToPath(context: android.content.Context, uri: Uri): String? {
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

private fun stageReelUriToLocalFile(
    context: android.content.Context,
    uri: Uri,
    exportDir: File,
    index: Int
): File? {
    if (uri.scheme == "file") {
        val existing = uri.path?.let { File(it) }
        if (existing != null && existing.exists()) return existing
    }

    val resolvedPath = resolveReelUriToPath(context, uri)
    if (!resolvedPath.isNullOrBlank()) {
        val file = File(resolvedPath)
        if (file.exists()) return file
    }

    return runCatching {
        val staged = File(exportDir, "clip_src_${index}_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(staged).use { output ->
                input.copyTo(output)
            }
        } ?: return null
        staged
    }.getOrNull()
}

private suspend fun trimClipToNewVideo(
    context: android.content.Context,
    clip: VideoClip,
    startMs: Long,
    endMs: Long
): Uri? {
    if (endMs <= startMs) return null
    return withContext(Dispatchers.IO) {
        var staged: File? = null
        var output: File? = null
        try {
            val workDir = File(context.cacheDir, "clip_trim_exports").apply { mkdirs() }
            val sourceUri = resolveClipUriForReel(clip.filePath) ?: return@withContext null
            staged = stageReelUriToLocalFile(context, sourceUri, workDir, 0) ?: return@withContext null
            output = File(workDir, "Spotr-Trim-${System.currentTimeMillis()}.mp4")

            val args = arrayOf(
                "-y",
                "-ss", String.format(Locale.US, "%.3f", startMs / 1000.0),
                "-i", staged.absolutePath,
                "-t", String.format(Locale.US, "%.3f", (endMs - startMs) / 1000.0),
                "-c:v", "mpeg4",
                "-q:v", "10",
                "-threads", "2",
                "-c:a", "aac",
                "-movflags", "+faststart",
                output.absolutePath
            )
            val session = FFmpegKit.executeWithArguments(args)
            if (!ReturnCode.isSuccess(session.returnCode) || !output.exists()) {
                Log.w(
                    "ClipsScreenRefactored",
                    "TRIM_FAILED rc=${session.returnCode}: ${session.allLogsAsString}"
                )
                return@withContext null
            }
            persistReelVideoToMediaStore(context, output)
        } catch (t: Throwable) {
            Log.e("ClipsScreenRefactored", "Trim failed", t)
            null
        } finally {
            staged?.delete()
            output?.delete()
        }
    }
}

private fun copyClipMetadataForTrim(
    context: android.content.Context,
    source: VideoClip,
    trimmedUri: Uri,
    fallbackTeamName: String?
) {
    val uriString = trimmedUri.toString()
    fun prefs(name: String) = context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)

    val teamPrefs = prefs("video_team_names")
    val team = teamPrefs.getString(source.id, null)
        ?: teamPrefs.getString(source.filePath, null)
        ?: fallbackTeamName
    if (!team.isNullOrBlank()) {
        teamPrefs.edit().putString(uriString, team).apply()
    }

    val startPrefs = prefs("video_start_times")
    val startedAt = startPrefs.getLong(source.id, 0L).takeIf { it > 0L }
        ?: startPrefs.getLong(source.filePath, 0L).takeIf { it > 0L }
        ?: source.createdAt
    startPrefs.edit().putLong(uriString, startedAt).apply()

    val kidPrefs = prefs("video_kid_names")
    (kidPrefs.getString(source.id, null) ?: kidPrefs.getString(source.filePath, null))
        ?.takeIf { it.isNotBlank() }
        ?.let { kidPrefs.edit().putString(uriString, it).apply() }

    val opponentPrefs = prefs("video_opponent_names")
    (opponentPrefs.getString(source.id, null) ?: opponentPrefs.getString(source.filePath, null))
        ?.takeIf { it.isNotBlank() }
        ?.let { opponentPrefs.edit().putString(uriString, it).apply() }

    source.momentTag?.takeIf { it.isNotBlank() && !it.startsWith("Reel:", ignoreCase = true) }?.let {
        prefs("video_custom_names").edit().putString(uriString, it).apply()
    }
}

private fun persistReelVideoToMediaStore(context: android.content.Context, file: File): Uri? {
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

