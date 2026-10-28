@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.playerid.app.ui.screens

import android.Manifest
import android.app.Activity
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
import android.speech.RecognizerIntent
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.playerid.app.data.VideoClip
import com.playerid.app.data.Player
import com.playerid.app.BuildConfig
import com.playerid.app.domain.team.MemoryResultSelection
import com.playerid.app.data.DetectionResultSerializer
import com.playerid.app.data.PlayerDatabase
import com.playerid.app.domain.team.MemorySeasonIntent
import com.playerid.app.domain.team.parseMemorySearchQuery
import com.playerid.app.domain.team.LibraryActionRequest
import com.playerid.app.domain.team.ActionScheduleGame
import com.playerid.app.domain.team.parseLibraryActionRequest
import com.playerid.app.domain.team.scheduleQuestionAnswer
import com.playerid.app.domain.team.ScheduleQuestionAnswer
import com.playerid.app.domain.team.LibraryNavigationRequest
import com.playerid.app.domain.team.LibraryDestination
import com.playerid.app.domain.team.parseLibraryNavigation
import com.playerid.app.domain.team.resolveLibraryNavigation
import com.playerid.app.domain.team.AskPlayloomQuery
import com.playerid.app.domain.team.interpretAskPlayloom
import com.playerid.app.domain.team.resolveMemoryPlayerAssignment
import com.playerid.app.domain.team.parseMemoryQuestion
import com.playerid.app.domain.team.answerMemoryQuestion
import com.playerid.app.domain.team.withSelectedSeasonYear
import com.playerid.app.domain.team.answerRosterQuestion
import com.playerid.app.data.repositories.toProfile
import com.playerid.app.ui.components.AskPlayloomResults
import com.playerid.app.ui.components.AskPlayloomAnswer
import com.playerid.app.ui.components.CaptureLibrarySearchBar
import com.playerid.app.ui.components.PlayloomGoPage
import androidx.compose.runtime.MutableState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.playerid.app.ui.components.rememberMemoryFilterScroll
import com.playerid.app.domain.team.matchesScheduleOpponent
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Groups
import com.playerid.app.reelVideoRelativePathsForRestoredClips
import com.playerid.app.ui.icons.CrossedLacrosseSticksIcon
import com.playerid.app.ui.icons.GroundBallIcon
import com.playerid.app.ui.icons.StopSignIcon
import com.playerid.app.viewmodels.TeamViewModel
import com.playerid.app.viewmodels.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
private const val PROVIDE_OPPONENT_HEADER = "Provide Opponent"
private const val TEAMMATE_REVIEW_PREFS = "teammate_clip_reviews_v1"
private const val TEAMMATE_BANNER_DISMISSED_BATCH = "dismissed_banner_batch"
private const val TEAMMATE_DEMO_REVIEW_ID = "debug_teammate_review_demo_v6"
private const val SHARE_DEMO_CLIP_ID = "debug_share_feature_demo_v1"
private val SHARE_DEMO_DETECTED_PLAYER_IDS = setOf("debug_player_12", "debug_player_7", "debug_player_21")
private const val RECENT_CLIP_WINDOW_MS = 48L * 60L * 60L * 1000L
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

private data class ReelPickerMemoryGroup(
    val key: String,
    val opponentLabel: String,
    val seasonLabel: String,
    val videos: List<VideoClip>
)

private data class RestoredTeamClipGroup(
    val key: String,
    val teamLabel: String,
    val sections: List<VideoLibrarySection>
) {
    val videos: List<VideoClip> get() = sections.flatMap { it.videos }
}

private data class RestoredGridClipGroup(
    val key: String,
    val teamLabel: String?,
    val title: String,
    val subtitle: String,
    val videos: List<VideoClip>
)

private data class ClipPlayerSuggestion(
    val name: String,
    val jerseyNumber: String,
    val confidence: Float
)

private data class TeammateReviewClip(
    val clip: VideoClip,
    val players: List<ClipPlayerSuggestion>,
    val isDemo: Boolean = false
)

private data class PendingClipImport(
    val uri: Uri,
    val capturedAtMs: Long?,
    val team: String? = null,
    val player: String? = null,
    val seasonStartYear: Int? = null,
    val opponent: String? = null
)

private data class CameraRollVideo(
    val uri: Uri,
    val durationMs: Long,
    val imported: Boolean
)

