package com.playerid.app.ui.screens

import com.playerid.app.ui.memory.MemoryShareBadge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.AnimatedVisibility
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.playerid.app.ui.components.rememberMemoryFilterScroll
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import com.playerid.app.platform.SpeechRecognitionService
import com.playerid.app.ui.theme.SpotrHighlightOrange
import kotlinx.coroutines.Job
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.MemoryShareResult
import com.playerid.app.domain.memory.selectedMemoriesForSharing
import com.playerid.app.domain.memory.MemoryImportProgress
import com.playerid.app.domain.memory.MemoryImportResult
import com.playerid.app.domain.memory.MemoryImportFailure
import com.playerid.app.domain.team.*
import com.playerid.app.ui.memory.CaptureOrganizationDialog
import com.playerid.app.ui.memory.CaptureOrganizationFilters
import com.playerid.app.ui.memory.CaptureChoiceMenu
import com.playerid.app.ui.components.MemoryCollectionTab
import com.playerid.app.ui.memory.MemoryImportClassificationPage
import com.playerid.app.ui.components.MemoryCollectionGroupHeading
import com.playerid.app.ui.components.CaptureLibrarySearchBar
import com.playerid.app.ui.components.PlayloomGoPage
import com.playerid.app.ui.components.AskPlayloomResults
import com.playerid.app.ui.components.AskPlayloomAnswer
import com.playerid.app.ui.components.MemoryViewerToolbar
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CameraCaptureLibraryPage(
    repository: LocalTeamRepository,
    onNavigate: (LibraryNavigationRequest) -> Unit,
    onOpenMore: () -> Unit,
    goMode: Boolean = false,
    goRecentRequests: MutableState<List<String>>? = null,
    loadCaptures: suspend () -> List<MediaReference>,
    thumbnail: @Composable (MediaReference, Modifier) -> Unit,
    playback: @Composable (MediaReference, Modifier) -> Unit,
    importMedia: (suspend ((MemoryImportProgress) -> Unit) -> MemoryImportResult)? = null,
    shareMedia: (suspend (List<MediaReference>) -> MemoryShareResult)? = null,
    shareMediaWithRecipient: (suspend (List<MediaReference>, String?) -> MemoryShareResult)? = null,
    videoEditor: (@Composable (MediaReference, () -> Unit, (MediaReference) -> Unit) -> Unit)? = null,
    reelBuilder: (@Composable (MemoryReelDraft) -> Unit)? = null,
    removeMedia: (suspend (MediaReference) -> Unit)? = null,
    voiceSearch: SpeechRecognitionService? = null,
    onLongPressHaptic: (() -> Unit)? = null,
    loadDuration: (suspend (MediaReference) -> Long)? = null,
    onFullScreenViewerChange: (Boolean) -> Unit = {},
    playlistPlayback: (@Composable (MediaReference, Modifier, () -> Unit) -> Unit)? = null
) {
    var captures by remember { mutableStateOf<List<MediaReference>>(emptyList()) }
    var selected by remember { mutableStateOf<MediaReference?>(null) }
    var playlist by remember { mutableStateOf<List<MediaReference>>(emptyList()) }
    var playlistIndex by remember { mutableIntStateOf(0) }
    var playlistFinished by remember { mutableStateOf(false) }
    var playlistGeneration by remember { mutableIntStateOf(0) }
    var editingVideo by remember { mutableStateOf(false) }
    var editingFromLibrary by remember { mutableStateOf(false) }
    var buildingReel by remember { mutableStateOf(false) }
    var showingReels by rememberSaveable { mutableStateOf(false) }
    var goBrowsing by rememberSaveable { mutableStateOf(false) }
    val libraryGridState = rememberLazyGridState()
    var filtersExpanded by remember { mutableStateOf(true) }
    val filterScroll = rememberMemoryFilterScroll(libraryGridState) { filtersExpanded = it }
    var reelYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    val currentLoader by rememberUpdatedState(loadCaptures)
    val snapshot by repository.snapshot.collectAsState()
    var teamFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var seasonFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var opponentFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var playerFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var unassignedOnly by rememberSaveable { mutableStateOf(false) }
    var editingDetails by remember { mutableStateOf(false) }
    var detailsTarget by remember { mutableStateOf<MediaReference?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val localRecentRequests = rememberSaveable { mutableStateOf(emptyList<String>()) }
    var recentGoRequests by (goRecentRequests ?: localRecentRequests)
    var goRequestStarted by rememberSaveable { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importProgress by remember { mutableStateOf<MemoryImportProgress?>(null) }
    var importFailures by remember { mutableStateOf<List<MemoryImportFailure>>(emptyList()) }
    var showingImportFailures by remember { mutableStateOf(false) }
    var classifyingImports by remember { mutableStateOf<List<MediaReference>>(emptyList()) }
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var sharing by remember { mutableStateOf(false) }
    var shareMessage by remember { mutableStateOf<String?>(null) }
    var shareError by remember { mutableStateOf<String?>(null) }
    var noteTarget by remember { mutableStateOf<MediaReference?>(null) }
    var removeTarget by remember { mutableStateOf<MediaReference?>(null) }
    var removeBatch by remember { mutableStateOf<List<MediaReference>?>(null) }
    var selectionAction by remember { mutableStateOf(MemorySelectionAction.SHARE) }
    var selectMenuOpen by remember { mutableStateOf(false) }
    var reelOrder by remember { mutableStateOf(emptyList<String>()) }
    var reelTitle by remember { mutableStateOf("") }
    var reelDraft by remember { mutableStateOf<List<MediaReference>?>(null) }
    var completedReel by remember { mutableStateOf<MediaReference?>(null) }
    var reelEditTarget by remember { mutableStateOf<MediaReference?>(null) }
    var reviewingSearchReel by remember { mutableStateOf(false) }
    var reelShareRequested by remember { mutableStateOf(false) }
    var reelShareRecipient by remember { mutableStateOf<String?>(null) }
    var pendingSearchShare by remember { mutableStateOf<Pair<List<MediaReference>, String?>?>(null) }
    var actionAnswer by remember { mutableStateOf<String?>(null) }
    var relatedScheduleGames by remember { mutableStateOf<List<ActionScheduleGame>>(emptyList()) }
    var actionAnswerTitle by remember { mutableStateOf("Action bar") }
    var durations by remember { mutableStateOf(emptyMap<String, Long>()) }
    var removing by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var voiceTranscript by remember { mutableStateOf("") }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var voiceJob by remember { mutableStateOf<Job?>(null) }
    val searchIdeas = remember(snapshot.players, snapshot.assignedPlayerIds, snapshot.games, snapshot.captureOrganizations) {
        captureLibrarySearchIdeas(snapshot)
    }
    val scope = rememberCoroutineScope()
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val filter = normalizeCaptureFilter(
        CaptureLibraryFilter(teamFilter, seasonFilter, opponentFilter, playerFilter, unassignedOnly),
        snapshot
    )
    var lastScheduleAnswer by remember(filter.teamName) { mutableStateOf<ScheduleQuestionAnswer?>(null) }
    val parsedSearch = remember(searchQuery, snapshot.players, snapshot.teams, snapshot.games, snapshot.seasons,
        snapshot.captureOrganizations, seasonFilter) {
        interpretAskPlayloom(searchQuery, snapshot.players, snapshot.teams.map { it.name },
            snapshot.games.map { it.opponentName } + snapshot.captureOrganizations.values.mapNotNull { it.opponentName })
            .withSelectedSeasonYear(snapshot.seasons.firstOrNull { it.id == seasonFilter }?.let(snapshot::captureSeasonYear))
    }
    var searchOverride by remember(searchQuery, parsedSearch) { mutableStateOf<AskPlayloomQuery?>(null) }
    val interpretedSearch = searchOverride ?: parsedSearch
    LaunchedEffect(parsedSearch) {
        if (searchQuery.isNotBlank() && parsedSearch.issue == null) {
            if (parsedSearch.players.isNotEmpty()) playerFilter = null
            if (parsedSearch.teams.isNotEmpty()) teamFilter = null
            if (parsedSearch.opponents.isNotEmpty()) opponentFilter = null
            if (parsedSearch.filters.seasonIntent != MemorySeasonIntent.CURRENT &&
                (parsedSearch.filters.dateFrom != null || parsedSearch.filters.seasonIntent != null ||
                    parsedSearch.filters.seasonYear != null)) seasonFilter = null
            unassignedOnly = false
        }
    }
    val pickingReel = selecting && selectionAction == MemorySelectionAction.REEL
    // Year menu on the Reels tab lists only years that have reels.
    val reelYears = captures.filter { captureFileKey(it) in snapshot.captureReels }
        .mapNotNull { captureDate(it, snapshot)?.year }.distinct().sortedDescending()
    val activeReelYear = reelYear?.takeIf { it in reelYears }
    // Apply collection/year/picker constraints before first/latest selects a limited result.
    val candidates = (if (showingReels) captures else filterCaptures(captures, snapshot, filter)).filter {
        (!pickingReel || it.kind == MediaKind.VIDEO) &&
            (!showingReels || activeReelYear == null || captureDate(it, snapshot)?.year == activeReelYear)
    }
    val visibleCaptures = if (goMode && actionAnswer != null) {
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val known = snapshot.games.map { it.opponentName } +
            snapshot.captureOrganizations.values.mapNotNull { it.opponentName }
        captures.filter { media ->
            val key = captureFileKey(media)
            val details = snapshot.captureOrganizations[key]
            key !in snapshot.captureReels && captureDate(media, snapshot)?.let { it <= today } == true &&
                matchesScheduleOpponent(relatedScheduleGames, details?.teamName,
                    details?.let(snapshot::captureOpponentName), known)
        }
    } else searchCaptureLibrary(
        candidates, snapshot, searchQuery, interpretation = interpretedSearch, reelsOnly = showingReels)
    val playableMatches = com.playerid.app.domain.memory.temporaryMemoryPlaylist(visibleCaptures, snapshot)
    val questionAnswer = answerMemoryQuestion(
        interpretedSearch, visibleCaptures.size, showingReels,
        opponents = visibleCaptures.mapNotNull { media ->
            snapshot.captureOrganizations[captureFileKey(media)]?.let(snapshot::captureOpponentName)
        },
        firstDate = visibleCaptures.firstOrNull()?.let { captureDate(it, snapshot)?.toString() },
        firstLocation = visibleCaptures.firstOrNull()?.let { media ->
            val gameId = snapshot.captureOrganizations[captureFileKey(media)]?.gameId
            snapshot.games.firstOrNull { it.id == gameId }?.locationName
        }
    )
    val selectedForSharing = selectedMemoriesForSharing(visibleCaptures, selectedIds)
    LaunchedEffect(visibleCaptures) {
        selectedIds = selectedForSharing.map { it.identifier }.toSet()
    }
    val toggleSelection: (MediaReference) -> Unit = { media ->
        selectedIds = if (media.identifier in selectedIds) selectedIds - media.identifier else selectedIds + media.identifier
    }
    // Android flow: the Memories grid is the reel picker; Next opens Edit Reel, and Edit Reel's Back/Add Memories return here.
    val startReelPicking: (List<MediaReference>, String) -> Unit = { current, title ->
        dismissKeyboard()
        showingReels = false
        selectionAction = MemorySelectionAction.REEL
        reelOrder = current.map { it.identifier }
        reelTitle = title
        selectedIds = reelOrder.toSet()
        selecting = true
        buildingReel = false
    }
    fun openSearchReelDraft(sources: List<MediaReference>, title: String, share: Boolean = false, recipient: String? = null) {
        if (sources.size < 2) {
            shareError = "A reel needs at least 2 matching videos."
            return
        }
        dismissKeyboard()
        shareError = null
        reelEditTarget = null
        reelTitle = title
        reelDraft = sources.toList()
        reelOrder = emptyList()
        selecting = false
        selectedIds = emptySet()
        selected = null
        showingReels = false
        reviewingSearchReel = true
        reelShareRequested = share
        reelShareRecipient = recipient
        buildingReel = true
    }
    // Reels are never trimmed: editing one reopens Edit Reel with its saved source order and title, like Android.
    val openReelEditor: (MediaReference) -> Unit = { reelMedia ->
        val reel = snapshot.captureReels[captureFileKey(reelMedia)]
        if (reel != null && reelBuilder != null) {
            dismissKeyboard()
            reelEditTarget = reelMedia
            reelTitle = reel.name
            reelDraft = reel.sourceFileKeys.mapNotNull { key -> captures.firstOrNull { captureFileKey(it) == key } }
            selecting = false
            selectedIds = emptySet()
            selected = null
            reviewingSearchReel = false
            reelShareRequested = false
            reelShareRecipient = null
            buildingReel = true
        }
    }
    // Reel commands open a reviewable draft; ordinary searches stay in the current library tab.
    val runActionBarCommand: (String) -> Boolean = command@{ text ->
        if (goMode && text.isNotBlank()) {
            goRequestStarted = true
            recentGoRequests = (listOf(text.trim()) + recentGoRequests)
                .distinctBy { it.lowercase() }.take(4)
        }
        actionAnswer = null
        relatedScheduleGames = emptyList()
        goBrowsing = false
        parseLibraryNavigation(text)?.let { request ->
            dismissKeyboard()
            lastScheduleAnswer = null
            val resolved = resolveLibraryNavigation(request,
                snapshot.teams.filter { it.isActive && !it.isArchived }.map { it.name },
                snapshot.cameraOrganization?.teamName ?: filter.teamName)
            resolved.error?.let { actionAnswerTitle = "Navigation"; actionAnswer = it }
            resolved.request?.let {
                searchQuery = ""
                when (it.destination) {
                    LibraryDestination.MEMORIES -> { showingReels = false; goBrowsing = true }
                    LibraryDestination.REELS -> { showingReels = true; goBrowsing = true }
                    else -> onNavigate(it)
                }
            }
            return@command true
        }
        if (parseMemoryQuestion(text) != null) {
            lastScheduleAnswer = null
            searchQuery = text
            dismissKeyboard()
            return@command true
        }
        val request = parseLibraryActionRequest(text) ?: run {
            lastScheduleAnswer = null
            return@command false
        }
        if (request !is LibraryActionRequest.Schedule) lastScheduleAnswer = null
        actionAnswerTitle = when (request) {
            is LibraryActionRequest.Roster -> "Roster lookup"
            is LibraryActionRequest.Schedule -> "Schedule"
            else -> "Action bar"
        }
        if (request is LibraryActionRequest.Schedule) {
            dismissKeyboard()
            val games = actionScheduleGames(snapshot).filter { filter.teamName == null || it.teamName == filter.teamName }
            val answer = scheduleQuestionAnswer(request, games, previousAnswer = lastScheduleAnswer)
            lastScheduleAnswer = answer
            relatedScheduleGames = answer.games
            showingReels = false
            actionAnswer = answer.text
            return@command true
        }
        if (request is LibraryActionRequest.Roster) {
            dismissKeyboard()
            actionAnswer = answerRosterQuestion(request, snapshot.players, filter.teamName)
            return@command true
        }
        searchQuery = ""
        dismissKeyboard()
        if (loading || importing) {
            actionAnswer = "Wait for the memories to finish loading or importing, then try again."
            return@command true
        }
        when (request) {
            is LibraryActionRequest.Roster -> return@command true
            is LibraryActionRequest.Count -> {
                searchQuery = text
                return@command true
            }
            is LibraryActionRequest.Schedule -> {
                return@command true
            }
            is LibraryActionRequest.Unsupported -> {
                actionAnswer = request.message
                return@command true
            }
            is LibraryActionRequest.Share -> {
                if (shareMediaWithRecipient == null) {
                    actionAnswer = "Sharing isn't available here."
                    return@command true
                }
                val criteria = interpretAskPlayloom(request.query, snapshot.players, snapshot.teams.map { it.name },
                    snapshot.games.map { it.opponentName } + snapshot.captureOrganizations.values.mapNotNull { it.opponentName })
                    .withSelectedSeasonYear(snapshot.seasons.firstOrNull { it.id == seasonFilter }?.let(snapshot::captureSeasonYear))
                if (criteria.issue != null) {
                    actionAnswer = criteria.issue
                    return@command true
                }
                val shareFilter = filter.copy(
                    playerId = if (criteria.players.isNotEmpty()) null else filter.playerId,
                    teamName = if (criteria.teams.isNotEmpty()) null else filter.teamName,
                    opponent = if (criteria.opponents.isNotEmpty()) null else filter.opponent,
                    unassignedOnly = false
                )
                val matches = searchCaptureLibrary(filterCaptures(captures, snapshot, shareFilter), snapshot,
                    request.query, interpretation = criteria, reelsOnly = false)
                searchQuery = request.query
                if (matches.isEmpty()) shareError = "No memories matched \"${request.query}\"."
                else pendingSearchShare = matches to request.recipient
                return@command true
            }
            is LibraryActionRequest.Reel -> Unit
        }
        if (reelBuilder == null || (request.share && shareMediaWithRecipient == null)) {
            actionAnswer = "Reel creation or sharing isn't available here."
            return@command true
        }
        val command = request.command
        val criteria = interpretAskPlayloom(command.searchQuery, snapshot.players, snapshot.teams.map { it.name },
            snapshot.games.map { it.opponentName } + snapshot.captureOrganizations.values.mapNotNull { it.opponentName })
            .withSelectedSeasonYear(snapshot.seasons.firstOrNull { it.id == seasonFilter }?.let(snapshot::captureSeasonYear))
        if (criteria.issue != null) {
            actionAnswer = criteria.issue
            return@command true
        }
        val commandFilter = filter.copy(
            playerId = if (criteria.players.isNotEmpty()) null else filter.playerId,
            teamName = if (criteria.teams.isNotEmpty()) null else filter.teamName,
            opponent = if (criteria.opponents.isNotEmpty()) null else filter.opponent,
            seasonId = if (criteria.filters.seasonIntent == MemorySeasonIntent.CURRENT) filter.seasonId
                else if (criteria.filters.dateFrom != null || criteria.filters.seasonYear != null) null else filter.seasonId,
            unassignedOnly = false
        )
        val candidates = filterCaptures(captures, snapshot, commandFilter)
            .filter { it.kind == MediaKind.VIDEO && captureFileKey(it) !in snapshot.captureReels }
        val sources = com.playerid.app.domain.memory.temporaryMemoryPlaylist(
            searchCaptureLibrary(candidates, snapshot, command.searchQuery, interpretation = criteria, reelsOnly = false), snapshot
        )
        searchQuery = command.searchQuery
        dismissKeyboard()
        when (sources.size) {
            0 -> shareError = "No videos matched \"${command.searchQuery}\". Check the capture dates and saved opponents in Edit memory."
            1 -> shareError = "Only 1 memory matched that reel request. A reel needs at least 2 videos."
            else -> openSearchReelDraft(sources, command.title, request.share, request.recipient)
        }
        true
    }
    LaunchedEffect(pickingReel, selectedIds, loadDuration) {
        val loader = loadDuration ?: return@LaunchedEffect
        if (!pickingReel) return@LaunchedEffect
        captures.filter { it.identifier in selectedIds && it.identifier !in durations }.forEach { media ->
            val millis = try { loader(media) } catch (_: CameraCaptureException) { return@forEach }
            durations = durations + (media.identifier to millis)
        }
    }
    val startShareRequest: (List<MediaReference>, String?) -> Unit = { memories, recipient ->
        if (!sharing && !importing && (shareMedia != null || shareMediaWithRecipient != null)) {
            dismissKeyboard()
            val request = memories.toList()
            sharing = true
            shareMessage = null
            shareError = null
            scope.launch {
                withContext(NonCancellable) {
                    try {
                        if (shareMediaWithRecipient != null) shareMediaWithRecipient(request, recipient)
                        else shareMedia?.invoke(request)
                    } catch (failure: CameraCaptureException) {
                        shareError = failure.message ?: "PlayerID couldn't share these memories."
                    } finally {
                        sharing = false
                    }
                }
            }
        }
    }
    val startShare: (List<MediaReference>) -> Unit = { startShareRequest(it, null) }
    LaunchedEffect(pendingSearchShare) {
        pendingSearchShare?.let { (memories, recipient) ->
            pendingSearchShare = null
            startShareRequest(memories, recipient)
        }
    }

    val toggleVoiceSearch: () -> Unit = {
        val service = voiceSearch
        if (listening) {
            service?.cancel()
            voiceJob?.cancel()
        } else if (service != null) {
            voiceError = null
            voiceTranscript = ""
            listening = true
            voiceJob = scope.launch {
                try {
                    val spoken = service.recognizeOnce { voiceTranscript = it }?.trim()
                    if (!spoken.isNullOrEmpty()) {
                        if (!runActionBarCommand(spoken)) searchQuery = spoken
                    }
                } catch (failure: CameraCaptureException) {
                    voiceError = failure.message ?: "Voice search isn't available right now. You can type your request."
                } finally {
                    listening = false
                }
            }
        }
    }
    DisposableEffect(voiceSearch) { onDispose { voiceSearch?.cancel(); voiceJob?.cancel() } }

    LaunchedEffect(filter) {
        teamFilter = filter.teamName
        seasonFilter = filter.seasonId
        opponentFilter = filter.opponent
        playerFilter = filter.playerId
    }

    LaunchedEffect(loadAttempt) {
        loading = true
        error = null
        try {
            captures = currentLoader()
        } catch (failure: CameraCaptureException) {
            error = failure.message ?: "PlayerID couldn't load saved captures."
        } finally {
            loading = false
        }
    }

    val editedMedia = selected
    if (playlist.isNotEmpty() && playlistPlayback != null) {
        DisposableEffect(Unit) {
            onFullScreenViewerChange(true)
            onDispose { onFullScreenViewerChange(false) }
        }
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            MemoryViewerToolbar(onBack = { playlist = emptyList() }, windowInsets = WindowInsets(0, 0, 0, 0)) {
                Text("${playlistIndex + 1} / ${playlist.size}", color = Color.White)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                key(playlistIndex, playlistGeneration) {
                    val playingIndex = playlistIndex
                    playlistPlayback(playlist[playingIndex], Modifier.fillMaxSize()) {
                        if (playlistIndex == playingIndex && !playlistFinished) {
                            if (playlistIndex < playlist.lastIndex) playlistIndex++ else playlistFinished = true
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(enabled = playlistIndex > 0, onClick = {
                    playlistFinished = false; playlistIndex--
                }) { Text("Previous") }
                TextButton(onClick = {
                    playlistFinished = false; playlistIndex = 0; playlistGeneration++
                }) { Text(if (playlistFinished) "Replay all" else "Restart") }
                TextButton(enabled = playlistIndex < playlist.lastIndex, onClick = {
                    playlistFinished = false; playlistIndex++
                }) { Text("Next") }
            }
            Text("Temporary playlist - oldest first. No reel saved.",
                Modifier.padding(12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val draft = reelDraft
    if (buildingReel && reelBuilder != null && draft != null) {
        // Keyed so Edit Reel restarts for the saved reel after an action-bar command; return stays outside key.
        key(reelEditTarget?.identifier) { reelBuilder(MemoryReelDraft(
            selection = draft,
            title = reelTitle,
            onAddMemories = startReelPicking,
            onBack = { current, title ->
                if (reviewingSearchReel) {
                    buildingReel = false
                    reelDraft = null
                    reviewingSearchReel = false
                    reelEditTarget = null
                    reelShareRequested = false
                    reelShareRecipient = null
                } else if (reelEditTarget == null) startReelPicking(current, title) else {
                    buildingReel = false
                    reelDraft = null
                    reelOrder = emptyList()
                    reelEditTarget = null
                    showingReels = true
                }
            },
            onSaved = onSaved@{ result ->
                reviewingSearchReel = false
                val replaced = reelEditTarget
                captures = (listOf(result) + captures).distinctBy { it.identifier }
                showingReels = true
                buildingReel = false
                reelDraft = null
                reelOrder = emptyList()
                reelEditTarget = null
                selecting = false
                selectedIds = emptySet()
                if (reelShareRequested) {
                    val recipient = reelShareRecipient
                    reelShareRequested = false
                    reelShareRecipient = null
                    loadAttempt++
                    startShareRequest(listOf(result), recipient)
                    return@onSaved
                }
                if (replaced == null) {
                    completedReel = result
                    loadAttempt++
                } else scope.launch {
                    withContext(NonCancellable) {
                        val oldKey = captureFileKey(replaced)
                        val newKey = captureFileKey(result)
                        try {
                            if (oldKey in snapshot.captureFavorites) repository.setCaptureFavorite(newKey, true)
                            snapshot.captureNotes[oldKey]?.let { repository.setCaptureNote(newKey, it) }
                            removeMedia?.invoke(replaced)
                            captures = captures.filterNot { it.identifier == replaced.identifier }
                        } catch (failure: CameraCaptureException) {
                            shareError = "The updated reel was saved, but the old version couldn't be removed. ${failure.message.orEmpty()}"
                        } catch (failure: TeamStorageException) {
                            shareError = "The updated reel was saved, but some of its details couldn't be copied. ${failure.message.orEmpty()}"
                        } finally {
                            loadAttempt++
                        }
                    }
                }
            }
        )) }
        return
    }
    if (classifyingImports.isNotEmpty()) {
        val pending = classifyingImports
        MemoryImportClassificationPage(
            items = pending,
            repository = repository,
            suggestedTeam = teamFilter,
            thumbnail = thumbnail,
            onCancel = {
                // Like Android, backing out imports nothing: remove the copies made from Photos.
                classifyingImports = emptyList()
                scope.launch {
                    withContext(NonCancellable) {
                        var kept = 0
                        pending.forEach { media ->
                            try {
                                removeMedia?.invoke(media) ?: kept++
                                if (removeMedia != null) captures = captures.filterNot { it.identifier == media.identifier }
                            } catch (failure: CameraCaptureException) {
                                kept++
                            } catch (failure: TeamStorageException) {
                                kept++
                            }
                        }
                        importMessage = if (kept == 0) null else "$kept item(s) were kept as Unassigned."
                        loadAttempt++
                    }
                }
            },
            onImported = {
                classifyingImports = emptyList()
                importMessage = "Imported ${pending.size} ${if (pending.size == 1) "memory" else "memories"}."
                loadAttempt++
            }
        )
        return
    }
    if (editingVideo && editedMedia != null && videoEditor != null) {
        videoEditor(editedMedia, {
            editingVideo = false
            if (editingFromLibrary) selected = null
            editingFromLibrary = false
        }) { result ->
            captures = (listOf(result) + captures).distinctBy { it.identifier }
            selected = if (editingFromLibrary) null else result
            editingVideo = false
            editingFromLibrary = false
            shareMessage = "Trimmed copy saved to Memories. Use its thumbnail Share icon to share."
            loadAttempt++
        }
        return
    }

    val goLanding = goMode && !goRequestStarted && !goBrowsing && !selecting
    val submitGoRequest: (String) -> Unit = { text ->
        voiceError = null
        if (!runActionBarCommand(text)) searchQuery = text
    }
    val goSearchBar: @Composable () -> Unit = {
        CaptureLibrarySearchBar(
            query = searchQuery,
            onQueryChange = {
                actionAnswer = null; goBrowsing = false; searchQuery = it
                if (it.isBlank()) goRequestStarted = false
            },
            onVoiceSearch = if (voiceSearch != null) toggleVoiceSearch else null,
            listening = listening,
            onSubmit = submitGoRequest,
            voiceError = voiceError,
            onDismissVoiceError = { voiceError = null }
        )
    }
    Column(Modifier.fillMaxSize()) {
        shareMessage?.let { Text(it, Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall) }
        shareError?.let {
            Text(it, Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        selected?.let { media ->
            DisposableEffect(Unit) {
                onFullScreenViewerChange(true)
                onDispose { onFullScreenViewerChange(false) }
            }
            MemoryViewerToolbar(onBack = { selected = null }, windowInsets = WindowInsets(0, 0, 0, 0)) {
                if (captureFileKey(media) in snapshot.captureReels) {
                    if (reelBuilder != null) IconButton(onClick = { openReelEditor(media) }) {
                        Icon(Icons.Default.Edit, "Edit reel", tint = Color.White)
                    }
                    TextButton(onClick = { detailsTarget = media; editingDetails = true }) {
                        Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit details", color = Color.White)
                    }
                } else {
                    TextButton(onClick = {
                        dismissKeyboard()
                        if (media.kind == MediaKind.VIDEO && videoEditor != null) {
                            editingFromLibrary = false
                            editingVideo = true
                        } else {
                            detailsTarget = media
                            editingDetails = true
                        }
                    }) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit memory", color = Color.White)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black)) {
                playback(media, Modifier.fillMaxSize())
            }
        } ?: run {
            if (!goLanding) {
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
            if (goMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    dismissKeyboard()
                    searchQuery = ""; actionAnswer = null; goBrowsing = false; goRequestStarted = false
                }) { Icon(Icons.Default.ArrowBack, "Back to Playloom Go") }
                Text("Playloom Go", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            goSearchBar()
            if (actionAnswer != null) {
                AskPlayloomAnswer(actionAnswerTitle, actionAnswer.orEmpty())
            } else if (searchQuery.isNotBlank()) AskPlayloomResults(
                interpretedSearch, visibleCaptures.size, showingReels,
                onChange = { searchOverride = it },
                onClear = { searchQuery = ""; searchOverride = null },
                answer = if (loading || importing) null else questionAnswer,
                playableCount = playableMatches.size,
                onPlayAll = if (playlistPlayback == null || loading || importing || showingReels) null else ({
                    dismissKeyboard()
                    playlistIndex = 0
                    playlistFinished = false
                    playlist = playableMatches.toList()
                }),
                onCreateReel = if (reelBuilder == null || showingReels || loading || importing || selecting) null else ({
                    openSearchReelDraft(playableMatches, "Highlight Reel")
                }),
                onShare = if ((shareMedia == null && shareMediaWithRecipient == null) ||
                    loading || importing || sharing || selecting) null else ({
                    dismissKeyboard()
                    selectionAction = MemorySelectionAction.SHARE
                    selectedIds = visibleCaptures.map { it.identifier }.toSet()
                    selecting = true
                })
            )
            Spacer(Modifier.height(6.dp))
            } else Text(
                "Memories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            if (importMedia != null) {
                if (importing && importProgress == null) Text(
                    "Opening Photos...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                importMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                importError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (importing && importProgress != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (importFailures.isNotEmpty()) TextButton(onClick = { showingImportFailures = true }) {
                    Text("View failed items")
                }
            }
            AnimatedVisibility(visible = (filtersExpanded || selecting) && (!goMode || actionAnswer == null)) {
            if (showingReels) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                CaptureChoiceMenu(
                    label = activeReelYear?.toString() ?: "Year",
                    active = activeReelYear != null,
                    allLabel = "All years",
                    choices = reelYears.map { it.toString() to it.toString() },
                    onSelect = { reelYear = it?.toInt() }
                )
            } else CaptureOrganizationFilters(snapshot, filter, remember(captures) { captures.map(::captureFileKey).toSet() }) { updated ->
                teamFilter = updated.teamName
                seasonFilter = updated.seasonId
                opponentFilter = updated.opponent
                playerFilter = updated.playerId
                unassignedOnly = updated.unassignedOnly
            }
            }
            if ((!goMode || actionAnswer == null) && (!goMode || searchQuery.isNotBlank() || goBrowsing || selecting)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Keep every header control on one line; narrower phones get tighter labels instead of wrapping.
            val compactHeader = maxWidth < 400.dp
            val headerButtonPadding = PaddingValues(horizontal = if (compactHeader) 6.dp else 12.dp)
            val selectStyle = if (maxWidth < 340.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(if (compactHeader) 0.dp else 4.dp)) {
                    MemoryCollectionTab("Memories", !showingReels, !showingReels, !importing && !sharing, {
                        showingReels = false; goBrowsing = true; selecting = false; selectedIds = emptySet()
                    }, compact = compactHeader)
                    MemoryCollectionTab("Reels", showingReels, showingReels, reelBuilder != null && !importing && !sharing, {
                        showingReels = true; goBrowsing = true; selecting = false; selectedIds = emptySet()
                    }, compact = compactHeader)
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selecting) TextButton(
                    enabled = !sharing && !removing,
                    contentPadding = headerButtonPadding,
                    onClick = {
                        dismissKeyboard()
                        selecting = false
                        selectedIds = emptySet()
                        reelOrder = emptyList()
                        reelDraft = null
                        reelEditTarget = null
                        reviewingSearchReel = false
                        reelShareRequested = false
                        reelShareRecipient = null
                    }
                ) { Text("Cancel", style = selectStyle, fontWeight = FontWeight.SemiBold) }
                else Box {
                    IconButton(onClick = { dismissKeyboard(); selectMenuOpen = true }) {
                        Icon(Icons.Default.MoreHoriz, "Memory actions")
                    }
                    DropdownMenu(expanded = selectMenuOpen, onDismissRequest = { selectMenuOpen = false }) {
                if (importMedia != null) {
                    DropdownMenuItem(
                        text = { Text("Upload memory") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        enabled = !importing && !loading && !sharing && !selecting,
                        onClick = {
                            selectMenuOpen = false
                            dismissKeyboard()
                            importing = true
                            importMessage = null
                            importError = null
                            importProgress = null
                            importFailures = emptyList()
                            scope.launch {
                                withContext(NonCancellable) {
                                    try {
                                        val result = importMedia { importProgress = it }
                                        if (result.imported.isNotEmpty()) {
                                            captures = (result.imported + captures).distinctBy { it.identifier }
                                            showingReels = false
                                            classifyingImports = result.imported
                                        }
                                        importFailures = result.failures
                                        if (result.failures.isNotEmpty()) importError =
                                            "${result.failures.size} item(s) could not be imported. The rest are ready to classify."
                                    } catch (failure: CameraCaptureException) {
                                        importError = failure.message ?: "PlayerID couldn't import these items."
                                    } finally {
                                        loadAttempt++
                                        importing = false
                                    }
                                }
                            }
                        }
                    )
                }
                        if (removeMedia != null) DropdownMenuItem(
                            enabled = !loading && !importing && !sharing && !removing && visibleCaptures.isNotEmpty(),
                            text = { Text(if (showingReels) "Remove reels" else "Remove memories") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = {
                                selectMenuOpen = false
                                selectionAction = MemorySelectionAction.REMOVE
                                selectedIds = emptySet()
                                selecting = true
                            }
                        )
                        if (reelBuilder != null && !showingReels) DropdownMenuItem(
                            enabled = !loading && !importing && !sharing && !removing,
                            text = { Text("Create reel") },
                            leadingIcon = { Icon(Icons.Default.Movie, null) },
                            onClick = {
                                selectMenuOpen = false
                                reelEditTarget = null
                                val team = filter.teamName ?: snapshot.cameraOrganization?.teamName
                                startReelPicking(emptyList(), team?.let { "$it Highlight Reel" } ?: "Highlight Reel")
                            }
                        )
                        if (shareMedia != null) DropdownMenuItem(
                            enabled = !loading && !importing && !sharing && !removing && visibleCaptures.isNotEmpty(),
                            text = { Text(if (showingReels) "Share reels" else "Share memories") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                selectMenuOpen = false
                                selectionAction = MemorySelectionAction.SHARE
                                selectedIds = emptySet()
                                selecting = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Teams") },
                            leadingIcon = { Icon(Icons.Default.Groups, null) },
                            onClick = {
                                selectMenuOpen = false
                                onNavigate(LibraryNavigationRequest(LibraryDestination.TEAMS))
                            }
                        )
                        if (!goMode) DropdownMenuItem(
                            text = { Text("Settings & more") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { selectMenuOpen = false; onOpenMore() }
                        )
                    }
                }
            }
            }
            }
            }
            if (pickingReel) {
                val picked = visibleCaptures.filter { it.identifier in selectedIds }
                val totalMillis = picked.takeIf { list -> list.isNotEmpty() && list.all { it.identifier in durations } }
                    ?.sumOf { durations.getValue(it.identifier) }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Select memories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${picked.size} ${if (picked.size == 1) "memory" else "memories"} selected",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        totalMillis?.let {
                            Text("Total ${formatReelDuration(it)}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(enabled = picked.isNotEmpty() && !loading, onClick = {
                        val pickedIds = picked.map { it.identifier }.toSet()
                        val kept = reelOrder.filter { it in pickedIds }
                        val orderedIds = kept + com.playerid.app.domain.memory.chronologicalReelSources(picked, snapshot)
                            .map { it.identifier }.filterNot { it in kept }
                        reelDraft = orderedIds.mapNotNull { id -> picked.firstOrNull { it.identifier == id } }
                        selecting = false
                        buildingReel = true
                    }) { Text("Next") }
                }
            } else if (selecting) {
                val removingSelection = selectionAction == MemorySelectionAction.REMOVE
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedForSharing.size} selected", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall)
                    if (removingSelection) Button(
                        enabled = selectedForSharing.isNotEmpty() && !removing && !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = { removeBatch = selectedForSharing }
                    ) { Text("Remove") }
                    else Button(enabled = selectedForSharing.isNotEmpty() && !sharing && !loading,
                        onClick = { startShare(selectedForSharing) }) {
                        Text(if (sharing) "Sharing..." else "Share")
                    }
                }
                Text(
                    "Select ${if (showingReels) "reels" else "memories"} to ${if (removingSelection) "remove" else "share"}. Changing search or filters removes hidden selections.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
            Surface(Modifier.fillMaxWidth().height(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)))
            }
            }
            if (goMode && actionAnswer != null && relatedScheduleGames.isNotEmpty() && !loading && error == null) {
                Text(if (visibleCaptures.isEmpty()) "No past saved memories against this opponent."
                    else "Past memories against ${relatedScheduleGames.map { it.opponent }.distinct().joinToString()}",
                    Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
            }
            when {
                goLanding -> PlayloomGoPage(
                    hints = searchIdeas,
                    recentRequests = recentGoRequests,
                    onSubmit = submitGoRequest,
                    onClearRecent = { recentGoRequests = emptyList() },
                    searchBar = goSearchBar
                )
                loading -> Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Column(
                    Modifier.fillMaxSize().weight(1f).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { loadAttempt++ }) { Text("Retry") }
                }
                goMode && actionAnswer != null && visibleCaptures.isEmpty() ->
                    Spacer(Modifier.fillMaxSize().weight(1f))
                goMode && actionAnswer == null && searchQuery.isBlank() && !goBrowsing && !selecting ->
                    Spacer(Modifier.fillMaxSize().weight(1f))
                !showingReels && questionAnswer != null && visibleCaptures.isEmpty() ->
                    Spacer(Modifier.fillMaxSize().weight(1f))
                captures.isEmpty() -> Box(Modifier.fillMaxSize().weight(1f).padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No captures yet. Photos and videos you take from Camera will appear here.")
                }
                visibleCaptures.isEmpty() -> Column(
                    Modifier.fillMaxSize().weight(1f).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (showingReels) "No reels match your search. Create reels from the Memories tab with Select > Create reel."
                        else "No memories match your search and filters.")
                    TextButton(onClick = {
                        teamFilter = null; seasonFilter = null; opponentFilter = null; reelYear = null
                        playerFilter = null; unassignedOnly = false
                        searchQuery = ""
                        dismissKeyboard()
                    }) { Text("Clear search and filters") }
                }
                else -> BoxWithConstraints(Modifier.fillMaxSize().weight(1f)) {
                    val groups = if (showingReels) groupReelsByYear(visibleCaptures, snapshot).map {
                        Triple(it.reels, it.year?.toString() ?: "Undated", "")
                    } else groupCaptureLibrary(visibleCaptures, snapshot).map {
                        Triple(it.captures, it.opponent ?: "No opponent", captureGroupDateLabel(it))
                    }
                    LazyVerticalGrid(
                        modifier = Modifier.nestedScroll(filterScroll).fillMaxSize(),
                        state = libraryGridState,
                        columns = GridCells.Fixed(if (maxWidth >= 600.dp) 3 else 2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groups.forEachIndexed { index, (groupCaptures, title, subtitle) ->
                            item(key = "group-$index", span = { GridItemSpan(maxLineSpan) }) {
                                MemoryCollectionGroupHeading(
                                    teamLabel = null,
                                    title = title,
                                    subtitle = subtitle
                                )
                            }
                            items(groupCaptures, key = MediaReference::identifier) { media ->
                                var actionsExpanded by remember { mutableStateOf(false) }
                                val key = captureFileKey(media)
                                val reel = snapshot.captureReels[key]
                                val favorite = key in snapshot.captureFavorites
                                Card(shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(
                                        Modifier.fillMaxWidth().height(124.dp).background(Color.Black)
                                            .then(if (selecting) Modifier.toggleable(
                                                value = media.identifier in selectedIds,
                                                enabled = !sharing, role = Role.Checkbox,
                                                onValueChange = { toggleSelection(media) }
                                            ) else Modifier.combinedClickable(
                                                enabled = !sharing,
                                                onLongClick = if (removeMedia != null || reel != null) ({
                                                    onLongPressHaptic?.invoke()
                                                        ?: haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                    dismissKeyboard(); actionsExpanded = true
                                                }) else null,
                                                onClick = { dismissKeyboard(); selected = media }
                                            ))
                                    ) {
                                        thumbnail(media, Modifier.fillMaxSize())
                                        val (badgeLabel, badgeColor) = captureLibraryBadge(
                                            reel?.name, snapshot.captureMomentTags[key], MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // Reel cards show no tag.
                                        if (reel == null) Text(badgeLabel, style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.align(Alignment.TopStart)
                                                .padding(start = 6.dp, top = 6.dp, end = if (selecting) 40.dp else 6.dp)
                                                .background(badgeColor.copy(alpha = 0.88f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp))
                                        if (selecting) {
                                            Checkbox(checked = media.identifier in selectedIds,
                                                enabled = !sharing, onCheckedChange = { toggleSelection(media) },
                                                modifier = Modifier.align(Alignment.TopEnd))
                                        } else if (reel == null) {
                                            if (removeMedia != null) Box(Modifier.align(Alignment.BottomEnd)) {
                                                DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text("Remove memory") },
                                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                        enabled = !sharing && !importing && !removing,
                                                        onClick = { actionsExpanded = false; removeTarget = media }
                                                    )
                                                }
                                            }
                                        } else Box(Modifier.align(Alignment.BottomEnd)) {
                                            IconButton(
                                                enabled = !sharing && !importing && !removing,
                                                onClick = { dismissKeyboard(); actionsExpanded = true },
                                                modifier = Modifier.size(34.dp)
                                            ) { Icon(Icons.Default.MoreHoriz, if (reel != null) "Reel actions" else "Memory actions", tint = Color.White,
                                                modifier = Modifier.size(24.dp)) }
                                            DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                                                DropdownMenuItem(
                                                    text = { Text(if (reel != null) "Edit Reel" else "Edit memory") },
                                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                                    onClick = {
                                                        actionsExpanded = false
                                                        // Android's Edit memory opens Edit Reel for reels and the full-screen trim editor for videos.
                                                        if (reel != null && reelBuilder != null) {
                                                            openReelEditor(media)
                                                        } else if (media.kind == MediaKind.VIDEO && videoEditor != null) {
                                                            selected = media
                                                            editingFromLibrary = true
                                                            editingVideo = true
                                                        } else {
                                                            detailsTarget = media
                                                            editingDetails = true
                                                        }
                                                    }
                                                )
                                                if (removeMedia != null) DropdownMenuItem(
                                                    text = { Text(if (reel != null) "Remove Reel" else "Remove memory") },
                                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                    onClick = { actionsExpanded = false; removeTarget = media }
                                                )
                                            }
                                        }
                                        if (!selecting && shareMedia != null) IconButton(
                                            enabled = !sharing && !importing,
                                            onClick = { startShare(listOf(media)) },
                                            modifier = Modifier.align(Alignment.BottomStart).size(34.dp)
                                        ) {
                                            MemoryShareBadge("Share this memory")
                                        }
                                        if (!selecting) IconButton(
                                            onClick = { scope.launch { repository.setCaptureFavorite(key, !favorite) } },
                                            modifier = Modifier.align(Alignment.BottomCenter).size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (favorite) Icon(Icons.Default.Star, "Remove from favorites",
                                                    tint = SpotrHighlightOrange, modifier = Modifier.size(24.dp))
                                                else {
                                                    Icon(Icons.Default.Star, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                                    Icon(Icons.Default.Star, "Add to favorites", tint = Color.White,
                                                        modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                    run {
                                        val note = snapshot.captureNotes[key].orEmpty()
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .clickable(enabled = !selecting && !sharing) { dismissKeyboard(); noteTarget = media }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (note.isBlank()) Icon(Icons.Default.Edit, null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                            Text(note.ifBlank { "What Happened" }, style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                                color = if (note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                                    else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    noteTarget?.let { media ->
        val key = captureFileKey(media)
        val existing = snapshot.captureNotes[key].orEmpty()
        var draft by remember(key) { mutableStateOf(existing) }
        var noteError by remember(key) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { noteTarget = null },
            title = { Text(if (existing.isBlank()) "What Happened" else "Edit Comment") },
            text = {
                // Tapping outside the field or the keyboard's Done key hides the keyboard.
                Column(Modifier.pointerInput(Unit) { detectTapGestures { dismissKeyboard() } }) {
                    OutlinedTextField(
                        value = draft, onValueChange = { draft = it.take(MAX_CAPTURE_NOTE_LENGTH) },
                        placeholder = { Text("Add notes about this memory") },
                        minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { dismissKeyboard() })
                    )
                    noteError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dismissKeyboard()
                    scope.launch {
                        try {
                            repository.setCaptureNote(key, draft)
                            noteTarget = null
                        } catch (failure: IllegalArgumentException) {
                            noteError = failure.message
                        }
                    }
                }) { Text(if (existing.isBlank()) "Add" else "Save") }
            },
            dismissButton = { TextButton(onClick = { dismissKeyboard(); noteTarget = null }) { Text("Cancel") } }
        )
    }
    (removeTarget?.let { listOf(it) } ?: removeBatch)?.let { pending ->
        val count = pending.size
        val closeRemove = { removeTarget = null; removeBatch = null }
        val allReels = pending.all { captureFileKey(it) in snapshot.captureReels }
        val noun = if (allReels) (if (count == 1) "reel" else "reels") else (if (count == 1) "memory" else "memories")
        AlertDialog(
            onDismissRequest = { if (!removing) closeRemove() },
            title = { Text("Remove ${noun}?") },
            text = {
                Text("$count $noun will be removed from the app. The original will remain on your phone.")
            },
            confirmButton = {
                TextButton(enabled = !removing && removeMedia != null, onClick = {
                    val remove = removeMedia ?: return@TextButton
                    removing = true
                    shareError = null
                    scope.launch {
                        withContext(NonCancellable) {
                            var failed = 0
                            try {
                                pending.forEach { media ->
                                    try {
                                        remove(media)
                                        captures = captures.filterNot { it.identifier == media.identifier }
                                        selectedIds = selectedIds - media.identifier
                                        if (selected?.identifier == media.identifier) selected = null
                                    } catch (failure: CameraCaptureException) {
                                        failed++
                                        shareError = failure.message ?: "PlayerID couldn't remove this memory."
                                    }
                                }
                                if (failed > 1) shareError = "$failed memories couldn't be removed. The others were removed."
                                if (failed == 0 && removeBatch != null) {
                                    selecting = false
                                    selectedIds = emptySet()
                                }
                            } finally {
                                removing = false
                                closeRemove()
                            }
                        }
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !removing, onClick = { closeRemove() }) { Text("Cancel") } }
        )
    }
    if (listening) {
        VoiceSearchListeningDialog(transcript = voiceTranscript, onCancel = toggleVoiceSearch)
    }
    completedReel?.let { reel ->
        AlertDialog(
            onDismissRequest = { completedReel = null },
            title = { Text("Reel ready") },
            text = { Text("Saved to Reels. Watch it now or share it with your team.") },
            confirmButton = { TextButton(onClick = { completedReel = null; selected = reel }) { Text("View") } },
            dismissButton = if (shareMedia != null) {
                { TextButton(onClick = { completedReel = null; startShare(listOf(reel)) }) { Text("Share") } }
            } else null
        )
    }
    if (showingImportFailures) {
        AlertDialog(
            onDismissRequest = { showingImportFailures = false },
            title = { Text("Items not imported") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    listItems(importFailures, key = { it.selectionNumber }) {
                        Text("Item ${it.selectionNumber}: ${it.message}", Modifier.padding(vertical = 6.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showingImportFailures = false }) { Text("OK") } }
        )
    }
    if (editingDetails) {
        detailsTarget?.let { media ->
            CaptureOrganizationDialog(
                fileKey = captureFileKey(media),
                repository = repository,
                onDismiss = { editingDetails = false }
            )
        }
    }
}

internal fun captureLibraryBadge(
    reelName: String?,
    momentTag: String?,
    defaultColor: Color
): Pair<String, Color> {
    reelName?.takeIf { it.isNotBlank() }?.let { return it to Color(0xFF2B2F33) }
    return when (momentTag?.trim()?.lowercase().orEmpty()) {
        "goal", "score", "winning goal" -> "Score" to Color(0xFFE87522)
        "assist" -> "Assist" to Color(0xFF168C8C)
        "save" -> "Save" to Color(0xFF7950BC)
        "defensive stop", "big play" -> "Defensive Stop" to Color(0xFFD9534F)
        "faceoff win", "win" -> "Faceoff Win" to Color(0xFF399875)
        "ground ball" -> "Ground Ball" to Color(0xFF3676B8)
        else -> momentTag?.trim()?.takeIf { it.isNotEmpty() }?.let { it to defaultColor }
            ?: ("Tag" to defaultColor)
    }
}

private enum class MemorySelectionAction { SHARE, REMOVE, REEL }

/** What the Memories page hands the platform reel editor; Back and Add Memories reopen the grid picker. */
class MemoryReelDraft(
    val selection: List<MediaReference>,
    val title: String,
    val onAddMemories: (List<MediaReference>, String) -> Unit,
    val onBack: (List<MediaReference>, String) -> Unit,
    val onSaved: (MediaReference) -> Unit,
)

/** Shown while voice search hears speech; closes on its own after a pause, or Cancel stops listening. */
@Composable
private fun VoiceSearchListeningDialog(transcript: String, onCancel: () -> Unit) {
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition()
    val scale by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(700),
            androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    AlertDialog(
        onDismissRequest = onCancel,
        title = null,
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(80.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = 1.6f - scale }
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                    )
                    Box(
                        Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Listening…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(transcript.ifBlank { "Say your request, then pause. Cancel discards it." },
                    style = MaterialTheme.typography.bodyMedium, maxLines = 4,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}
