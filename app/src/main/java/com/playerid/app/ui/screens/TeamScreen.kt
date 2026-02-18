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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
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
import com.playerid.app.roster.RosterCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    teamSnapRepository: TeamSnapRepository? = null,
    onNavigateToCrowdSourced: () -> Unit = {},
    onNavigateToWebImport: (String) -> Unit = {},
    onNavigateToAppImport: (String, Boolean) -> Unit = { _, _ -> },
    onNavigateToVideoLibrary: (String) -> Unit = { }
) {
    val selectedTeam by teamViewModel.selectedTeam.collectAsState()
    val isTeamSelected by teamViewModel.isTeamSelected.collectAsState()
    
    // Show team management if a team is selected, otherwise show team selection
    if (isTeamSelected && selectedTeam != null) {
        TeamManagementView(
            teamName = selectedTeam!!,
            playerViewModel = playerViewModel,
            teamViewModel = teamViewModel,
            onClearTeam = {
                teamViewModel.clearTeamSelection()
                playerViewModel.setSelectedTeam(null)
            },
            onNavigateToWebImport = onNavigateToWebImport,
            onNavigateToAppImport = onNavigateToAppImport,
            onNavigateToVideoLibrary = onNavigateToVideoLibrary
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
    onNavigateToVideoLibrary: (String) -> Unit = { }
) {
    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    val teamPlayers = remember(allPlayers, teamName) {
        allPlayers.filter { it.team == teamName }
    }
    var sortMode by remember { mutableStateOf(PlayerSortMode.NUMBER) }
    var positionFilter by remember { mutableStateOf<String?>(null) }
    var yearFilter by remember { mutableStateOf<String?>(null) }
    var positionExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    val positionOptions = remember(teamPlayers) {
        teamPlayers.map { it.position.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val yearOptionsAll = remember(teamPlayers) {
        teamPlayers.map { it.academicYear.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy(::yearSortKey).thenBy { it })
    }
    val yearOptionsForPosition = remember(teamPlayers, positionFilter) {
        if (positionFilter == null) {
            yearOptionsAll
        } else {
            teamPlayers
                .filter { it.position.equals(positionFilter, ignoreCase = true) }
                .map { it.academicYear.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sortedWith(compareBy(::yearSortKey).thenBy { it })
        }
    }
    val positionOptionsForYear = remember(teamPlayers, yearFilter) {
        if (yearFilter == null) {
            positionOptions
        } else {
            teamPlayers
                .filter { it.academicYear.equals(yearFilter, ignoreCase = true) }
                .map { it.position.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }
    val filteredPlayers = remember(teamPlayers, positionFilter, yearFilter) {
        teamPlayers.filter { player ->
            val positionMatch = positionFilter?.let { it.equals(player.position, ignoreCase = true) } ?: true
            val yearMatch = yearFilter?.let { it.equals(player.academicYear, ignoreCase = true) } ?: true
            positionMatch && yearMatch
        }
    }
    val displayPlayers = remember(filteredPlayers, sortMode) {
        when (sortMode) {
            PlayerSortMode.NUMBER -> filteredPlayers.sortedWith(
                compareBy<Player> { it.number.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.number }
            )
            PlayerSortMode.LAST_NAME -> filteredPlayers.sortedWith(
                compareBy<Player> { playerLastName(it.name).lowercase() }
                    .thenBy { it.name.lowercase() }
            )
        }
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
                            Text("From app capture")
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
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            IconButton(onClick = onClearTeam) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Leave team")
            }
        }

        LaunchedEffect(positionFilter, yearOptionsForPosition) {
            if (yearFilter != null && !yearOptionsForPosition.contains(yearFilter!!)) {
                yearFilter = null
            }
        }

        LaunchedEffect(yearFilter, positionOptionsForYear) {
            if (positionFilter != null && !positionOptionsForYear.contains(positionFilter!!)) {
                positionFilter = null
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

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showImportRosterOptions = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = "Import")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Roster")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onNavigateToVideoLibrary(teamName) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Videos")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Video Library")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { sortMode = PlayerSortMode.NUMBER },
                colors = if (sortMode == PlayerSortMode.NUMBER) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("Number")
            }
            OutlinedButton(
                onClick = { sortMode = PlayerSortMode.LAST_NAME },
                colors = if (sortMode == PlayerSortMode.LAST_NAME) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text("Last name")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = positionExpanded,
                onExpandedChange = { positionExpanded = !positionExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = positionFilter ?: "All positions",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Position") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = positionExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = positionExpanded,
                    onDismissRequest = { positionExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            positionFilter = null
                            positionExpanded = false
                        }
                    )
                    positionOptions.forEach { option ->
                        val isEnabled = positionOptionsForYear.contains(option)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option,
                                    color = if (isEnabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            },
                            onClick = {
                                if (isEnabled) {
                                    positionFilter = option
                                    positionExpanded = false
                                }
                            }
                        )
                    }
                }
            }

            if (yearOptionsAll.size > 1) {
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = !yearExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = yearFilter ?: "All years",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                yearFilter = null
                                yearExpanded = false
                            }
                        )
                        yearOptionsAll.forEach { option ->
                            val isEnabled = yearOptionsForPosition.contains(option)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        color = if (isEnabled) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                },
                                onClick = {
                                    if (isEnabled) {
                                        yearFilter = option
                                        yearExpanded = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
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
        } else if (displayPlayers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No matches",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No players match those filters",
                        style = MaterialTheme.typography.bodyLarge,
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
                playerToDelete?.let { playerViewModel.deletePlayer(it) }
                showDeletePlayerDialog = false
                playerToDelete = null
            }
        )
    }

    if (showOcrImportDialog && ocrImageUri != null) {
        RosterOcrImportDialog(
            teamName = teamName,
            imageUri = ocrImageUri!!,
            onDismiss = {
                showOcrImportDialog = false
                ocrImageUri = null
            },
            onImport = { candidates: List<RosterCandidate> ->
                playerViewModel.importRosterCandidates(
                    teamName = teamName,
                    candidates = candidates,
                    addedBy = teamViewModel.getCurrentUser()
                )
                showOcrImportDialog = false
                ocrImageUri = null
            }
        )
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