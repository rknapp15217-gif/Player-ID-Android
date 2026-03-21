package com.playerid.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.data.Player
import com.playerid.app.ui.dialogs.AddPlayerDialog
import com.playerid.app.ui.dialogs.AddTeamDialog
import com.playerid.app.ui.dialogs.DeleteTeamDialog
import com.playerid.app.ui.dialogs.DeletePlayerDialog
import com.playerid.app.ui.dialogs.EditPlayerDialog
import com.playerid.app.ui.dialogs.EditTeamColorsDialog
import com.playerid.app.ui.dialogs.RenameTeamDialog
import com.playerid.app.ui.components.*
import com.playerid.app.ui.theme.*
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel
import com.playerid.app.data.teamsnap.TeamSnapRepository
import com.playerid.app.roster.RosterCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    teamSnapRepository: TeamSnapRepository? = null,
    onNavigateToCrowdSourced: () -> Unit = {},
    onNavigateToWebImport: (String) -> Unit = {},
    onNavigateToAppImport: (String, Boolean) -> Unit = { _, _ -> },
    onNavigateToVideoLibrary: (String) -> Unit = { },
    onVideoSelected: (android.net.Uri, List<Player>) -> Unit = { _, _ -> },
    onVideoEdit: (android.net.Uri) -> Unit = { }
) {
    val selectedTeam by teamViewModel.selectedTeam.collectAsState()

    // Always land on team selection first; enter management only after explicit selection here.
    var showTeamSelection by rememberSaveable { mutableStateOf(true) }

    if (!showTeamSelection && selectedTeam != null) {
        TeamManagementView(
            teamName = selectedTeam!!,
            playerViewModel = playerViewModel,
            teamViewModel = teamViewModel,
            onClearTeam = {
                showTeamSelection = true
                teamViewModel.clearTeamSelection()
                playerViewModel.setSelectedTeam(null)
            },
            onNavigateToWebImport = onNavigateToWebImport,
            onNavigateToAppImport = onNavigateToAppImport,
            onNavigateToVideoLibrary = onNavigateToVideoLibrary,
            onVideoSelected = onVideoSelected,
            onVideoEdit = onVideoEdit
        )
    } else {
        TeamSelectionView(
            teamViewModel = teamViewModel,
            teamSnapRepository = teamSnapRepository,
            onTeamSelected = { teamName ->
                if (teamName == "__BROWSE_ALL_TEAMS__") {
                    onNavigateToCrowdSourced()
                } else {
                    showTeamSelection = false
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
    var showEditTeamColorsDialog by remember { mutableStateOf(false) }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = { showAddTeamDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create team", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Team")
            }
        }
        
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
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TeamColorDot(label = "Home", colorHex = team.color)
                                            TeamColorDot(label = "Away", colorHex = team.awayColor)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
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
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedTeamForAction = team.name
                                            showEditTeamColorsDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Colors", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedTeamForAction = team.name
                                            showRenameTeamDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rename", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedTeamForAction = team.name
                                            showDeleteTeamDialog = true
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Leave", style = MaterialTheme.typography.labelMedium)
                                    }
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
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddTeamDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Create team")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create New Team")
                                }
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
            onAdd = { teamName, sport, homeColor, awayColor, homeJerseyColor, awayJerseyColor ->
                teamViewModel.addTeam(
                    teamName = teamName,
                    sport = sport,
                    color = homeColor,
                    awayColor = awayColor,
                    homeJerseyColor = homeJerseyColor,
                    awayJerseyColor = awayJerseyColor
                )
                showAddTeamDialog = false
            }
        )
    }
    
    selectedTeamForAction?.let { teamName ->
        if (showEditTeamColorsDialog) {
            val teamToEdit = subscribedTeams.firstOrNull { it.name == teamName }
            if (teamToEdit != null) {
                EditTeamColorsDialog(
                    teamName = teamName,
                    initialHomeColor = teamToEdit.color,
                    initialAwayColor = teamToEdit.awayColor,
                    initialHomeJerseyColor = teamToEdit.homeJerseyColor,
                    initialAwayJerseyColor = teamToEdit.awayJerseyColor,
                    onDismiss = {
                        showEditTeamColorsDialog = false
                        selectedTeamForAction = null
                    },
                    onSave = { homeColor, awayColor, homeJerseyColor, awayJerseyColor ->
                        teamViewModel.updateTeamColors(
                            teamName = teamName,
                            color = homeColor,
                            awayColor = awayColor,
                            homeJerseyColor = homeJerseyColor,
                            awayJerseyColor = awayJerseyColor
                        )
                        showEditTeamColorsDialog = false
                        selectedTeamForAction = null
                    }
                )
            } else {
                showEditTeamColorsDialog = false
                selectedTeamForAction = null
            }
        }

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
                onTeamSelected(result.localTeamName)
                // TODO: Show success message with import stats
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementView(
    teamName: String,
    playerViewModel: PlayerViewModel,
    teamViewModel: TeamViewModel,
    onClearTeam: () -> Unit,
    onNavigateToWebImport: (String) -> Unit,
    onNavigateToAppImport: (String, Boolean) -> Unit,
    onNavigateToVideoLibrary: (String) -> Unit = { },
    onVideoSelected: (android.net.Uri, List<Player>) -> Unit = { _, _ -> },
    onVideoEdit: (android.net.Uri) -> Unit = { }
) {
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeamMeta = remember(subscribedTeams, teamName) {
        subscribedTeams.firstOrNull { it.name == teamName }
    }
    val homeColor = parseTeamColor(selectedTeamMeta?.color, fallback = Color(0xFF1976D2))
    val awayColor = parseTeamColor(selectedTeamMeta?.awayColor, fallback = Color(0xFFFFFFFF))
    val homeTextColor = if (homeColor.luminance() > 0.55f) Color.Black else Color.White

    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    val teamPlayers = remember(allPlayers, teamName) {
        allPlayers.filter { it.team == teamName }
    }
    val displayPlayers = remember(teamPlayers) {
        teamPlayers.sortedWith(
            compareBy<Player> { it.number.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.number }
        )
    }
    
    // Dialog states
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var showDeletePlayerDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showOcrImportDialog by remember { mutableStateOf(false) }
    var showImportRosterOptions by remember { mutableStateOf(false) }
    var ocrImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val rosterImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            ocrImageUri = uri
            showOcrImportDialog = true
        }
    }

    if (showImportRosterOptions) {
        AlertDialog(
            onDismissRequest = { showImportRosterOptions = false },
            title = { Text("Import roster") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Open your roster app (TeamSnap, GameChanger, etc.), take a screenshot, then import it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            showImportRosterOptions = false
                            rosterImagePicker.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Import screenshot")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("From screenshot")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            showImportRosterOptions = false
                            onNavigateToAppImport(teamName, true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = "Import app")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("From app")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            showImportRosterOptions = false
                            onNavigateToWebImport(teamName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Import website")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("From website")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportRosterOptions = false }) {
                    Text("Close")
                }
            }
        )
    }

    
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
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(listOf(homeColor, awayColor)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = teamName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = homeTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearTeam) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave team")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddPlayerDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = homeColor,
                contentColor = homeTextColor
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(12.dp))
            Text("Add Team Player", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showImportRosterOptions = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, awayColor)
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = "Import roster")
            Spacer(modifier = Modifier.width(10.dp))
            Text("Import Team Roster", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))
        
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
                items(displayPlayers) { player ->
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
                playerViewModel.deletePlayer(playerToDelete!!)
                showDeletePlayerDialog = false
                playerToDelete = null
            }
        )
    }

}

