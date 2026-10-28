package com.playerid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.*
import com.playerid.app.domain.camera.cameraJerseyOptions
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import com.playerid.app.ui.dialogs.teamComposeColor
import com.playerid.app.ui.memory.captureGameLabel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CameraGameSetupSheet(repository: LocalTeamRepository, onDismiss: () -> Unit) {
    val snapshot by repository.snapshot.collectAsState()
    val initial = remember { snapshot.cameraOrganization }
    var team by remember { mutableStateOf(initial?.teamName) }
    var player by remember { mutableStateOf(initial?.playerId) }
    var season by remember { mutableStateOf(initial?.seasonId) }
    var game by remember { mutableStateOf(initial?.gameId) }
    var jersey by remember { mutableStateOf(snapshot.cameraJerseyLabel) }
    var opponent by remember {
        mutableStateOf(snapshot.games.firstOrNull { it.id == initial?.gameId }?.opponentName ?: initial?.opponentName.orEmpty())
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showJerseyInfo by remember { mutableStateOf(false) }
    var opponentFocused by remember { mutableStateOf(false) }
    var opponentFiltering by remember { mutableStateOf(false) }
    val selectedTeam = snapshot.teams.firstOrNull { it.name == team }
    // Fixed accent: the sheet's text and field colors don't change with the selected team.
    val teamPrimary = MaterialTheme.colorScheme.primary
    val matchupFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = teamPrimary,
        unfocusedBorderColor = teamPrimary.copy(alpha = 0.45f),
        focusedLabelColor = teamPrimary,
        unfocusedLabelColor = teamPrimary.copy(alpha = 0.75f),
        cursorColor = teamPrimary
    )
    var suggestionTime by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(team) {
        while (true) {
            suggestionTime = Clock.System.now().toEpochMilliseconds()
            delay(60_000)
        }
    }
    val suggestion = snapshot.suggestCameraGame(team, suggestionTime)
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    val scope = rememberCoroutineScope()
    fun dismiss() {
        if (!busy) { dismissKeyboard(); onDismiss() }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)).imePadding()) {
        val sheetMaxHeight = maxHeight * 0.9f
        Box(Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { dismiss() })
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Game setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("YOUR TEAM", style = MaterialTheme.typography.labelLarge,
                    color = teamPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SetupMenu(
                    value = snapshot.teams.firstOrNull { it.name == team }?.let { "${it.sport.ifBlank { "Other" }} \u00b7 ${it.name}" }.orEmpty(),
                    label = "Team",
                    choices = snapshot.teams.filter { it.isActive && !it.isArchived }
                        .sortedWith(compareBy({ it.sport }, { it.name })).map { it.name to "${it.sport} \u00b7 ${it.name}" },
                    emptyLabel = "No team (unassigned)", enabled = !busy,
                    showLabel = false,
                    colors = matchupFieldColors,
                    choiceGroups = snapshot.teams.filter { it.isActive && !it.isArchived }
                        .sortedWith(compareBy({ it.sport }, { it.name }))
                        .groupBy { it.sport.ifBlank { "Other" } }
                        .mapValues { (_, teams) -> teams.map { it.name to it.name } }
                ) {
                    team = it; player = it?.let { name -> snapshot.assignedPlayerIds[name] }
                    season = null; game = null; opponent = ""; jersey = null; opponentFiltering = false
                }
                if (snapshot.teams.isEmpty()) Text("Add a team under More > Teams.")
                Spacer(Modifier.height(8.dp))
                val jerseyOptions = cameraJerseyOptions(selectedTeam)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Jersey color", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { showJerseyInfo = !showJerseyInfo }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Info, contentDescription = "About jersey color",
                            tint = if (showJerseyInfo) teamPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    }
                }
                if (showJerseyInfo) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text("Jersey color helps to find other teammates in the memories",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    jerseyOptions.forEach { (label, hex) ->
                        OutlinedButton(
                            enabled = !busy && team != null,
                            onClick = { jersey = label },
                            border = androidx.compose.foundation.BorderStroke(1.dp,
                                if ((jersey ?: jerseyOptions.first().first) == label)
                                    teamPrimary.copy(alpha = 0.32f)
                                else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(Modifier.size(16.dp).background(teamComposeColor(hex), CircleShape)
                                .border(1.dp, Color.White, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal)
                        }
                    }
                }
                Divider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("MATCHUP", style = MaterialTheme.typography.labelLarge,
                    color = teamPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (suggestion != null && suggestion.id != game) {
                    Text("Current scheduled game: ${captureGameLabel(suggestion)}",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(enabled = !busy, onClick = {
                        dismissKeyboard()
                        season = suggestion.sportSeasonId
                        game = suggestion.id
                        opponent = suggestion.opponentName
                    }) { Text("Use scheduled matchup") }
                }
                // Field and suggestion chips scroll into view together so the chips aren't left under the keyboard.
                val opponentArea = remember { BringIntoViewRequester() }
                LaunchedEffect(opponentFocused, opponent) {
                    if (opponentFocused) {
                        delay(300) // let the keyboard finish resizing the sheet
                        opponentArea.bringIntoView()
                    }
                }
                Column(Modifier.bringIntoViewRequester(opponentArea)) {
                OutlinedTextField(
                    value = opponent, onValueChange = { opponent = it; game = null; opponentFiltering = true },
                    label = { Text("Opponent") }, enabled = !busy && team != null,
                    singleLine = true, modifier = Modifier.fillMaxWidth().onFocusChanged { state ->
                        if (state.isFocused && !opponentFocused) opponentFiltering = false
                        opponentFocused = state.isFocused
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    colors = matchupFieldColors
                )
                // Suggestions from the team's schedule and opponents already saved on memories, narrowed as the user types.
                val opponentOptions = remember(snapshot.games, snapshot.seasons, snapshot.captureOrganizations, team) {
                    team?.let { selected ->
                        // The team's own opponents first, then other teams' (a team without a schedule still gets suggestions).
                        val own = captureOpponentOptions(snapshot, selected, null)
                        val others = snapshot.teams.map { it.name }.filter { it != selected }
                            .flatMap { captureOpponentOptions(snapshot, it, null) }
                        (own + others).distinctBy(::opponentKey)
                    }.orEmpty()
                }
                // Shown only once the user starts typing, narrowed to names matching the typed text.
                val opponentMatches = if (team == null || !opponentFiltering || opponent.isBlank()) emptyList()
                    else opponentSuggestions(opponentOptions, opponent, filtering = true)
                if (opponentMatches.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opponentMatches.forEach { option ->
                            SuggestionChip(
                                enabled = !busy,
                                onClick = {
                                    opponent = option
                                    opponentFiltering = false
                                    game = snapshot.games.firstOrNull {
                                        it.id == suggestion?.id && it.opponentName.equals(option, ignoreCase = true)
                                    }?.id
                                    dismissKeyboard()
                                },
                                label = { Text(option, maxLines = 1) }
                            )
                        }
                    }
                }
                }
                Spacer(Modifier.height(6.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(enabled = !busy, onClick = { dismiss() }) { Text("Cancel") }
                    TextButton(enabled = !busy, onClick = {
                        busy = true; error = null; dismissKeyboard()
                        val details = team?.let { CaptureOrganization(it, season, game, player, opponent.trim().ifEmpty { null }) }
                        scope.launch {
                            try {
                                repository.setCameraOrganization(details, jersey.takeIf { details != null })
                                onDismiss()
                            } catch (failure: TeamStorageException) {
                                error = failure.message ?: "PlayerID couldn't save game setup."
                            } catch (failure: IllegalArgumentException) {
                                error = failure.message ?: "Check the selected team and game."
                            } finally { busy = false }
                        }
                    }) { Text(if (busy) "Saving..." else "Done") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupMenu(
    value: String,
    label: String,
    emptyLabel: String,
    choices: List<Pair<String, String>>,
    enabled: Boolean,
    showLabel: Boolean = true,
    choiceGroups: Map<String, List<Pair<String, String>>>? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = {
        if (enabled) expanded = !expanded
    }) {
        OutlinedTextField(value, {}, readOnly = true, enabled = enabled,
            label = if (showLabel) ({ Text(label) }) else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            colors = colors,
            modifier = Modifier.fillMaxWidth().menuAnchor())
        ExposedDropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(emptyLabel) }, onClick = { expanded = false; onSelect(null) })
            if (choiceGroups != null) choiceGroups.forEach { (group, items) ->
                Text(group.uppercase(), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 6.dp))
                items.forEach { (id, text) ->
                    DropdownMenuItem(text = { Text(text) }, contentPadding = PaddingValues(start = 28.dp, end = 12.dp),
                        onClick = { expanded = false; onSelect(id) })
                }
            } else choices.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(id) })
            }
        }
    }
}
