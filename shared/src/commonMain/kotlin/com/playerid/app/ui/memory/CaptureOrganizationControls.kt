package com.playerid.app.ui.memory

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import com.playerid.app.domain.recognition.JerseyObservation
import com.playerid.app.domain.recognition.matchJerseyPlayers
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import com.playerid.app.domain.team.*
import com.playerid.app.ui.components.MemoryFilterMenuLabel
import com.playerid.app.ui.components.MemoryFilterControl
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun CaptureOrganizationFilters(
    snapshot: TeamWorkspaceSnapshot,
    filter: CaptureLibraryFilter,
    savedCaptureKeys: Set<String>,
    onChange: (CaptureLibraryFilter) -> Unit
) {
    val seasons = snapshot.seasons.filter { filter.teamName == null || it.teamName == filter.teamName }
        .map { it to snapshot.captureSeasonYear(it) }
        .distinctBy { it.second }
        .sortedByDescending { it.second }
    val players = captureFilterPlayers(snapshot, filter.teamName).distinctBy { it.name.trim().lowercase() }
    val opponents = captureFilterOpponents(snapshot, filter, savedCaptureKeys)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.8.dp)) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CaptureChoiceMenu(
            modifier = Modifier.weight(1f), category = "Player",
            label = snapshot.players.firstOrNull { it.id == filter.playerId }?.name ?: "All",
            active = filter.playerId != null,
            allLabel = "All players",
            choices = players.map { it.id to it.name },
            onSelect = { onChange(filter.copy(playerId = it, unassignedOnly = false)) }
        )
        CaptureChoiceMenu(
            modifier = Modifier.weight(1f), category = "Season",
            label = snapshot.seasons.firstOrNull { it.id == filter.seasonId }?.let(snapshot::captureSeasonYear) ?: "All",
            active = filter.seasonId != null,
            allLabel = "All seasons",
            choices = seasons.map { (season, year) -> season.id to year },
            onSelect = { onChange(filter.copy(seasonId = it, opponent = null, unassignedOnly = false)) }
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CaptureChoiceMenu(
            modifier = Modifier.weight(1f), category = "Team",
            label = if (filter.unassignedOnly) "Unassigned" else filter.teamName ?: "All Teams",
            active = filter.teamName != null || filter.unassignedOnly,
            allLabel = "All teams",
            choices = snapshot.teams.map { it.name to it.name }.sortedBy { it.second } +
                (UNASSIGNED_TEAM_CHOICE to "Unassigned"),
            onSelect = {
                onChange(
                    if (it == UNASSIGNED_TEAM_CHOICE) CaptureLibraryFilter(unassignedOnly = true)
                    else CaptureLibraryFilter(teamName = it)
                )
            }
        )
        CaptureChoiceMenu(
            modifier = Modifier.weight(1f), category = "Opponent",
            label = filter.opponent ?: "All",
            active = filter.opponent != null,
            allLabel = "All opponents",
            choices = opponents.map { it to it },
            onSelect = { onChange(filter.copy(opponent = it, unassignedOnly = false)) }
        )
    }
    }
}

/** Team-menu id for "memories with no team"; can't clash with a team name because it's never shown as one. */
private const val UNASSIGNED_TEAM_CHOICE = "\u0000unassigned"

@Composable
internal fun CaptureChoiceMenu(
    label: String,
    active: Boolean,
    allLabel: String,
    choices: List<Pair<String, String>>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    category: String? = null,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        if (category != null) MemoryFilterControl(category, label, active) { if (enabled) expanded = true }
        else MemoryFilterMenuLabel(label, active) { if (enabled) expanded = true }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { expanded = false; onSelect(null) })
            choices.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(id) })
            }
        }
    }
}

fun captureGameLabel(game: GameScheduleProfile): String {
    val date = Instant.fromEpochMilliseconds(game.scheduledStartMs)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "$date - ${game.gameLabel.ifBlank { "vs ${game.opponentName}" }}"
}