private suspend fun deleteClip(context: android.content.Context, video: com.playerid.app.data.VideoClip) {
    return withContext(Dispatchers.IO) {
        val videoUri = android.net.Uri.parse(video.filePath)
        val videoId = video.id
        
        try {
            // Try MediaStore deletion for content:// URIs
            if (videoUri.scheme == "content") {
                context.contentResolver.delete(videoUri, null, null)
            }
            
            // Also try file deletion for file:// URIs
            if (videoUri.scheme == "file") {
                val file = java.io.File(videoUri.path ?: "")
                file.delete()
            }
        } catch (e: Exception) {
            android.util.Log.w("deleteClip", "Error deleting file", e)
        }
        
        // Clean up stored metadata for this video
        val prefs = listOf(
            context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_highlights", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE)
        )
        prefs.forEach { pref ->
            pref.edit()
                .remove(videoId)
                .remove(video.filePath)
                .apply()
        }
    }
}

private suspend fun loadRecordedVideosForTeam(context: android.content.Context, teamName: String): List<com.playerid.app.data.VideoClip> {
    return withContext(Dispatchers.IO) {
        val videos = mutableListOf<com.playerid.app.data.VideoClip>()
        val teamPrefs = context.getSharedPreferences("video_team_names", android.content.Context.MODE_PRIVATE)
        val startPrefs = context.getSharedPreferences("video_start_times", android.content.Context.MODE_PRIVATE)
        val moviesDirs = context.getExternalFilesDirs(android.os.Environment.DIRECTORY_MOVIES)
            .filterNotNull()
        val logTag = "TeamHighlights"
        val seenIds = mutableSetOf<String>()

        if (moviesDirs.isEmpty()) {
            android.util.Log.d(logTag, "Movies dirs empty; cannot load team videos")
            return@withContext videos
        }

        try {
            var matchedCount = 0
            for (moviesDir in moviesDirs) {
                val videoFiles = moviesDir.listFiles { file ->
                    file.isFile && file.extension.equals("mp4", ignoreCase = true)
                } ?: emptyArray()
                android.util.Log.d(logTag, "Movies dir: ${moviesDir.absolutePath}; files: ${videoFiles.size}")

                for (file in videoFiles.sortedByDescending { it.lastModified() }) {
                    val videoPath = file.absolutePath
                    val fileUriString = android.net.Uri.fromFile(file).toString()
                    if (seenIds.contains(videoPath) || seenIds.contains(fileUriString)) {
                        continue
                    }
                    val storedTeamName = teamPrefs.getString(videoPath, null)
                        ?: teamPrefs.getString(fileUriString, null)
                    if (storedTeamName != teamName) {
                        continue
                    }

                    val storedStartTime = startPrefs.getLong(videoPath, 0L)
                        .takeIf { it > 0L }
                        ?: startPrefs.getLong(fileUriString, 0L).takeIf { it > 0L }
                    val createdAt = storedStartTime ?: file.lastModified()
                    val gameDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date(createdAt))

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
                        com.playerid.app.data.VideoClip(
                            id = videoPath,
                            filePath = android.net.Uri.fromFile(file).toString(),
                            duration = duration,
                            createdAt = createdAt,
                            gameDate = gameDate,
                            gameTitle = file.nameWithoutExtension,
                            isHighlight = false
                        )
                    )
                    seenIds.add(videoPath)
                    seenIds.add(fileUriString)
                    matchedCount += 1
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

                android.util.Log.d(logTag, "MediaStore Movies/PlayerID rows: ${cursor.count}")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val contentUri = android.content.ContentUris.withAppendedId(collection, id)
                    val uriString = contentUri.toString()
                    if (seenIds.contains(uriString)) {
                        continue
                    }

                    val storedTeamName = teamPrefs.getString(uriString, null)
                    if (storedTeamName != teamName) {
                        continue
                    }

                    val displayName = cursor.getString(nameIndex) ?: "clip_$id"
                    val dateTaken = cursor.getLong(dateTakenIndex)
                    val dateAdded = cursor.getLong(dateAddedIndex)
                    val storedStartTime = startPrefs.getLong(uriString, 0L)
                        .takeIf { it > 0L }
                    val createdAt = storedStartTime ?: if (dateTaken > 0) dateTaken else dateAdded * 1000
                    val gameDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date(createdAt))
                    val duration = cursor.getLong(durationIndex)

                    videos.add(
                        com.playerid.app.data.VideoClip(
                            id = uriString,
                            filePath = uriString,
                            duration = duration,
                            createdAt = createdAt,
                            gameDate = gameDate,
                            gameTitle = displayName.substringBeforeLast(".", displayName),
                            isHighlight = false
                        )
                    )
                    seenIds.add(uriString)
                    matchedCount += 1
                }
            }
            android.util.Log.d(logTag, "Matched team videos for '$teamName': $matchedCount")
        } catch (e: Exception) {
            android.util.Log.e("loadRecordedVideosForTeam", "Error loading videos for team $teamName", e)
        }

        videos
    }
}

