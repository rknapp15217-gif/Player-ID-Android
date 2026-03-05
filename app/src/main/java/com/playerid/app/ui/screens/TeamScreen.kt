package com.playerid.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.data.Player
import com.playerid.app.ui.dialogs.AddPlayerDialog
import com.playerid.app.ui.dialogs.AddTeamDialog
import com.playerid.app.ui.dialogs.DeletePlayerDialog
import com.playerid.app.ui.dialogs.DeleteTeamDialog
import com.playerid.app.ui.dialogs.EditPlayerDialog
import com.playerid.app.ui.dialogs.RenameTeamDialog
import com.playerid.app.ui.components.*
import com.playerid.app.ui.theme.*
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel
import com.playerid.app.data.teamsnap.TeamSnapRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    teamSnapRepository: TeamSnapRepository? = null,
    onNavigateToCrowdSourced: () -> Unit = {}
) {
    val selectedTeam by teamViewModel.selectedTeam.collectAsState()
    val isTeamSelected by teamViewModel.isTeamSelected.collectAsState()
    
    // Show team management if a team is selected, otherwise show team selection
    if (isTeamSelected && selectedTeam != null) {
        TeamManagementView(
            teamName = selectedTeam!!,
            playerViewModel = playerViewModel,
            teamViewModel = teamViewModel,
            onClearTeam = { teamViewModel.clearTeamSelection() }
        )
    } else {
        TeamSelectionView(
            teamViewModel = teamViewModel,
            teamSnapRepository = teamSnapRepository,
            onTeamSelected = { teamName ->
                if (teamName == "__BROWSE_ALL_TEAMS__") {
                    onNavigateToCrowdSourced()
                } else {
                    teamViewModel.selectTeam(teamName)
                    playerViewModel.setSelectedTeam(teamName)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamSelectionView(
    teamViewModel: TeamViewModel,
    teamSnapRepository: TeamSnapRepository? = null,
    onTeamSelected: (String) -> Unit
) {
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val subscribedTeamsWithStats by teamViewModel.subscribedTeamsWithStats.collectAsState()
    
    // Dialog states for team management
    var showAddTeamDialog by remember { mutableStateOf(false) }
    var showRenameTeamDialog by remember { mutableStateOf(false) }
    var showDeleteTeamDialog by remember { mutableStateOf(false) }
    var selectedTeamForAction by remember { mutableStateOf<String?>(null) }
    var showManagementMode by remember { mutableStateOf(false) }
    var showTipsDialog by remember { mutableStateOf(false) }
    var showTeamSnapImportDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Modern header with gradient
        SpotrScreenHeader(
            title = "My Teams",
            subtitle = "Teams you've subscribed to for player management",
            icon = Icons.Default.Groups,
            gradient = listOf(SpotrGreen, SpotrTeal)
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Teams content area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subscribed teams list or empty state
                if (subscribedTeams.isNotEmpty()) {
                    items(subscribedTeams) { team ->
                        val teamStats = subscribedTeamsWithStats.find { it.name == team.name }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = team.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (teamStats != null) {
                                            Text(
                                                text = "${teamStats.playerCount} players • by ${teamStats.createdBy}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { onTeamSelected(team.name) },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Select")
                                    }
                                }
                            }
                            
                            // Show management buttons only when in management mode
                            if (showManagementMode) {
                                // Rename team button
                                IconButton(
                                    onClick = { 
                                        selectedTeamForAction = team.name
                                        showRenameTeamDialog = true 
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit, 
                                        contentDescription = "Rename team",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // Leave team button
                                IconButton(
                                    onClick = { 
                                        selectedTeamForAction = team.name
                                        showDeleteTeamDialog = true 
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ExitToApp, 
                                        contentDescription = "Leave team",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        // Empty state for no subscribed teams
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Teams Yet",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Browse all existing teams to find teams like North Allegheny, or create your own new team",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom actions section
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Management mode toggle (only show when there are teams)
                if (subscribedTeams.isNotEmpty()) {
                    Button(
                        onClick = { showManagementMode = !showManagementMode },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (showManagementMode) 
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        else 
                            ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            if (showManagementMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (showManagementMode) "Done managing" else "Manage teams"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showManagementMode) "Done" else "Manage")
                    }
                }
                
                // Create new team button
                OutlinedButton(
                    onClick = { showAddTeamDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Team")
                }
                
                // TeamSnap import button (only show if repository is available)
                if (teamSnapRepository != null) {
                    OutlinedButton(
                        onClick = { showTeamSnapImportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Import")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from TeamSnap")
                    }
                }
                
                // Bottom row with Browse button and Tips icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Browse existing teams button (less prominent)
                    OutlinedButton(
                        onClick = { onTeamSelected("__BROWSE_ALL_TEAMS__") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browse All Existing Teams")
                    }
                    
                    // Tips lightbulb icon
                    IconButton(
                        onClick = { showTipsDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "Show tips",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Team Management Dialogs
    if (showAddTeamDialog) {
        AddTeamDialog(
            onDismiss = { showAddTeamDialog = false },
            onAdd = { teamName ->
                teamViewModel.addTeam(teamName)
                showAddTeamDialog = false
            }
        )
    }
    
    selectedTeamForAction?.let { teamName ->
        if (showRenameTeamDialog) {
            RenameTeamDialog(
                teamName = teamName,
                onDismiss = { 
                    showRenameTeamDialog = false
                    selectedTeamForAction = null
                },
                onRename = { newName ->
                    teamViewModel.renameTeam(teamName, newName)
                    showRenameTeamDialog = false
                    selectedTeamForAction = null
                }
            )
        }
        
        if (showDeleteTeamDialog) {
            DeleteTeamDialog(
                teamName = teamName,
                onDismiss = { 
                    showDeleteTeamDialog = false
                    selectedTeamForAction = null
                },
                onDelete = {
                    teamViewModel.unsubscribeFromTeam(teamName)
                    showDeleteTeamDialog = false
                    selectedTeamForAction = null
                }
            )
        }
    }
    
    // Tips Dialog
    if (showTipsDialog) {
        AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tips")
                }
            },
            text = {
                Text(
                    text = "• Tap team name to select and manage players\n\n" +
                           "• Use 'Manage' button to rename teams or leave teams\n\n" +
                           "• 'Browse All Existing Teams' to find and join teams\n\n" +
                           "• 'Create New Team' adds a team that everyone can see\n\n" +
                           "• In camera view, tap a player to learn team colors automatically",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showTipsDialog = false }
                ) {
                    Text("Got it")
                }
            }
        )
    }
    
    // TeamSnap Import Dialog
    if (showTeamSnapImportDialog && teamSnapRepository != null) {
        TeamSnapImportDialog(
            teamSnapRepository = teamSnapRepository,
            onDismiss = { showTeamSnapImportDialog = false },
            onImportComplete = { result ->
                showTeamSnapImportDialog = false
                // Auto-select the imported team
                onTeamSelected(result.team.name)
                // TODO: Show success message with import stats
            }
        )
    }
}

@Composable
fun TeamManagementView(
    teamName: String,
    playerViewModel: PlayerViewModel,
    teamViewModel: TeamViewModel,
    onClearTeam: () -> Unit
) {
    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    val teamPlayers = remember(allPlayers, teamName) {
        allPlayers.filter { it.team == teamName }
    }
    
    // Dialog states
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var showDeletePlayerDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${teamPlayers.size} players",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onClearTeam) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Leave team")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add player button
        Button(
            onClick = { showAddPlayerDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Team Player")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Team players list
        if (teamPlayers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "No players",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No players in $teamName yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add players to start collaborating!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(teamPlayers) { player ->
                    TeamPlayerCard(
                        player = player,
                        onEdit = { editingPlayer = player },
                        onDelete = { 
                            playerToDelete = player
                            showDeletePlayerDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showAddPlayerDialog) {
        val availableTeams by teamViewModel.availableTeams.collectAsState()
        AddPlayerDialog(
            teamName = teamName,
            onDismiss = { showAddPlayerDialog = false },
            onAdd = { player ->
                playerViewModel.addPlayer(player, teamViewModel.getCurrentUser())
                showAddPlayerDialog = false
            },
            availableTeams = availableTeams.map { it.name },
            currentUser = teamViewModel.getCurrentUser()
        )
    }
    
    editingPlayer?.let { player ->
        val availableTeams by teamViewModel.availableTeams.collectAsState()
        EditPlayerDialog(
            player = player,
            hideTeamField = true, // Hide team field since we're in team context
            onDismiss = { editingPlayer = null },
            onSave = { updatedPlayer ->
                playerViewModel.updatePlayer(updatedPlayer)
                editingPlayer = null
            },
            availableTeams = availableTeams.map { it.name }
        )
    }
    
    // Delete player confirmation dialog
    if (showDeletePlayerDialog && playerToDelete != null) {
        DeletePlayerDialog(
            player = playerToDelete!!,
            onDismiss = { 
                showDeletePlayerDialog = false
                playerToDelete = null
            },
            onDelete = {
                playerToDelete?.let { playerViewModel.deletePlayer(it) }
                showDeletePlayerDialog = false
                playerToDelete = null
            }
        )
    }
}

@Composable
fun TeamPlayerCard(
    player: Player,
    onEdit: (Player) -> Unit,
    onDelete: (Player) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jersey number
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.number,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Player info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "${player.position} • ${player.academicYear}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons
            Row {
                IconButton(onClick = { onEdit(player) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = { onDelete(player) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}