/** Unsaved edits to one memory's tag, team, player, season, game and opponent. */
@Stable
class CaptureDetailsDraft internal constructor(
    val fileKey: String,
    val originalTag: String?,
    team: String?, season: String?, game: String?, player: String?, opponent: String, tag: String?
) {
    var team by mutableStateOf(team)
    var season by mutableStateOf(season)
    var game by mutableStateOf(game)
    var player by mutableStateOf(player)
    var opponent by mutableStateOf(opponent)
    var tag by mutableStateOf(tag)
    var addedCustomTags by mutableStateOf(emptyList<String>())
    var customTag by mutableStateOf("")
    /** A year chosen for a season the team doesn't have yet; created on save. Exclusive with [season]. */
    var newSeasonYear by mutableStateOf<Int?>(null)

    /**
     * Android's import rule: a team, season and opponent, plus a player whenever the team has a My Player.
     */
    fun isCompleteForImport(snapshot: TeamWorkspaceSnapshot): Boolean {
        val team = team ?: return false
        return opponent.isNotBlank() &&
            (player != null || captureFilterPlayers(snapshot, team).isEmpty()) &&
            (season != null || newSeasonYear != null)
    }

    /** Saves the details first, then the tag, to [targetKey]; nothing is written before this is called. */
    suspend fun save(repository: LocalTeamRepository, targetKey: String = fileKey) {
        val fileKey = targetKey
        val year = newSeasonYear
        val team = team
        if (team != null && year != null) {
            season = repository.ensureCaptureSeason(team, year, Clock.System.now().toEpochMilliseconds())
            newSeasonYear = null
        }
        val snapshot = repository.snapshot.value
        val organization = team?.let {
            val typed = opponent.trim()
            val canonical = captureOpponentOptions(snapshot, it, season)
                .firstOrNull { option -> option.equals(typed, ignoreCase = true) } ?: typed
            CaptureOrganization(it, season, game, player, canonical.ifEmpty { null })
        }
        repository.organizeCapture(fileKey, organization)
        val chosenTag = tag
        if (chosenTag != originalTag) {
            if (chosenTag == null) {
                repository.clearCaptureTag(fileKey)
            } else {
                repository.tagCapture(
                    fileKey,
                    chosenTag,
                    rememberCustom = addedCustomTags.any { it.equals(chosenTag, ignoreCase = true) }
                )
            }
        }
    }
}

@Composable
fun rememberCaptureDetailsDraft(fileKey: String, repository: LocalTeamRepository): CaptureDetailsDraft =
    remember(fileKey) {
        val snapshot = repository.snapshot.value
        val existing = snapshot.captureOrganizations[fileKey]
        val tag = snapshot.captureMomentTags[fileKey]
        CaptureDetailsDraft(
            fileKey, tag, existing?.teamName, existing?.seasonId, existing?.gameId, existing?.playerId,
            existing?.let(snapshot::captureOpponentName).orEmpty(), tag
        ).also { draft ->
            if (existing?.seasonId == null && existing?.gameId == null) {
                draft.newSeasonYear = snapshot.captureSeasonYearForKey(fileKey)?.toIntOrNull()
            }
        }
    }

