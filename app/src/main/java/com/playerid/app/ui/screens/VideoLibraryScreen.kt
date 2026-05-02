package com.playerid.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.data.Player
import com.playerid.app.data.VideoClip
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
data class VideoLibrarySection(
    val key: String,
    val title: String,
    val subtitle: String,
    val videos: List<VideoClip>
)

@ExperimentalMaterial3Api
@Composable
fun VideoLibraryScreen(
    teamName: String,
    videos: List<VideoClip>,
    sections: List<VideoLibrarySection> = emptyList(),
    rosterPlayers: List<Player>,
    isLoading: Boolean,
    lastRefreshedLabel: String,
    emptyStateTitle: String = "No videos yet",
    emptyStateSubtitle: String = "Record a video to get started",
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onVideoSelected: (Uri, List<Player>) -> Unit,
    onVideoEdit: (Uri) -> Unit,
    onVideoShare: (Uri) -> Unit,
    onVideoDelete: (VideoClip) -> Unit,
    onVideoNameChanged: (VideoClip, String) -> Unit = { _, _ -> },
    onOpponentChanged: (VideoClip, String) -> Unit = { _, _ -> },
    onToggleHighlight: (VideoClip) -> Unit = {},
    onCreateHighlightReel: (HighlightReelFilter) -> Unit = {},
    showTopBar: Boolean = true
) {
    var selectedFilter by remember { mutableStateOf(HighlightReelFilter.ALL) }
    var showFilters by remember { mutableStateOf(false) } // Collapsed by default for simplicity
    val displaySections = remember(videos, sections) {
        if (sections.isNotEmpty()) {
            sections
        } else if (videos.isNotEmpty()) {
            listOf(VideoLibrarySection(key = "all", title = "All Clips", subtitle = "${videos.size} clips", videos = videos))
        } else {
            emptyList()
        }
    }
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
                        emptyStateTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        emptyStateSubtitle,
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Highlights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "$highlightCount ready in ${selectedFilter.label.lowercase(Locale.getDefault())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onCreateHighlightReel(selectedFilter) },
                                    enabled = highlightCount > 0
                                ) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play")
                                }
                            }

                            if (showFilters) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
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
                            } else {
                                TextButton(onClick = { showFilters = true }, modifier = Modifier.align(Alignment.Start)) {
                                    Text("Change timeframe")
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displaySections.forEach { section ->
                        item(key = "section-${section.key}") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    section.title.ifBlank { "Unspecified" }.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    section.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(items = section.videos, key = { it.id }) { video ->
                            val clipTitleOverride = if (section.title.isBlank()) {
                                "${video.momentTag ?: "Clip"} • ${formatDate(video.gameDate)}"
                            } else {
                                null
                            }
                            VideoClipCard(
                                video = video,
                                titleOverride = clipTitleOverride,
                                onPlayClick = {
                                    onVideoSelected(Uri.parse(video.filePath), rosterPlayers)
                                },
                                onEditClick = {
                                    onVideoEdit(Uri.parse(video.filePath))
                                },
                                onShareClick = {
                                    onVideoShare(Uri.parse(video.filePath))
                                },
                                onDeleteClick = {
                                    onVideoDelete(video)
                                },
                                onNameChanged = { newName ->
                                    onVideoNameChanged(video, newName)
                                },
                                onOpponentChanged = { opponent ->
                                    onOpponentChanged(video, opponent)
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
}

@ExperimentalMaterial3Api
@Composable
private fun VideoClipCard(
    video: VideoClip,
    titleOverride: String? = null,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNameChanged: (String) -> Unit = {},
    onOpponentChanged: (String) -> Unit = {},
    onToggleHighlight: () -> Unit = {}
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var editedName by remember(video.id, video.momentTag) { mutableStateOf(video.momentTag ?: "Clip") }
    val context = LocalContext.current
    val thumbnail by rememberVideoThumbnail(context, video.filePath)

    if (showDetailsDialog) {
        val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
        val currentOpponent = opponentPrefs.getString(video.id, null) ?: ""
        val allOpponents = remember {
            opponentPrefs.all.values
                .filterIsInstance<String>()
                .filter { it.isNotBlank() }
                .map { it.trim() }
                .distinct()
                .sortedBy { it.lowercase() }
        }
        val focusRequester = remember { FocusRequester() }
        var opponentField by remember {
            mutableStateOf(
                TextFieldValue(
                    text = currentOpponent,
                    selection = TextRange(0, currentOpponent.length)
                )
            )
        }
        var titleField by remember {
            mutableStateOf(
                TextFieldValue(
                    text = editedName,
                    selection = TextRange(0, editedName.length)
                )
            )
        }
        var opponentDropdownExpanded by remember { mutableStateOf(false) }
        val filteredOpponents = remember(opponentField.text) {
            val query = opponentField.text.trim().lowercase()
            if (query.isEmpty()) allOpponents
            else allOpponents.filter { it.lowercase().contains(query) }
        }
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Edit Clip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = titleField,
                        onValueChange = { titleField = it },
                        label = { Text("Clip title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = opponentDropdownExpanded && filteredOpponents.isNotEmpty(),
                        onExpandedChange = { opponentDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = opponentField,
                            onValueChange = {
                                opponentField = it
                                opponentDropdownExpanded = true
                            },
                            label = { Text("Opponent") },
                            placeholder = { Text("e.g. Lions FC") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = opponentDropdownExpanded && filteredOpponents.isNotEmpty(),
                            onDismissRequest = { opponentDropdownExpanded = false }
                        ) {
                            filteredOpponents.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        opponentField = TextFieldValue(
                                            text = suggestion,
                                            selection = TextRange(suggestion.length)
                                        )
                                        opponentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            },
            dismissButton = {
                TextButton(onClick = { showDetailsDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTitle = titleField.text
                        if (newTitle.isNotBlank()) {
                            editedName = newTitle
                            onNameChanged(newTitle)
                        }
                        onOpponentChanged(opponentField.text.trim())
                        showDetailsDialog = false
                    }
                ) { Text("Save") }
            }
        )
    }

    val momentColor = when (video.momentTag) {
        "Goal"     -> Color(0xFF2E7D32)
        "Assist"   -> Color(0xFF1565C0)
        "Save"     -> Color(0xFF6A1B9A)
        "Big Play" -> Color(0xFFE65100)
        else       -> Color(0xFF37474F)
    }
    val momentIcon = when (video.momentTag) {
        "Goal"     -> Icons.Default.Star
        "Assist"   -> Icons.Default.Share
        "Save"     -> Icons.Default.StarBorder
        "Big Play" -> Icons.Default.PlayArrow
        else       -> Icons.Default.VideoLibrary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: video frame thumbnail with colored fallback
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clickable(onClick = onPlayClick)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(momentColor, momentColor.copy(alpha = 0.72f))
                        )
                    )
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "Clip preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        momentIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }
            }

            // Right: title, Share, Include
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    titleOverride ?: (video.momentTag ?: "Clip"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onShareClick,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onToggleHighlight,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (video.isHighlight) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Include", fontSize = 12.sp)
                    }
                }
            }

            // Overflow menu
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Details") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            showDetailsDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Video") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
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
            try { retriever.release() } catch (_: Exception) { }
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
