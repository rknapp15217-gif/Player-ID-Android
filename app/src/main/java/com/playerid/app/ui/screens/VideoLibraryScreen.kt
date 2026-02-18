package com.playerid.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.data.Player
import com.playerid.app.data.VideoClip
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Video Library Screen
 * 
 * Displays all recorded videos for a team.
 * User can select a video to play with player overlay options.
 */
@ExperimentalMaterial3Api
@Composable
fun VideoLibraryScreen(
    teamName: String,
    videos: List<VideoClip>,
    rosterPlayers: List<Player>,
    isLoading: Boolean,
    lastRefreshedLabel: String,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onVideoSelected: (Uri, List<Player>) -> Unit,
    onVideoEdit: (Uri) -> Unit,
    onVideoDelete: (VideoClip) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Video Library", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(teamName, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        if (lastRefreshedLabel.isNotBlank()) {
                            Text(
                                lastRefreshedLabel,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, "Refresh")
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Loading videos...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (videos.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No videos yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Record a video to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = videos, key = { it.id }) { video ->
                        val scope = rememberCoroutineScope()
                        val density = LocalDensity.current
                        val defaultRevealPx = with(density) { 112.dp.toPx() }
                        val extraRevealPx = with(density) { 12.dp.toPx() }
                        val revealPxState = remember { mutableStateOf(defaultRevealPx) }
                        val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
                        val revealPx = revealPxState.value
                        val dragState = rememberDraggableState { delta ->
                            val newOffset = (offsetX.value + delta).coerceIn(-revealPx, 0f)
                            scope.launch { offsetX.snapTo(newOffset) }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    "Delete",
                                    modifier = Modifier
                                        .clickable {
                                            scope.launch {
                                                offsetX.animateTo(0f)
                                                onVideoDelete(video)
                                            }
                                        }
                                        .onSizeChanged { size ->
                                            revealPxState.value = size.width.toFloat() + extraRevealPx
                                        }
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .draggable(
                                        state = dragState,
                                        orientation = Orientation.Horizontal,
                                        onDragStopped = {
                                            scope.launch {
                                                val shouldReveal = kotlin.math.abs(offsetX.value) > revealPx / 2
                                                val target = if (shouldReveal) -revealPx else 0f
                                                offsetX.animateTo(target)
                                            }
                                        }
                                    )
                            ) {
                                VideoClipCard(
                                    video = video,
                                    onPlayClick = {
                                        onVideoSelected(Uri.parse(video.filePath), rosterPlayers)
                                    },
                                    onEditClick = {
                                        onVideoEdit(Uri.parse(video.filePath))
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

/**
 * Card displaying a single video clip
 */
@ExperimentalMaterial3Api
@Composable
private fun VideoClipCard(
    video: VideoClip,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp, 60.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        video.gameTitle.ifBlank { "Game Video" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatDate(video.gameDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatDuration(video.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View")
                }

                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
