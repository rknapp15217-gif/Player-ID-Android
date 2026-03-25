package com.playerid.app.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Video Playback Screen with selectable player overlays
 * 
 * Shows a list of detected players and allows user to toggle
 * which players should have name bubbles displayed during playback
 */
@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VideoPlaybackScreen(
    videoUri: Uri,
    detectedPlayers: List<Player>,
    onNavigateBack: () -> Unit,
    playlistUris: List<Uri> = emptyList() // For highlight reel mode
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var videoDuration by remember { mutableLongStateOf(0L) }
    var currentVideoIndex by remember { mutableIntStateOf(0) }
    
    val isPlaylistMode = playlistUris.isNotEmpty()
    val totalVideos = if (isPlaylistMode) playlistUris.size else 1
    
    // Track which players are selected for overlay
    var selectedPlayerIds by remember { 
        mutableStateOf(detectedPlayers.take(min(3, detectedPlayers.size)).map { it.id }.toSet())
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            if (isPlaylistMode) {
                // Add all videos to playlist
                playlistUris.forEach { uri ->
                    addMediaItem(MediaItem.fromUri(uri))
                }
                repeatMode = Media3Player.REPEAT_MODE_OFF // Don't repeat in playlist mode
            } else {
                setMediaItem(MediaItem.fromUri(videoUri))
                repeatMode = Media3Player.REPEAT_MODE_ONE
            }
            prepare()
        }
    }

    // Track playback progress
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Update current position periodically
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            if (isPlaylistMode) {
                currentVideoIndex = exoPlayer.currentMediaItemIndex
            }
            delay(100)
        }
    }

    // Update playback state
    DisposableEffect(Unit) {
        val listener = object : Media3Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Media3Player.STATE_READY) {
                    videoDuration = exoPlayer.duration
                }
            }
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (isPlaylistMode) {
                    currentVideoIndex = exoPlayer.currentMediaItemIndex
                }
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
            if (!isPlaylistMode) {
                TopAppBar(
                    title = {
                        Text(
                            "Video Playback",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Video player
            Box(
                modifier = if (isPlaylistMode) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                }
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

                if (!isPlaylistMode) {
                    // Overlay selected player name bubbles
                    Box(modifier = Modifier.fillMaxSize()) {
                        detectedPlayers.filter { selectedPlayerIds.contains(it.id) }.forEach { player ->
                            // Position bubbles at different locations (row-by-row layout)
                            val rowIndex = detectedPlayers.filter { selectedPlayerIds.contains(it.id) }
                                .indexOf(player)
                            val row = rowIndex / 3
                            val col = rowIndex % 3

                            val bubbleOffset = Offset(
                                x = 40.dp.value + (col * 120.dp.value),
                                y = 40.dp.value + (row * 100.dp.value)
                            )

                            PlayerNameBubble(
                                playerName = player.name,
                                jerseyNumber = player.number,
                                offset = bubbleOffset,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            "${currentVideoIndex + 1} / $totalVideos",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (videoDuration > 0) {
                            LinearProgressIndicator(
                                progress = { currentPosition.toFloat() / videoDuration.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.35f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        IconButton(onClick = { isPlaying = !isPlaying }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            if (!isPlaylistMode) {
                // Playback progress bar
                if (videoDuration > 0) {
                    LinearProgressIndicator(
                        progress = { currentPosition.toFloat() / videoDuration.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Playback controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying }
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        "${(currentPosition / 1000 / 60) % 60}:${(currentPosition / 1000) % 60} / " +
                        "${(videoDuration / 1000 / 60) % 60}:${(videoDuration / 1000) % 60}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Player selection for overlay (hidden in playlist mode)
            if (!isPlaylistMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Select players to show during playback",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                if (detectedPlayers.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "No players detected",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(detectedPlayers) { player ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPlayerIds.contains(player.id))
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${player.name} #${player.number}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!player.position.isNullOrBlank()) {
                                            Text(
                                                player.position!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = selectedPlayerIds.contains(player.id),
                                        onCheckedChange = { isChecked ->
                                            selectedPlayerIds = if (isChecked) {
                                                selectedPlayerIds + player.id
                                            } else {
                                                selectedPlayerIds - player.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

/**
 * Renders a circular name bubble for a player with jersey number
 */
@Composable
private fun PlayerNameBubble(
    playerName: String,
    jerseyNumber: String,
    offset: Offset,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .padding(start = offset.x.dp, top = offset.y.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.8f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "#$jerseyNumber",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                playerName.split(" ").lastOrNull() ?: playerName,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 2
            )
        }
    }
}
