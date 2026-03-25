package com.playerid.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.playerid.app.data.Player
import com.playerid.app.data.VideoClip
import com.playerid.app.ui.components.SpotrScreenHeader
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreen(
    playerViewModel: PlayerViewModel,
    teamViewModel: TeamViewModel,
    onNavigateToTeams: () -> Unit
) {
    val teamName by teamViewModel.selectedTeam.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeam = remember(subscribedTeams, teamName) {
        subscribedTeams.firstOrNull { it.name == teamName }
    }
    val teamPrimary = parseScreenTeamColor(selectedTeam?.color, Color(0xFF1976D2))
    val teamSecondary = parseScreenTeamColor(selectedTeam?.awayColor, Color(0xFFE3F2FD))
    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var videos by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var cleanupInProgress by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoClip?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTeamMenu by remember { mutableStateOf(false) }
    var highlightPlaylistUris by remember { mutableStateOf<List<Uri>?>(null) }

    LaunchedEffect(teamName) {
        val selected = teamName
        if (selected.isNullOrBlank()) {
            videos = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        videos = loadTeamVideosForClips(context, selected)
        isLoading = false
    }

    if (teamName.isNullOrBlank()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select a team to view clips", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateToTeams) {
                    Text("Go to Teams")
                }
            }
        }
        return
    }

    val rosterPlayers = remember(allPlayers, teamName) {
        allPlayers.filter { it.team == teamName }
    }

    highlightPlaylistUris?.let { playlistUris ->
        VideoPlaybackScreen(
            videoUri = playlistUris.first(),
            detectedPlayers = emptyList(),
            onNavigateBack = { highlightPlaylistUris = null },
            playlistUris = playlistUris
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SpotrScreenHeader(
            title = "Clips",
            icon = Icons.Default.PlayArrow,
            gradient = listOf(teamPrimary, teamSecondary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(teamSecondary.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Box {
                OutlinedButton(
                    onClick = { showTeamMenu = true },
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, teamPrimary)
                ) {
                    Text("Team: ${teamName ?: "Select"}", color = teamPrimary)
                }
                DropdownMenu(
                    expanded = showTeamMenu,
                    onDismissRequest = { showTeamMenu = false }
                ) {
                    subscribedTeams.forEach { team ->
                        DropdownMenuItem(
                            text = { Text(team.name) },
                            onClick = {
                                playerViewModel.setSelectedTeam(team.name)
                                teamViewModel.selectTeam(team.name)
                                showTeamMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (cleanupInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = teamPrimary,
                    trackColor = teamSecondary.copy(alpha = 0.35f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            VideoLibraryScreen(
                teamName = teamName!!,
                videos = videos,
                rosterPlayers = rosterPlayers,
                isLoading = isLoading,
                lastRefreshedLabel = "",
                onNavigateBack = {},
                onRefresh = {
                    scope.launch {
                        isLoading = true
                        videos = loadTeamVideosForClips(context, teamName!!)
                        isLoading = false
                    }
                },
                onVideoSelected = { uri, _ ->
                    try {
                        val playIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(playIntent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No video player app found", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Unable to open clip", Toast.LENGTH_SHORT).show()
                    }
                },
                onVideoEdit = { _ -> },
                onVideoDelete = { video ->
                    videoToDelete = video
                    showDeleteDialog = true
                },
                onVideoNameChanged = { video, newName ->
                    val prefs = context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString(video.id, newName).apply()
                    videos = videos.map { if (it.id == video.id) it.copy(gameTitle = newName) else it }
                },
                onToggleHighlight = { video ->
                    val newStatus = !video.isHighlight
                    val prefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(video.id, newStatus).apply()
                    videos = videos.map { if (it.id == video.id) it.copy(isHighlight = newStatus) else it }
                },
                onCreateHighlightReel = { filter ->
                    val highlights = videos.filter { it.isHighlight }
                    val filtered = filterHighlightsForClips(highlights, filter)
                    if (filtered.isEmpty()) {
                        Toast.makeText(context, "No highlights found for ${filter.label}", Toast.LENGTH_SHORT).show()
                    } else {
                        highlightPlaylistUris = filtered.map { Uri.parse(it.filePath) }
                    }
                },
                showTopBar = false
            )
        }
    }

    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("Delete team clips?") },
            text = { Text("This removes clips associated with $teamName from your phone and clears saved mappings.") },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanupDialog = false
                        cleanupInProgress = true
                        scope.launch {
                            cleanupTeamClipsForClips(context, teamName!!)
                            videos = loadTeamVideosForClips(context, teamName!!)
                            cleanupInProgress = false
                        }
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }

    if (showDeleteDialog && videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete clip?") },
            text = { Text("This will permanently delete '${videoToDelete!!.gameTitle}' from your phone.") },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = videoToDelete
                        showDeleteDialog = false
                        if (target != null) {
                            scope.launch {
                                deleteClipForClips(context, target)
                                videos = loadTeamVideosForClips(context, teamName!!)
                                videoToDelete = null
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }
}

private suspend fun deleteClipForClips(context: android.content.Context, video: VideoClip) {
    return withContext(Dispatchers.IO) {
        val videoUri = android.net.Uri.parse(video.filePath)
        val videoId = video.id

        try {
            if (videoUri.scheme == "content") {
                context.contentResolver.delete(videoUri, null, null)
            }
            if (videoUri.scheme == "file") {
                val file = java.io.File(videoUri.path ?: "")
                file.delete()
            }
        } catch (_: Exception) {
        }

        val prefs = listOf(
            context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
        )
        prefs.forEach { pref ->
            pref.edit()
                .remove(videoId)
                .remove(video.filePath)
                .apply()
        }
    }
}

private suspend fun loadTeamVideosForClips(context: android.content.Context, teamName: String): List<VideoClip> {
    return withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoClip>()
        val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        val startPrefs = context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
        val highlightPrefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
        val customNamePrefs = context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
        val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
        val moviesDirs = context.getExternalFilesDirs(android.os.Environment.DIRECTORY_MOVIES).filterNotNull()
        val seenIds = mutableSetOf<String>()

        for (moviesDir in moviesDirs) {
            val videoFiles = moviesDir.listFiles { file ->
                file.isFile && file.extension.equals("mp4", ignoreCase = true)
            } ?: emptyArray()

            for (file in videoFiles.sortedByDescending { it.lastModified() }) {
                val videoPath = file.absolutePath
                val fileUriString = android.net.Uri.fromFile(file).toString()
                if (seenIds.contains(videoPath) || seenIds.contains(fileUriString)) continue

                val storedTeamName = teamPrefs.getString(videoPath, null)
                    ?: teamPrefs.getString(fileUriString, null)
                if (storedTeamName != teamName) continue

                val storedStartTime = startPrefs.getLong(videoPath, 0L)
                    .takeIf { it > 0L }
                    ?: startPrefs.getLong(fileUriString, 0L).takeIf { it > 0L }
                val createdAt = storedStartTime ?: file.lastModified()
                val gameDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(createdAt))
                val customName = customNamePrefs.getString(videoPath, null)
                    ?: customNamePrefs.getString(fileUriString, null)
                val opponentName = opponentPrefs.getString(videoPath, null)
                    ?: opponentPrefs.getString(fileUriString, null)
                val isHighlight = highlightPrefs.getBoolean(videoPath, false)
                    || highlightPrefs.getBoolean(fileUriString, false)
                val gameTitle = buildClipTitle(
                    baseTitle = file.nameWithoutExtension,
                    opponentName = opponentName,
                    customName = customName
                )

                val duration = try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(videoPath)
                    val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    retriever.release()
                    durationMs
                } catch (_: Exception) {
                    0L
                }

                videos.add(
                    VideoClip(
                        id = videoPath,
                        filePath = android.net.Uri.fromFile(file).toString(),
                        duration = duration,
                        createdAt = createdAt,
                        gameDate = gameDate,
                        gameTitle = gameTitle,
                        isHighlight = isHighlight
                    )
                )
                seenIds.add(videoPath)
                seenIds.add(fileUriString)
            }
        }

        val resolver = context.contentResolver
        val collection = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            android.provider.MediaStore.Video.Media._ID,
            android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
            android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
            android.provider.MediaStore.MediaColumns.DATE_TAKEN,
            android.provider.MediaStore.MediaColumns.DATE_ADDED,
            android.provider.MediaStore.Video.Media.DURATION
        )
        val selection = "${android.provider.MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf("Movies/PlayerID/")
        val sortOrder = "${android.provider.MediaStore.MediaColumns.DATE_ADDED} DESC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_ADDED)
            val durationIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val contentUri = android.content.ContentUris.withAppendedId(collection, id)
                val uriString = contentUri.toString()
                if (seenIds.contains(uriString)) continue

                val storedTeamName = teamPrefs.getString(uriString, null)
                if (storedTeamName != teamName) continue

                val displayName = cursor.getString(nameIndex) ?: "clip_$id"
                val dateTaken = cursor.getLong(dateTakenIndex)
                val dateAdded = cursor.getLong(dateAddedIndex)
                val storedStartTime = startPrefs.getLong(uriString, 0L).takeIf { it > 0L }
                val createdAt = storedStartTime ?: if (dateTaken > 0) dateTaken else dateAdded * 1000
                val gameDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(createdAt))
                val duration = cursor.getLong(durationIndex)
                val customName = customNamePrefs.getString(uriString, null)
                val opponentName = opponentPrefs.getString(uriString, null)
                val isHighlight = highlightPrefs.getBoolean(uriString, false)
                val baseTitle = displayName.substringBeforeLast(".", displayName)
                val gameTitle = buildClipTitle(
                    baseTitle = baseTitle,
                    opponentName = opponentName,
                    customName = customName
                )

                videos.add(
                    VideoClip(
                        id = uriString,
                        filePath = uriString,
                        duration = duration,
                        createdAt = createdAt,
                        gameDate = gameDate,
                        gameTitle = gameTitle,
                        isHighlight = isHighlight
                    )
                )
                seenIds.add(uriString)
            }
        }

        videos
    }
}

private suspend fun cleanupTeamClipsForClips(context: android.content.Context, teamName: String): Int {
    return withContext(Dispatchers.IO) {
        val teamVideos = loadTeamVideosForClips(context, teamName)
        var deletedCount = 0
        for (video in teamVideos) {
            try {
                deleteClipForClips(context, video)
                deletedCount += 1
            } catch (_: Exception) {
            }
        }
        deletedCount
    }
}

private fun parseScreenTeamColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

private fun filterHighlightsForClips(videos: List<VideoClip>, filter: HighlightReelFilter): List<VideoClip> {
    val now = System.currentTimeMillis()
    val oneDayMillis = 24L * 60L * 60L * 1000L
    val oneWeekMillis = 7L * oneDayMillis
    val oneMonthMillis = 30L * oneDayMillis
    val oneSeasonMillis = 120L * oneDayMillis

    val filtered = when (filter) {
        HighlightReelFilter.ALL -> videos
        HighlightReelFilter.TODAY -> videos.filter { now - it.createdAt < oneDayMillis }
        HighlightReelFilter.THIS_WEEK -> videos.filter { now - it.createdAt < oneWeekMillis }
        HighlightReelFilter.THIS_MONTH -> videos.filter { now - it.createdAt < oneMonthMillis }
        HighlightReelFilter.THIS_SEASON -> videos.filter { now - it.createdAt < oneSeasonMillis }
    }

    // Highlight reels should play in timeline order from oldest to newest.
    return filtered.sortedBy { it.createdAt }
}

private fun buildClipTitle(baseTitle: String, opponentName: String?, customName: String?): String {
    val trimmedCustom = customName?.trim().orEmpty()
    if (trimmedCustom.isNotEmpty()) return trimmedCustom

    val trimmedOpponent = opponentName?.trim().orEmpty()
    if (trimmedOpponent.isEmpty()) return baseTitle

    return if (baseTitle.contains("vs", ignoreCase = true)) {
        baseTitle
    } else {
        "$baseTitle vs $trimmedOpponent"
    }
}