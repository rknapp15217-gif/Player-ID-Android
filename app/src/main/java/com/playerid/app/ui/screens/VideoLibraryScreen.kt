package com.playerid.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import com.playerid.app.data.Player
import com.playerid.app.data.VideoClip
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    onVideoDelete: (VideoClip) -> Unit,
    onVideoNameChanged: (VideoClip, String) -> Unit = { _, _ -> },
    onToggleHighlight: (VideoClip) -> Unit = {},
    onCreateHighlightReel: (HighlightReelFilter) -> Unit = {},
    showTopBar: Boolean = true
) {
    var selectedFilter by remember { mutableStateOf(HighlightReelFilter.ALL) }
    var showFilters by remember { mutableStateOf(false) } // Collapsed by default for simplicity
    Scaffold(
        topBar = {
            if (showTopBar) {
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
                // Highlight Reel section
                val allHighlights = videos.filter { it.isHighlight }
                val filteredHighlights = filterHighlightsByDate(allHighlights, selectedFilter)
                val highlightCount = filteredHighlights.size
                
                if (allHighlights.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Simplified: Only show filters when user taps "Change"
                        if (showFilters) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HighlightReelFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { 
                                            selectedFilter = filter
                                            showFilters = false
                                        },
                                        label = { Text(filter.label) }
                                    )
                                }
                            }
                        }
                        
                        // Large, obvious primary action
                        Button(
                            onClick = { onCreateHighlightReel(selectedFilter) },
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                            enabled = highlightCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (highlightCount > 0) "✨ Watch Highlights" 
                                        else "No highlights yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                                if (highlightCount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "$highlightCount clips • ${selectedFilter.label}",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                        
                        // Subtle option to change timeframe
                        if (!showFilters) {
                            TextButton(
                                onClick = { showFilters = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Change timeframe", fontSize = 13.sp)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = videos, key = { it.id }) { video ->
                        VideoClipCard(
                            video = video,
                            onPlayClick = {
                                onVideoSelected(Uri.parse(video.filePath), rosterPlayers)
                            },
                            onEditClick = {
                                onVideoEdit(Uri.parse(video.filePath))
                            },
                            onDeleteClick = {
                                onVideoDelete(video)
                            },
                            onNameChanged = { newName ->
                                onVideoNameChanged(video, newName)
                            },
                            onToggleHighlight = {
                                onToggleHighlight(video)
                            }
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun VideoClipCard(
    video: VideoClip,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNameChanged: (String) -> Unit = {},
    onToggleHighlight: () -> Unit = {}
) {
    var showNameEditDialog by remember { mutableStateOf(false) }
    var editedName by remember(video.id, video.gameTitle) { mutableStateOf(video.gameTitle) }
    val context = LocalContext.current
    val thumbnail by rememberVideoThumbnail(context, video.filePath)

    if (showNameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            title = { Text("Edit Clip Name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Clip name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showNameEditDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onNameChanged(editedName)
                        }
                        showNameEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clickable(onClick = onPlayClick)
                    .background(Color.Black)
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "Clip preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            video.gameTitle.ifBlank { "Game Video" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            "${formatDateTime(video.createdAt)} • ${formatDuration(video.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { showNameEditDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit name",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete clip",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Watch", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onToggleHighlight,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (video.isHighlight) Color(0xFF4CAF50).copy(alpha = 0.14f) else Color.Transparent
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (video.isHighlight) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (video.isHighlight) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = if (video.isHighlight) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (video.isHighlight) "Included" else "Include",
                        fontSize = 13.sp,
                        color = if (video.isHighlight) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 13.sp)
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

private fun formatDateTime(timestampMillis: Long): String {
    return try {
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(timestampMillis))
    } catch (_: Exception) {
        ""
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

enum class HighlightReelFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_SEASON("This Season")
}

private fun filterHighlightsByDate(videos: List<VideoClip>, filter: HighlightReelFilter): List<VideoClip> {
    val now = System.currentTimeMillis()
    val oneDayMillis = 24L * 60L * 60L * 1000L
    val oneWeekMillis = 7L * oneDayMillis
    val oneMonthMillis = 30L * oneDayMillis
    val oneSeasonMillis = 120L * oneDayMillis // ~4 months
    
    return when (filter) {
        HighlightReelFilter.ALL -> videos
        HighlightReelFilter.TODAY -> videos.filter { now - it.createdAt < oneDayMillis }
        HighlightReelFilter.THIS_WEEK -> videos.filter { now - it.createdAt < oneWeekMillis }
        HighlightReelFilter.THIS_MONTH -> videos.filter { now - it.createdAt < oneMonthMillis }
        HighlightReelFilter.THIS_SEASON -> videos.filter { now - it.createdAt < oneSeasonMillis }
    }
}

@Composable
private fun rememberVideoThumbnail(context: Context, filePath: String) = produceState<Bitmap?>(
    initialValue = null,
    key1 = filePath
) {
    value = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val sourceUri = Uri.parse(filePath)
            if (sourceUri.scheme == "content") {
                retriever.setDataSource(context, sourceUri)
            } else {
                val directPath = if (sourceUri.scheme == "file") sourceUri.path else filePath
                if (directPath.isNullOrBlank()) return@withContext null
                retriever.setDataSource(directPath)
            }

            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime()
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
