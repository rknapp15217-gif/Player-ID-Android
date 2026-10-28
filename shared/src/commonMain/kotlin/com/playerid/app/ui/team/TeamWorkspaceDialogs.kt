package com.playerid.app.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.GameScheduleProfile
import com.playerid.app.domain.team.ScheduleGameForm
import com.playerid.app.domain.team.ScheduleImportEntry
import com.playerid.app.domain.team.parseScheduleText
import com.playerid.app.roster.RosterCandidate
import com.playerid.app.roster.parseRosterText
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun RosterTextImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<RosterCandidate>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<RosterCandidate>?>(null) }
    val duplicateNumbers = preview?.groupBy { it.number }?.any { it.value.size > 1 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import roster text") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Paste roster text, one player per line, such as:\n7 Alex Smith\n12 Jordan Lee\n\nExisting jersey numbers will be updated. Review every recognized player before importing.")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; preview = null },
                    label = { Text("Roster text") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp)
                )
                TextButton(onClick = { preview = parseRosterText(text.lines()).candidates }, enabled = text.isNotBlank()) {
                    Text("Preview players")
                }
                preview?.let { candidates ->
                    Text("${candidates.size} recognized players")
                    if (candidates.isEmpty()) Text("No players recognized. Check the text and jersey numbers.")
                    if (duplicateNumbers) Text(
                        "Duplicate jersey numbers found. Correct the text before importing.",
                        color = MaterialTheme.colorScheme.error
                    )
                    candidates.forEach { Text("#${it.number} ${it.name} ${it.position}") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { preview?.let(onImport) },
                enabled = !preview.isNullOrEmpty() && !duplicateNumbers
            ) { Text("Import previewed players") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun ScheduleTextImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<ScheduleImportEntry>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<List<ScheduleImportEntry>?>(null) }
    val timeZone = TimeZone.currentSystemDefault()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import schedule text") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Paste website text or CSV with this header and format:\ndate,start,end,opponent,location\n2026-10-20,18:00,19:30,Tigers,Main Field\n\nTimes use ${timeZone.id}. Only previewed games are imported; unrecognized rows are skipped. Exact repeat imports are not added again.")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; preview = null },
                    label = { Text("Schedule text or CSV") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp)
                )
                TextButton(
                    onClick = { preview = parseScheduleText(text, timeZone.id) },
                    enabled = text.isNotBlank()
                ) { Text("Preview games") }
                preview?.let { entries ->
                    Text("${entries.size} recognized games")
                    if (entries.isEmpty()) Text("No games recognized. Include dates, times, and opponents.")
                    entries.forEach {
                        val local = Instant.fromEpochMilliseconds(it.startMs).toLocalDateTime(timeZone)
                        Text("$local - ${it.gameLabel.ifBlank { "vs ${it.opponent}" }} - ${it.locationName.orEmpty()}")
                        if (it.endMs <= it.startMs) {
                            Text("End time must be after start time.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { preview?.let(onImport) },
                enabled = !preview.isNullOrEmpty() && preview?.all { it.endMs > it.startMs } == true
            ) { Text("Import previewed games") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun ScheduleGameDialog(
    onDismiss: () -> Unit,
    onSubmit: (ScheduleGameForm) -> Unit,
    existing: GameScheduleProfile? = null,
    onDelete: (() -> Unit)? = null
) {
    val timeZone = TimeZone.currentSystemDefault()
    var form by remember(existing?.id) {
        mutableStateOf(if (existing == null) ScheduleGameForm() else {
            val start = Instant.fromEpochMilliseconds(existing.scheduledStartMs).toLocalDateTime(timeZone)
            val end = Instant.fromEpochMilliseconds(existing.scheduledEndMs).toLocalDateTime(timeZone)
            ScheduleGameForm(
                opponent = existing.opponentName,
                date = start.date.toString(),
                startTime = start.time.toString(),
                endTime = end.time.toString(),
                location = existing.locationName.orEmpty(),
                label = existing.gameLabel
            )
        })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add game" else "Edit game") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Times use ${TimeZone.currentSystemDefault().id}. Start and end must be on the same date.")
                onDelete?.let { delete ->
                    TextButton(onClick = delete) { Text("Delete game", color = MaterialTheme.colorScheme.error) }
                }
                OutlinedTextField(form.opponent, { form = form.copy(opponent = it) }, label = { Text("Opponent") }, singleLine = true)
                OutlinedTextField(form.label, { form = form.copy(label = it) }, label = { Text("Game label (optional)") }, singleLine = true)
                OutlinedTextField(form.date, { form = form.copy(date = it) }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(form.startTime, { form = form.copy(startTime = it) }, label = { Text("Start (24-hour HH:MM)") }, singleLine = true)
                OutlinedTextField(form.endTime, { form = form.copy(endTime = it) }, label = { Text("End (24-hour HH:MM)") }, singleLine = true)
                OutlinedTextField(form.location, { form = form.copy(location = it) }, label = { Text("Location (optional)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(form) }) { Text("Save game") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun AssignedPlayerDialog(
    players: List<PlayerProfile>,
    onDismiss: () -> Unit,
    onAssign: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your player") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (players.isEmpty()) Text("Add players to the roster first.")
                players.forEach { player ->
                    TextButton(onClick = { onAssign(player.id) }) { Text("#${player.number} ${player.name}") }
                }
                TextButton(onClick = { onAssign(null) }) { Text("No assigned player") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
