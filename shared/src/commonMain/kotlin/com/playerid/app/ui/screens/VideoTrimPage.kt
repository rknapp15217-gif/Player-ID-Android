package com.playerid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.playerid.app.ui.theme.SpotrHighlightOrange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.*
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.TeamStorageException
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.platform.MediaReference
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import com.playerid.app.ui.memory.CaptureDetailsFields
import com.playerid.app.ui.memory.rememberCaptureDetailsDraft
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimPage(
    media: MediaReference,
    repository: LocalTeamRepository,
    loadDuration: suspend (MediaReference) -> Long,
    export: suspend (MediaReference, VideoTrimRange) -> MediaReference,
    cancelExport: () -> Unit,
    preview: @Composable (MediaReference, VideoTrimRange, Modifier) -> Unit,
    loadFilmstrip: (suspend (MediaReference, Int) -> List<ImageBitmap>)? = null,
    readJerseys: (suspend (MediaReference) -> List<com.playerid.app.domain.recognition.JerseyObservation>)? = null,
    onBack: () -> Unit,
    onSaved: (MediaReference) -> Unit
) {
    var duration by remember(media) { mutableStateOf<Long?>(null) }
    var fractions by remember(media) { mutableStateOf(0f..1f) }
    var range by remember(media) { mutableStateOf<VideoTrimRange?>(null) }
    var loading by remember(media) { mutableStateOf(true) }
    var busy by remember(media) { mutableStateOf(false) }
    var error by remember(media) { mutableStateOf<String?>(null) }
    var loadAttempt by remember(media) { mutableIntStateOf(0) }
    var exported by remember(media) { mutableStateOf<MediaReference?>(null) }
    val scope = rememberCoroutineScope()
    val details = rememberCaptureDetailsDraft(captureFileKey(media), repository)
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    var filmstrip by remember(media) { mutableStateOf(emptyList<ImageBitmap>()) }
    LaunchedEffect(media) {
        if (loadFilmstrip != null) {
            filmstrip = try {
                loadFilmstrip(media, FILMSTRIP_FRAMES)
            } catch (failure: CameraCaptureException) {
                emptyList()
            }
        }
    }
    LaunchedEffect(media, loadAttempt) {
        loading = true
        error = null
        try {
            val loaded = loadDuration(media)
            VideoTrimRange(0, loaded).validate(loaded)
            duration = loaded
            range = VideoTrimRange(0, loaded)
        } catch (failure: CameraCaptureException) {
            error = failure.message ?: "Cannot read this video's duration."
        } catch (failure: IllegalArgumentException) {
            error = failure.message
        } finally {
            loading = false
        }
    }
    val save: () -> Unit = save@{
        val sourceDuration = duration
        val selection = range
        // Untrimmed (or unreadable) videos only save their details; a trimmed range also exports a copy.
        val trimmed = sourceDuration != null && selection != null && selection != VideoTrimRange(0, sourceDuration)
        if (sourceDuration != null && selection == null) return@save
        busy = true
        error = null
        dismissKeyboard()
        scope.launch {
            withContext(NonCancellable) {
                try {
                    if (exported == null) details.save(repository)
                    if (!trimmed || selection == null) {
                        onBack()
                        return@withContext
                    }
                    val result = exported ?: export(media, selection).also { exported = it }
                    repository.copyCaptureDetails(captureFileKey(media), captureFileKey(result))
                    // The original memory is always kept; the trimmed copy is added alongside it.
                    onSaved(result)
                } catch (failure: CameraCaptureException) {
                    error = failure.message ?: "Trim export failed. Your original is unchanged."
                } catch (failure: TeamStorageException) {
                    error = if (exported == null) {
                        failure.message ?: "Couldn't save this memory's details."
                    } else {
                        "The trimmed copy is saved, but its details could not be copied. Tap Retry details; no second copy will be created. ${failure.message.orEmpty()}"
                    }
                } catch (failure: IllegalArgumentException) {
                    error = failure.message ?: "Check the selected range and retry."
                } finally {
                    busy = false
                }
            }
        }
    }
    val updateFractions: (ClosedFloatingPointRange<Float>) -> Unit = { value ->
        val sourceDuration = duration
        if (sourceDuration != null) {
            fractions = value
            range = try {
                videoTrimRange(value.start, value.endInclusive, sourceDuration).also { error = null }
            } catch (failure: IllegalArgumentException) {
                error = failure.message
                null
            }
        }
    }
    // Android's ClipEditorScreen (preview + trim slider) with its tag/opponent editors below the slider.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    // The screen ends at the keyboard's top edge; while typing, the preview steps aside so the field stays visible.
    val keyboardOpen = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                enabled = !busy,
                onClick = { exported?.let(onSaved) ?: onBack() }
            ) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Edit memory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
        }
        if (!keyboardOpen) Box(Modifier.fillMaxWidth().weight(0.9f).background(Color.Black), contentAlignment = Alignment.Center) {
            val selection = range
            if (selection != null && !busy && exported == null) preview(media, selection, Modifier.fillMaxSize())
            if (loading || busy) CircularProgressIndicator(color = Color.White)
        }
        val sourceDuration = duration
        // The trim bar scrolls with the details so scrolling down hides it.
        Column(Modifier.fillMaxWidth().weight(1.1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(top = 12.dp)) {
            if (sourceDuration != null) {
                MemoryTrimTimeline(fractions, sourceDuration, filmstrip, enabled = !busy && exported == null, onChange = updateFractions)
            } else if (loading) {
                Text("Loading memory...", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (busy) Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Saving...", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = cancelExport) { Text("Cancel export") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (!loading && sourceDuration == null) TextButton(onClick = { loadAttempt++ }) { Text("Retry") }
            CaptureDetailsFields(
                details, repository, enabled = !busy && exported == null,
                modifier = Modifier.padding(vertical = 8.dp),
                readJerseys = readJerseys?.let { read -> { read(media) } }
            )
        }
        Button(
            enabled = !loading && !busy && (sourceDuration == null || range != null),
            onClick = save,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(when { busy -> "Saving..."; exported != null -> "Retry details"; else -> "Save" }) }
    }
    }
}

