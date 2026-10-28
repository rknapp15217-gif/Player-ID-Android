package com.playerid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.*
import com.playerid.app.domain.team.*
import com.playerid.app.platform.MediaReference
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android-matched "Edit Reel" screen. Memories are picked in the Memories grid first; "Add Memories" and Back
 * return there with the current order and title so the grid stays the single picker, as on Android.
 */
@Composable
fun MemoryReelBuilderPage(
    initialSelection: List<MediaReference>,
    initialTitle: String,
    repository: LocalTeamRepository,
    build: suspend (List<MediaReference>, String) -> MediaReference,
    cancelBuild: () -> Unit,
    thumbnail: @Composable (MediaReference, Modifier) -> Unit,
    preview: @Composable (MediaReference, Modifier) -> Unit,
    onAddMemories: (List<MediaReference>, String) -> Unit,
    onBack: (List<MediaReference>, String) -> Unit,
    onSaved: (MediaReference) -> Unit,
    loadDuration: (suspend (MediaReference) -> Long)? = null
) {
    var name by remember { mutableStateOf(initialTitle) }
    var ordered by remember { mutableStateOf(initialSelection) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var exported by remember { mutableStateOf<MediaReference?>(null) }
    var playingPreview by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf(false) }
    var durations by remember { mutableStateOf(emptyMap<String, Long>()) }
    val snapshot by repository.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    val editorScroll = rememberLazyListState()
    DisposableEffect(Unit) { onDispose { cancelBuild() } }
    val editable = !busy && exported == null
    val firstMemory = ordered.firstOrNull()
    val currentTitle = { name.trim().ifBlank { initialTitle } }
    LaunchedEffect(firstMemory) { playingPreview = false }
    LaunchedEffect(ordered, loadDuration) {
        val loader = loadDuration ?: return@LaunchedEffect
        ordered.filter { it.identifier !in durations }.forEach { media ->
            val millis = try { loader(media) } catch (_: CameraCaptureException) { return@forEach }
            durations = durations + (media.identifier to millis)
        }
    }
    val totalMillis = ordered.takeIf { list -> list.isNotEmpty() && list.all { it.identifier in durations } }
        ?.sumOf { durations.getValue(it.identifier) }
    val save: () -> Unit = {
        dismissKeyboard()
        playingPreview = false
        val sources = ordered.toList()
        val title = currentTitle()
        busy = true
        error = null
        scope.launch {
            withContext(NonCancellable) {
                try {
                    validateReelSelection(title, sources)
                    val result = exported ?: build(sources, title).also { exported = it }
                    repository.saveCaptureReel(captureFileKey(result), title, sources)
                    onSaved(result)
                } catch (failure: CameraCaptureException) {
                    error = failure.message ?: "Reel export failed. Your sources are unchanged."
                } catch (failure: TeamStorageException) {
                    error = "The video is saved, but its reel name/source list could not be saved. Retry details without creating another video. Leaving now keeps it as a normal video in Memories. ${failure.message.orEmpty()}"
                } catch (failure: IllegalArgumentException) {
                    error = failure.message ?: "Check the reel name and selected videos."
                } finally {
                    busy = false
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !busy, onClick = {
                dismissKeyboard()
                onBack(ordered, currentTitle())
            }) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Edit Reel", Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                modifier = Modifier.padding(end = 6.dp),
                enabled = !busy && ordered.size >= 2, onClick = { save() }) {
                Text(if (busy) "Saving..." else if (exported != null) "Retry details" else "Save")
            }
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Building reel. Keep PlayerID in the foreground.", Modifier.padding(horizontal = 16.dp))
            TextButton(onClick = cancelBuild) { Text("Cancel export") }
        }
        if (!busy) Text("Review the title, clips and order. Save creates the reel.",
            Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = editorScroll,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "preview") {
                Box(
                    Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .clickable(enabled = firstMemory != null && editable) { playingPreview = !playingPreview },
                    contentAlignment = Alignment.Center
                ) {
                    if (firstMemory == null) {
                        Text("Add memories to start your reel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        if (playingPreview && !busy && !fullScreen) {
                            key(firstMemory.identifier) { preview(firstMemory, Modifier.fillMaxSize()) }
                        } else {
                            thumbnail(firstMemory, Modifier.fillMaxSize())
                            Icon(Icons.Default.PlayArrow, "Preview first memory", tint = Color.White,
                                modifier = Modifier.size(28.dp))
                        }
                        totalMillis?.let {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.34f)).padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(formatReelDuration(it), fontSize = 10.sp, color = Color.White.copy(alpha = 0.95f),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Surface(
                            Modifier.align(Alignment.BottomEnd).padding(8.dp).size(30.dp).clip(CircleShape)
                                .clickable(enabled = !busy) { playingPreview = false; fullScreen = true },
                            shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White
                        ) { Icon(Icons.Default.Fullscreen, "View full screen", Modifier.padding(5.dp).fillMaxSize()) }
                    }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(name, onValueChange = { name = it }, label = { Text("Reel title") },
                    singleLine = true, enabled = editable, modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
            item {
                Spacer(Modifier.height(14.dp))
                Text("Memories in this reel (${ordered.size})", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
            }
            itemsIndexed(ordered, key = { _, media -> media.identifier }) { index, media ->
                Surface(shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.15f))) {
                            thumbnail(media, Modifier.fillMaxSize())
                        }
                        Column(Modifier.weight(1f)) {
                            Text(snapshot.captureMomentTags[captureFileKey(media)] ?: "Memory ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            snapshot.captureOrganizations[captureFileKey(media)]?.let(snapshot::captureOpponentName)?.let {
                                Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        durations[media.identifier]?.let {
                            Text(formatReelDuration(it), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            IconButton(enabled = editable && index > 0, modifier = Modifier.size(28.dp),
                                onClick = { ordered = moveReelSource(ordered, index, index - 1) }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Move clip ${index + 1} earlier")
                            }
                            IconButton(enabled = editable && index < ordered.lastIndex, modifier = Modifier.size(28.dp),
                                onClick = { ordered = moveReelSource(ordered, index, index + 1) }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Move clip ${index + 1} later")
                            }
                        }
                        IconButton(enabled = editable, modifier = Modifier.size(28.dp),
                            onClick = { ordered = ordered.filterNot { it.identifier == media.identifier } }) {
                            Icon(Icons.Default.Close, "Remove clip ${index + 1} from reel")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            item {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(enabled = editable, modifier = Modifier.fillMaxWidth(), onClick = {
                    dismissKeyboard()
                    playingPreview = false
                    onAddMemories(ordered, currentTitle())
                }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Memories")
                }
                if (ordered.size < 2) Text("Add at least two memories to save a reel. Source memories are kept.",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    }
    if (fullScreen && firstMemory != null) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            key(firstMemory.identifier) { preview(firstMemory, Modifier.fillMaxSize()) }
            IconButton(onClick = { fullScreen = false }, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Icon(Icons.Default.Close, "Close full screen", tint = Color.White)
            }
        }
    }
    }
}

fun formatReelDuration(millis: Long): String {
    val totalSeconds = (millis + 500) / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
