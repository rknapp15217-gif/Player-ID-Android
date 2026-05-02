package com.playerid.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.playerid.app.data.Player
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.data.VideoClip
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NO_OPPONENT_FILTER = "No opponent specified"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreen(
    playerViewModel: PlayerViewModel,
    teamViewModel: TeamViewModel,
    cameraHandoffToken: Int = 0,
    onNavigateToTeams: () -> Unit
) {
    val cameraTeam by teamViewModel.selectedTeam.collectAsState()
    // Local state per screen; Camera only hands off on explicit camera navigation exits.
    var localTeamName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(cameraHandoffToken) {
        localTeamName = cameraTeam
    }
    val teamName = localTeamName
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeam = remember(subscribedTeams, teamName) {
        subscribedTeams.firstOrNull { it.name == teamName }
    }
    val teamPrimary = parseScreenTeamColor(selectedTeam?.color, Color(0xFF1976D2))
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
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoStartInShareFlow by remember { mutableStateOf(false) }
    var shareDialogUri by remember { mutableStateOf<Uri?>(null) }
    var shareSelectedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSharePlayerList by remember { mutableStateOf(false) }
    var showManualShareOptions by remember { mutableStateOf(false) }
    val shareContactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val savedUri = shareDialogUri
            if (savedUri != null) {
                val contact = readSelectedContact(context, contactUri)
                if (contact != null) {
                    shareVideoToPhoneContact(context, savedUri, emptyList(), contact)
                    shareDialogUri = null
                }
            }
        }
    }
    var selectedGameKey by remember(teamName) { mutableStateOf<String?>(null) }

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
    val shareRosterPlayers = remember(rosterPlayers) {
        rosterPlayers.sortedWith(
            compareBy<Player>(
                { it.number.toIntOrNull() ?: Int.MAX_VALUE },
                { it.number },
                { it.name }
            )
        )
    }
    val opponentLookup = remember(videos) {
        buildClipOpponentLookup(context, videos)
    }
    val gameSections = remember(videos, opponentLookup) {
        buildClipGameSections(videos, opponentLookup)
    }
    val selectedGameSection = remember(gameSections, selectedGameKey) {
        gameSections.firstOrNull { it.key == selectedGameKey }
    }
    // Chips remain opponent-based; list sections can split by date.
    val visibleSections = remember(selectedGameSection, videos, opponentLookup) {
        if (selectedGameSection != null) {
            buildClipDateSubSections(selectedGameSection.videos, opponentLookup)
        } else {
            buildClipListSectionsForAllGames(videos, opponentLookup)
        }
    }
    val visibleVideos = remember(visibleSections) {
        visibleSections.flatMap { it.videos }
    }
    LaunchedEffect(gameSections, selectedGameKey) {
        if (selectedGameKey != null && selectedGameSection == null) {
            selectedGameKey = null
        }
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

    // Regular (non-share) playback: open native phone player for smooth scrubbing
    LaunchedEffect(selectedVideoUri, selectedVideoStartInShareFlow) {
        val uri = selectedVideoUri
        if (uri != null && !selectedVideoStartInShareFlow) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
            }
            selectedVideoUri = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Team switcher — left aligned
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .clickable { showTeamMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        teamName ?: "Select team",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Switch team",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showTeamMenu,
                    onDismissRequest = { showTeamMenu = false }
                ) {
                    subscribedTeams.forEach { team ->
                        DropdownMenuItem(
                            text = { Text(team.name) },
                            onClick = {
                                localTeamName = team.name
                                showTeamMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (cleanupInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                "Jump to opponent",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedGameKey == null,
                    onClick = { selectedGameKey = null },
                    label = { Text("All games") }
                )
                gameSections.forEach { section ->
                    FilterChip(
                        selected = selectedGameKey == section.key,
                        onClick = {
                            selectedGameKey = if (selectedGameKey == section.key) null else section.key
                        },
                        label = {
                            Text(section.title.ifBlank { "Unspecified" })
                        }
                    )
                }
            }

            selectedGameSection?.let { section ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(section.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(section.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { selectedGameKey = null }) {
                            Text("Show all")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            VideoLibraryScreen(
                teamName = teamName!!,
                videos = visibleVideos,
                sections = visibleSections,
                rosterPlayers = rosterPlayers,
                isLoading = isLoading,
                lastRefreshedLabel = "",
                emptyStateTitle = if (selectedGameSection != null) "No clips in this game" else "No videos yet",
                emptyStateSubtitle = if (selectedGameSection != null) {
                    "Try another game or return to all games"
                } else {
                    "Record a video to get started"
                },
                onNavigateBack = {},
                onRefresh = {
                    scope.launch {
                        isLoading = true
                        videos = loadTeamVideosForClips(context, teamName!!)
                        isLoading = false
                    }
                },
                onVideoSelected = { uri, _ ->
                    selectedVideoStartInShareFlow = false
                    selectedVideoUri = uri
                },
                onVideoEdit = { uri ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        Toast.makeText(context, "No video editor found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onOpponentChanged = { video, opponent ->
                    val prefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString(video.id, opponent).apply()
                    // Refresh the video list to rebuild sections with new opponent
                    scope.launch {
                        videos = loadTeamVideosForClips(context, teamName!!)
                    }
                },
                onVideoShare = { uri ->
                    shareSelectedPlayerIds = emptySet()
                    showSharePlayerList = false
                    showManualShareOptions = false
                    shareDialogUri = uri
                },
                onVideoDelete = { video ->
                    videoToDelete = video
                    showDeleteDialog = true
                },
                onVideoNameChanged = { video, newName ->
                    val prefs = context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString(video.id, newName).apply()
                    videos = videos.map { if (it.id == video.id) it.copy(momentTag = newName) else it }
                },
                onToggleHighlight = { video ->
                    val newStatus = !video.isHighlight
                    val prefs = context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(video.id, newStatus).apply()
                    videos = videos.map { if (it.id == video.id) it.copy(isHighlight = newStatus) else it }
                },
                onCreateHighlightReel = { filter ->
                    val highlights = visibleVideos.filter { it.isHighlight }
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
    } // end outer Column

    // Share flow: unified dialog (Team Parents + My Contacts in one screen)
    shareDialogUri?.let { savedUri ->
        Dialog(
            onDismissRequest = { shareDialogUri = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Share clip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // ── Team Parents ──────────────────────────────────────
                    Text(
                        "Team Parents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (rosterPlayers.isEmpty()) {
                        Text(
                            "No players on roster yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Collapsible player list header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSharePlayerList = !showSharePlayerList },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (shareSelectedPlayerIds.isEmpty()) "Choose players"
                                else "${shareSelectedPlayerIds.size} player${if (shareSelectedPlayerIds.size == 1) "" else "s"} selected",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                if (showSharePlayerList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (showSharePlayerList) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = {
                                shareSelectedPlayerIds = shareRosterPlayers
                                    .filter { it.addedBy.any(Char::isDigit) && it.addedBy.filter(Char::isDigit).length >= 10 }
                                    .map { it.id }
                                    .toSet()
                            }) {
                                Text("Select all")
                            }
                            if (shareSelectedPlayerIds.isNotEmpty()) {
                                TextButton(onClick = { shareSelectedPlayerIds = emptySet() }) {
                                    Text("Clear")
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            shareRosterPlayers.forEach { player ->
                                val hasParentContact = player.addedBy.any(Char::isDigit) &&
                                    player.addedBy.filter(Char::isDigit).length >= 10
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(if (hasParentContact) 1f else 0.4f)
                                        .clickable(enabled = hasParentContact) {
                                            shareSelectedPlayerIds = if (shareSelectedPlayerIds.contains(player.id)) {
                                                shareSelectedPlayerIds - player.id
                                            } else {
                                                shareSelectedPlayerIds + player.id
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (hasParentContact) {
                                        Checkbox(
                                            checked = shareSelectedPlayerIds.contains(player.id),
                                            onCheckedChange = { checked ->
                                                shareSelectedPlayerIds = if (checked) {
                                                    shareSelectedPlayerIds + player.id
                                                } else {
                                                    shareSelectedPlayerIds - player.id
                                                }
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(48.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "#${player.number} ${player.name}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                    }
                                }
                            }
                        }
                        } // end if showSharePlayerList
                        val selectedPlayers = shareRosterPlayers.filter { shareSelectedPlayerIds.contains(it.id) }
                        if (shareSelectedPlayerIds.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val recipients = buildTeamShareRecipients(selectedPlayers)
                                    if (recipients.isEmpty()) {
                                        launchPersonalShareChooser(context, savedUri, "Share clip")
                                    } else {
                                        shareVideoToTeamRecipients(
                                            context = context,
                                            videoUri = savedUri,
                                            recipients = recipients,
                                            players = selectedPlayers,
                                            highlightTag = null,
                                            customMessage = ""
                                        )
                                    }
                                    shareDialogUri = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Send to ${shareSelectedPlayerIds.size} parent${if (shareSelectedPlayerIds.size == 1) "" else "s"}"
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // ── My Contacts ──────────────────────────────────────
                    Text(
                        "My Contacts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { shareContactPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick contact from phone")
                    }

                    TextButton(
                        onClick = { shareDialogUri = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Old share overlay (non-share playback still uses selectedVideoUri/VideoPlaybackScreen below)
    selectedVideoUri?.let { videoUri ->
        if (selectedVideoStartInShareFlow) {
            // Kept for safety but should not be reached anymore
            selectedVideoStartInShareFlow = false
            selectedVideoUri = null
        }
    }

    } // end Box

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
                        isHighlight = isHighlight,
                        momentTag = customName
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
                        isHighlight = isHighlight,
                        momentTag = customName
                    )
                )
                seenIds.add(uriString)
            }
        }

        videos
    }
}

private suspend fun cleanupTeamClipsForClips(context: android.content.Context, teamName: String) {
    withContext(Dispatchers.IO) {
        val teamVideos = loadTeamVideosForClips(context, teamName)
        for (video in teamVideos) {
            try {
                deleteClipForClips(context, video)
            } catch (_: Exception) {
            }
        }
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

private fun buildClipOpponentLookup(
    context: android.content.Context,
    videos: List<VideoClip>
): Map<String, String?> {
    val opponentPrefs = context.getSharedPreferences("video_opponent_names", android.content.Context.MODE_PRIVATE)
    return videos.associate { video ->
        val fileUri = android.net.Uri.parse(video.filePath)
        val filePath = fileUri.path
        video.id to (
            opponentPrefs.getString(video.id, null)
                ?: opponentPrefs.getString(video.filePath, null)
                ?: filePath?.let { opponentPrefs.getString(it, null) }
        )?.trim().takeIf { !it.isNullOrBlank() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun buildClipGameSections(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    return videos
        .groupBy { video ->
            opponentLookup[video.id]?.takeIf(String::isNotBlank)?.lowercase() ?: NO_OPPONENT_FILTER
        }
        .map { (key, groupedVideos) ->
            val sample = groupedVideos.maxByOrNull { it.createdAt } ?: groupedVideos.first()
            val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)
            val displayTitle = opponent?.split(" ")?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) } ?: ""
            // Collect unique dates for subtitle
            val dates = groupedVideos.map { it.gameDate }.distinct().sorted()
            val dateLabel = dates.joinToString(", ") { formatClipFilterDate(it) }
            VideoLibrarySection(
                key = key,
                title = displayTitle,
                subtitle = dateLabel,
                videos = groupedVideos.sortedByDescending { it.createdAt }
            )
        }
        .sortedByDescending { section ->
            section.videos.maxOfOrNull { it.createdAt } ?: 0L
        }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun buildClipDateSubSections(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    val sample = videos.maxByOrNull { it.createdAt } ?: return emptyList()
    val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)

    // Keep unspecified as one section without date headers.
    if (opponent == null) {
        return listOf(
            VideoLibrarySection(
                key = NO_OPPONENT_FILTER,
                title = "",
                subtitle = "",
                videos = videos.sortedByDescending { it.createdAt }
            )
        )
    }

    val displayTitle = opponent.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    return videos
        .groupBy { it.gameDate }
        .map { (date, groupedVideos) ->
            VideoLibrarySection(
                key = "${opponent.lowercase()}|$date",
                title = displayTitle,
                subtitle = formatClipFilterDate(date),
                videos = groupedVideos.sortedByDescending { it.createdAt }
            )
        }
        .sortedByDescending { section ->
            section.videos.maxOfOrNull { it.createdAt } ?: 0L
        }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun buildClipListSectionsForAllGames(
    videos: List<VideoClip>,
    opponentLookup: Map<String, String?>
): List<VideoLibrarySection> {
    val groupedByOpponent = videos
        .groupBy { video ->
            opponentLookup[video.id]?.takeIf(String::isNotBlank)?.lowercase() ?: NO_OPPONENT_FILTER
        }
        .entries
        .sortedByDescending { (_, opponentVideos) ->
            opponentVideos.maxOfOrNull { it.createdAt } ?: 0L
        }

    return groupedByOpponent
        .flatMap { (opponentKey, opponentVideos) ->
            val sample = opponentVideos.maxByOrNull { it.createdAt } ?: opponentVideos.first()
            val opponent = opponentLookup[sample.id]?.takeIf(String::isNotBlank)

            if (opponent == null) {
                listOf(
                    VideoLibrarySection(
                        key = NO_OPPONENT_FILTER,
                        title = "",
                        subtitle = "",
                        videos = opponentVideos.sortedByDescending { it.createdAt }
                    )
                )
            } else {
                val displayTitle = opponent.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                opponentVideos
                    .groupBy { it.gameDate }
                    .map { (date, groupedVideos) ->
                        VideoLibrarySection(
                            key = "$opponentKey|$date",
                            title = displayTitle,
                            subtitle = formatClipFilterDate(date),
                            videos = groupedVideos.sortedByDescending { it.createdAt }
                        )
                    }
                    .sortedByDescending { section ->
                        section.videos.maxOfOrNull { it.createdAt } ?: 0L
                    }
            }
        }
}

private fun formatClipFilterDate(dateString: String): String {
    return try {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateString)
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(date ?: java.util.Date())
    } catch (_: Exception) {
        dateString
    }
}