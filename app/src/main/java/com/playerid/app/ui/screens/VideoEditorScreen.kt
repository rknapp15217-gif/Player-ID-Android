@file:OptIn(ExperimentalMaterial3Api::class)

package com.playerid.app.ui.screens

import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import kotlin.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as Media3Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.playerid.app.data.Player
import com.playerid.app.data.Overlay
import com.playerid.app.data.OverlayType
import com.playerid.app.data.OverlayPosition
import com.playerid.app.ui.composables.drawPlayerOverlay
import kotlinx.coroutines.delay

private const val TAG = "VideoEditorScreen"
private const val OVERLAY_TEXT_SIZE = 40f

data class NameBubble(
    val id: String,
    val playerName: String,
    val jerseyNumber: String,
    val position: Offset,
    val isVisible: Boolean = true,
    val isSelected: Boolean = false
)

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VideoEditorScreen(
    videoUri: Uri,
    roster: List<Player>,
    onNavigateBack: () -> Unit,
    onSaveVideo: (List<NameBubble>) -> Unit
) {
    val context = LocalContext.current
    var nameBubbles by remember { mutableStateOf(listOf<NameBubble>()) }
    var textOverlays by remember { mutableStateOf(listOf<Overlay>()) }
    var selectedBubble by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showPlayerSelector by remember { mutableStateOf(false) }
    var tapPosition by remember { mutableStateOf(Offset.Zero) }
    var showOverlayPanel by remember { mutableStateOf(false) }
    var overlayText by remember { mutableStateOf("") }
    var selectedOverlayPosition by remember { mutableStateOf(OverlayPosition.BOTTOM_CENTER) }
    var overlayDragOffset by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var overlayDragActive by remember { mutableStateOf(false) }
    
    // Video Trimming & Playback State
    var videoDuration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var trimRange by remember { mutableStateOf(0f..1f) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            Log.d(TAG, "Initializing ExoPlayer with URI: $videoUri")
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            repeatMode = Media3Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    // Sync isPlaying state with player
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Track current position and enforce trim range
    LaunchedEffect(exoPlayer, trimRange, videoDuration) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            if (videoDuration > 0) {
                val startMs = (trimRange.start * videoDuration).toLong()
                val endMs = (trimRange.endInclusive * videoDuration).toLong()
                
                if (currentPosition < startMs || currentPosition > endMs) {
                    exoPlayer.seekTo(startMs)
                }
            }
            delay(50) // High frequency update for smooth UI
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Media3Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Media3Player.STATE_READY) {
                    videoDuration = exoPlayer.duration
                    Log.d(TAG, "Video ready. Duration: $videoDuration ms")
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer Error: ${error.message}", error)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(showOverlayPanel) {
        if (showOverlayPanel) {
            isPlaying = false
            exoPlayer.seekTo(0L)
        }
    }

    LaunchedEffect(showOverlayPanel, textOverlays) {
        if (!showOverlayPanel) return@LaunchedEffect
        val existingOverlay = textOverlays.firstOrNull() ?: return@LaunchedEffect
        if (overlayText.isBlank()) {
            overlayText = existingOverlay.text
        }
        selectedOverlayPosition = existingOverlay.position
        overlayDragOffset = getOverlayPositionOffset(
            position = existingOverlay.position,
            canvasWidth = canvasSize.width.toFloat(),
            canvasHeight = canvasSize.height.toFloat()
        )
    }

    LaunchedEffect(showOverlayPanel, overlayText, canvasSize) {
        if (!showOverlayPanel || overlayText.isBlank()) return@LaunchedEffect
        if (overlayDragOffset == null && canvasSize.width > 1 && canvasSize.height > 1) {
            overlayDragOffset = Offset(
                x = canvasSize.width / 2f,
                y = canvasSize.height * 0.2f
            )
        }
    }

    val addOrReplaceOverlay: () -> Unit = add@{
        val trimmedText = overlayText.trim()
        if (trimmedText.isBlank()) return@add
        val positionFromDrag = overlayDragOffset?.let { offset ->
            getOverlayPositionFromOffset(
                offset = offset,
                canvasWidth = canvasSize.width.toFloat(),
                canvasHeight = canvasSize.height.toFloat()
            )
        } ?: selectedOverlayPosition
        val newOverlay = Overlay(
            id = "overlay_${System.currentTimeMillis()}",
            type = OverlayType.CUSTOM,
            text = trimmedText,
            timestamp = 0L,
            duration = 5_000L,
            position = positionFromDrag
        )
        textOverlays = listOf(newOverlay)
        overlayText = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit & Trim Clip", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onSaveVideo(nameBubbles) }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 16.dp)
            ) {
                // Playback scrubbing slider
                if (videoDuration > 0) {
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { exoPlayer.seekTo(it.toLong()) },
                        valueRange = 0f..videoDuration.toFloat(),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Trimming Slider
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trim", style = MaterialTheme.typography.labelSmall)
                    RangeSlider(
                        value = trimRange,
                        onValueChange = { trimRange = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }
                
                VideoEditorControls(
                    isPlaying = isPlaying,
                    onPlayPause = { isPlaying = !isPlaying },
                    onAutoDetect = { /* TODO */ },
                    nameBubbles = nameBubbles,
                    textOverlays = textOverlays,
                    overlayText = overlayText,
                    showOverlayPanel = showOverlayPanel,
                    onShowOverlayPanelChange = { showOverlayPanel = it },
                    onBubbleVisibilityToggle = { id ->
                        nameBubbles = nameBubbles.map { if (it.id == id) it.copy(isVisible = !it.isVisible) else it }
                    },
                    onDeleteBubble = { id -> nameBubbles = nameBubbles.filter { it.id != id } },
                    onOverlayTextChange = { overlayText = it },
                    onAddOverlay = addOrReplaceOverlay,
                    onDeleteOverlay = { id ->
                        textOverlays = textOverlays.filter { it.id != id }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                val overlayPreviewText = if (overlayText.isNotBlank()) {
                    overlayText
                } else {
                    textOverlays.firstOrNull()?.text.orEmpty()
                }
                val isOverlayEditActive = showOverlayPanel && overlayPreviewText.isNotBlank()

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(showOverlayPanel, overlayPreviewText, canvasSize, overlayDragOffset) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (isOverlayEditActive) {
                                        overlayDragActive = true
                                        overlayDragOffset = offset
                                        return@detectDragGestures
                                    }

                                    val tappedBubble = nameBubbles.find { bubble ->
                                        val distance = kotlin.math.sqrt(
                                            (offset.x - bubble.position.x) * (offset.x - bubble.position.x) +
                                            (offset.y - bubble.position.y) * (offset.y - bubble.position.y)
                                        )
                                        distance < 60f
                                    }

                                    if (tappedBubble != null) {
                                        selectedBubble = tappedBubble.id
                                    } else {
                                        tapPosition = offset
                                        showPlayerSelector = true
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    if (isOverlayEditActive && overlayDragActive) {
                                        overlayDragOffset = change.position
                                        change.consume()
                                        return@detectDragGestures
                                    }

                                    selectedBubble?.let { id ->
                                        nameBubbles = nameBubbles.map {
                                            if (it.id == id) it.copy(position = it.position + dragAmount) else it
                                        }
                                    }
                                },
                                onDragEnd = {
                                    selectedBubble = null
                                    overlayDragActive = false
                                }
                            )
                        }
                ) {
                    // Draw name bubbles
                    nameBubbles.filter { it.isVisible }.forEach { bubble ->
                        drawPlayerOverlay(
                            playerName = bubble.playerName,
                            jerseyNumber = bubble.jerseyNumber,
                            position = bubble.position,
                            isSelected = bubble.id == selectedBubble,
                            debugWidth = 0,
                            debugHeight = 0
                        )
                    }
                    
                    // Draw text overlays if currently in playback range
                    textOverlays.filter { overlay ->
                        overlay.isEnabled && 
                        currentPosition >= overlay.timestamp && 
                        currentPosition < overlay.timestamp + overlay.duration
                    }.forEach { overlay ->
                        val overlayPositionOffset = getOverlayPositionOffset(
                            position = overlay.position,
                            canvasWidth = size.width,
                            canvasHeight = size.height
                        )
                        
                        drawOverlayText(
                            text = overlay.text,
                            position = overlayPositionOffset,
                            canvasWidth = size.width,
                            canvasHeight = size.height
                        )
                    }

                    if (showOverlayPanel && overlayPreviewText.isNotBlank()) {
                        val overlayPositionOffset = overlayDragOffset ?: getOverlayPositionOffset(
                            position = selectedOverlayPosition,
                            canvasWidth = size.width,
                            canvasHeight = size.height
                        )

                        drawOverlayText(
                            text = overlayPreviewText,
                            position = overlayPositionOffset,
                            canvasWidth = size.width,
                            canvasHeight = size.height
                        )
                    }
                }
            }
            
            if (showOverlayPanel) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    OverlayEditorSheetContent(
                        overlayText = overlayText,
                        onOverlayTextChange = { overlayText = it },
                        onAddOverlay = {
                            addOrReplaceOverlay()
                            showOverlayPanel = false
                        },
                        onDone = { showOverlayPanel = false }
                    )
                }
            } else {
                Card(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, top = 200.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Text(
                        "Tap video to tag player\nDrag bubbles to move",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
    
    if (showPlayerSelector) {
        AlertDialog(
            onDismissRequest = { showPlayerSelector = false },
            title = { Text("Tag Player") },
            text = {
                LazyRow {
                    items(roster) { player ->
                        PlayerSelectionCard(
                            player = player,
                            onSelect = {
                                val newBubble = NameBubble(
                                    id = "bubble_${System.currentTimeMillis()}",
                                    playerName = player.name,
                                    jerseyNumber = player.number,
                                    position = tapPosition
                                )
                                nameBubbles = nameBubbles + newBubble
                                showPlayerSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionCard(player: Player, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        modifier = Modifier.padding(4.dp).width(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(player.number, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(player.name, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onAutoDetect: () -> Unit,
    nameBubbles: List<NameBubble>,
    textOverlays: List<Overlay>,
    overlayText: String,
    showOverlayPanel: Boolean,
    onShowOverlayPanelChange: (Boolean) -> Unit,
    onBubbleVisibilityToggle: (String) -> Unit,
    onDeleteBubble: (String) -> Unit,
    onOverlayTextChange: (String) -> Unit,
    onAddOverlay: () -> Unit,
    onDeleteOverlay: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play/Pause controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                }
                
                Button(onClick = onAutoDetect, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Auto-Tag", fontSize = 12.sp)
                }
                
                Button(
                    onClick = { onShowOverlayPanelChange(!showOverlayPanel) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text(
                        if (textOverlays.isEmpty()) "Add Overlay" else "Edit Overlay",
                        fontSize = 12.sp
                    )
                }
            }
            
            // Overlay editor renders over the video to keep touches responsive.
            
            // Name Bubbles List
            if (nameBubbles.isNotEmpty()) {
                Text("Name Bubbles:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(nameBubbles) { bubble ->
                        InputChip(
                            selected = true,
                            onClick = { onBubbleVisibilityToggle(bubble.id) },
                            label = { Text("#${bubble.jerseyNumber}", fontSize = 10.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Cancel, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp).clickable { onDeleteBubble(bubble.id) }
                                )
                            }
                        )
                    }
                }
            }
            
            // Text Overlays List
            if (textOverlays.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDeleteOverlay(textOverlays.first().id) }) {
                        Text("Remove Overlay", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayEditorSheetContent(
    overlayText: String,
    onOverlayTextChange: (String) -> Unit,
    onAddOverlay: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edit Overlay", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDone) { Text("Done") }
        }

        // Text Input (user-entered overlays only)
        OutlinedTextField(
            value = overlayText,
            onValueChange = onOverlayTextChange,
            label = { Text("Overlay Text", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            singleLine = true
        )

        Text("Drag the text on the video to move it.", fontSize = 11.sp)

        // Add Overlay Button
        Button(
            onClick = onAddOverlay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            enabled = overlayText.isNotBlank()
        ) {
            Text("Add Overlay", fontSize = 12.sp)
        }
    }
}

@Composable
fun OverlayPositionGrid(
    selectedPosition: OverlayPosition,
    onPositionSelect: (OverlayPosition) -> Unit
) {
    val positions = listOf(
        listOf(OverlayPosition.TOP_LEFT, OverlayPosition.TOP_CENTER, OverlayPosition.TOP_RIGHT),
        listOf(OverlayPosition.MIDDLE_LEFT, OverlayPosition.MIDDLE_CENTER, OverlayPosition.MIDDLE_RIGHT),
        listOf(OverlayPosition.BOTTOM_LEFT, OverlayPosition.BOTTOM_CENTER, OverlayPosition.BOTTOM_RIGHT)
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        positions.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { position ->
                    OutlinedButton(
                        onClick = { onPositionSelect(position) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedPosition == position) Color(0xFF2196F3).copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (selectedPosition == position) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = if (selectedPosition == position) 2.dp else 1.dp,
                            color = if (selectedPosition == position) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(getPositionLabel(position), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

fun getPositionLabel(position: OverlayPosition): String {
    return when (position) {
        OverlayPosition.TOP_LEFT -> "TL"
        OverlayPosition.TOP_CENTER -> "TC"
        OverlayPosition.TOP_RIGHT -> "TR"
        OverlayPosition.MIDDLE_LEFT -> "ML"
        OverlayPosition.MIDDLE_CENTER -> "MC"
        OverlayPosition.MIDDLE_RIGHT -> "MR"
        OverlayPosition.BOTTOM_LEFT -> "BL"
        OverlayPosition.BOTTOM_CENTER -> "BC"
        OverlayPosition.BOTTOM_RIGHT -> "BR"
    }
}

fun getOverlayPositionOffset(position: OverlayPosition, canvasWidth: Float, canvasHeight: Float): Offset {
    val xOffset = when (position) {
        OverlayPosition.TOP_LEFT, OverlayPosition.MIDDLE_LEFT, OverlayPosition.BOTTOM_LEFT -> canvasWidth * 0.1f
        OverlayPosition.TOP_CENTER, OverlayPosition.MIDDLE_CENTER, OverlayPosition.BOTTOM_CENTER -> canvasWidth * 0.5f
        OverlayPosition.TOP_RIGHT, OverlayPosition.MIDDLE_RIGHT, OverlayPosition.BOTTOM_RIGHT -> canvasWidth * 0.9f
    }
    
    val yOffset = when (position) {
        OverlayPosition.TOP_LEFT, OverlayPosition.TOP_CENTER, OverlayPosition.TOP_RIGHT -> canvasHeight * 0.15f
        OverlayPosition.MIDDLE_LEFT, OverlayPosition.MIDDLE_CENTER, OverlayPosition.MIDDLE_RIGHT -> canvasHeight * 0.5f
        OverlayPosition.BOTTOM_LEFT, OverlayPosition.BOTTOM_CENTER, OverlayPosition.BOTTOM_RIGHT -> canvasHeight * 0.85f
    }
    
    return Offset(xOffset, yOffset)
}

fun getOverlayPositionFromOffset(offset: Offset, canvasWidth: Float, canvasHeight: Float): OverlayPosition {
    val col = when {
        offset.x < canvasWidth / 3f -> 0
        offset.x < (canvasWidth * 2f) / 3f -> 1
        else -> 2
    }
    val row = when {
        offset.y < canvasHeight / 3f -> 0
        offset.y < (canvasHeight * 2f) / 3f -> 1
        else -> 2
    }

    return when (row) {
        0 -> when (col) {
            0 -> OverlayPosition.TOP_LEFT
            1 -> OverlayPosition.TOP_CENTER
            else -> OverlayPosition.TOP_RIGHT
        }
        1 -> when (col) {
            0 -> OverlayPosition.MIDDLE_LEFT
            1 -> OverlayPosition.MIDDLE_CENTER
            else -> OverlayPosition.MIDDLE_RIGHT
        }
        else -> when (col) {
            0 -> OverlayPosition.BOTTOM_LEFT
            1 -> OverlayPosition.BOTTOM_CENTER
            else -> OverlayPosition.BOTTOM_RIGHT
        }
    }
}

fun isOverlayHit(offset: Offset, center: Offset, text: String): Boolean {
    if (text.isBlank()) return false
    val textPaint = Paint().apply {
        textSize = OVERLAY_TEXT_SIZE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(text)
    val textHeight = OVERLAY_TEXT_SIZE
    val padding = 12f
    val left = center.x - (textWidth / 2f) - padding
    val right = center.x + (textWidth / 2f) + padding
    val top = center.y - (textHeight / 2f) - padding
    val bottom = center.y + (textHeight / 2f) + padding
    return offset.x in left..right && offset.y in top..bottom
}

fun DrawScope.drawOverlayText(
    text: String,
    position: Offset,
    canvasWidth: Float,
    canvasHeight: Float
) {
    // Draw larger overlay text with no background
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = OVERLAY_TEXT_SIZE
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(text)
    val textX = position.x - (textWidth / 2f)
    val textY = position.y + (textPaint.textSize / 2f) - (textPaint.descent() / 2f)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(text, textX, textY, textPaint)
    }
}