package com.playerid.app.ui.screens

import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.data.VideoClip
import com.playerid.app.viewmodels.TeamViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClipsStableScreen(
    teamViewModel: TeamViewModel,
    onNavigateToTeams: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeamFromVm by teamViewModel.selectedTeam.collectAsState()

    var selectedTeam by remember { mutableStateOf<String?>(null) }
    var showTeamMenu by remember { mutableStateOf(false) }
    var showSeasonMenu by remember { mutableStateOf(false) }
    var showOpponentMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var clips by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<String?>(null) }
    var selectedOpponent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTeamFromVm, subscribedTeams) {
        if (selectedTeam.isNullOrBlank()) {
            selectedTeam = selectedTeamFromVm ?: subscribedTeams.firstOrNull()?.name
        }
    }

    LaunchedEffect(selectedTeam) {
        val team = selectedTeam
        if (team.isNullOrBlank()) {
            clips = emptyList()
            selectedSeason = null
            selectedOpponent = null
            return@LaunchedEffect
        }
        isLoading = true
        clips = loadStableTeamClips(context, team)
        selectedSeason = null
        selectedOpponent = null
        isLoading = false
    }

    // Extract seasons and opponents from current clips
    val availableSeasons = remember(clips) {
        extractSeasons(clips).sortedByDescending { it }
    }
    val availableOpponents = remember(clips) {
        extractOpponents(clips).sorted()
    }

    // Apply filters in simple, explicit steps
    val seasonFilteredClips = remember(clips, selectedSeason) {
        applySeasonFilter(clips, selectedSeason)
    }
    val filteredClips = remember(seasonFilteredClips, selectedOpponent) {
        applyOpponentFilter(seasonFilteredClips, selectedOpponent)
    }

    // Group clips by game date
    val gameGroups = remember(filteredClips) {
        groupClipsByDate(filteredClips)
    }

    if (subscribedTeams.isEmpty()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No teams yet", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateToTeams) {
                    Text("Go to Teams")
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Team selector
        Box {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                onClick = { showTeamMenu = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = selectedTeam ?: "Select team",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = showTeamMenu, onDismissRequest = { showTeamMenu = false }) {
                subscribedTeams.forEach { team ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                team.name,
                                fontWeight = if (team.name == selectedTeam) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            selectedTeam = team.name
                            showTeamMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Season filter
            Box {
                FilterChip(
                    selected = selectedSeason != null,
                    onClick = { showSeasonMenu = true },
                    label = {
                        Text(
                            text = selectedSeason ?: "Season",
                            fontSize = 12.sp
                        )
                    },
                    trailingIcon = if (selectedSeason != null) {
                        { Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp)) }
                    } else null
                )
                if (selectedSeason != null && !showSeasonMenu) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(
                            onClick = { selectedSeason = null },
                            modifier = Modifier.size(24.dp)
                        ) {}
                    }
                }
                DropdownMenu(expanded = showSeasonMenu, onDismissRequest = { showSeasonMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All seasons") },
                        onClick = {
                            selectedSeason = null
                            showSeasonMenu = false
                        }
                    )
                    availableSeasons.forEach { season ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    season,
                                    fontWeight = if (season == selectedSeason) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedSeason = season
                                showSeasonMenu = false
                            }
                        )
                    }
                }
            }

            // Opponent filter
            Box {
                FilterChip(
                    selected = selectedOpponent != null,
                    onClick = { showOpponentMenu = true },
                    label = {
                        Text(
                            text = selectedOpponent ?: "Opponent",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = if (selectedOpponent != null) {
                        { Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp)) }
                    } else null
                )
                if (selectedOpponent != null && !showOpponentMenu) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(
                            onClick = { selectedOpponent = null },
                            modifier = Modifier.size(24.dp)
                        ) {}
                    }
                }
                DropdownMenu(expanded = showOpponentMenu, onDismissRequest = { showOpponentMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All opponents") },
                        onClick = {
                            selectedOpponent = null
                            showOpponentMenu = false
                        }
                    )
                    availableOpponents.forEach { opponent ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    opponent,
                                    fontWeight = if (opponent == selectedOpponent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                selectedOpponent = opponent
                                showOpponentMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clips list
        when {
            isLoading -> {
                Text("Loading clips...", style = MaterialTheme.typography.bodyMedium)
            }
            filteredClips.isEmpty() && clips.isNotEmpty() -> {
                Text("No clips match the selected filters", style = MaterialTheme.typography.bodyMedium)
            }
            filteredClips.isEmpty() -> {
                Text("No clips found for this team", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    gameGroups.forEach { (dateLabel, groupClips) ->
                        item {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(groupClips, key = { it.id }) { clip ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = clip.gameTitle.ifBlank { "Clip" },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatDuration(clip.duration),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

private suspend fun loadStableTeamClips(context: android.content.Context, teamName: String): List<VideoClip> {
    return withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoClip>()
        val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        val startPrefs = context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
        val customNamePrefs = context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
        val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
        val seen = mutableSetOf<String>()

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )

        context.contentResolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(collection, id)
                val uriString = uri.toString()
                if (seen.contains(uriString)) continue

                val storedTeam = teamPrefs.getString(uriString, null)?.trim()
                if (!storedTeam.equals(teamName, ignoreCase = true)) continue

                val createdAt = startPrefs.getLong(uriString, 0L).takeIf { it > 0L }
                    ?: cursor.getLong(dateAddedIndex) * 1000
                val displayName = cursor.getString(nameIndex) ?: "clip_$id"
                val baseTitle = displayName.substringBeforeLast('.', displayName)
                val customName = customNamePrefs.getString(uriString, null)
                val opponentName = opponentPrefs.getString(uriString, null)
                val gameTitle = when {
                    !customName.isNullOrBlank() -> customName
                    !opponentName.isNullOrBlank() -> "$baseTitle vs ${opponentName.trim()}"
                    else -> baseTitle
                }

                val duration = cursor.getLong(durationIndex).takeIf { it > 0L } ?: run {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        retriever.release()
                        value
                    } catch (_: Exception) {
                        0L
                    }
                }

                videos.add(
                    VideoClip(
                        id = uriString,
                        filePath = uriString,
                        duration = duration,
                        createdAt = createdAt,
                        gameDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt)),
                        gameTitle = gameTitle
                    )
                )
                seen.add(uriString)
            }
        }

        videos.sortedByDescending { it.createdAt }
    }
}

private fun extractSeasons(clips: List<VideoClip>): List<String> {
    val seasons = mutableListOf<String>()
    for (clip in clips) {
        val season = parseSeason(clip.gameDate)
        if (season.isNotEmpty() && !seasons.contains(season)) {
            seasons.add(season)
        }
    }
    return seasons
}

private fun extractOpponents(clips: List<VideoClip>): List<String> {
    val opponents = mutableListOf<String>()
    for (clip in clips) {
        val opponent = parseOpponentFromTitle(clip.gameTitle)
        if (opponent.isNotEmpty() && !opponents.contains(opponent)) {
            opponents.add(opponent)
        }
    }
    return opponents
}

private fun applySeasonFilter(clips: List<VideoClip>, selectedSeason: String?): List<VideoClip> {
    if (selectedSeason.isNullOrBlank()) {
        return clips
    }
    return clips.filter { clip ->
        val clipSeason = parseSeason(clip.gameDate)
        clipSeason.equals(selectedSeason, ignoreCase = true)
    }
}

private fun applyOpponentFilter(clips: List<VideoClip>, selectedOpponent: String?): List<VideoClip> {
    if (selectedOpponent.isNullOrBlank()) {
        return clips
    }
    return clips.filter { clip ->
        val clipOpponent = parseOpponentFromTitle(clip.gameTitle)
        clipOpponent.equals(selectedOpponent, ignoreCase = true)
    }
}

private fun groupClipsByDate(clips: List<VideoClip>): List<Pair<String, List<VideoClip>>> {
    val groups = mutableListOf<Pair<String, List<VideoClip>>>()
    val grouped = clips.groupBy { formatDate(it.createdAt) }
    for ((date, clipsForDate) in grouped) {
        groups.add(Pair(date, clipsForDate))
    }
    return groups
}

private fun parseSeason(dateString: String): String {
    return try {
        val parts = dateString.split("-")
        if (parts.size >= 1) {
            val year = parts[0].toIntOrNull() ?: return ""
            "${year}-${year + 1}"
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }
}

private fun parseOpponentFromTitle(title: String): String {
    return try {
        if (title.contains(" vs ", ignoreCase = true)) {
            val parts = title.split(" vs ", ignoreCase = true)
            if (parts.size > 1) {
                parts[1].trim()
            } else {
                ""
            }
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        "Unknown date"
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