private suspend fun cleanupTeamClips(context: android.content.Context, teamName: String): Int {
    return withContext(Dispatchers.IO) {
        val logTag = "TeamHighlights"
        val teamVideos = loadRecordedVideosForTeam(context, teamName)
        var deletedCount = 0

        for (video in teamVideos) {
            try {
                deleteClip(context, video)
                deletedCount += 1
            } catch (e: Exception) {
                android.util.Log.w(logTag, "Failed to delete clip ${video.filePath}", e)
            }
        }

        android.util.Log.d(logTag, "Deleted clips for team '$teamName': $deletedCount")
        deletedCount
    }
}

private enum class PlayerSortMode {
    NUMBER,
    LAST_NAME
}

private fun playerLastName(name: String): String {
    val cleaned = name.trim()
    if (cleaned.isEmpty()) return ""
    return if (cleaned.contains(",")) {
        cleaned.substringBefore(",").trim()
    } else {
        cleaned.substringAfterLast(" ", cleaned).trim()
    }
}

private fun yearSortKey(year: String): Int {
    return when (year.trim().lowercase()) {
        "freshman" -> 1
        "sophomore" -> 2
        "junior" -> 3
        "senior" -> 4
        else -> 99
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

@Composable
private fun TeamColorDot(label: String, colorHex: String?) {
    val color = parseTeamColor(colorHex, fallback = Color(0xFF9E9E9E))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseTeamColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}