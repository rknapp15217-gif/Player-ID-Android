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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.playerid.app.data.VideoClip
import com.playerid.app.reelVideoRelativePathsForRestoredClips
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
private val PlaysAccentColor = Color(0xFFFF6B5B)
private val RestoredSwipeActionWidth = 118.dp
private const val CLIP_COMMENTARY_PREFS = "video_clip_commentary"
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
    val kidOptions by teamViewModel.kidOptions.collectAsState()
    val scope = rememberCoroutineScope()

    var localTeamName by remember { mutableStateOf<String?>(null) }
    var clips by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showFiltersMenu by remember { mutableStateOf(false) }
    val filterListState = rememberLazyListState()
    var expandedKidSection by remember { mutableStateOf(false) }
    var expandedSeasonSection by remember { mutableStateOf(false) }
    var expandedTeamSection by remember { mutableStateOf(false) }
    var expandedOpponentSection by remember { mutableStateOf(false) }
    var pendingRevealSection by remember { mutableStateOf<String?>(null) }
    val playerOptionsRequester = remember { BringIntoViewRequester() }
    val seasonOptionsRequester = remember { BringIntoViewRequester() }
    val teamOptionsRequester = remember { BringIntoViewRequester() }
    val opponentOptionsRequester = remember { BringIntoViewRequester() }
    var selectedKid by remember(localTeamName) { mutableStateOf<String?>(null) }
    var selectedSeasonKey by remember(localTeamName) { mutableStateOf<String?>(null) }
    var selectedOpponentKey by remember(localTeamName) { mutableStateOf<String?>(null) }
    var showReelsOnly by remember(localTeamName) { mutableStateOf(false) }
    var savedCollections by remember(localTeamName) { mutableStateOf<List<ReelFilterCollection>>(emptyList()) }
    var showSaveCollectionDialog by remember { mutableStateOf(false) }
    var collectionNameInput by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoToDelete by remember { mutableStateOf<VideoClip?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isBuildingReel by remember { mutableStateOf(false) }
    var showReelPickerDialog by remember { mutableStateOf(false) }
    var showReelsMenu by remember { mutableStateOf(false) }
    var reelSelectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reelNameInput by remember { mutableStateOf("") }
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

    LaunchedEffect(localTeamName) {
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
        savedCollections = loadReelFilterCollections(context, teamName)
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
    val availableKidOptions = remember(kidOptions, clips, kidLookup) {
        val lookupKids = clips
            .mapNotNull { clip -> kidLookup[clip.id] }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
        if (kidOptions.isEmpty()) lookupKids else kidOptions
    }
    val kidFilter = selectedKid?.trim().orEmpty()
    val kidScopedVideos = remember(clips, kidLookup, kidFilter) {
        if (kidFilter.isEmpty()) {
            clips
        } else {
            clips.filter { clip ->
                kidLookup[clip.id]?.equals(kidFilter, ignoreCase = true) == true
            }
        }
    }
    val availableSeasons = remember(kidScopedVideos) {
        kidScopedVideos
            .mapNotNull { clip -> parseRestoredSeasonKey(clip.gameDate) }
            .distinctBy { it.key }
            .sortedByDescending { it.startYear }
    }
    val seasonScopedVideos = remember(kidScopedVideos, selectedSeasonKey) {
        if (selectedSeasonKey == null) {
            kidScopedVideos
        } else {
            kidScopedVideos.filter { clip ->
                parseRestoredSeasonKey(clip.gameDate)?.key == selectedSeasonKey
            }
        }
    }
    val availableOpponents = remember(seasonScopedVideos, opponentLookup) {
        seasonScopedVideos
            .groupBy { clip ->
                opponentLookup[clip.id]
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.lowercase()
                    ?: NO_OPPONENT_FILTER_RESTORED
            }
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
    val opponentScopedVideos = remember(seasonScopedVideos, opponentLookup, selectedOpponentKey) {
        if (selectedOpponentKey == null) {
            seasonScopedVideos
        } else {
            seasonScopedVideos.filter { clip ->
                val opponentKey = opponentLookup[clip.id]
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.lowercase()
                    ?: NO_OPPONENT_FILTER_RESTORED
                opponentKey == selectedOpponentKey
            }
        }
    }
    val reelScopedVideos = remember(clips, opponentScopedVideos, showReelsOnly) {
        fun isReelClip(clip: VideoClip): Boolean {
            return clip.momentTag?.startsWith("Reel:", ignoreCase = true) == true ||
                clip.gameTitle.startsWith("Spotr-Reel", ignoreCase = true) ||
                clip.gameTitle.startsWith("Reel:", ignoreCase = true)
        }

        if (!showReelsOnly) {
            opponentScopedVideos
        } else {
            val filteredReels = opponentScopedVideos.filter(::isReelClip)
            if (filteredReels.isNotEmpty()) {
                filteredReels
            } else {
                // If active filters hide reels, show team reels so the tab does not appear empty.
                clips.filter(::isReelClip)
            }
        }
    }
    val visibleSections = remember(reelScopedVideos, opponentLookup) {
        buildRestoredClipListSectionsForAllGames(reelScopedVideos, opponentLookup)
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

    LaunchedEffect(pendingRevealSection) {
        val sectionToReveal = pendingRevealSection ?: return@LaunchedEffect
        pendingRevealSection = null
        delay(150)
        when (sectionToReveal) {
            "player" -> playerOptionsRequester.bringIntoView()
            "season" -> seasonOptionsRequester.bringIntoView()
            "team" -> teamOptionsRequester.bringIntoView()
            "opponent" -> opponentOptionsRequester.bringIntoView()
        }
    }

    selectedVideoUri?.let { videoUri ->
        VideoPlaybackScreen(
            videoUri = videoUri,
            detectedPlayers = emptyList(),
            onNavigateBack = { selectedVideoUri = null }
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
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedSeasonKey != null || selectedOpponentKey != null || selectedKid != null,
                    onClick = {
                        expandedKidSection = false
                        expandedSeasonSection = false
                        expandedTeamSection = false
                        expandedOpponentSection = false
                        showFiltersMenu = true
                    },
                    label = { Text("Find Memories", fontWeight = FontWeight.SemiBold) },
                    trailingIcon = {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PlaysAccentColor.copy(alpha = 0.12f),
                        selectedLabelColor = PlaysAccentColor,
                        selectedTrailingIconColor = PlaysAccentColor
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        FilterChip(
                            selected = showReelsOnly || showReelPickerDialog,
                            enabled = !isBuildingReel,
                            onClick = { showReelsMenu = true },
                            label = {
                                Text(
                                    when {
                                        showReelPickerDialog -> "Create reel (${reelSelectionIds.size})"
                                        showReelsOnly -> "Watching reels"
                                        else -> "Reels"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PlaysAccentColor.copy(alpha = 0.12f),
                                selectedLabelColor = PlaysAccentColor,
                                selectedLeadingIconColor = PlaysAccentColor,
                                selectedTrailingIconColor = PlaysAccentColor
                            )
                        )

                        DropdownMenu(
                            expanded = showReelsMenu,
                            onDismissRequest = { showReelsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Watch") },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                },
                                onClick = {
                                    showReelsOnly = !showReelsOnly
                                    showReelPickerDialog = false
                                    showReelsMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Create") },
                                leadingIcon = {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                                },
                                onClick = {
                                    if (visibleVideos.isEmpty()) {
                                        Toast.makeText(context, "No clips match current filters", Toast.LENGTH_SHORT).show()
                                        showReelsMenu = false
                                        return@DropdownMenuItem
                                    }
                                    reelSelectionIds = emptySet()
                                    reelNameInput = buildCollectionName(
                                        selectedKid = selectedKid,
                                        selectedSeasonKey = selectedSeasonKey,
                                        selectedOpponentKey = selectedOpponentKey,
                                        reelsOnly = false
                                    )
                                    showReelPickerDialog = true
                                    showReelsOnly = false
                                    showReelsMenu = false
                                }
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = PlaysAccentColor.copy(alpha = 0.12f),
                        onClick = { uploadLauncher.launch(arrayOf("video/*")) }
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
            }

            if (savedCollections.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    savedCollections.forEach { collection ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                selectedKid = collection.kid
                                selectedSeasonKey = collection.seasonKey
                                selectedOpponentKey = collection.opponentKey
                                showReelsOnly = collection.reelsOnly
                            },
                            label = { Text(collection.name) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val suggested = buildCollectionName(
                            selectedKid = selectedKid,
                            selectedSeasonKey = selectedSeasonKey,
                            selectedOpponentKey = selectedOpponentKey,
                            reelsOnly = showReelsOnly
                        )
                        collectionNameInput = suggested
                        showSaveCollectionDialog = true
                    }
                ) {
                    Text("Save View")
                }
            }

        }

        if (showSaveCollectionDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSaveCollectionDialog = false },
                title = { Text("Save smart collection") },
                text = {
                    OutlinedTextField(
                        value = collectionNameInput,
                        onValueChange = { collectionNameInput = it },
                        label = { Text("Collection name") },
                        singleLine = true
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showSaveCollectionDialog = false }) {
                        Text("Cancel")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val teamName = localTeamName
                            val name = collectionNameInput.trim()
                            if (teamName.isNullOrBlank() || name.isEmpty()) {
                                showSaveCollectionDialog = false
                                return@TextButton
                            }
                            val updated = upsertReelFilterCollection(
                                context = context,
                                teamName = teamName,
                                collection = ReelFilterCollection(
                                    name = name,
                                    kid = selectedKid,
                                    seasonKey = selectedSeasonKey,
                                    opponentKey = selectedOpponentKey,
                                    reelsOnly = showReelsOnly
                                )
                            )
                            savedCollections = updated
                            showSaveCollectionDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            )
        }

        if (showReelPickerDialog) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 6.dp, bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 2.dp,
                color = PlaysAccentColor.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Build a highlight reel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${reelSelectionIds.size} clips",
                            style = MaterialTheme.typography.labelLarge,
                            color = PlaysAccentColor
                        )
                    }

                    OutlinedTextField(
                        value = reelNameInput,
                        onValueChange = { reelNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reel name") },
                        placeholder = { Text("Game highlights") },
                        singleLine = true,
                        enabled = !isBuildingReel
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(
                                onClick = { reelSelectionIds = visibleVideos.map { it.id }.toSet() },
                                enabled = !isBuildingReel
                            ) {
                                Text("All")
                            }
                            TextButton(
                                onClick = { reelSelectionIds = emptySet() },
                                enabled = !isBuildingReel
                            ) {
                                Text("Clear")
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        TextButton(
                            onClick = { showReelPickerDialog = false },
                            enabled = !isBuildingReel
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val selectedTeamName = localTeamName
                                val selectedIds = reelSelectionIds
                                if (selectedTeamName.isNullOrBlank()) {
                                    Toast.makeText(context, "Select a team first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedIds.isEmpty()) {
                                    Toast.makeText(context, "Select at least one clip", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val reelName = reelNameInput.trim().ifBlank { "Highlight reel" }

                                val selectedReelClips = visibleVideos.filter { it.id in selectedIds }
                                if (selectedReelClips.isEmpty()) {
                                    Toast.makeText(context, "Select at least one clip", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                scope.launch {
                                    isBuildingReel = true
                                    try {
                                        Log.d("ClipsScreenRefactored", "MAKE_REEL_START selected=${selectedReelClips.size}")
                                        Toast.makeText(context, "Preparing reel for sharing...", Toast.LENGTH_SHORT).show()

                                        val reelUris = selectedReelClips.mapNotNull { clip ->
                                            resolveClipUriForReel(clip.filePath)
                                        }
                                        Log.d("ClipsScreenRefactored", "MAKE_REEL_URI_RESOLVED requested=${selectedReelClips.size} resolved=${reelUris.size}")
                                        if (reelUris.size != selectedReelClips.size) {
                                            Toast.makeText(
                                                context,
                                                "Some selected clips are unavailable. Re-select clips and try again.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@launch
                                        }
                                        val reelOpponents = selectedReelClips
                                            .mapNotNull { clip -> opponentLookup[clip.id]?.trim()?.takeIf(String::isNotEmpty) }
                                            .distinctBy { it.lowercase() }
                                        val suggestedReelTitle = if (reelOpponents.size == 1) {
                                            "$selectedTeamName vs ${reelOpponents.first()}"
                                        } else {
                                            "$selectedTeamName Custom Reel"
                                        }
                                        val reelTitle = if (reelName == "Highlight reel") suggestedReelTitle else reelName
                                        val scenario = if (reelOpponents.size == 1) "opponent" else "top_plays"

                                        var timedOut = false
                                        val sharedReelUri = withTimeoutOrNull(REEL_BUILD_TIMEOUT_MS) {
                                            buildShareableReelForRefactored(
                                                context = context,
                                                clipUris = reelUris,
                                                reelTitle = reelTitle,
                                                teamName = selectedTeamName,
                                                opponentName = reelOpponents.firstOrNull(),
                                                scenario = scenario
                                            )
                                        } ?: run {
                                            timedOut = true
                                            null
                                        }
                                        Log.d("ClipsScreenRefactored", "MAKE_REEL_BUILD_RESULT uri=$sharedReelUri")

                                        if (sharedReelUri != null) {
                                            localTeamName?.let { teamName ->
                                                clips = loadRestoredClips(context, teamName)
                                            }
                                            launchPersonalShareChooser(
                                                context = context,
                                                videoUri = sharedReelUri,
                                                shareTitle = "Share highlight reel"
                                            )
                                            Toast.makeText(
                                                context,
                                                "Reel saved to Movies/Spotr",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.d("ClipsScreenRefactored", "MAKE_REEL_SHARE_LAUNCHED uri=$sharedReelUri")
                                        } else if (timedOut) {
                                            Toast.makeText(
                                                context,
                                                "Reel build timed out. Try fewer clips.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(context, "Unable to build reel video. Please try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (ce: CancellationException) {
                                        Log.w("ClipsScreenRefactored", "MAKE_REEL_CANCELLED", ce)
                                        throw ce
                                    } catch (t: Throwable) {
                                        Log.e("ClipsScreenRefactored", "Make Reel failed", t)
                                        Toast.makeText(
                                            context,
                                            "Reel creation failed. Please try again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isBuildingReel = false
                                        showReelPickerDialog = false
                                    }
                                }
                            },
                            enabled = !isBuildingReel,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(if (isBuildingReel) "Building..." else "Create & share")
                        }
                        }
                    }
                }
            }
        }

        if (showFiltersMenu) {
            val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        showFiltersMenu = false
                    }
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(screenHeightDp * 0.75f),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    LazyColumn(
                        state = filterListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        item(key = "filters_title") {
                            Text(
                                "Filters",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        item(key = "header_player") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                onClick = {
                                    val willExpand = !expandedKidSection
                                    expandedKidSection = willExpand
                                    if (willExpand) {
                                        pendingRevealSection = "player"
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Player", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = PlaysAccentColor)
                                    Icon(
                                        if (expandedKidSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PlaysAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (expandedKidSection) {
                            item(key = "player_options") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(playerOptionsRequester)
                                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RestoredFilterOption(
                                        label = "All players",
                                        selected = selectedKid == null,
                                        onClick = { selectedKid = null }
                                    )
                                    if (availableKidOptions.isEmpty()) {
                                        RestoredFilterOption(
                                            label = "No players available",
                                            selected = false,
                                            onClick = {}
                                        )
                                    }
                                    availableKidOptions.forEach { kidName ->
                                        RestoredFilterOption(
                                            label = kidName,
                                            selected = selectedKid.equals(kidName, ignoreCase = true),
                                            onClick = {
                                                selectedKid = kidName
                                                localTeamName?.let { teamViewModel.selectKidForTeam(it, kidName) }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "header_season") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                onClick = {
                                    val willExpand = !expandedSeasonSection
                                    expandedSeasonSection = willExpand
                                    if (willExpand) {
                                        pendingRevealSection = "season"
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Season", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = PlaysAccentColor)
                                    Icon(
                                        if (expandedSeasonSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PlaysAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (expandedSeasonSection) {
                            item(key = "season_options") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(seasonOptionsRequester)
                                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RestoredFilterOption(
                                        label = "All seasons",
                                        selected = selectedSeasonKey == null,
                                        onClick = { selectedSeasonKey = null }
                                    )
                                    if (availableSeasons.isEmpty()) {
                                        RestoredFilterOption(
                                            label = "No seasons available",
                                            selected = false,
                                            onClick = {}
                                        )
                                    }
                                    availableSeasons.forEach { season ->
                                        RestoredFilterOption(
                                            label = season.label,
                                            selected = selectedSeasonKey == season.key,
                                            onClick = { selectedSeasonKey = season.key }
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "header_team") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                onClick = {
                                    val willExpand = !expandedTeamSection
                                    expandedTeamSection = willExpand
                                    if (willExpand) {
                                        pendingRevealSection = "team"
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Team", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = PlaysAccentColor)
                                    Icon(
                                        if (expandedTeamSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PlaysAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (expandedTeamSection) {
                            item(key = "team_options") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(teamOptionsRequester)
                                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (subscribedTeams.isEmpty()) {
                                        RestoredFilterOption(
                                            label = "No teams available",
                                            selected = false,
                                            onClick = {}
                                        )
                                    }
                                    subscribedTeams.forEach { team ->
                                        RestoredFilterOption(
                                            label = team.name,
                                            selected = team.name == localTeamName,
                                            onClick = {
                                                localTeamName = team.name
                                                selectedKid = teamViewModel.getSelectedKidForTeam(team.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "header_opponent") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                onClick = {
                                    val willExpand = !expandedOpponentSection
                                    expandedOpponentSection = willExpand
                                    if (willExpand) {
                                        pendingRevealSection = "opponent"
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Opponent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = PlaysAccentColor)
                                    Icon(
                                        if (expandedOpponentSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PlaysAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (expandedOpponentSection) {
                            item(key = "opponent_options") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(opponentOptionsRequester)
                                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RestoredFilterOption(
                                        label = "All opponents",
                                        selected = selectedOpponentKey == null,
                                        onClick = { selectedOpponentKey = null }
                                    )
                                    if (availableOpponents.isEmpty()) {
                                        RestoredFilterOption(
                                            label = "No opponents available",
                                            selected = false,
                                            onClick = {}
                                        )
                                    }
                                    availableOpponents.forEach { opponent ->
                                        RestoredFilterOption(
                                            label = opponent.label,
                                            selected = selectedOpponentKey == opponent.key,
                                            onClick = { selectedOpponentKey = opponent.key }
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "filters_bottom_spacer") {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading clips...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            localTeamName.isNullOrBlank() -> {
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
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    visibleSections.forEach { section ->
                        item(key = "header_${section.key}") {
                            if (section.title.isNotBlank() || section.subtitle.isNotBlank()) {
                                Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    if (section.title.isNotBlank()) {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (section.subtitle.isNotBlank()) {
                                        Text(
                                            text = section.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    onOpen = {
                                        if (reelModeEnabled) {
                                            toggleReelClipSelection(clip.id)
                                        } else {
                                            selectedVideoUri = Uri.parse(clip.filePath)
                                        }
                                    },
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
                                    }
                                )

                                if (showReelPickerDialog) {
                                    val selected = reelSelectionIds.contains(clip.id)
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(10.dp),
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.88f),
                                        tonalElevation = 4.dp,
                                        shadowElevation = 4.dp,
                                        onClick = {
                                            toggleReelClipSelection(clip.id)
                                        }
                                    ) {
                                        Checkbox(
                                            checked = selected,
                                            onCheckedChange = { checked ->
                                                reelSelectionIds = if (checked == selected) {
                                                    reelSelectionIds
                                                } else if (checked) {
                                                    reelSelectionIds + clip.id
                                                } else {
                                                    reelSelectionIds - clip.id
                                                }
                                            }
                                        )
                                    }
                                }
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

@Composable
private fun RestoredFilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = if (selected) PlaysAccentColor.copy(alpha = 0.12f) else Color.Transparent,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) PlaysAccentColor else MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun momentTagConfig(tag: String?): Triple<ImageVector, Color, String> {
    val normalized = tag?.trim().orEmpty()
    return when {
        normalized.equals("Goal", ignoreCase = true) -> Triple(Icons.Default.Favorite, Color(0xFF2E7D32), "Goal")
        normalized.equals("Save", ignoreCase = true) -> Triple(Icons.Default.Shield, Color(0xFF6A1B9A), "Save")
        normalized.equals("Faceoff Win", ignoreCase = true) -> Triple(Icons.Default.EmojiEvents, Color(0xFFF9A825), "Faceoff Win")
        normalized.equals("Assist", ignoreCase = true) -> Triple(Icons.Default.Share, Color(0xFF1565C0), "Assist")
        normalized.equals("Defensive Stop", ignoreCase = true) -> Triple(Icons.Default.Star, Color(0xFFE65100), "Defensive Stop")
        normalized.equals("Win", ignoreCase = true) || normalized.equals("Winning Goal", ignoreCase = true) -> Triple(Icons.Default.EmojiEvents, Color(0xFFF9A825), "Faceoff Win")
        normalized.equals("Big Play", ignoreCase = true) -> Triple(Icons.Default.Star, Color(0xFFE65100), "Defensive Stop")
        normalized.isNotEmpty() -> Triple(Icons.Default.PlayArrow, MaterialTheme.colorScheme.onSurfaceVariant, normalized)
        else -> Triple(Icons.Default.PlayArrow, MaterialTheme.colorScheme.onSurfaceVariant, "Tag")
    }
}

@Composable
private fun RestoredClipRow(
    clip: VideoClip,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleHighlight: () -> Unit,
    onTagChanged: (String?) -> Unit
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
    val tagOptions = listOf(
        "Goal",
        "Save",
        "Faceoff Win",
        "Assist",
        "Defensive Stop"
    )

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
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
                    clip.momentTag?.takeIf { it.isNotBlank() }?.let { tag ->
                        val (tagIcon, tagColor, tagLabel) = momentTagConfig(tag)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.42f))
                                .clickable { showTagPicker = true }
                                .padding(horizontal = 15.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Icon(
                                    imageVector = tagIcon,
                                    contentDescription = tagLabel,
                                    tint = tagColor,
                                    modifier = Modifier.size(39.dp)
                                )
                                Text(
                                    text = tagLabel,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isCommentaryExpanded = !isCommentaryExpanded },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isCommentaryExpanded) "Collapse commentary" else "Expand commentary",
                            tint = if (isCommentaryExpanded || commentary.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onToggleHighlight, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Toggle highlight",
                            tint = if (clip.isHighlight) PlaysAccentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isCommentaryExpanded || commentary.isNotBlank()) {
                    if (isCommentaryExpanded) {
                        val keyboardController = LocalSoftwareKeyboardController.current
                        OutlinedTextField(
                            value = commentary,
                            onValueChange = { updated ->
                                commentary = updated
                                persistClipCommentary(context, clip, updated)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            label = { Text("Commentary") },
                            placeholder = { Text("Add notes about this clip") },
                            minLines = 2,
                            maxLines = 4,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    isCommentaryExpanded = false
                                }
                            )
                        )
                    } else {
                        Text(
                            text = commentary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (showTagPicker) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTagPicker = false },
                title = { Text("Edit tag") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tagOptions.forEach { label ->
                            val isSelected = clip.momentTag?.equals(label, ignoreCase = true) == true
                            TextButton(
                                onClick = {
                                    onTagChanged(if (isSelected) null else label)
                                    showTagPicker = false
                                },
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
                        if (!clip.momentTag.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    onTagChanged(null)
                                    showTagPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Clear tag")
                            }
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
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
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
    opponentName: String?,
    scenario: String
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
                "-map", "[vout]",
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

