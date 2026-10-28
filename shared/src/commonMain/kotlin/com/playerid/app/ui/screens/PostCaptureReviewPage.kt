package com.playerid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.MemoryShareResult
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.TeamStorageException
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.domain.team.defaultCaptureMomentTags
import com.playerid.app.platform.MediaReference
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PostCaptureReviewPage(
    media: MediaReference,
    repository: LocalTeamRepository,
    shareMedia: suspend (List<MediaReference>) -> MemoryShareResult,
    deleteMedia: suspend (MediaReference) -> Unit,
    confirmBeforeDelete: Boolean = true,
    onImmediateDelete: (() -> Unit)? = null,
    onDismiss: (deleted: Boolean) -> Unit
) {
    val snapshot by repository.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    var busy by remember(media) { mutableStateOf(false) }
    var error by remember(media) { mutableStateOf<String?>(null) }
    var message by remember(media) { mutableStateOf<String?>(null) }
    var customOpen by remember(media) { mutableStateOf(false) }
    var customText by remember(media) { mutableStateOf("") }
    var renaming by remember(media) { mutableStateOf<String?>(null) }
    var editingCustomTags by remember(media) { mutableStateOf(false) }
    var confirmDelete by remember(media) { mutableStateOf(false) }
    var fileDeleted by remember(media) { mutableStateOf(false) }
    val runAction: (suspend () -> Unit) -> Unit = { action ->
        if (!busy) {
            dismissKeyboard()
            busy = true
            error = null
            message = null
            scope.launch {
                withContext(NonCancellable) {
                    try {
                        action()
                    } catch (failure: CameraCaptureException) {
                        error = failure.message ?: "PlayerID couldn't complete this action."
                    } catch (failure: TeamStorageException) {
                        error = if (fileDeleted) {
                            "The clip was deleted, but its saved details could not be cleared. Tap Delete to retry. ${failure.message.orEmpty()}"
                        } else failure.message ?: "PlayerID couldn't save these changes."
                    } catch (failure: IllegalArgumentException) {
                        error = failure.message ?: "Check the moment tag and retry."
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }
    val tagClip: (String, Boolean) -> Unit = { label, custom ->
        runAction {
            repository.tagCapture(captureFileKey(media), label, rememberCustom = custom)
            onDismiss(false)
        }
    }
    val deleteClip: () -> Unit = {
        runAction {
            deleteMedia(media)
            fileDeleted = true
            repository.forgetCapture(captureFileKey(media))
            onDismiss(true)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.BottomCenter) {
        val landscape = maxWidth > maxHeight
        val spacing = if (landscape) 8.dp else 18.dp
        Surface(
            Modifier.fillMaxWidth(0.96f).padding(bottom = if (landscape) 8.dp else 18.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(
                    horizontal = 20.dp, vertical = if (landscape) 10.dp else 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Quick actions are compact buttons so they don't look like the moment tags below.
                // Buttons size to their labels (no weights) so no word is cut off.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        enabled = !busy && !fileDeleted,
                        onClick = {
                            runAction {
                                // Stay on the review after sharing so the clip can still be tagged.
                                message = when (shareMedia(listOf(media))) {
                                    MemoryShareResult.COMPLETED -> "Shared. Tag what happened below, or tap Skip."
                                    MemoryShareResult.CANCELLED -> "Sharing canceled. Your clip is still saved."
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            if (confirmBeforeDelete) confirmDelete = true
                            else if (onImmediateDelete != null) onImmediateDelete()
                            else deleteClip()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", maxLines = 1, softWrap = false)
                    }
                    TextButton(
                        enabled = !busy,
                        onClick = { onDismiss(fileDeleted) },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("Skip", maxLines = 1, softWrap = false)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.SkipNext, null, Modifier.size(18.dp))
                    }
                }
                Divider()
                if (fileDeleted) Text("Clip deleted. Retry Delete to finish clearing its saved details.",
                    style = MaterialTheme.typography.bodySmall)
                Text("What happened?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                (defaultCaptureMomentTags + "Custom").chunked(
                    if (landscape) defaultCaptureMomentTags.size + 1 else 2
                ).forEach { labels ->
                    Row(horizontalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 12.dp)) {
                        labels.forEach { label ->
                            if (label == "Custom") {
                                ReviewMomentCard(
                                    label = label, icon = Icons.Default.Add,
                                    enabled = !busy && !fileDeleted,
                                    onClick = { renaming = null; customText = ""; customOpen = true },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                            ReviewMomentCard(
                                label = label,
                                emoji = when (label) {
                                    "Goal" -> "\uD83E\uDD4D"
                                    "Save" -> "\uD83E\uDDE4"
                                    "Faceoff Win" -> "\uD83C\uDFC1"
                                    "Assist" -> "\uD83C\uDFAF"
                                    else -> "\uD83D\uDEE1\uFE0F"
                                },
                                enabled = !busy && !fileDeleted,
                                onClick = { tagClip(label, false) },
                                modifier = Modifier.weight(1f)
                            )
                            }
                        }
                        if (labels.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (snapshot.customCaptureMomentTags.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Custom tags", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(enabled = !busy, onClick = { editingCustomTags = !editingCustomTags }) {
                            Text(if (editingCustomTags) "Done" else "Edit")
                        }
                    }
                }
                snapshot.customCaptureMomentTags.chunked(2).forEach { labels ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        labels.forEach { label ->
                            if (!editingCustomTags) {
                                ReviewMomentCard(
                                    label = label, icon = Icons.Default.Label,
                                    enabled = !busy && !fileDeleted,
                                    onClick = { tagClip(label, false) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Card(modifier = Modifier.weight(1f).height(80.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                                    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                                        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            IconButton(enabled = !busy, onClick = {
                                                renaming = label
                                                customText = label
                                                customOpen = true
                                            }) { Icon(Icons.Default.Edit, "Rename custom tag $label", Modifier.size(16.dp)) }
                                            IconButton(enabled = !busy, onClick = {
                                                runAction { repository.removeCustomCaptureTag(label) }
                                            }) {
                                                Icon(Icons.Default.Delete, "Remove custom tag $label",
                                                    Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (labels.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

        }
    }
    if (customOpen) {
        AlertDialog(
            onDismissRequest = { if (!busy) customOpen = false },
            title = { Text(if (renaming == null) "Custom moment" else "Rename custom tag") },
            text = {
                Column {
                    OutlinedTextField(customText, onValueChange = { customText = it },
                        label = { Text("Moment tag") }, singleLine = true, enabled = !busy)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && customText.isNotBlank(), onClick = {
                    val previous = renaming
                    runAction {
                        if (previous == null) {
                            repository.tagCapture(captureFileKey(media), customText, rememberCustom = true)
                            customOpen = false
                            onDismiss(false)
                        } else {
                            repository.renameCustomCaptureTag(previous, customText)
                            customOpen = false
                        }
                    }
                }) { Text(if (renaming == null) "Save tag" else "Rename") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { customOpen = false }) { Text("Cancel") }
            }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this clip?") },
            text = { Text("This permanently removes only the clip you just recorded from PlayerID.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    deleteClip()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewMomentCard(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (emoji != null) {
                Text(emoji, fontSize = 32.sp)
                Spacer(Modifier.height(4.dp))
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            } else {
                if (icon != null) Icon(icon, null, Modifier.size(22.dp))
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}
