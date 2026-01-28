package com.playerid.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.playerid.app.data.Player
import com.playerid.app.ui.composables.drawPlayerOverlay

data class NameBubble(
    val id: String,
    val playerName: String,
    val jerseyNumber: String,
    val position: Offset,
    val isVisible: Boolean = true,
    val isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
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

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Edit Video",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onSaveVideo(nameBubbles) }
                    ) {
                        Icon(Icons.Default.Save, "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            VideoEditorControls(
                isPlaying = isPlaying,
                onPlayPause = { 
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                    isPlaying = !isPlaying
                },
                onAutoDetect = {
                    // TODO: Implement auto-detection
                },
                nameBubbles = nameBubbles,
                onBubbleVisibilityToggle = { bubbleId ->
                    nameBubbles = nameBubbles.map { bubble ->
                        if (bubble.id == bubbleId) {
                            bubble.copy(isVisible = !bubble.isVisible)
                        } else bubble
                    }
                },
                onDeleteBubble = { bubbleId ->
                    nameBubbles = nameBubbles.filter { it.id != bubbleId }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
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
                                        val bubbleCenter = bubble.position
                                        val distance = kotlin.math.sqrt(
                                            (offset.x - bubbleCenter.x) * (offset.x - bubbleCenter.x) +
                                            (offset.y - bubbleCenter.y) * (offset.y - bubbleCenter.y)
                                        )
                                        distance < 50f 
                                    }
                                    
                                    if (tappedBubble != null) {
                                        selectedBubble = tappedBubble.id
                                    } else {
                                        tapPosition = offset
                                        showPlayerSelector = true
                                    }
                                },
                                onDrag = { _, dragAmount ->
                                    selectedBubble?.let { bubbleId ->
                                        nameBubbles = nameBubbles.map { bubble ->
                                            if (bubble.id == bubbleId) {
                                                bubble.copy(
                                                    position = Offset(
                                                        bubble.position.x + dragAmount.x,
                                                        bubble.position.y + dragAmount.y
                                                    )
                                                )
                                            } else bubble
                                        }
                                    }
                                },
                                onDragEnd = {
                                    selectedBubble = null
                                }
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
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    "Tap to add name bubble\nDrag to move bubbles",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
    
    if (showPlayerSelector) {
        AlertDialog(
            onDismissRequest = { showPlayerSelector = false },
            title = { Text("Select Player") },
            text = {
                LazyRow {
                    items(roster) { player ->
                        PlayerSelectionCard(
                            player = player,
                            onSelect = {
                                val newBubble = NameBubble(
                                    id = "bubble_${System.currentTimeMillis()}",
                                    playerName = player.name,
                                    jerseyNumber = player.number.toString(),
                                    position = tapPosition
                                )
                                nameBubbles = nameBubbles + newBubble
                                showPlayerSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlayerSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionCard(
    player: Player,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .padding(4.dp)
            .width(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1976D2)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.number.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            Text(
                player.name,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 2
            )
        }
    }
}

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                
                Button(
                    onClick = onAutoDetect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Detect", fontSize = 12.sp)
                }
                
                Text(
                    "${nameBubbles.size} bubbles",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            if (nameBubbles.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nameBubbles) { bubble ->
                        BubbleControlCard(
                            bubble = bubble,
                            onVisibilityToggle = { onBubbleVisibilityToggle(bubble.id) },
                            onDelete = { onDeleteBubble(bubble.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BubbleControlCard(
    bubble: NameBubble,
    onVisibilityToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bubble.isVisible) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "#${bubble.jerseyNumber}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                bubble.playerName,
                fontSize = 10.sp,
                maxLines = 1
            )
            
            Row {
                IconButton(
                    onClick = onVisibilityToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (bubble.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle visibility",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Red
                    )
                }
            }
        }
    }
}