@Composable
private fun CameraRollVideoTile(
    video: CameraRollVideo,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = video.uri) {
        value = withContext(Dispatchers.IO) { loadRestoredClipThumbnail(context, video.uri.toString()) }
    }
    Card(
        onClick = onClick,
        enabled = !video.imported,
        modifier = Modifier.fillMaxWidth().height(132.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (video.imported) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xB8E1E3E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Already imported", color = Color(0xFF40464D), fontWeight = FontWeight.Bold)
                }
            } else if (selected) {
                Box(modifier = Modifier.fillMaxSize().border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.padding(5.dp))
                }
            }
            Text(
                formatRestoredDuration(video.durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

private fun canonicalOpponentName(value: String?, knownOpponents: List<String>): String? {
    val normalized = value?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
    if (normalized.isBlank()) return null
    return knownOpponents.firstOrNull { it.equals(normalized, ignoreCase = true) } ?: normalized
}

@Composable
private fun OpponentAutocompleteField(
    value: String,
    knownOpponents: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(value, knownOpponents) {
        val query = value.trim()
        if (query.isBlank()) {
            knownOpponents.take(6)
        } else {
            knownOpponents.filter { it.contains(query, ignoreCase = true) }.take(6)
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            placeholder = { Text("Enter opponent or event") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            matches.forEach { opponent ->
                DropdownMenuItem(
                    text = { Text(opponent) },
                    onClick = {
                        onValueChange(opponent)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ImportClassificationChoice(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(82.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PendingImportPreview(
    pending: PendingClipImport,
    clipNumber: Int,
    detailsAbove: Boolean = false,
    showTapToPreview: Boolean = true,
    onPlay: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = pending.uri) {
        value = withContext(Dispatchers.IO) {
            loadRestoredClipThumbnail(context, pending.uri.toString())
        }
    }
    val capturedLabel = pending.capturedAtMs?.let(::formatRestoredDate) ?: "Date unavailable"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (detailsAbove) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Memory $clipNumber", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(capturedLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (detailsAbove) 156.dp else 112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
        thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Preview memory $clipNumber",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.16f))
        )
            if (showTapToPreview) {
                Text(
                    text = "Tap to preview",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 9.dp)
                )
            }
        }
        if (!detailsAbove) {
            Text("Memory $clipNumber", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                capturedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun isReelTaggedClip(clip: VideoClip): Boolean {
    return clip.momentTag?.startsWith("Reel:", ignoreCase = true) == true ||
        clip.gameTitle.startsWith("Spotr-Reel", ignoreCase = true) ||
        clip.gameTitle.startsWith("Reel:", ignoreCase = true)
}

private fun reelDisplayTitleForClip(clip: VideoClip): String {
    val raw = (clip.momentTag?.takeIf { it.isNotBlank() } ?: clip.gameTitle).trim()
    return if (raw.startsWith("Reel:", ignoreCase = true)) raw.substring(5).trim() else raw
}

private fun withDebugShareDemo(videos: List<VideoClip>): List<VideoClip> {
    if (!BuildConfig.DEBUG || videos.any { it.id == SHARE_DEMO_CLIP_ID }) return videos
    val source = videos.filterNot(::isReelTaggedClip).maxByOrNull { it.createdAt } ?: return videos
    val demo = source.copy(
        id = SHARE_DEMO_CLIP_ID,
        createdAt = System.currentTimeMillis() + 1,
        gameDate = "2026-05-10",
        gameTitle = "Pine-Richland",
        momentTag = "Great Clear & Assist"
    )
    return listOf(demo) + videos
}

private fun debugShareRoster(teamName: String): List<Player> = listOf(
    Player(id = "debug_player_12", number = "12", name = "Mason Davis", position = "Midfield", team = teamName, academicYear = "Junior", addedBy = "+14125550112"),
    Player(id = "debug_player_7", number = "7", name = "Ethan Walker", position = "Attack", team = teamName, academicYear = "Senior", addedBy = "+14125550107"),
    Player(id = "debug_player_21", number = "21", name = "Liam Turner", position = "Defense", team = teamName, academicYear = "Sophomore", addedBy = "+14125550121"),
    Player(id = "debug_player_4", number = "4", name = "Jackson Reed", position = "Goalie", team = teamName, academicYear = "Junior", addedBy = "+14125550104"),
    Player(id = "debug_player_11", number = "11", name = "Aiden King", position = "Attack", team = teamName, academicYear = "Freshman", addedBy = "+14125550111"),
    Player(id = "debug_player_18", number = "18", name = "Noah Brown", position = "Midfield", team = teamName, academicYear = "Sophomore", addedBy = "+14125550118"),
    Player(id = "debug_player_9", number = "9", name = "Carter Hall", position = "Defense", team = teamName, academicYear = "Senior", addedBy = "+14125550109")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreenRefactored(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    cameraHandoffToken: Int = 0,
    goMode: Boolean = false,
    goRecentRequests: MutableState<List<String>>? = null,
    onNavigateToTeams: () -> Unit,
    onNavigate: (LibraryNavigationRequest) -> Unit,
    onOpenMore: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actionKeyboard = LocalSoftwareKeyboardController.current
    val cameraTeam by teamViewModel.selectedTeam.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val playerOptions by teamViewModel.kidOptions.collectAsState()
    val scope = rememberCoroutineScope()

    var localTeamName by remember { mutableStateOf<String?>(null) }
    val rosterFlow = remember(localTeamName) { playerViewModel.observeTeamRoster(localTeamName.orEmpty()) }
    val teamRoster by rosterFlow.collectAsState(initial = emptyList())
    var clips by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAllTeams by remember { mutableStateOf(false) }
    val searchRosterFlow = remember(showAllTeams, subscribedTeams, localTeamName) {
        val teams = if (showAllTeams) subscribedTeams.map { it.name } else listOfNotNull(localTeamName)
        if (teams.isEmpty()) flowOf(emptyList<Player>())
        else combine(teams.map(playerViewModel::observeTeamRoster)) { rosters -> rosters.flatMap { it } }
    }
    val searchRoster by searchRosterFlow.collectAsState(initial = emptyList())
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val localRecentRequests = rememberSaveable { mutableStateOf(emptyList<String>()) }
    var recentGoRequests by (goRecentRequests ?: localRecentRequests)
    var goRequestStarted by rememberSaveable { mutableStateOf(false) }
    var showTeamFilterMenu by remember { mutableStateOf(false) }
    var showPlayerFilterMenu by remember { mutableStateOf(false) }
    var showSeasonFilterMenu by remember { mutableStateOf(false) }
    var showOpponentFilterMenu by remember { mutableStateOf(false) }
    var selectedKid by remember(localTeamName) { mutableStateOf<String?>(null) }
    var selectedSeasonKey by remember(localTeamName) { mutableStateOf<String?>(null) }
    var selectedOpponentKey by remember(localTeamName) { mutableStateOf<String?>(null) }
    var opponentMetadataVersion by remember { mutableStateOf(0) }
    var showReelsOnly by remember(localTeamName) { mutableStateOf(false) }
    var goBrowsing by rememberSaveable { mutableStateOf(false) }
    var isClipsMenuExpanded by rememberSaveable { mutableStateOf(true) }
    val clipsGridState = rememberLazyGridState()
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoClip by remember { mutableStateOf<VideoClip?>(null) }
    var temporaryPlaylist by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var selectedVideoStartsInShareFlow by remember { mutableStateOf(false) }
    var requestedShareRecipient by remember { mutableStateOf<String?>(null) }
    var actionAnswer by remember { mutableStateOf<String?>(null) }
    var relatedScheduleGames by remember { mutableStateOf<List<ActionScheduleGame>>(emptyList()) }
    var actionAnswerTitle by remember { mutableStateOf("Action bar") }
    var lastScheduleAnswer by remember(localTeamName, showAllTeams) { mutableStateOf<ScheduleQuestionAnswer?>(null) }
    var voiceSearchError by remember { mutableStateOf<String?>(null) }
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
    var showDeletePicker by remember { mutableStateOf(false) }
    var deleteSelectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteSelectionConfirmation by remember { mutableStateOf(false) }
    var showSharePicker by remember { mutableStateOf(false) }
    var shareSelectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedShareVideoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showCollectionActionsMenu by remember { mutableStateOf(false) }
    var reelNameInput by remember { mutableStateOf("") }
    var draftReelIds by remember { mutableStateOf<List<String>?>(null) }
    var reviewingSearchReel by remember { mutableStateOf(false) }
    var shareReviewedReel by remember { mutableStateOf(false) }
    var reviewedReelRecipient by remember { mutableStateOf<String?>(null) }
    var reelEditorSelectionIdsOverride by remember { mutableStateOf<List<String>?>(null) }
    var reelSelectionEditTarget by remember { mutableStateOf<VideoClip?>(null) }
    var pendingImports by remember { mutableStateOf<List<PendingClipImport>>(emptyList()) }
    var pendingImportTeam by remember { mutableStateOf<String?>(null) }
    var previewPendingImport by remember { mutableStateOf<PendingClipImport?>(null) }
    var showCameraRollPicker by remember { mutableStateOf(false) }
    var cameraRollVideos by remember { mutableStateOf<List<CameraRollVideo>>(emptyList()) }
    var selectedCameraRollUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var isLoadingCameraRoll by remember { mutableStateOf(false) }
    var pendingActionRequest by remember { mutableStateOf<String?>(null) }
    var showTeammateReview by rememberSaveable { mutableStateOf(false) }
    var teammateReviewClip by remember { mutableStateOf<TeammateReviewClip?>(null) }
    var sharedPlayers by remember { mutableStateOf<List<String>?>(null) }
    var reviewQueueVersion by remember { mutableStateOf(0) }
    var dismissedReviewBatch by remember(localTeamName) {
        mutableStateOf(
            context.getSharedPreferences(TEAMMATE_REVIEW_PREFS, android.content.Context.MODE_PRIVATE)
                .getString("$TEAMMATE_BANNER_DISMISSED_BATCH:${localTeamName.orEmpty()}", null)
        )
    }
    val toggleReelClipSelection: (String) -> Unit = { clipId ->
        reelSelectionIds = if (reelSelectionIds.contains(clipId)) {
            reelSelectionIds - clipId
        } else {
            reelSelectionIds + clipId
        }
    }
    val toggleShareClipSelection: (String) -> Unit = { clipId ->
        shareSelectionIds = if (clipId in shareSelectionIds) {
            shareSelectionIds - clipId
        } else {
            shareSelectionIds + clipId
        }
    }
    val isClipSelectionMode = showDeletePicker || showReelPickerDialog || showSharePicker

    val filterScroll = rememberMemoryFilterScroll(clipsGridState) {
        if (!isClipSelectionMode) isClipsMenuExpanded = it
    }
    LaunchedEffect(isClipsMenuExpanded) {
        if (!isClipsMenuExpanded) {
            showTeamFilterMenu = false
            showPlayerFilterMenu = false
            showSeasonFilterMenu = false
            showOpponentFilterMenu = false
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

    fun openCameraRollPicker() {
        selectedCameraRollUris = emptySet()
        showCameraRollPicker = true
        isLoadingCameraRoll = true
        scope.launch {
            cameraRollVideos = withContext(Dispatchers.IO) {
                val persistedUris = context
                    .getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
                    .all.keys.mapNotNull(::storedClipKeyToUri)
                loadCameraRollVideos(
                    context,
                    persistedUris + clips.map { Uri.parse(it.filePath) }
                )
            }
            isLoadingCameraRoll = false
        }
    }
    val cameraRollPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCameraRollPicker()
        else Toast.makeText(context, "Video access is needed to upload memories", Toast.LENGTH_SHORT).show()
    }
    val submitActionBar: (String) -> Unit = submit@{ text ->
        if (goMode && text.isNotBlank()) {
            goRequestStarted = true
            recentGoRequests = (listOf(text.trim()) + recentGoRequests)
                .distinctBy { it.lowercase() }.take(4)
        }
        actionAnswer = null
        relatedScheduleGames = emptyList()
        goBrowsing = false
        voiceSearchError = null
        actionKeyboard?.hide()
        parseLibraryNavigation(text)?.let { request ->
            pendingActionRequest = null
            lastScheduleAnswer = null
            val resolved = resolveLibraryNavigation(request, subscribedTeams.map { it.name },
                localTeamName.takeUnless { showAllTeams })
            resolved.error?.let { actionAnswerTitle = "Navigation"; actionAnswer = it }
            resolved.request?.let {
                searchQuery = ""
                when (it.destination) {
                    LibraryDestination.MEMORIES -> { showReelsOnly = false; goBrowsing = true }
                    LibraryDestination.REELS -> { showReelsOnly = true; goBrowsing = true }
                    else -> onNavigate(it)
                }
            }
            return@submit
        }
        val request = parseLibraryActionRequest(text)
        if (request !is LibraryActionRequest.Schedule) lastScheduleAnswer = null
        if (parseMemoryQuestion(text) != null) {
            pendingActionRequest = null
            searchQuery = text
        } else if (request != null) {
            pendingActionRequest = text
            if (request !is LibraryActionRequest.Roster && request !is LibraryActionRequest.Schedule) searchQuery = ""
            if (request is LibraryActionRequest.Reel) {
                selectedKid = null
                selectedOpponentKey = null
                showReelsOnly = false
            }
        } else searchQuery = text
    }
    val searchVoiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotEmpty()) {
                submitActionBar(spoken)
            } else voiceSearchError = "No speech was heard. Try again or type your request."
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            voiceSearchError = "Voice recognition stopped. Try again or type your request."
        }
    }

    val importPlayerOptions: List<String> = remember(clips, playerOptions) {
        val playersFromClips = buildRestoredClipKidLookup(context, clips)
            .values
            .mapNotNull { player -> player?.trim()?.takeIf(String::isNotBlank) }
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedBy { it.lowercase(Locale.getDefault()) }
        playersFromClips.ifEmpty {
            playerOptions
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinctBy { it.lowercase(Locale.getDefault()) }
                .sortedBy { it.lowercase(Locale.getDefault()) }
        }
    }

    if (showCameraRollPicker) {
        Dialog(
            onDismissRequest = { showCameraRollPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showCameraRollPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        Text(
                            "Select memories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${selectedCameraRollUris.size} selected", style = MaterialTheme.typography.labelLarge)
                    }
                    HorizontalDivider()
                    if (isLoadingCameraRoll) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f).padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gridItems(cameraRollVideos, key = { it.uri.toString() }) { video ->
                                CameraRollVideoTile(
                                    video = video,
                                    selected = video.uri in selectedCameraRollUris,
                                    onClick = {
                                        selectedCameraRollUris = if (video.uri in selectedCameraRollUris) {
                                            selectedCameraRollUris - video.uri
                                        } else {
                                            selectedCameraRollUris + video.uri
                                        }
                                    }
                                )
                            }
                        }
                        Button(
                            enabled = selectedCameraRollUris.isNotEmpty(),
                            onClick = {
                                val selectedTeamName = localTeamName ?: return@Button
                                val selectedUris = cameraRollVideos
                                    .map { it.uri }
                                    .filter { it in selectedCameraRollUris }
                                showCameraRollPicker = false
                                scope.launch {
                                    pendingImports = withContext(Dispatchers.IO) {
                                        selectedUris.map { uri ->
                                            readPendingClipImport(context, uri).copy(team = selectedTeamName)
                                        }
                                    }
                                    pendingImportTeam = selectedTeamName
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
                        ) {
                            Text("Add ${selectedCameraRollUris.size} ${if (selectedCameraRollUris.size == 1) "Memory" else "Memories"}")
                        }
                    }
                }
            }
        }
    }

    if (pendingImports.isNotEmpty()) {
        val importTeam = pendingImportTeam
        val currentSeasonStartYear = restoredSeasonStartYear(System.currentTimeMillis())
        val seasonYears = remember(pendingImports, currentSeasonStartYear) {
            (pendingImports.mapNotNull { it.seasonStartYear } +
                (currentSeasonStartYear downTo currentSeasonStartYear - 12))
                .distinct()
                .sortedDescending()
        }
        var batchPlayerMenuOpen by remember { mutableStateOf(false) }
        var batchSeasonMenuOpen by remember { mutableStateOf(false) }
        var batchTeamMenuOpen by remember { mutableStateOf(false) }
        var importClassificationStep by remember(pendingImports.firstOrNull()?.uri) {
            mutableStateOf(if (pendingImports.size == 1) 2 else 1)
        }
        var useBatchClassification by remember(pendingImports.firstOrNull()?.uri) {
            mutableStateOf(pendingImports.size > 1)
        }
        var playerMenuForUri by remember { mutableStateOf<String?>(null) }
        var seasonMenuForUri by remember { mutableStateOf<String?>(null) }
        var teamMenuForUri by remember { mutableStateOf<String?>(null) }
        val importTeamOptions = remember(subscribedTeams, importTeam) {
            (subscribedTeams.map { it.name } + listOfNotNull(importTeam))
                .filter(String::isNotBlank)
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        }
        val knownImportOpponents = remember(context, opponentMetadataVersion) {
            context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
                .all.values.filterIsInstance<String>()
                .mapNotNull { canonicalOpponentName(it, emptyList()) }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        }
        val canImport = pendingImports.all {
            !it.team.isNullOrBlank() && !it.player.isNullOrBlank() &&
                it.seasonStartYear != null && !it.opponent.isNullOrBlank()
        }

        Dialog(
            onDismissRequest = {
                pendingImports = emptyList()
                pendingImportTeam = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (importClassificationStep == 2) {
                            IconButton(onClick = {
                                if (pendingImports.size == 1) {
                                    pendingImports = emptyList()
                                    pendingImportTeam = null
                                } else {
                                    importClassificationStep = 1
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                        Text(
                            if (importClassificationStep == 1) "Importing ${pendingImports.size} memories" else "Step 2 of 2",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            pendingImports = emptyList()
                            pendingImportTeam = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel import")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { if (importClassificationStep == 1) 0.18f else 1f },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )

                    if (importClassificationStep == 1) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "How would you like to classify these memories?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "You can change these details later.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            if (pendingImports.size > 1) {
                                ImportClassificationChoice(
                                    title = "Batch classify",
                                    subtitle = "Use the same details for all ${pendingImports.size} memories",
                                    icon = Icons.Default.Layers,
                                    onClick = {
                                        useBatchClassification = true
                                        importClassificationStep = 2
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            ImportClassificationChoice(
                                title = "Classify individually",
                                subtitle = if (pendingImports.size == 1) "Set details for memory" else "Set details for each memory",
                                icon = Icons.Default.PersonOutline,
                                onClick = {
                                    useBatchClassification = false
                                    importClassificationStep = 2
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Memories to import (${pendingImports.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(pendingImports, key = { _, item -> item.uri.toString() }) { index, pending ->
                                    Box(modifier = Modifier.width(132.dp)) {
                                        PendingImportPreview(
                                            pending = pending,
                                            clipNumber = index + 1,
                                            onPlay = { previewPendingImport = pending }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = {
                                    pendingImports = emptyList()
                                    pendingImportTeam = null
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) { Text("Cancel") }
                        }
                    } else {
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    Text(
                        text = if (useBatchClassification) {
                            "Set details for all ${pendingImports.size} memories"
                        } else if (pendingImports.size == 1) {
                            "Set details for memory"
                        } else {
                            "Set details for each memory"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (useBatchClassification) {
                            "These details will be applied to every memory."
                        } else {
                            "Review each memory and adjust its details."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (useBatchClassification) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Player", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { batchPlayerMenuOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    pendingImports.mapNotNull { it.player }.distinct().singleOrNull() ?: "Player for all",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = batchPlayerMenuOpen,
                                onDismissRequest = { batchPlayerMenuOpen = false }
                            ) {
                                importPlayerOptions.forEach { player ->
                                    DropdownMenuItem(
                                        text = { Text(player) },
                                        onClick = {
                                            pendingImports = pendingImports.map { it.copy(player = player) }
                                            batchPlayerMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Team", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { batchTeamMenuOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    pendingImports.mapNotNull { it.team }.distinct().singleOrNull() ?: "Choose team",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = batchTeamMenuOpen,
                                onDismissRequest = { batchTeamMenuOpen = false }
                            ) {
                                importTeamOptions.forEach { team ->
                                    DropdownMenuItem(
                                        text = { Text(team) },
                                        onClick = {
                                            pendingImports = pendingImports.map { it.copy(team = team) }
                                            batchTeamMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Season", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { batchSeasonMenuOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    pendingImports.mapNotNull { it.seasonStartYear }.distinct().singleOrNull()
                                        ?.let(::restoredSeasonLabel) ?: "Season for all",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = batchSeasonMenuOpen,
                                onDismissRequest = { batchSeasonMenuOpen = false }
                            ) {
                                seasonYears.forEach { startYear ->
                                    DropdownMenuItem(
                                        text = { Text(restoredSeasonLabel(startYear)) },
                                        onClick = {
                                            pendingImports = pendingImports.map { it.copy(seasonStartYear = startYear) }
                                            batchSeasonMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Opponent / Event", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        OpponentAutocompleteField(
                            value = pendingImports.mapNotNull { it.opponent }.distinct().singleOrNull().orEmpty(),
                            knownOpponents = knownImportOpponents,
                            onValueChange = { opponent ->
                                pendingImports = pendingImports.map { it.copy(opponent = opponent) }
                            }
                        )
                        }
                    }
                    }
                    if (!useBatchClassification) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(pendingImports, key = { _, item -> item.uri.toString() }) { index, pending ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PendingImportPreview(
                                    pending = pending,
                                    clipNumber = index + 1,
                                    detailsAbove = true,
                                    showTapToPreview = false,
                                    onPlay = { previewPendingImport = pending }
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Player", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { playerMenuForUri = pending.uri.toString() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                pending.player ?: "Choose player",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = playerMenuForUri == pending.uri.toString(),
                                            onDismissRequest = { playerMenuForUri = null }
                                        ) {
                                            importPlayerOptions.forEach { player ->
                                                DropdownMenuItem(
                                                    text = { Text(player) },
                                                    onClick = {
                                                        pendingImports = pendingImports.map {
                                                            if (it.uri == pending.uri) it.copy(player = player) else it
                                                        }
                                                        playerMenuForUri = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Team", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { teamMenuForUri = pending.uri.toString() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(pending.team ?: "Choose team", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        DropdownMenu(
                                            expanded = teamMenuForUri == pending.uri.toString(),
                                            onDismissRequest = { teamMenuForUri = null }
                                        ) {
                                            importTeamOptions.forEach { team ->
                                                DropdownMenuItem(
                                                    text = { Text(team) },
                                                    onClick = {
                                                        pendingImports = pendingImports.map {
                                                            if (it.uri == pending.uri) it.copy(team = team) else it
                                                        }
                                                        teamMenuForUri = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Season", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { seasonMenuForUri = pending.uri.toString() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                pending.seasonStartYear?.let(::restoredSeasonLabel) ?: "Choose season",
                                                maxLines = 1
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = seasonMenuForUri == pending.uri.toString(),
                                            onDismissRequest = { seasonMenuForUri = null }
                                        ) {
                                            seasonYears.forEach { startYear ->
                                                DropdownMenuItem(
                                                    text = { Text(restoredSeasonLabel(startYear)) },
                                                    onClick = {
                                                        pendingImports = pendingImports.map {
                                                            if (it.uri == pending.uri) it.copy(seasonStartYear = startYear) else it
                                                        }
                                                        seasonMenuForUri = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Opponent / Event", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    OpponentAutocompleteField(
                                        value = pending.opponent.orEmpty(),
                                        knownOpponents = knownImportOpponents,
                                        onValueChange = { opponent ->
                                            pendingImports = pendingImports.map {
                                                if (it.uri == pending.uri) it.copy(opponent = opponent) else it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (pendingImports.size == 1) {
                                    pendingImports = emptyList()
                                    pendingImportTeam = null
                                } else {
                                    importClassificationStep = 1
                                }
                            },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) { Text("Back") }
                Button(
                    enabled = canImport,
                    onClick = {
                        val teamEditor = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE).edit()
                        val startEditor = context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE).edit()
                        val kidEditor = context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE).edit()
                        val opponentEditor = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE).edit()
                        pendingImports.forEach { pending ->
                            val uriKey = pending.uri.toString()
                            teamEditor.putString(uriKey, pending.team)
                            kidEditor.putString(uriKey, pending.player)
                            opponentEditor.putString(
                                uriKey,
                                canonicalOpponentName(pending.opponent, knownImportOpponents)
                            )
                            startEditor.putLong(
                                uriKey,
                                timestampInRestoredSeason(pending.capturedAtMs, pending.seasonStartYear!!)
                            )
                        }
                        teamEditor.apply()
                        kidEditor.apply()
                        startEditor.apply()
                        opponentEditor.apply()
                        val importedCount = pendingImports.size
                        pendingImports = emptyList()
                        pendingImportTeam = null
                        scope.launch {
                            isLoading = true
                            clips = loadRestoredClips(context, localTeamName ?: importTeam!!)
                            isLoading = false
                        }
                        val message = if (importedCount == 1) "Play uploaded" else "$importedCount plays uploaded"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1.5f).height(52.dp)
                        ) {
                            Text("Import ${pendingImports.size} Memories")
                        }
                    }
                }
            }
        }
        }

        previewPendingImport?.let { pending ->
            Dialog(
                onDismissRequest = { previewPendingImport = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                VideoPlaybackScreen(
                    videoUri = pending.uri,
                    detectedPlayers = emptyList(),
                    onNavigateBack = { previewPendingImport = null }
                )
            }
        }
    }

    LaunchedEffect(localTeamName, showAllTeams, subscribedTeams) {
        if (showAllTeams) {
            isLoading = true
            clips = subscribedTeams
                .flatMap { team -> loadRestoredClips(context, team.name) }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
                .let(::withDebugShareDemo)
            isLoading = false
            return@LaunchedEffect
        }
        val teamName = localTeamName
        if (teamName.isNullOrBlank()) {
            clips = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        clips = withDebugShareDemo(loadRestoredClips(context, teamName))
        selectedSeasonKey = null
        selectedOpponentKey = null
        showReelsOnly = false
        if (selectedKid == null) {
            selectedKid = teamViewModel.getSelectedKidForTeam(teamName)
        }
        isLoading = false
    }

    val opponentLookup = remember(clips, opponentMetadataVersion) {
        buildRestoredClipOpponentLookup(context, clips)
    }
    val kidLookup = remember(clips) {
        buildRestoredClipKidLookup(context, clips)
    }
    val teamLookup = remember(clips) {
        buildRestoredClipTeamLookup(context, clips)
    }
    val teammateReviewClips by produceState<List<TeammateReviewClip>>(
        initialValue = emptyList(),
        key1 = clips to teamRoster,
        key2 = reviewQueueVersion
    ) {
        value = withContext(Dispatchers.IO) {
            loadRecentTeammateReviewClips(context, clips, teamRoster)
        }
    }
    val teammateReviewBatch = remember(teammateReviewClips) {
        teammateReviewClips.map { it.clip.id }.sorted().joinToString("|")
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
    val searchHintExamples = remember(availableKidOptions, availableOpponents, searchRoster) {
        val playerName = availableKidOptions.firstOrNull()
        val opponentName = availableOpponents
            .firstOrNull { it.label != NO_OPPONENT_FILTER_RESTORED }
            ?.label
        searchRoster.firstOrNull { it.isActive && it.number.isNotBlank() }
            ?.let { listOf("Who is number ${it.number}?") }.orEmpty() + listOf(
            playerName?.let { "How many faceoff wins did $it have yesterday?" } ?: "How many faceoff wins did I record yesterday?",
            "Who is our next opponent?",
            opponentName?.let { "When do we play $it?" } ?: "When is our next game?",
            opponentName?.let { "Find all goals against $it" } ?: "Find all goals",
            playerName?.let { "Create Reel of all $it's goals this season" } ?: "Create Reel of all goals this season",
            opponentName?.let { "Find the last save against $it" } ?: "Find the last save",
            playerName?.let { "Create Reel of $it's favorite plays" } ?: "Create Reel of favorite plays",
            "Find all defensive stops this season",
            opponentName?.let { "Create Reel of goals against $it" } ?: "Create Reel of all goals"
        ).distinct()
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
    val searchPlayers = remember(searchRoster) { searchRoster.map { it.toProfile() } }
    val assignedPlayerNames = remember(searchPlayers, kidLookup, teamLookup) {
        kidLookup.mapValues { (id, name) -> resolveMemoryPlayerAssignment(name, teamLookup[id], searchPlayers)?.name }
    }
    val parsedSearch = remember(searchQuery, searchPlayers, subscribedTeams, opponentLookup, selectedSeasonKey, availableSeasons) {
        interpretAskPlayloom(searchQuery, searchPlayers,
            subscribedTeams.map { it.name }, opponentLookup.values.filterNotNull())
            .withSelectedSeasonYear(availableSeasons.firstOrNull { it.key == selectedSeasonKey }?.startYear?.toString())
    }
    var searchOverride by remember(searchQuery, parsedSearch) { mutableStateOf<AskPlayloomQuery?>(null) }
    val interpretedSearch = searchOverride ?: parsedSearch
    LaunchedEffect(parsedSearch) {
        if (searchQuery.isNotBlank() && parsedSearch.issue == null) {
            if (parsedSearch.players.isNotEmpty()) selectedKid = null
            if (parsedSearch.opponents.isNotEmpty()) selectedOpponentKey = null
            if (parsedSearch.filters.seasonIntent != MemorySeasonIntent.CURRENT &&
                (parsedSearch.filters.dateFrom != null || parsedSearch.filters.seasonIntent != null ||
                    parsedSearch.filters.seasonYear != null)) selectedSeasonKey = null
        }
    }
    val noteLookup = remember(clips, searchQuery) { clips.associate { it.id to loadClipCommentary(context, it) } }
    val searchReelSources = remember(reelIncludedClipIds, clipsById) {
        reelIncludedClipIds.mapValues { (_, ids) -> ids.mapNotNull(clipsById::get) }
    }
    val searchedVideos = remember(reelScopedVideos, searchQuery, assignedPlayerNames, opponentLookup, interpretedSearch, teamLookup, noteLookup, searchReelSources,
        clips, relatedScheduleGames, actionAnswer, pendingActionRequest) {
        if (goMode && (actionAnswer != null || pendingActionRequest != null)) {
            val now = System.currentTimeMillis()
            clips.filter { clip ->
                !isReelTaggedClip(clip) && clip.createdAt <= now &&
                    matchesScheduleOpponent(relatedScheduleGames, teamLookup[clip.id],
                        opponentLookup[clip.id], opponentLookup.values.filterNotNull() + relatedScheduleGames.map { it.opponent })
            }
        } else filterRestoredClipsBySearchQuery(
            videos = reelScopedVideos,
            query = searchQuery,
            kidLookup = assignedPlayerNames,
            opponentLookup = opponentLookup,
            interpretation = interpretedSearch,
            teamLookup = teamLookup,
            noteLookup = noteLookup,
            reelSources = searchReelSources
        )
    }
    LaunchedEffect(pendingActionRequest, isLoading) {
        val text = pendingActionRequest ?: return@LaunchedEffect
        if (isLoading) return@LaunchedEffect
        val request = parseLibraryActionRequest(text) ?: return@LaunchedEffect
        actionAnswerTitle = when (request) {
            is LibraryActionRequest.Roster -> "Roster lookup"
            is LibraryActionRequest.Schedule -> "Schedule"
            else -> "Action bar"
        }
        when (request) {
            is LibraryActionRequest.Roster -> {
                try {
                    val selectedTeam = localTeamName.takeUnless { showAllTeams }
                    val teams = selectedTeam?.let(::listOf) ?: subscribedTeams.map { it.name }
                    val roster = teams.distinct().flatMap { team ->
                        playerViewModel.observeTeamRoster(team).first().map { it.toProfile() }
                    }
                    actionAnswer = answerRosterQuestion(request, roster, selectedTeam)
                } catch (failure: android.database.sqlite.SQLiteException) {
                    Log.e("ClipsScreenRefactored", "Unable to read roster for voice lookup", failure)
                    actionAnswer = "Couldn't read the saved roster. Please try again or open Teams."
                }
                pendingActionRequest = null
                return@LaunchedEffect
            }
            is LibraryActionRequest.Count -> {
                searchQuery = text
                pendingActionRequest = null
                return@LaunchedEffect
            }
            is LibraryActionRequest.Schedule -> {
                try {
                    val teams = if (showAllTeams || localTeamName == null) subscribedTeams.map { it.name } else listOfNotNull(localTeamName)
                    val games = teams.flatMap { team ->
                        teamViewModel.getGamesForTeam(team).first().map { game ->
                            ActionScheduleGame(game.id, team, game.opponentName, game.scheduledStartMs, game.locationName)
                        }
                    }
                    val answer = scheduleQuestionAnswer(request, games, previousAnswer = lastScheduleAnswer)
                    lastScheduleAnswer = answer
                    relatedScheduleGames = answer.games
                    showReelsOnly = false
                    actionAnswer = answer.text
                } catch (failure: android.database.sqlite.SQLiteException) {
                    android.util.Log.e("ClipsScreen", "Unable to read schedule for action bar", failure)
                    actionAnswer = "Couldn't read the saved schedule. Please try again."
                    lastScheduleAnswer = null
                }
                pendingActionRequest = null
                return@LaunchedEffect
            }
            is LibraryActionRequest.Unsupported -> {
                actionAnswer = request.message
                pendingActionRequest = null
                return@LaunchedEffect
            }
            is LibraryActionRequest.Share -> {
                searchQuery = request.query
                actionAnswer = "Select the matching memories, then tap Share to send them to ${request.recipient}."
                pendingActionRequest = null
                return@LaunchedEffect
            }
            is LibraryActionRequest.Reel -> Unit
        }
        val criteria = interpretAskPlayloom(request.command.searchQuery, searchPlayers,
            subscribedTeams.map { it.name }, opponentLookup.values.filterNotNull())
            .withSelectedSeasonYear(availableSeasons.firstOrNull { it.key == selectedSeasonKey }?.startYear?.toString())
        if (criteria.issue != null) {
            actionAnswer = criteria.issue
            pendingActionRequest = null
            return@LaunchedEffect
        }
        val matchingClips = filterRestoredClipsBySearchQuery(
            videos = clips.filterNot(::isReelTaggedClip).filter { clip ->
                selectedSeasonKey == null || criteria.filters.dateFrom != null ||
                    criteria.filters.seasonYear != null ||
                    parseRestoredSeasonKey(clip.gameDate)?.key == selectedSeasonKey
            },
            query = request.command.searchQuery,
            kidLookup = assignedPlayerNames,
            opponentLookup = opponentLookup,
            interpretation = criteria,
            teamLookup = teamLookup,
            noteLookup = noteLookup,
            reelSources = searchReelSources
        ).sortedBy { it.createdAt }
        if (matchingClips.size < 2) {
            pendingActionRequest = null
            actionAnswer = if (matchingClips.isEmpty()) "No saved videos matched that reel request. Check the saved player, tags, dates and opponent."
                else "Only 1 saved video matched. A reel needs at least 2 videos."
            return@LaunchedEffect
        }
        val selectedTeamName = localTeamName
        if (selectedTeamName.isNullOrBlank()) {
            pendingActionRequest = null
            Toast.makeText(context, "Select a team before creating a reel", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        searchQuery = request.command.searchQuery
        reelNameInput = request.command.title
        reelEditorSelectionIdsOverride = null
        reelSelectionEditTarget = null
        reviewingSearchReel = true
        shareReviewedReel = request.share
        reviewedReelRecipient = request.recipient
        draftReelIds = matchingClips.map { it.id }
        pendingActionRequest = null
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
    val visibleTeamGroups = remember(
        searchedVideos,
        opponentLookup,
        teamLookup,
        showAllTeams,
        showReelsOnly,
        kidFilter,
        selectedSeasonKey,
        selectedOpponentKey,
        reelIncludedClipIds,
        clipsById,
        kidLookup
    ) {
        val reelFiltersActive = showReelsOnly && (kidFilter.isNotEmpty() || selectedSeasonKey != null || selectedOpponentKey != null)
        val groupedVideos = if (showAllTeams) {
            searchedVideos.groupBy { clip -> teamLookup[clip.id]?.takeIf(String::isNotBlank) ?: "Team not set" }
        } else {
            mapOf("" to searchedVideos)
        }
        groupedVideos.entries
            .sortedBy { (teamName, _) -> teamName.lowercase(Locale.getDefault()) }
            .map { (teamName, teamVideos) ->
                RestoredTeamClipGroup(
                    key = teamName.lowercase(Locale.getDefault()).ifBlank { "selected_team" },
                    teamLabel = teamName,
                    sections = buildRestoredClipListSectionsForAllGames(
                        teamVideos,
                        opponentLookup,
                        groupByOpponent = !showReelsOnly,
                        reelSortOverride = if (reelFiltersActive) { videos ->
                            videos.sortedWith(compareByDescending<VideoClip> { reelMatchScore(it) }.thenByDescending { it.createdAt })
                        } else null
                    )
                )
            }
    }
    val visibleSections = remember(visibleTeamGroups) {
        visibleTeamGroups.flatMap { it.sections }
    }
    val visibleVideos = remember(visibleSections) {
        visibleSections.flatMap { it.videos }
    }
    LaunchedEffect(visibleVideos, showSharePicker) {
        if (showSharePicker) {
            val visibleIds = visibleVideos.map { it.id }.toSet()
            shareSelectionIds = shareSelectionIds.intersect(visibleIds)
        }
    }
    val reelPickerMemoryGroups = remember(visibleVideos, opponentLookup) {
        buildReelPickerMemoryGroups(visibleVideos, opponentLookup)
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
        val matchingClip = selectedVideoClip ?: clips.firstOrNull { clip ->
            Uri.parse(clip.filePath) == videoUri ||
                (isReelTaggedClip(clip) && resolveClipUriForReel(clip.filePath) == videoUri)
        }
        VideoPlaybackScreen(
            videoUri = videoUri,
            playlistClips = temporaryPlaylist,
            playlistUris = temporaryPlaylist.map { Uri.parse(it.filePath) },
            temporaryPlaylist = temporaryPlaylist.isNotEmpty(),
            clipMetadata = matchingClip,
            detectedPlayers = if (matchingClip?.id == SHARE_DEMO_CLIP_ID) {
                (teamRoster + debugShareRoster(localTeamName.orEmpty())).distinctBy { it.id }
            } else {
                teamRoster
            },
            preselectedSharePlayerIds = if (matchingClip?.id == SHARE_DEMO_CLIP_ID) SHARE_DEMO_DETECTED_PLAYER_IDS else emptySet(),
            isReviewMode = teammateReviewClip != null,
            startInShareFlow = selectedVideoStartsInShareFlow,
            showPlaybackUi = !selectedVideoStartsInShareFlow,
            shareVideoUris = selectedShareVideoUris,
            requestedRecipient = requestedShareRecipient,
            onNavigateBack = {
                temporaryPlaylist = emptyList()
                selectedVideoUri = null
                selectedVideoClip = null
                selectedVideoStartsInShareFlow = false
                selectedShareVideoUris = emptyList()
                requestedShareRecipient = null
            },
            onEditReel = matchingClip?.takeIf(::isReelTaggedClip)?.let { reelClip ->
                {
                    selectedVideoUri = null
                    selectedVideoClip = null
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
                        Toast.makeText(context, "Trimming memory...", Toast.LENGTH_SHORT).show()
                        val trimmedUri = trimClipToNewVideo(context, target, startMs, endMs)
                        if (trimmedUri != null) {
                            copyClipMetadataForTrim(context, target, trimmedUri, localTeamName)
                            reloadClipsForCurrentScope()
                            Toast.makeText(context, "Trimmed memory saved", Toast.LENGTH_SHORT).show()
                            editingClip = null
                        } else {
                            Toast.makeText(context, "Could not trim this memory", Toast.LENGTH_SHORT).show()
                        }
                    } catch (t: Throwable) {
                        Log.e("ClipsScreenRefactored", "Saving trimmed clip failed", t)
                        Toast.makeText(context, "Could not save trimmed memory", Toast.LENGTH_SHORT).show()
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
                showReelPickerDialog = !reviewingSearchReel
                reviewingSearchReel = false
                shareReviewedReel = false
                reviewedReelRecipient = null
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
                            if (orderedClips.size != orderedIds.size || clipUris.size != orderedClips.size || orderedClips.size < 2) {
                                Toast.makeText(context, "A reel needs at least 2 available selected videos", Toast.LENGTH_SHORT).show()
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
                                if (shareReviewedReel) {
                                    requestedShareRecipient = reviewedReelRecipient
                                    selectedVideoStartsInShareFlow = true
                                    selectedShareVideoUris = listOf(reelUri)
                                    selectedVideoUri = reelUri
                                } else completedReelUri = reelUri
                                reviewingSearchReel = false
                                shareReviewedReel = false
                                reviewedReelRecipient = null
                                draftReelIds = null
                                reelSelectionIds = emptySet()
                                Toast.makeText(context, "Reel saved to Movies/Spotr", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Unable to build reel video", Toast.LENGTH_SHORT).show()
                            }
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
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
                            Toast.makeText(context, "Select at least one memory", Toast.LENGTH_SHORT).show()
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
                Text("Select a team to view memories", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateToTeams) {
                    Text("Go to Teams")
                }
            }
        }
        return
    }

    if (showTeammateReview) {
        val reviewClip = teammateReviewClip
        if (reviewClip == null) {
            TeammateReviewScreen(
                clips = teammateReviewClips,
                onBack = { showTeammateReview = false },
                onReview = { teammateReviewClip = it }
            )
        } else {
            TeammateClipReviewScreen(
                reviewClip = reviewClip,
                roster = teamRoster,
                onBack = { teammateReviewClip = null },
                onFullscreen = {
                    selectedVideoClip = reviewClip.clip
                    selectedVideoUri = Uri.parse(reviewClip.clip.filePath)
                },
                onShare = { selectedPlayers ->
                    launchPersonalShareChooser(
                        context = context,
                        videoUri = Uri.parse(reviewClip.clip.filePath),
                        shareTitle = "Share this PlayLoom memory"
                    )
                    markTeammateReviewComplete(context, reviewClip.clip, selectedPlayers)
                    sharedPlayers = selectedPlayers
                    teammateReviewClip = null
                    showTeammateReview = false
                    reviewQueueVersion += 1
                }
            )
        }
        return
    }

    sharedPlayers?.let { players ->
        AlertDialog(
            onDismissRequest = { sharedPlayers = null },
            title = { Text("Memory shared") },
            text = {
                Text("Shared with ${players.joinToString()}. The memory is waiting for them in PlayLoom.")
            },
            confirmButton = {
                TextButton(onClick = { sharedPlayers = null }) { Text("Done") }
            }
        )
    }

    val goLanding = goMode && !goRequestStarted && !goBrowsing && !isClipSelectionMode && !showReelPickerDialog
    val goSearchBar: @Composable () -> Unit = {
        CaptureLibrarySearchBar(
            query = searchQuery,
            onQueryChange = {
                actionAnswer = null; goBrowsing = false; pendingActionRequest = null; searchQuery = it
                if (it.isBlank()) goRequestStarted = false
            },
            onSubmit = submitActionBar,
            voiceError = voiceSearchError,
            onDismissVoiceError = { voiceSearchError = null },
            onVoiceSearch = {
                voiceSearchError = null
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Playloom Go")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                try {
                    searchVoiceLauncher.launch(intent)
                } catch (failure: android.content.ActivityNotFoundException) {
                    Log.e("ClipsScreenRefactored", "No voice recognition activity available", failure)
                    voiceSearchError = "Voice search is unavailable. Enable a speech recognition service in Android Settings, or type your request."
                } catch (failure: SecurityException) {
                    Log.e("ClipsScreenRefactored", "Voice recognition permission denied", failure)
                    voiceSearchError = "Voice access was denied. Check microphone permissions for your speech recognition service in Android Settings, or type your request."
                }
            }
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (!goLanding) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            AnimatedVisibility(
                visible = goMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    actionKeyboard?.hide()
                    searchQuery = ""; actionAnswer = null; pendingActionRequest = null
                    goBrowsing = false; goRequestStarted = false
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Playloom Go") }
                Text("Playloom Go", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                goSearchBar()
            }
            if (actionAnswer != null) {
                AskPlayloomAnswer(actionAnswerTitle, actionAnswer.orEmpty())
            } else if (pendingActionRequest == null && searchQuery.isNotBlank()) AskPlayloomResults(
                interpretedSearch, visibleVideos.size, showReelsOnly,
                onChange = { searchOverride = it },
                onClear = { searchQuery = ""; searchOverride = null },
                playableCount = if (showReelsOnly) 0 else visibleVideos.size,
                onPlayAll = if (isLoading || showReelsOnly || visibleVideos.isEmpty()) null else ({
                    actionKeyboard?.hide()
                    temporaryPlaylist = visibleVideos.distinctBy { it.id }.sortedBy { it.createdAt }.toList()
                    selectedVideoClip = temporaryPlaylist.first()
                    selectedVideoStartsInShareFlow = false
                    selectedShareVideoUris = emptyList()
                    requestedShareRecipient = null
                    selectedVideoUri = Uri.parse(temporaryPlaylist.first().filePath)
                }),
                onCreateReel = if (isLoading || isBuildingReel || showReelsOnly || isClipSelectionMode) null else ({
                    if (localTeamName.isNullOrBlank()) {
                        Toast.makeText(context, "Select a team before creating a reel", Toast.LENGTH_SHORT).show()
                    } else {
                        actionKeyboard?.hide()
                        reelNameInput = "Highlight Reel"
                        reviewingSearchReel = true
                        shareReviewedReel = false
                        reviewedReelRecipient = null
                        reelEditorSelectionIdsOverride = null
                        reelSelectionEditTarget = null
                        draftReelIds = visibleVideos.distinctBy { it.id }.sortedBy { it.createdAt }.map { it.id }
                    }
                }),
                onShare = if (isLoading || isBuildingReel || isClipSelectionMode) null else ({
                    actionKeyboard?.hide()
                    shareSelectionIds = visibleVideos.map { it.id }.toSet()
                    requestedShareRecipient = null
                    showSharePicker = true
                }),
                answer = if (isLoading) null else answerMemoryQuestion(
                    interpretedSearch, visibleVideos.size, showReelsOnly,
                    opponents = visibleVideos.mapNotNull { opponentLookup[it.id] },
                    firstDate = visibleVideos.firstOrNull()?.let {
                        java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate().toString()
                    }
                )
            )

                }
            }

            AnimatedVisibility(visible = (isClipsMenuExpanded || isClipSelectionMode) &&
                (!goMode || actionAnswer == null && pendingActionRequest == null)) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    com.playerid.app.ui.components.MemoryFilterControl(
                        category = "Player", value = selectedKid ?: "All",
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
                Box(Modifier.weight(1f)) {
                    com.playerid.app.ui.components.MemoryFilterControl(
                        category = "Season", value = availableSeasons.firstOrNull { it.key == selectedSeasonKey }?.label ?: "All",
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
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    com.playerid.app.ui.components.MemoryFilterControl(
                        category = "Team", value = if (showAllTeams) "All Teams" else localTeamName ?: "All",
                        isActive = !showAllTeams && !localTeamName.isNullOrBlank(),
                        onClick = { showTeamFilterMenu = true }
                    )
                    DropdownMenu(expanded = showTeamFilterMenu, onDismissRequest = { showTeamFilterMenu = false }) {
                        if (subscribedTeams.size > 1) {
                            DropdownMenuItem(
                                text = { Text("All teams") },
                                onClick = {
                                    showAllTeams = true
                                    showTeamFilterMenu = false
                                }
                            )
                        }
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
                Box(Modifier.weight(1f)) {
                    com.playerid.app.ui.components.MemoryFilterControl(
                        category = "Opponent", value = availableOpponents.firstOrNull { it.key == selectedOpponentKey }?.label ?: "All",
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

            }
            }
            if ((!goMode || actionAnswer == null && pendingActionRequest == null) &&
                (!goMode || searchQuery.isNotBlank() || goBrowsing || isClipSelectionMode || showReelPickerDialog)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                        .padding(top = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isClipSelectionMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClipsFileTab(
                            label = "Memories",
                            highlighted = !showReelsOnly,
                            underlined = !showReelsOnly && !showReelPickerDialog,
                            enabled = !isBuildingReel && !showReelPickerDialog,
                            onClick = {
                                showReelsOnly = false
                                goBrowsing = true
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
                                goBrowsing = true
                                showReelPickerDialog = false
                            }
                        )
                    }
                }

                if (!isClipSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box {
                            IconButton(onClick = { showCollectionActionsMenu = true }) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "Memory actions")
                            }
                            DropdownMenu(
                                expanded = showCollectionActionsMenu,
                                onDismissRequest = { showCollectionActionsMenu = false }
                            ) {
                        DropdownMenuItem(
                            text = { Text("Upload memory") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                showCollectionActionsMenu = false
                                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_VIDEO
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }
                                if (ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    openCameraRollPicker()
                                } else {
                                    cameraRollPermissionLauncher.launch(permission)
                                }
                            }
                        )
                            DropdownMenuItem(
                                text = { Text("Remove memories") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showCollectionActionsMenu = false
                                    deleteSelectionIds = emptySet()
                                    showDeletePicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Create reel") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showCollectionActionsMenu = false
                                    when {
                                        opponentScopedVideos.none { !isReelTaggedClip(it) } -> Toast.makeText(
                                            context,
                                            "No memories available to build a reel",
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
                                            showReelPickerDialog = true
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share memories") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showCollectionActionsMenu = false
                                    shareSelectionIds = emptySet()
                                    showSharePicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Teams") },
                                leadingIcon = { Icon(Icons.Default.Groups, null) },
                                onClick = { showCollectionActionsMenu = false; onNavigateToTeams() }
                            )
                            if (!goMode) DropdownMenuItem(
                                text = { Text("Settings & more") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { showCollectionActionsMenu = false; onOpenMore() }
                            )
                            }
                        }
                    }
                }
            }

            }
            AnimatedVisibility(
                visible = isClipsMenuExpanded &&
                    teammateReviewClips.isNotEmpty() &&
                    teammateReviewBatch != dismissedReviewBatch &&
                    !showReelPickerDialog,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val dismissDistancePx = with(LocalDensity.current) { 84.dp.toPx() }
                var bannerDragOffset by remember(teammateReviewBatch) { mutableStateOf(0f) }
                val animatedBannerOffset by animateFloatAsState(
                    targetValue = bannerDragOffset,
                    animationSpec = tween(160),
                    label = "teammate_banner_swipe"
                )
                val bannerDragState = rememberDraggableState { delta ->
                    bannerDragOffset = (bannerDragOffset + delta).coerceIn(-dismissDistancePx, 0f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .padding(top = 10.dp)
                ) {
                    if (animatedBannerOffset < -1f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(84.dp)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            onClick = {
                                    dismissedReviewBatch = teammateReviewBatch
                                    context.getSharedPreferences(TEAMMATE_REVIEW_PREFS, android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .putString(
                                            "$TEAMMATE_BANNER_DISMISSED_BATCH:${localTeamName.orEmpty()}",
                                            teammateReviewBatch
                                        )
                                        .apply()
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Dismiss", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .offset { IntOffset(animatedBannerOffset.roundToInt(), 0) }
                            .draggable(
                                state = bannerDragState,
                                orientation = Orientation.Horizontal,
                                onDragStopped = {
                                    bannerDragOffset = if (bannerDragOffset <= -dismissDistancePx * 0.55f) {
                                        -dismissDistancePx
                                } else {
                                        0f
                                    }
                                }
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE2F7EA),
                        contentColor = Color(0xFF075D32),
                        onClick = {
                            if (bannerDragOffset < 0f) bannerDragOffset = 0f else showTeammateReview = true
                        }
                    ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(44.dp))
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).size(22.dp),
                                shape = CircleShape,
                                color = Color(0xFF079447),
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(5.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Share the moments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your memories include other players",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF315B43)
                            )
                            Text(
                                text = "Let their families see it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF315B43)
                            )
                        }
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(0xFF079447),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Review and share", modifier = Modifier.padding(11.dp))
                        }
                    }
                    }
                }
            }

        }

        if (showDeletePicker) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Select memories to remove", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${deleteSelectionIds.size} ${if (deleteSelectionIds.size == 1) "memory" else "memories"} selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(onClick = {
                    showDeletePicker = false
                    deleteSelectionIds = emptySet()
                }) {
                    Text("Cancel")
                }
                Button(
                    enabled = deleteSelectionIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { showDeleteSelectionConfirmation = true }
                ) {
                    Text("Remove")
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
                            text = "Select memories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedClips.size} ${if (selectedClips.size == 1) "memory" else "memories"} selected",
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
                            reviewingSearchReel = false
                            shareReviewedReel = false
                            reviewedReelRecipient = null
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
                                    selectedClips.sortedBy { it.createdAt }.map { it.id }
                                        .filterNot { it in reelEditorSelectionIdsOverride.orEmpty() }
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

        if (showSharePicker) {
            val selectedClips = visibleVideos
                .distinctBy { it.id }
                .filter { it.id in shareSelectionIds }
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
                        Text("Select Memories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${selectedClips.size} ${if (selectedClips.size == 1) "memory" else "memories"} selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = {
                        showSharePicker = false
                        shareSelectionIds = emptySet()
                    }) {
                        Text("Cancel")
                    }
                    Button(
                        enabled = selectedClips.isNotEmpty(),
                        onClick = {
                            val selectedUris = selectedClips.map { Uri.parse(it.filePath) }
                            selectedVideoClip = selectedClips.first()
                            selectedVideoUri = selectedUris.first()
                            selectedShareVideoUris = selectedUris
                            selectedVideoStartsInShareFlow = true
                            showSharePicker = false
                            shareSelectionIds = emptySet()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
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

        }
        if (goMode && actionAnswer != null && relatedScheduleGames.isNotEmpty() && !isLoading) {
            Text(if (visibleVideos.isEmpty()) "No past saved memories against this opponent."
                else "Past memories against ${relatedScheduleGames.map { it.opponent }.distinct().joinToString()}",
                Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
        }
        when {
            goLanding -> PlayloomGoPage(
                hints = searchHintExamples,
                recentRequests = recentGoRequests,
                onSubmit = submitActionBar,
                onClearRecent = { recentGoRequests = emptyList() },
                searchBar = goSearchBar
            )
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading memories...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            goMode && (actionAnswer != null || pendingActionRequest != null) && visibleVideos.isEmpty() ->
                Spacer(Modifier.fillMaxSize())
            goMode && actionAnswer == null && pendingActionRequest == null && searchQuery.isBlank() &&
                !goBrowsing && !isClipSelectionMode && !showReelPickerDialog ->
                Spacer(Modifier.fillMaxSize())
            localTeamName.isNullOrBlank() && !showAllTeams && !(goMode && actionAnswer != null) -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a team to view memories", style = MaterialTheme.typography.bodyMedium)
                }
            }
            visibleVideos.isEmpty() -> {
                if (!showReelsOnly && interpretedSearch.question != null && interpretedSearch.issue == null) {
                    Spacer(Modifier.fillMaxSize())
                } else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No videos yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                val gridColumnCount = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumnCount),
                    state = clipsGridState,
                    modifier = Modifier
                        .nestedScroll(filterScroll)
                        .then(
                            if (isClipSelectionMode) Modifier.weight(1f) else Modifier.fillMaxSize()
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val gridGroups = if (showReelPickerDialog) {
                        reelPickerMemoryGroups.map { group ->
                            RestoredGridClipGroup(
                                key = group.key,
                                teamLabel = null,
                                title = group.opponentLabel,
                                subtitle = group.seasonLabel,
                                videos = group.videos
                            )
                        }
                    } else {
                        visibleTeamGroups.flatMap { teamGroup ->
                            teamGroup.sections.mapIndexed { index, section ->
                                RestoredGridClipGroup(
                                    key = "${teamGroup.key}|${section.key}",
                                    teamLabel = teamGroup.teamLabel.takeIf { showAllTeams && index == 0 },
                                    title = section.title,
                                    subtitle = section.subtitle,
                                    videos = section.videos
                                )
                            }
                        }
                    }
                    gridGroups.forEach { group ->
                        item(
                            key = "memory_group_${group.key}",
                            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
                                group.teamLabel?.let { teamLabel ->
                                    Text(
                                        teamLabel,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(44.dp)
                                            .background(OpponentGroupColor, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            group.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (group.subtitle.isNotBlank()) {
                                            Text(
                                                group.subtitle,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(3.dp)
                                        .background(OpponentGroupColor)
                                )
                            }
                        }
                    gridItems(group.videos, key = { it.id }) { clip ->
                        RestoredClipGridTile(
                            clip = clip,
                            reelMode = isClipSelectionMode,
                            isSelectedForReel = when {
                                showDeletePicker -> clip.id in deleteSelectionIds
                                showSharePicker -> clip.id in shareSelectionIds
                                else -> clip.id in reelSelectionIds
                            },
                            onToggleReelSelection = {
                                when {
                                    showDeletePicker -> deleteSelectionIds = if (clip.id in deleteSelectionIds) {
                                        deleteSelectionIds - clip.id
                                    } else {
                                        deleteSelectionIds + clip.id
                                    }
                                    showSharePicker -> toggleShareClipSelection(clip.id)
                                    else -> toggleReelClipSelection(clip.id)
                                }
                            },
                            onPlay = {
                                selectedVideoClip = clip
                                selectedVideoUri = Uri.parse(clip.filePath)
                            },
                            onEdit = {
                                editingClipAllowsTrim = true
                                editingClip = clip
                            },
                            onShare = {
                                selectedVideoClip = clip
                                selectedVideoUri = Uri.parse(clip.filePath)
                                selectedShareVideoUris = emptyList()
                                selectedVideoStartsInShareFlow = true
                            },
                            onDelete = {
                                videoToDelete = clip
                                showDeleteDialog = true
                            },
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
            }
        }

        if (showDeleteDialog && videoToDelete != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Remove memory?") },
                text = { Text("1 memory will be removed from the app. The original will remain on your phone.") },
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
                                    removeRestoredMemory(context, target)
                                    localTeamName?.let { teamName ->
                                        clips = loadRestoredClips(context, teamName)
                                    }
                                    videoToDelete = null
                                }
                            }
                        }
                    ) {
                        Text("Remove")
                    }
                }
            )
        }

        if (showDeleteSelectionConfirmation) {
            val selectedMemories = clips.filter { it.id in deleteSelectionIds }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteSelectionConfirmation = false },
                title = { Text("Remove memories?") },
                text = {
                    Text(
                        "${selectedMemories.size} ${if (selectedMemories.size == 1) "memory" else "memories"} " +
                            "will be removed from the app. The ${if (selectedMemories.size == 1) "original" else "originals"} " +
                            "will remain on your phone."
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSelectionConfirmation = false }) { Text("Cancel") }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteSelectionConfirmation = false
                            showDeletePicker = false
                            scope.launch {
                                selectedMemories.forEach { removeRestoredMemory(context, it) }
                                clips = if (showAllTeams) {
                                    subscribedTeams.flatMap { team -> loadRestoredClips(context, team.name) }
                                        .distinctBy { it.id }
                                        .sortedByDescending { it.createdAt }
                                } else {
                                    localTeamName?.let { loadRestoredClips(context, it) }.orEmpty()
                                }
                                deleteSelectionIds = emptySet()
                            }
                        }
                    ) { Text("Remove") }
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
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleHighlight: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showActionsMenu by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    var commentary by remember(clip.id, clip.filePath) {
        mutableStateOf(loadClipCommentary(context, clip))
    }
    var showCommentEditor by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    val previewBitmap by produceState<Bitmap?>(initialValue = null, clip.id, clip.filePath) {
        value = withContext(Dispatchers.IO) {
            loadRestoredClipThumbnail(context, clip.filePath)
        }
    }
    val isReel = remember(clip.momentTag, clip.gameTitle) { isReelTaggedClip(clip) }
    val label = if (isReel) {
        reelDisplayTitleForClip(clip)
    } else {
        clip.momentTag?.takeIf { it.isNotBlank() } ?: "Memory"
    }
    val labelColor = if (isReel) Color(0xFF2B2F33) else momentTagConfig(clip.momentTag).second

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = reelMode) { onToggleReelSelection() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .background(Color.Black)
                .clickable(enabled = !reelMode, onClick = onPlay),
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

            if (!reelMode) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 6.dp, top = 6.dp, end = 40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(labelColor.copy(alpha = 0.88f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showActionsMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Memory actions",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showActionsMenu,
                        onDismissRequest = { showActionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit memory") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showActionsMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove memory") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { showActionsMenu = false; onDelete() }
                        )
                    }
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(34.dp)
                ) {
                    Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share memory",
                            tint = Color(0xFF1C8CFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleHighlight,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                ) {
                    if (clip.isHighlight) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Remove favorite",
                            tint = PlaysAccentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Add favorite",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (reelMode) {
                Surface(
                    onClick = onToggleReelSelection,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp),
                    shape = CircleShape,
                    color = if (isSelectedForReel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                    contentColor = if (isSelectedForReel) Color.White else Color(0xFF555B61),
                    border = if (isSelectedForReel) null else BorderStroke(1.5.dp, Color(0xFF555B61))
                ) {
                    if (isSelectedForReel) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected memory",
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
        }
        if (reelMode) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            )
        } else if (!isReel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCommentEditor = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (commentary.isBlank()) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = commentary.takeIf(String::isNotBlank) ?: "What Happened",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        }
    }

    if (showCommentEditor) {
        val keyboardController = LocalSoftwareKeyboardController.current
        AlertDialog(
            onDismissRequest = { showCommentEditor = false },
            title = { Text(if (commentary.isBlank()) "What Happened" else "Edit Comment") },
            text = {
                OutlinedTextField(
                    value = commentary,
                    onValueChange = { commentary = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add notes about this memory") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        persistClipCommentary(context, clip, commentary)
                        showCommentEditor = false
                    }
                ) { Text(if (commentary.isBlank()) "Add" else "Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCommentEditor = false }) { Text("Cancel") }
            }
        )
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
        val moment = clip.momentTag?.trim()?.takeIf { it.isNotEmpty() } ?: "Memory"
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

private suspend fun loadConfidentClipPlayers(
    context: android.content.Context,
    clip: VideoClip
): List<ClipPlayerSuggestion> {
    val dao = PlayerDatabase.getDatabase(context).videoDetectionResultDao()
    val uriPath = Uri.parse(clip.filePath).path
    val entity = listOfNotNull(clip.id, clip.filePath, uriPath)
        .distinct()
        .firstNotNullOfOrNull { key -> dao.getDetectionResult(key) }
        ?: return emptyList()
    val result = DetectionResultSerializer.deserialize(entity.detectionJson) ?: return emptyList()
    val minimumConfidence = if (entity.detectionMode.equals("ACCURATE", ignoreCase = true)) 0.90f else 0.72f
    val bubblesById = result.bubbles.associateBy { it.id }

    return result.tracks.mapNotNull { track ->
        if (track.samples.isEmpty()) return@mapNotNull null
        val confidence = track.samples.map { it.confidence }.average().toFloat()
        val bubble = bubblesById[track.playerId] ?: return@mapNotNull null
        if (confidence < minimumConfidence || bubble.playerName.isBlank()) return@mapNotNull null
        ClipPlayerSuggestion(
            name = bubble.playerName,
            jerseyNumber = bubble.jerseyNumber,
            confidence = confidence
        )
    }
        .distinctBy { it.name.lowercase(Locale.getDefault()) }
        .sortedByDescending { it.confidence }
        .take(3)
}

private suspend fun loadRecentTeammateReviewClips(
    context: android.content.Context,
    clips: List<VideoClip>,
    roster: List<Player>
): List<TeammateReviewClip> {
    val cutoff = System.currentTimeMillis() - RECENT_CLIP_WINDOW_MS
    val reviewPrefs = context.getSharedPreferences(TEAMMATE_REVIEW_PREFS, android.content.Context.MODE_PRIVATE)
    val reviewClips = mutableListOf<TeammateReviewClip>()
    clips.asSequence()
        .filterNot(::isReelTaggedClip)
        .filter { it.createdAt >= cutoff && !reviewPrefs.getBoolean(it.id, false) }
        .forEach { clip ->
            val players = loadConfidentClipPlayers(context, clip)
            if (players.isNotEmpty()) reviewClips += TeammateReviewClip(clip, players)
        }
    if (BuildConfig.DEBUG && !reviewPrefs.getBoolean(TEAMMATE_DEMO_REVIEW_ID, false)) {
        clips.filterNot(::isReelTaggedClip).maxByOrNull { it.createdAt }?.let { sourceClip ->
            val rosterPlayers = roster.take(3).mapIndexed { index, player ->
                ClipPlayerSuggestion(
                    name = player.name,
                    jerseyNumber = player.number,
                    confidence = listOf(0.98f, 0.94f, 0.91f)[index]
                )
            }
            val fallbackPlayers = listOf(
                ClipPlayerSuggestion("Alex Morgan", "7", 0.98f),
                ClipPlayerSuggestion("Jordan Lee", "12", 0.94f),
                ClipPlayerSuggestion("Taylor Smith", "21", 0.91f)
            )
            val mockPlayers = (rosterPlayers + fallbackPlayers)
                .distinctBy { it.name.lowercase(Locale.getDefault()) }
                .take(3)
            return listOf(
                TeammateReviewClip(
                    clip = sourceClip.copy(
                        id = TEAMMATE_DEMO_REVIEW_ID,
                        createdAt = System.currentTimeMillis(),
                        momentTag = "Demo memory"
                    ),
                    players = mockPlayers,
                    isDemo = true
                )
            )
        }
    }
    return reviewClips.distinctBy { it.clip.id }.sortedByDescending { it.clip.createdAt }
}

private fun markTeammateReviewComplete(
    context: android.content.Context,
    clip: VideoClip,
    playerNames: List<String>
) {
    val editor = context.getSharedPreferences(TEAMMATE_REVIEW_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putBoolean(clip.id, true)
        .putString("${clip.id}:players", JSONArray(playerNames).toString())
    if (clip.id != TEAMMATE_DEMO_REVIEW_ID) editor.putBoolean(clip.filePath, true)
    editor.apply()
}

internal fun loadPendingSharedMemoryCounts(context: android.content.Context): Map<String, Int> {
    val prefs = context.getSharedPreferences(TEAMMATE_REVIEW_PREFS, android.content.Context.MODE_PRIVATE)
    return prefs.all.asSequence()
        .filter { (key, value) -> key.endsWith(":players") && value is String }
        .flatMap { (_, value) ->
            val players = runCatching { JSONArray(value as String) }.getOrNull() ?: return@flatMap emptySequence()
            sequence {
                for (index in 0 until players.length()) {
                    players.optString(index).trim().takeIf(String::isNotBlank)?.let { yield(it.lowercase(Locale.getDefault())) }
                }
            }
        }
        .groupingBy { it }
        .eachCount()
}

@Composable
private fun TeammateReviewScreen(
    clips: List<TeammateReviewClip>,
    onBack: () -> Unit,
    onReview: (TeammateReviewClip) -> Unit
) {
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("Share with Families", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE7F8ED),
                    contentColor = Color(0xFF075D32)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(38.dp))
                        Text(
                            "These memories include other players from your team. Review and share the moments with their families.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                Text(
                    "${clips.size} ${if (clips.size == 1) "memory" else "memories"} ready to share",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(clips, key = { it.clip.id }) { review ->
                TeammateReviewRow(review = review, onReview = { onReview(review) })
            }
        }
    }
}

@Composable
private fun TeammateReviewRow(review: TeammateReviewClip, onReview: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val thumbnail by produceState<Bitmap?>(null, review.clip.filePath) {
        value = withContext(Dispatchers.IO) { loadRestoredClipThumbnail(context, review.clip.filePath) }
    }
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 76.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                thumbnail?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDFF3E6),
                    contentColor = Color(0xFF08743B)
                ) {
                    Text(
                        review.clip.momentTag?.takeIf { it.isNotBlank() } ?: "Play",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    review.players.joinToString { player ->
                        player.name + player.jerseyNumber.takeIf(String::isNotBlank)?.let { " #$it" }.orEmpty()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onReview,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF079447), contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("Review") }
        }
    }
}

@Composable
private fun TeammateClipReviewScreen(
    reviewClip: TeammateReviewClip,
    roster: List<Player>,
    onBack: () -> Unit,
    onFullscreen: () -> Unit,
    onShare: (List<String>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val detectedNames = remember(reviewClip) { reviewClip.players.map { it.name }.toSet() }
    var selectedNames by remember(reviewClip.clip.id) { mutableStateOf(detectedNames) }
    var showFullRoster by rememberSaveable(reviewClip.clip.id) { mutableStateOf(false) }
    val player = remember(reviewClip.clip.filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(reviewClip.clip.filePath)))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    BackHandler {
        if (showFullRoster) showFullRoster = false else onBack()
    }

    if (showFullRoster) {
        TeammateRosterPickerScreen(
            roster = roster,
            selectedNames = selectedNames,
            onBack = { showFullRoster = false },
            onSelectionChanged = { selectedNames = it }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Share this memory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(230.dp).background(Color.Black)
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowRewindButton(false)
                        setShowFastForwardButton(false)
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.visibility = android.view.View.GONE
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onFullscreen,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "View full screen", tint = Color.White)
            }
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Who was involved?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("PlayLoom suggested these players. You decide who to include.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            reviewClip.players.forEach { suggestion ->
                PlayerShareChoice(
                    name = suggestion.jerseyNumber.takeIf(String::isNotBlank)
                        ?.let { "#$it ${suggestion.name}" }
                        ?: suggestion.name,
                    detail = "",
                    selected = suggestion.name in selectedNames,
                    onSelectedChange = { selected ->
                        selectedNames = if (selected) selectedNames + suggestion.name else selectedNames - suggestion.name
                    }
                )
            }
            TextButton(
                onClick = { showFullRoster = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add another player", modifier = Modifier.fillMaxWidth())
            }
        }
        Button(
            enabled = selectedNames.isNotEmpty(),
            onClick = { onShare(selectedNames.sorted()) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF208A4B),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share it")
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Not now", color = Color(0xFF079447), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TeammateRosterPickerScreen(
    roster: List<Player>,
    selectedNames: Set<String>,
    onBack: () -> Unit,
    onSelectionChanged: (Set<String>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("Add players", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Full roster", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(roster, key = { it.id }) { player ->
                val selected = player.name in selectedNames
                PlayerShareChoice(
                    name = player.name,
                    detail = player.number.takeIf(String::isNotBlank)?.let { "#$it" }.orEmpty(),
                    selected = selected,
                    onSelectedChange = { checked ->
                        onSelectionChanged(if (checked) selectedNames + player.name else selectedNames - player.name)
                    }
                )
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp)) {
            Text("Done")
        }
    }
}

@Composable
private fun PlayerShareChoice(
    name: String,
    detail: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectedChange(!selected) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) Color(0xFF208A4B) else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .background(if (selected) Color(0xFF208A4B) else Color.Transparent, CircleShape)
                .clickable { onSelectedChange(!selected) },
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(name, modifier = Modifier.weight(1f))
        if (detail.isNotBlank()) Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private suspend fun removeRestoredMemory(context: android.content.Context, video: VideoClip) {
    withContext(Dispatchers.IO) {
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
    val playbackMetadata = remember(clip) { loadPlaybackClipMetadata(context, clip) }

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
                    text = if (allowTrim) "Trim memory" else "View memory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
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

                ClipMetadataTicket(
                    metadata = playbackMetadata,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 14.dp, start = 14.dp)
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
                        text = "Loading memory...",
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
                            Text("Full memory")
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
                    enabled = !isSaving && orderedIds.size >= 2,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            }
            if (!isSaving) Text("Review the title, clips and order. Save creates the reel.",
                Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)

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
                        text = "Memories in this reel (${orderedIds.size})",
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
                        Text("Add Memories")
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
            VideoPlaybackScreen(
                videoUri = Uri.parse(previewClip.filePath),
                clipMetadata = previewClip,
                detectedPlayers = emptyList(),
                onNavigateBack = { showFullScreenPreview = false }
            )
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
                text = clip?.momentTag?.takeIf { it.isNotBlank() } ?: clip?.gameTitle ?: "Memory",
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
                    text = "Add Memories",
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
                            text = "No memories match your filters",
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
    com.playerid.app.ui.components.MemoryFilterMenuLabel(label, isActive, onClick)
}

@Composable
private fun ClipsFileTab(
    label: String,
    highlighted: Boolean,
    underlined: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    com.playerid.app.ui.components.MemoryCollectionTab(label, highlighted, underlined, enabled, onClick)
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
    opponentName: String?,
    opponentOptions: List<String>,
    onOpponentChanged: (String?) -> Unit,
    onTitleChanged: (String) -> Unit,
    onEditReel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var commentary by remember(clip.id, clip.filePath) {
        mutableStateOf(loadClipCommentary(context, clip))
    }
    var isCommentaryExpanded by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    val suggestedPlayers by produceState<List<ClipPlayerSuggestion>?>(
        initialValue = null,
        key1 = clip.id,
        key2 = clip.filePath
    ) {
        value = withContext(Dispatchers.IO) { loadConfidentClipPlayers(context, clip) }
    }
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
        swipeOffsetTarget = (swipeOffsetTarget + delta).coerceIn(-actionWidthPx, 0f)
    }
    val rowScope = rememberCoroutineScope()
    var showTagPicker by remember(clip.id, clip.filePath) { mutableStateOf(false) }
    var showOpponentEditor by remember(clip.id, clip.filePath) { mutableStateOf(false) }
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
            if (swipeOffset < 0f) {
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
                            Text("Remove", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
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
                                        text = if (clipCount != null) "$clipCount ${if (clipCount == 1) "memory" else "memories"}" else "View memories",
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
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = tagStartPadding, top = 6.dp)
                                .padding(end = 46.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 150.dp)
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
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White,
                                onClick = { showOpponentEditor = true }
                            ) {
                                Text(
                                    text = opponentName?.takeIf(String::isNotBlank)?.let { "vs $it" } ?: "vs Opponent",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 150.dp).padding(horizontal = 8.dp, vertical = 4.dp)
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
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
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
                                    color = if (isSelectedForReel) MaterialTheme.colorScheme.primary else Color(0xFF555B61),
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

                    suggestedPlayers?.takeIf { !isReel && it.isNotEmpty() }?.let { players ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Players spotted",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                players.forEach { suggestion ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = buildString {
                                                append(suggestion.name)
                                                if (suggestion.jerseyNumber.isNotBlank()) append(" #${suggestion.jerseyNumber}")
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (!reelMode || clip.isHighlight) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                        )
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
                                    icon = Icons.Default.ContentCut,
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
                                placeholder = { Text("Add notes about this memory") },
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

        if (showOpponentEditor) {
            OpponentEditorDialog(
                currentOpponent = opponentName,
                opponentOptions = opponentOptions,
                onDismiss = { showOpponentEditor = false },
                onSave = { opponent ->
                    onOpponentChanged(opponent)
                    showOpponentEditor = false
                },
                onRemove = {
                    onOpponentChanged(null)
                    showOpponentEditor = false
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

@Composable
private fun OpponentEditorDialog(
    currentOpponent: String?,
    opponentOptions: List<String>,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    onRemove: () -> Unit
) {
    var opponentInput by remember(currentOpponent) { mutableStateOf(currentOpponent.orEmpty()) }
    val matchingOptions = remember(opponentInput, opponentOptions) {
        val query = opponentInput.trim()
        if (query.isEmpty()) opponentOptions.take(5) else opponentOptions
            .filter { it.contains(query, ignoreCase = true) }
            .sortedBy { if (it.startsWith(query, ignoreCase = true)) 0 else 1 }
            .take(5)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit opponent") },
        text = {
            Column {
                OutlinedTextField(
                    value = opponentInput,
                    onValueChange = { opponentInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Opponent name") },
                    placeholder = { Text("Enter opponent") },
                    singleLine = true
                )
                matchingOptions.forEach { option ->
                    TextButton(
                        onClick = { opponentInput = option },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(option, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = opponentInput.trim()
                    val canonical = opponentOptions.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
                    onSave(canonical.takeIf(String::isNotEmpty))
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!currentOpponent.isNullOrBlank()) TextButton(onClick = onRemove) { Text("Remove") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
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

private fun persistClipOpponent(
    context: android.content.Context,
    clip: VideoClip,
    opponentName: String?
) {
    val prefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
    val keys = listOfNotNull(clip.id, clip.filePath, Uri.parse(clip.filePath).path).distinct()
    prefs.edit().apply {
        keys.forEach { key ->
            if (opponentName.isNullOrBlank()) remove(key) else putString(key, opponentName.trim())
        }
    }.apply()
}

private fun filterRestoredClipsBySearchQuery(
    videos: List<VideoClip>,
    query: String,
    kidLookup: Map<String, String?>,
    opponentLookup: Map<String, String?>,
    interpretation: AskPlayloomQuery? = null,
    teamLookup: Map<String, String?> = emptyMap(),
    noteLookup: Map<String, String> = emptyMap(),
    reelSources: Map<String, List<VideoClip>> = emptyMap()
): List<VideoClip> {
    if (interpretation?.issue != null) return emptyList()
    val searchQuery = interpretation?.filters ?: parseMemorySearchQuery(query)
    val dateFrom = searchQuery.dateFrom
    if (searchQuery.momentTag == null && searchQuery.seasonIntent == null && searchQuery.terms.isEmpty() &&
        searchQuery.dateFrom == null && searchQuery.resultSelection == null &&
        !searchQuery.favoritesOnly && searchQuery.mediaType == null && searchQuery.seasonYear == null &&
        interpretation?.players.orEmpty().isEmpty() && interpretation?.teams.orEmpty().isEmpty() &&
        interpretation?.opponents.orEmpty().isEmpty()) {
        return videos
    }

    val currentSeasonStartYear = java.time.LocalDate.now().year
    val requestedSeasonStartYear = searchQuery.seasonYear?.toIntOrNull() ?: when (searchQuery.seasonIntent) {
        MemorySeasonIntent.PREVIOUS -> currentSeasonStartYear - 1
        MemorySeasonIntent.CURRENT -> currentSeasonStartYear
        else -> null
    }

    val knownOpponents = opponentLookup.values.filterNotNull()
    val matches = videos.filter { video ->
        val sources = if (interpretation != null && isReelTaggedClip(video)) reelSources[video.id].orEmpty()
            else listOf(video)
        val assignedSources = if (interpretation == null) sources else sources.filter { source ->
            interpretation.matchesAssignment(null, kidLookup[source.id], teamLookup[source.id], opponentLookup[source.id])
        }
        val hasIdentityFilter = interpretation != null && (interpretation.players.isNotEmpty() ||
            interpretation.teams.isNotEmpty() || interpretation.opponents.isNotEmpty())
        if (hasIdentityFilter && assignedSources.isEmpty()) return@filter false
        if (dateFrom != null) {
            val capturedDate = java.time.Instant.ofEpochMilli(video.createdAt)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
            if (capturedDate < dateFrom || capturedDate > searchQuery.dateThrough.orEmpty()) return@filter false
        }
        if (searchQuery.favoritesOnly && !video.isHighlight) return@filter false
        if (searchQuery.mediaType == "photo") return@filter false
        if (searchQuery.mediaType == "reel" && !isReelTaggedClip(video)) return@filter false
        val metadataSources = if (assignedSources.isEmpty() && !hasIdentityFilter) listOf(video) else assignedSources
        if (metadataSources.none { source ->
                (requestedSeasonStartYear == null ||
                    (parseRestoredSeasonKey(source.gameDate)?.startYear
                        ?: java.time.Instant.ofEpochMilli(source.createdAt)
                            .atZone(java.time.ZoneId.systemDefault()).year) == requestedSeasonStartYear) &&
                    (searchQuery.momentTag == null || canonicalMomentTag(source.momentTag) == searchQuery.momentTag)
            }) return@filter false

        val bubbleNames = if (interpretation != null) emptyList() else try {
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
            opponentLookup[video.id].orEmpty(),
            noteLookup[video.id].orEmpty()
        ) + bubbleNames

        val opponent = opponentLookup[video.id]
        val opponentAlias = opponent?.let { name ->
            com.playerid.app.domain.team.opponentSearchName(name, knownOpponents)
        }.orEmpty()
        val searchable = (searchableText + opponentAlias).joinToString(" ")
            .lowercase(Locale.getDefault())
            .replace("’s", " ")
            .replace("'s", " ")
            .map { if (it.isLetterOrDigit() || it == '#') it else ' ' }.joinToString("")
        val compact = searchable.replace(" ", "")

        searchQuery.terms.all { term ->
            searchable.contains(term) || compact.contains(term) ||
                (term.endsWith("s") && term.length > 3 && searchable.contains(term.dropLast(1)))
        }
    }

    return when {
        searchQuery.resultSelection == MemoryResultSelection.FIRST -> matches.sortedBy { it.createdAt }.take(searchQuery.resultLimit)
        searchQuery.resultSelection == MemoryResultSelection.LAST -> matches.sortedByDescending { it.createdAt }.take(searchQuery.resultLimit)
        else -> matches
    }
}

private fun currentRestoredSeasonStartYear(): Int {
    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    return if (month >= 8) year else year - 1
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

private fun buildRestoredClipTeamLookup(
    context: android.content.Context,
    videos: List<VideoClip>
): Map<String, String?> {
    val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
    return videos.associate { video ->
        val fileUri = Uri.parse(video.filePath)
        val filePath = fileUri.path
        video.id to (
            teamPrefs.getString(video.id, null)
                ?: teamPrefs.getString(video.filePath, null)
                ?: filePath?.let { teamPrefs.getString(it, null) }
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
            val displayTitle = opponent?.split(" ")?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                ?: PROVIDE_OPPONENT_HEADER
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
                title = PROVIDE_OPPONENT_HEADER,
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
                    title = PROVIDE_OPPONENT_HEADER,
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

private fun buildReelPickerMemoryGroups(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<ReelPickerMemoryGroup> {
    return videos
        .groupBy { clip ->
            val opponent = opponentLookup[clip.id]?.trim()?.takeIf(String::isNotBlank)
            val season = parseRestoredSeasonKey(clip.gameDate)
            (opponent ?: "Opponent not set") to season
        }
        .map { (groupKey, groupedVideos) ->
            val (opponent, season) = groupKey
            ReelPickerMemoryGroup(
                key = "${opponent.lowercase(Locale.getDefault())}|${season?.key ?: "unknown"}",
                opponentLabel = opponent,
                seasonLabel = season?.let { "${it.label} Season" } ?: "Season not set",
                videos = groupedVideos.sortedByDescending { it.createdAt }
            )
        }
        .sortedWith(
            compareBy<ReelPickerMemoryGroup> { it.opponentLabel.lowercase(Locale.getDefault()) }
                .thenByDescending { group ->
                    group.videos.firstNotNullOfOrNull { parseRestoredSeasonKey(it.gameDate)?.startYear }
                        ?: Int.MIN_VALUE
                }
        )
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

private fun readPendingClipImport(
    context: android.content.Context,
    uri: Uri
): PendingClipImport {
    var capturedAtMs: Long? = null

    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use
            fun positiveLong(columnName: String): Long? {
                val index = cursor.getColumnIndex(columnName)
                return if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index).takeIf { it > 0L } else null
            }

            capturedAtMs = positiveLong(MediaStore.MediaColumns.DATE_TAKEN)
                ?: positiveLong(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                ?: positiveLong(MediaStore.MediaColumns.DATE_MODIFIED)?.let { seconds -> seconds * 1000L }
                ?: positiveLong(MediaStore.MediaColumns.DATE_ADDED)?.let { seconds -> seconds * 1000L }
        }
    }

    return PendingClipImport(
        uri = uri,
        capturedAtMs = capturedAtMs,
        seasonStartYear = capturedAtMs?.let(::restoredSeasonStartYear)
    )
}

private fun storedClipKeyToUri(key: String): Uri? {
    return when {
        key.startsWith("content://") || key.startsWith("file://") -> Uri.parse(key)
        else -> java.io.File(key).takeIf { it.isFile }?.let(Uri::fromFile)
    }
}

private data class VideoIdentity(
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int
)

private fun readVideoIdentity(context: android.content.Context, uri: Uri): VideoIdentity? = runCatching {
    context.contentResolver.query(
        uri,
        arrayOf(
            MediaStore.MediaColumns.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        ),
        null,
        null,
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
        val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH))
        val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT))
        if (size > 0L && duration > 0L) VideoIdentity(size, duration, width, height) else null
    }
}.getOrNull()

private fun loadCameraRollVideos(
    context: android.content.Context,
    existingClipUris: List<Uri>
): List<CameraRollVideo> {
    val existingIdentities = existingClipUris
        .distinctBy { it.toString() }
        .mapNotNull { readVideoIdentity(context, it) }
        .toSet()
    val importedKeys = context
        .getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        .all
        .keys

    val videos = mutableListOf<CameraRollVideo>()
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DURATION,
        MediaStore.MediaColumns.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT
    )
    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
        val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
        while (cursor.moveToNext() && videos.size < 200) {
            val uri = ContentUris.withAppendedId(collection, cursor.getLong(idIndex))
            val duration = cursor.getLong(durationIndex)
            val identity = VideoIdentity(
                sizeBytes = cursor.getLong(sizeIndex),
                durationMs = duration,
                width = cursor.getInt(widthIndex),
                height = cursor.getInt(heightIndex)
            )
            videos += CameraRollVideo(
                uri = uri,
                durationMs = duration,
                imported = uri.toString() in importedKeys ||
                    identity in existingIdentities
            )
        }
    }
    return videos
}

private fun restoredSeasonStartYear(timestampMs: Long): Int {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
    val year = calendar.get(java.util.Calendar.YEAR)
    return if (calendar.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) year else year - 1
}

private fun restoredSeasonLabel(startYear: Int): String =
    "$startYear-${String.format(Locale.getDefault(), "%02d", (startYear + 1) % 100)}"

private fun timestampInRestoredSeason(capturedAtMs: Long?, seasonStartYear: Int): Long {
    val calendar = java.util.Calendar.getInstance()
    if (capturedAtMs == null) {
        calendar.clear()
        calendar.set(seasonStartYear, java.util.Calendar.AUGUST, 1, 12, 0, 0)
    } else {
        calendar.timeInMillis = capturedAtMs
        val targetYear = if (calendar.get(java.util.Calendar.MONTH) >= java.util.Calendar.AUGUST) {
            seasonStartYear
        } else {
            seasonStartYear + 1
        }
        calendar.set(java.util.Calendar.YEAR, targetYear)
    }
    return calendar.timeInMillis
}

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