private const val FILMSTRIP_FRAMES = 10
private val TrimYellow = Color(0xFFFFCC00)

/**
 * Photos-style trimmer: a filmstrip of frames framed by two chevron handles. The frame turns yellow once
 * the clip is trimmed, frames outside the selection are dimmed, and the time is shown above the strip.
 */
@Composable
private fun MemoryTrimTimeline(
    fractions: ClosedFloatingPointRange<Float>,
    durationMs: Long,
    frames: List<ImageBitmap>,
    enabled: Boolean,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    var widthPx by remember { mutableStateOf(0f) }
    var activeHandle by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end
    val current by rememberUpdatedState(fractions)
    val density = LocalDensity.current
    val handle = 16.dp
    val handlePx = with(density) { handle.toPx() }
    val trimmed = fractions.start > 0f || fractions.endInclusive < 1f
    val frameColor = if (trimmed || activeHandle != null) TrimYellow else Color(0xFFE5E5EA)
    val startMs = (fractions.start * durationMs).toLong()
    val endMs = (fractions.endInclusive * durationMs).toLong()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            when (activeHandle) {
                true -> videoTrimTimestamp(startMs)
                false -> videoTrimTimestamp(endMs)
                null -> videoTrimTimestamp((endMs - startMs).coerceAtLeast(0))
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (activeHandle != null || trimmed) TrimYellow.darken() else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .onSizeChanged { widthPx = it.width.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: continue
                            if (!change.pressed) { activeHandle = null; continue }
                            val track = (widthPx - 2 * handlePx).coerceAtLeast(1f)
                            val fraction = ((change.position.x - handlePx) / track).coerceIn(0f, 1f)
                            val range = current
                            val startHandle = activeHandle
                                ?: (kotlin.math.abs(fraction - range.start) <= kotlin.math.abs(fraction - range.endInclusive))
                            activeHandle = startHandle
                            change.consume()
                            onChange(
                                if (startHandle) fraction.coerceAtMost(range.endInclusive - 0.01f).coerceAtLeast(0f)..range.endInclusive
                                else range.start..fraction.coerceAtLeast(range.start + 0.01f).coerceAtMost(1f)
                            )
                        }
                    }
                }
        ) {
            val track = (widthPx - 2 * handlePx).coerceAtLeast(0f)
            val startX = with(density) { (handlePx + fractions.start * track).toDp() }
            val endX = with(density) { (handlePx + fractions.endInclusive * track).toDp() }
            val stripEnd = with(density) { (handlePx + track).toDp() }
            // Filmstrip
            Row(
                Modifier.padding(horizontal = handle).fillMaxSize().padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                frames.forEach { frame ->
                    Image(frame, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
            // Dim frames outside the selection
            Box(Modifier.offset(x = handle).width((startX - handle).coerceAtLeast(0.dp)).fillMaxHeight()
                .padding(vertical = 3.dp).background(Color.Black.copy(alpha = 0.55f)))
            Box(Modifier.offset(x = endX).width((stripEnd - endX).coerceAtLeast(0.dp)).fillMaxHeight()
                .padding(vertical = 3.dp).background(Color.Black.copy(alpha = 0.55f)))
            // Selection frame: top/bottom bars plus chevron handles
            Box(
                Modifier.offset(x = startX - handle).width(endX - startX + handle * 2).fillMaxHeight()
                    .border(3.dp, frameColor, RoundedCornerShape(6.dp))
            )
            TrimHandle(Icons.Default.ChevronLeft, "Trim start", frameColor,
                Modifier.offset(x = startX - handle).width(handle).fillMaxHeight(),
                RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
            TrimHandle(Icons.Default.ChevronRight, "Trim end", frameColor,
                Modifier.offset(x = endX).width(handle).fillMaxHeight(),
                RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
        }
    }
}

@Composable
private fun TrimHandle(icon: ImageVector, description: String, color: Color, modifier: Modifier, shape: Shape) {
    Box(modifier.background(color, shape), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.Black, modifier = Modifier.size(18.dp))
    }
}

/** Readable yellow for text on light backgrounds. */
private fun Color.darken(): Color = Color(red * 0.8f, green * 0.68f, blue * 0.2f, alpha)
