package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.playerid.app.data.Player
import com.playerid.app.roster.RosterSharingScreen
import com.playerid.app.data.VideoClip
import com.playerid.app.viewmodels.VideoViewModel
import com.playerid.app.viewmodels.PlayerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTeamScreen(
    teamName: String,
    rosterId: String,
    videoClips: List<VideoClip>,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onInviteSent: () -> Unit
) {
    val roster by playerViewModel.filteredPlayers.collectAsState(initial = emptyList())
    val selectedTeam by playerViewModel.selectedTeam.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Roster", "Clips")
    val videoViewModel: VideoViewModel = viewModel()
    var showInviteDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Team") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // ...existing code...
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (selectedTab) {
                0 -> Column(modifier = Modifier.fillMaxHeight()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = { showInviteDialog = true }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Invite")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Invite to Team")
                        }
                    }
                    Text(
                        text = "Team Roster",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    if (roster.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No players on this team yet.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            items(roster) { player ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${player.number}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(40.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = player.position,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (showInviteDialog) {
                        AlertDialog(
                            onDismissRequest = { showInviteDialog = false },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { showInviteDialog = false }) { Text("Close") }
                            },
                            title = { Text("Invite to Team") },
                            text = {
                                RosterSharingScreen(
                                    teamName = teamName,
                                    rosterId = rosterId,
                                    onBack = { showInviteDialog = false },
                                    onInviteSent = onInviteSent
                                )
                            }
                        )
                    }
                }
                1 -> ClipsTabContent(videoClips, videoViewModel)
            }
        }
    }
}

@Composable
fun ClipsTabContent(videoClips: List<VideoClip>, videoViewModel: VideoViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (videoClips.isEmpty()) {
            Text("No clips available.", style = MaterialTheme.typography.bodyMedium)
        } else {
            videoClips.forEach { clip ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(clip.gameTitle, style = MaterialTheme.typography.titleMedium)
                        Text("Date: ${clip.gameDate}", style = MaterialTheme.typography.bodySmall)
                        Text("Duration: ${clip.duration / 1000}s", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { /* TODO: Open export/share screen */ }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Export / Share")
                        }
                    }
                }
            }
        }
    }
}
