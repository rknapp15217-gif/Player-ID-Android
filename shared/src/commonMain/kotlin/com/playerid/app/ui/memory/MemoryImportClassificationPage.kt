package com.playerid.app.ui.memory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.TeamStorageException
import com.playerid.app.domain.team.TeamWorkspaceSnapshot
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.domain.team.captureFilterPlayers
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android's "Importing N memories" flow: step 1 chooses batch or individual classification (skipped
 * for one memory), step 2 sets team, player, season and opponent, then "Import N Memories" saves them.
 * [onCancel] runs when the user backs out without importing; [onImported] after the details are saved.
 */
@Composable
fun MemoryImportClassificationPage(
    items: List<MediaReference>,
    repository: LocalTeamRepository,
    suggestedTeam: String?,
    thumbnail: @Composable (MediaReference, Modifier) -> Unit,
    onCancel: () -> Unit,
    onImported: () -> Unit
) {
    val snapshot by repository.snapshot.collectAsState()
    val keys = remember(items) { items.map(::captureFileKey) }
    val drafts = remember(keys) {
        val start = repository.snapshot.value
        keys.associateWith { newImportDraft(it, start, suggestedTeam) }
    }
    val batchDraft = remember(keys) { newImportDraft(keys.first(), repository.snapshot.value, suggestedTeam) }
    var step by remember(keys) { mutableIntStateOf(if (items.size == 1) 2 else 1) }
    var batch by remember(keys) { mutableStateOf(items.size > 1) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val canImport = if (batch) batchDraft.isCompleteForImport(snapshot)
        else drafts.values.all { it.isCompleteForImport(snapshot) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step == 2 && items.size > 1) {
                IconButton(enabled = !saving, onClick = { step = 1; error = null }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            } else Spacer(Modifier.size(48.dp))
            Text(
                if (step == 1) "Importing ${items.size} memories" else if (items.size == 1) "Import memory" else "Step 2 of 2",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(enabled = !saving, onClick = onCancel) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel import")
            }
        }
        LinearProgressIndicator(
            progress = if (step == 1) 0.18f else 1f,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        if (step == 1) {
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(Modifier.size(72.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
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
                Spacer(Modifier.height(24.dp))
                ClassificationChoice("Batch classify", "Use the same details for all ${items.size} memories", Icons.Default.Layers) {
                    batch = true; step = 2
                }
                Spacer(Modifier.height(12.dp))
                ClassificationChoice("Classify individually", "Set details for each memory", Icons.Default.PersonOutline) {
                    batch = false; step = 2
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Memories to import (${items.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(items, key = { _, item -> item.identifier }) { index, item ->
                        ImportPreview(item, index + 1, thumbnail, Modifier.width(132.dp))
                    }
                }
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)) {
                Text("Cancel", maxLines = 1)
            }
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    when {
                        batch -> "Set details for all ${items.size} memories"
                        items.size == 1 -> "Set Memory"
                        else -> "Set details for each memory"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (batch) Text(
                    "These details will be applied to every memory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (batch) {
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        ImportDetailsCard { CaptureDetailsFields(batchDraft, repository, enabled = !saving, showTag = false) }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        itemsIndexed(items, key = { _, item -> item.identifier }) { index, item ->
                            // Thumbnail on the page background; the details to fill in sit in their own card below it.
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ImportPreview(item, index + 1, thumbnail, Modifier.fillMaxWidth())
                                ImportDetailsCard {
                                    CaptureDetailsFields(drafts.getValue(keys[index]), repository, enabled = !saving, showTag = false)
                                }
                            }
                        }
                    }
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            Button(
                enabled = canImport && !saving,
                onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        withContext(NonCancellable) {
                            try {
                                keys.forEach { key ->
                                    if (batch) batchDraft.save(repository, key) else drafts.getValue(key).save(repository)
                                }
                                onImported()
                            } catch (failure: TeamStorageException) {
                                error = failure.message ?: "PlayerID couldn't save these details."
                            } catch (failure: IllegalArgumentException) {
                                error = failure.message ?: "Check the details and try again."
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
            ) {
                Text(
                    if (saving) "Importing..." else "Import ${items.size} ${if (items.size == 1) "Memory" else "Memories"}",
                    maxLines = 1
                )
            }
        }
    }
}

/** Pre-fills what Android would: the current team (or the only team) and its only My Player / season. */
private fun newImportDraft(key: String, snapshot: TeamWorkspaceSnapshot, suggestedTeam: String?): CaptureDetailsDraft {
    val team = suggestedTeam?.takeIf { name -> snapshot.teams.any { it.name == name } }
        ?: snapshot.teams.singleOrNull()?.name
    val player = team?.let { captureFilterPlayers(snapshot, it).singleOrNull()?.id }
    val season = team?.let { name -> snapshot.seasons.filter { it.teamName == name && it.isActive }.singleOrNull()?.id }
    return CaptureDetailsDraft(key, null, team, season, null, player, "", null)
}

/** Outlined white card that marks the details as the part of the screen to fill in. */
@Composable
private fun ImportDetailsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { content() }
    }
}

@Composable
private fun ClassificationChoice(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ImportPreview(
    item: MediaReference,
    number: Int,
    thumbnail: @Composable (MediaReference, Modifier) -> Unit,
    modifier: Modifier
) {
    Column(modifier) {
        Text("Memory $number", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        thumbnail(item, Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(8.dp)))
    }
}
