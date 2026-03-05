package com.playerid.app.ui.screens

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.playerid.app.ui.composables.drawPlayerOverlay
import kotlinx.coroutines.delay

private const val TAG = "VideoEditorScreen"

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
    var selectedBubble by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showPlayerSelector by remember { mutableStateOf(false) }
    var tapPosition by remember { mutableStateOf(Offset.Zero) }
    
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
                    onBubbleVisibilityToggle = { id ->
                        nameBubbles = nameBubbles.map { if (it.id == id) it.copy(isVisible = !it.isVisible) else it }
                    },
                    onDeleteBubble = { id -> nameBubbles = nameBubbles.filter { it.id != id } }
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
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
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
                                onDrag = { _, dragAmount ->
                                    selectedBubble?.let { id ->
                                        nameBubbles = nameBubbles.map {
                                            if (it.id == id) it.copy(position = it.position + dragAmount) else it
                                        }
                                    }
                                },
                                onDragEnd = { selectedBubble = null }
                            )
                        }
                ) {
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
                }
            }
            
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
    onBubbleVisibilityToggle: (String) -> Unit,
    onDeleteBubble: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPause) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
            }
            
            Button(onClick = onAutoDetect, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Auto-Tag", fontSize = 12.sp)
            }
            
            if (nameBubbles.isNotEmpty()) {
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
        }
    }
}