/**
 * Tag picker (Android's "Edit tag") followed by team/player/season/game choices and the opponent editor
 * (Android's "Edit opponent"). Used by the Edit memory screen and the photo details dialog.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaptureDetailsFields(
    draft: CaptureDetailsDraft,
    repository: LocalTeamRepository,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    showTag: Boolean = true,
    readJerseys: (suspend () -> List<JerseyObservation>)? = null
) {
    val snapshot by repository.snapshot.collectAsState()
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    val scope = rememberCoroutineScope()
    var detecting by remember(draft.fileKey) { mutableStateOf(false) }
    var detectMessage by remember(draft.fileKey) { mutableStateOf<String?>(null) }
    var addingCustomTag by remember(draft.fileKey) { mutableStateOf(false) }
    var seasonWheelOpen by remember(draft.fileKey) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        val selectableTags = (defaultCaptureMomentTags + snapshot.customCaptureMomentTags +
            draft.addedCustomTags + listOfNotNull(draft.tag)).distinctBy { it.lowercase() }
        if (showTag) DetailsRow("Tag") {
            DetailsChoice(
                value = draft.tag ?: "No tag",
                active = draft.tag != null,
                enabled = enabled,
                noneLabel = "No tag",
                choices = selectableTags.map { it to it },
                selectedId = selectableTags.firstOrNull { draft.tag?.equals(it, ignoreCase = true) == true },
                onSelect = { draft.tag = it },
                extraItem = "Custom tag..." to { addingCustomTag = true }
            )
        }
        if (addingCustomTag) {
            val commitCustomTag = {
                val trimmed = draft.customTag.trim()
                if (trimmed.isNotEmpty()) {
                    val known = selectableTags.firstOrNull { it.equals(trimmed, ignoreCase = true) }
                    if (known == null) draft.addedCustomTags = draft.addedCustomTags + trimmed
                    draft.tag = known ?: trimmed
                    draft.customTag = ""
                    addingCustomTag = false
                }
                dismissKeyboard()
            }
            OutlinedTextField(
                value = draft.customTag,
                onValueChange = { draft.customTag = it },
                placeholder = { Text("Custom tag", maxLines = 1) },
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustomTag() })
            )
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = { draft.customTag = ""; addingCustomTag = false; dismissKeyboard() }) {
                    Text("Cancel", maxLines = 1)
                }
                TextButton(onClick = { commitCustomTag() }, enabled = enabled && draft.customTag.isNotBlank()) {
                    Text("Add tag", maxLines = 1)
                }
            }
        }
        if (showTag) Divider()
        DetailsRow("Team") {
            DetailsChoice(
                value = draft.team ?: "Unassigned",
                active = draft.team != null,
                enabled = enabled,
                noneLabel = "Unassigned",
                choices = snapshot.teams.map { it.name to it.name }.sortedBy { it.second },
                selectedId = draft.team,
                onSelect = {
                    draft.team = it; draft.season = null; draft.newSeasonYear = null
                    draft.game = null; draft.player = null; draft.opponent = ""
                }
            )
        }
        val team = draft.team
        if (team != null) {
            val seasons = snapshot.seasons.filter { it.teamName == team }
            val players = captureFilterPlayers(snapshot, team)
            Divider()
            DetailsRow("Player") {
                DetailsChoice(
                    value = snapshot.players.firstOrNull { it.id == draft.player }?.let { "${it.name} #${it.number}" } ?: "No player",
                    active = draft.player != null,
                    enabled = enabled,
                    noneLabel = "No player",
                    choices = players.map { it.id to "${it.name} #${it.number}" },
                    selectedId = draft.player,
                    onSelect = { draft.player = it }
                )
            }
            if (readJerseys != null && players.isNotEmpty()) {
                TextButton(
                    enabled = enabled && !detecting,
                    onClick = {
                        detecting = true
                        detectMessage = null
                        scope.launch {
                            try {
                                val match = matchJerseyPlayers(
                                    readJerseys(), players, snapshot.teams.firstOrNull { it.name == team }
                                ).firstOrNull()
                                if (match == null) {
                                    detectMessage = "No jersey number matched this team's roster."
                                } else {
                                    draft.player = match.player.id
                                    detectMessage = "Matched ${match.player.name} #${match.player.number}" +
                                        if (match.colorVerified) " (number and jersey color)" else " (number only)"
                                }
                            } catch (failure: CancellationException) {
                                throw failure
                            } catch (failure: Exception) {
                                detectMessage = failure.message ?: "Couldn't read this video."
                            } finally {
                                detecting = false
                            }
                        }
                    }
                ) { Text(if (detecting) "Reading jerseys..." else "Detect player from jersey") }
                detectMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Divider()
            val currentYear = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year }
            val selectedYear = draft.newSeasonYear
                ?: seasons.firstOrNull { it.id == draft.season }?.let { snapshot.captureSeasonYear(it).toIntOrNull() }
            DetailsRow("Season") {
                SeasonYearValue(
                    year = selectedYear,
                    open = seasonWheelOpen,
                    enabled = enabled,
                    onClick = { dismissKeyboard(); seasonWheelOpen = !seasonWheelOpen }
                )
            }
            if (seasonWheelOpen && enabled) {
                SeasonYearWheel(
                    // Always include the saved year so opening the wheel never changes the selection.
                    years = (maxOf(currentYear + 1, selectedYear ?: 0) downTo minOf(SEASON_WHEEL_FIRST_YEAR, selectedYear ?: Int.MAX_VALUE)).toList(),
                    selected = selectedYear,
                    onSelect = { year ->
                        // Prefer the season already chosen, then any season with that year, then a new one on save.
                        val existing = seasons.firstOrNull { it.id == draft.season && snapshot.captureSeasonYear(it) == year?.toString() }
                            ?: seasons.firstOrNull { snapshot.captureSeasonYear(it) == year?.toString() }
                        draft.season = existing?.id
                        draft.newSeasonYear = if (year != null && existing == null) year else null
                        draft.game = null
                    }
                )
            }
            Divider()
            // Keeps the Opponent field and its suggestions above the keyboard while typing.
            val opponentArea = remember { BringIntoViewRequester() }
            var opponentFocused by remember { mutableStateOf(false) }
            var opponentFiltering by remember { mutableStateOf(false) }
            LaunchedEffect(opponentFocused, draft.opponent) {
                if (opponentFocused) {
                    delay(300) // let the keyboard finish resizing the screen
                    opponentArea.bringIntoView()
                }
            }
            Column(Modifier.bringIntoViewRequester(opponentArea)) {
            DetailsRow("Opponent") {
                OutlinedTextField(
                    value = draft.opponent,
                    onValueChange = { draft.opponent = it; draft.game = null; opponentFiltering = true },
                    placeholder = { Text("Enter opponent", maxLines = 1) },
                    enabled = enabled,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .onFocusChanged { state ->
                            if (state.isFocused && !opponentFocused) opponentFiltering = false
                            opponentFocused = state.isFocused
                        },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() })
                )
            }
            // All of the team's scheduled and saved opponents (any season), so other games' opponents are offered too.
            val opponentMatches = if (!opponentFocused) emptyList()
                else opponentSuggestions(captureOpponentOptions(snapshot, team, null), draft.opponent, opponentFiltering)
            opponentMatches.forEach { option ->
                TextButton(
                    onClick = { draft.opponent = option; draft.game = null; dismissKeyboard() },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().padding(start = DetailsLabelWidth),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
            }
        }
    }
}

private val DetailsLabelWidth = 84.dp
private const val SEASON_WHEEL_FIRST_YEAR = 1990
private val WheelRowHeight = 40.dp
private const val WHEEL_VISIBLE_ROWS = 3

/** The Season row's value: just the year (or "None") with a chevron that opens the wheel. */
@Composable
private fun SeasonYearValue(year: Int?, open: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        year != null || open -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            year?.toString() ?: "None",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (year != null) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            maxLines = 1
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (open) "Close season picker" else "Choose season year",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * iOS-style spinning year picker: rows snap to the highlighted centre band and the centred row is
 * the selection. The first row is "None"; [years] follow, newest first.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeasonYearWheel(years: List<Int>, selected: Int?, onSelect: (Int?) -> Unit) {
    val entries: List<Int?> = listOf<Int?>(null) + years
    val initialIndex = entries.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val rowPx = with(LocalDensity.current) { WheelRowHeight.toPx() }
    val centredIndex by remember {
        derivedStateOf {
            val offsetRows = (listState.firstVisibleItemScrollOffset / rowPx + 0.5f).toInt()
            (listState.firstVisibleItemIndex + offsetRows).coerceIn(0, entries.lastIndex)
        }
    }
    val currentOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to centredIndex }
            .collect { (scrolling, index) -> if (!scrolling) currentOnSelect(entries[index]) }
    }
    val sidePadding = WheelRowHeight * (WHEEL_VISIBLE_ROWS / 2)
    Box(
        Modifier.fillMaxWidth().padding(start = DetailsLabelWidth).height(WheelRowHeight * WHEEL_VISIBLE_ROWS),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxWidth().height(WheelRowHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = sidePadding),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(entries) { index, year ->
                val distance = kotlin.math.abs(index - centredIndex)
                Box(
                    Modifier.fillMaxWidth().height(WheelRowHeight)
                        .clickable { scope.launch { listState.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        year?.toString() ?: "None",
                        style = if (distance == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = when (distance) { 0 -> 1f; 1 -> 0.55f; else -> 0.25f }),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** One form line: a fixed-width label and its control; nothing in the row wraps. */
@Composable
private fun DetailsRow(label: String, control: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(DetailsLabelWidth)
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { control() }
    }
}

/** Right-aligned, single-line value with a chevron that opens a dropdown of [choices]. */
@Composable
private fun DetailsChoice(
    value: String,
    active: Boolean,
    enabled: Boolean,
    noneLabel: String,
    choices: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    extraItem: Pair<String, () -> Unit>? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = true }.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            val checkIcon: @Composable () -> Unit = {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenuItem(
                text = { Text(noneLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = if (selectedId == null) checkIcon else null,
                onClick = { expanded = false; onSelect(null) }
            )
            choices.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = if (id == selectedId) checkIcon else null,
                    onClick = { expanded = false; onSelect(id) }
                )
            }
            extraItem?.let { (text, action) ->
                DropdownMenuItem(
                    text = { Text(text, maxLines = 1) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    onClick = { expanded = false; action() }
                )
            }
        }
    }
}

/** Details-only editor, used for photos (videos edit details on the Edit memory screen). */
@Composable
fun CaptureOrganizationDialog(
    fileKey: String,
    repository: LocalTeamRepository,
    onDismiss: () -> Unit
) {
    val draft = rememberCaptureDetailsDraft(fileKey, repository)
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Edit memory details") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptureDetailsFields(draft, repository, enabled = !busy)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                error = null
                dismissKeyboard()
                scope.launch {
                    try {
                        draft.save(repository)
                        onDismiss()
                    } catch (failure: TeamStorageException) {
                        error = failure.message
                    } catch (failure: IllegalArgumentException) {
                        error = failure.message
                    } finally {
                        busy = false
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } }
    )
}
