package com.playerid.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.playerid.app.R
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
    cameraHandoffToken: Int = 0,
    teamSnapRepository: TeamSnapRepository? = null,
    onNavigateToCrowdSourced: () -> Unit = {},
    onNavigateToWebImport: (String) -> Unit = {},
    onNavigateToAppImport: (String, Boolean) -> Unit = { _, _ -> },
    onNavigateToScheduleImport: (String) -> Unit = {},
    onNavigateToVideoLibrary: (String) -> Unit = { },
    onVideoSelected: (android.net.Uri, List<Player>) -> Unit = { _, _ -> },
    onVideoEdit: (android.net.Uri) -> Unit = { }
) {
    TeamSelectionView(
        teamViewModel = teamViewModel,
        playerViewModel = playerViewModel,
        teamSnapRepository = teamSnapRepository,
        onNavigateToWebImport = onNavigateToWebImport,
        onNavigateToAppImport = onNavigateToAppImport,
        onNavigateToScheduleImport = onNavigateToScheduleImport,
        onNavigateToVideoLibrary = onNavigateToVideoLibrary,
        onVideoSelected = onVideoSelected,
        onVideoEdit = onVideoEdit
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamSelectionView(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    teamSnapRepository: TeamSnapRepository? = null,
    onNavigateToWebImport: (String) -> Unit,
    onNavigateToAppImport: (String, Boolean) -> Unit,
    onNavigateToScheduleImport: (String) -> Unit,
    onNavigateToVideoLibrary: (String) -> Unit,
    onVideoSelected: (android.net.Uri, List<Player>) -> Unit,
    onVideoEdit: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val subscribedTeamsWithStats by teamViewModel.subscribedTeamsWithStats.collectAsState()
    
    // Dialog states
    var showAddTeamDialog by remember { mutableStateOf(false) }
    var showJoinTeamDialog by remember { mutableStateOf(false) }
    var showTeamSnapImportDialog by remember { mutableStateOf(false) }
    var selectedTeamName by rememberSaveable { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            OutlinedButton(
                onClick = { showJoinTeamDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Group, contentDescription = stringResource(R.string.join_team), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.join_team))
            }
            OutlinedButton(
                onClick = { showAddTeamDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_team), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.create_team))
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
                contentPadding = PaddingValues(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subscribed teams list or empty state
                if (subscribedTeams.isNotEmpty()) {
                    items(subscribedTeams, key = { it.name }) { team ->
                        val teamStats = subscribedTeamsWithStats.find { it.name == team.name }
                        val isSelected = selectedTeamName == team.name
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedTeamName = if (isSelected) null else team.name
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        parseTeamColor(team.color, fallback = Color(0xFF1976D2)).copy(alpha = 0.18f)
                                    } else {
                                        parseTeamColor(team.color, fallback = Color(0xFF1976D2)).copy(alpha = 0.10f)
                                    }
                                )
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
                                                text = "${teamStats.playerCount} players",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected_team))
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.select_team))
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
                                    text = "Join an existing team or create your own new team",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddTeamDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_team))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.create_new_team))
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
                // TeamSnap import button (only show if repository is available)
                if (teamSnapRepository != null) {
                    OutlinedButton(
                        onClick = { showTeamSnapImportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.import_roster))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.import_from_teamsnap))
                    }
                }
            }
        }
    }

    selectedTeamName?.let { selectedTeam ->
        Dialog(onDismissRequest = { selectedTeamName = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            ) {
                TeamManagementView(
                    teamName = selectedTeam,
                    playerViewModel = playerViewModel,
                    teamViewModel = teamViewModel,
                    onClearTeam = { selectedTeamName = null },
                    onNavigateToWebImport = onNavigateToWebImport,
                    onNavigateToAppImport = onNavigateToAppImport,
                    onNavigateToScheduleImport = onNavigateToScheduleImport,
                    onNavigateToVideoLibrary = onNavigateToVideoLibrary,
                    onVideoSelected = onVideoSelected,
                    onVideoEdit = onVideoEdit,
                    embedded = false
                )
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
    
    // Join Team Dialog
    if (showJoinTeamDialog) {
        JoinTeamDialog(
            teamViewModel = teamViewModel,
            subscribedTeams = subscribedTeams,
            onDismiss = { showJoinTeamDialog = false }
        )
    }

    // TeamSnap Import Dialog
    if (showTeamSnapImportDialog && teamSnapRepository != null) {
        TeamSnapImportDialog(
            teamSnapRepository = teamSnapRepository,
            onDismiss = { showTeamSnapImportDialog = false },
            onImportComplete = { result ->
                showTeamSnapImportDialog = false
                // Auto-select imported team to show shared action dock.
                selectedTeamName = result.localTeamName
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.teamsnap_import_success,
                        result.importedCount,
                        result.skippedCount
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}

@Composable
fun JoinTeamDialog(
    teamViewModel: TeamViewModel,
    subscribedTeams: List<com.playerid.app.data.Team>,
    onDismiss: () -> Unit
) {
    val availableTeams by teamViewModel.availableTeams.collectAsState()
    val teamsWithStats by teamViewModel.teamsWithStats.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val subscribedNames = remember(subscribedTeams) { subscribedTeams.map { it.name }.toSet() }
    val filteredTeams = remember(availableTeams, searchQuery, subscribedNames) {
        availableTeams
            .filter { it.name !in subscribedNames }
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.join_a_team), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.close), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_teams)) },
                    leadingIcon = { Icon(Icons.Default.PeopleAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredTeams.isEmpty()) {
                    Text(
                        text = if (searchQuery.isEmpty()) stringResource(R.string.no_other_teams_available) else stringResource(R.string.no_teams_match, searchQuery),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filteredTeams, key = { it.name }) { team ->
                            val stats = teamsWithStats.find { it.name == team.name }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = parseTeamColor(team.color, fallback = Color(0xFF1976D2)).copy(alpha = 0.10f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(team.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        if (stats != null) {
                                            Text(
                                                "${stats.playerCount} players",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Button(onClick = {
                                        teamViewModel.subscribeToTeam(team.name)
                                        onDismiss()
                                    }) {
                                        Text(stringResource(R.string.join))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementView(
    teamName: String,
    playerViewModel: PlayerViewModel,
    teamViewModel: TeamViewModel,
    onClearTeam: () -> Unit,
    onNavigateToWebImport: (String) -> Unit,
    onNavigateToAppImport: (String, Boolean) -> Unit,
    onNavigateToScheduleImport: (String) -> Unit,
    onNavigateToVideoLibrary: (String) -> Unit = { },
    onVideoSelected: (android.net.Uri, List<Player>) -> Unit = { _, _ -> },
    onVideoEdit: (android.net.Uri) -> Unit = { },
    embedded: Boolean = false
) {
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeamMeta = remember(subscribedTeams, teamName) {
        subscribedTeams.firstOrNull { it.name == teamName }
    }
    val homeColor = parseTeamColor(selectedTeamMeta?.color, fallback = Color(0xFF1976D2))
    val awayColor = parseTeamColor(selectedTeamMeta?.awayColor, fallback = Color(0xFFFFFFFF))

    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    val teamPlayers = remember(allPlayers, teamName) {
        allPlayers.filter { it.team == teamName }
    }
    val kidOptions by teamViewModel.kidOptions.collectAsState()
    var assignedKid by remember(teamName) {
        mutableStateOf(teamViewModel.getAssignedKidForTeam(teamName) ?: "Tyson")
    }
    var kidExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(teamName, kidOptions) {
        assignedKid = teamViewModel.getAssignedKidForTeam(teamName)
            ?: kidOptions.firstOrNull()
            ?: "Tyson"
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
    var showRenameTeamDialog by remember { mutableStateOf(false) }
    var showEditTeamColorsDialog by remember { mutableStateOf(false) }
    var showLeaveTeamDialog by remember { mutableStateOf(false) }
    var showTeamActions by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showRoster by remember { mutableStateOf(false) }
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

    val primaryActionHeight = 52.dp
    val primaryActionSpacing = 10.dp

    if (showImportRosterOptions) {
        AlertDialog(
            onDismissRequest = { showImportRosterOptions = false },
            title = { Text(stringResource(R.string.import_roster)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.import_roster_help),
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
                            Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.from_screenshot))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.from_screenshot))
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
                            Icon(Icons.Default.PhoneAndroid, contentDescription = stringResource(R.string.from_app))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.from_app))
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
                            Icon(Icons.Default.Language, contentDescription = stringResource(R.string.from_website))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.from_website))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportRosterOptions = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    
    Column(
        modifier = Modifier
            .then(if (embedded) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().wrapContentHeight())
            .verticalScroll(rememberScrollState())
            .padding(if (embedded) 12.dp else 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!embedded) {
                    Text(
                        text = teamName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearTeam) {
                    Icon(
                        imageVector = if (embedded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (embedded) stringResource(R.string.collapse_team) else stringResource(R.string.back)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showImportRosterOptions = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(primaryActionHeight)
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.import_roster))
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.import_team_roster), fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(primaryActionSpacing))

        OutlinedButton(
            onClick = { onNavigateToScheduleImport(teamName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(primaryActionHeight)
        ) {
            Icon(Icons.Default.Language, contentDescription = "Upload schedule")
            Spacer(modifier = Modifier.width(10.dp))
            Text("Upload Schedule", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(primaryActionSpacing))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Assigned Kid",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = kidExpanded,
                    onExpandedChange = { kidExpanded = !kidExpanded }
                ) {
                    OutlinedTextField(
                        value = assignedKid,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Choose kid") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kidExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = kidExpanded,
                        onDismissRequest = { kidExpanded = false }
                    ) {
                        kidOptions.forEach { kidName ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(kidName) },
                                onClick = {
                                    assignedKid = kidName
                                    kidExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { teamViewModel.assignKidToTeam(teamName, assignedKid) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assign Kid To Team")
                }
            }
        }

        Spacer(modifier = Modifier.height(primaryActionSpacing))

        OutlinedButton(
            onClick = { showRoster = !showRoster },
            modifier = Modifier
                .fillMaxWidth()
                .height(primaryActionHeight)
        ) {
            Icon(Icons.Default.Group, contentDescription = stringResource(R.string.edit_roster))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (showRoster) stringResource(R.string.hide_roster, teamPlayers.size)
                else stringResource(R.string.show_roster, teamPlayers.size)
            )
        }

        Spacer(modifier = Modifier.height(primaryActionSpacing))

        // Team players list and add player (collapsed by default)
        if (showRoster) {
            Button(
                onClick = { showAddPlayerDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_player))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.add_player_to_team), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (teamPlayers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = stringResource(R.string.no_players),
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_players_in_team, teamName),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.add_players_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayPlayers.forEach { player ->
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

        Spacer(modifier = Modifier.height(if (showRoster) 16.dp else primaryActionSpacing))

        OutlinedButton(
            onClick = { showTeamActions = !showTeamActions },
            modifier = Modifier
                .fillMaxWidth()
                .height(primaryActionHeight)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit team settings")
            Spacer(modifier = Modifier.width(10.dp))
            Text(if (showTeamActions) "Hide Edit Team Settings" else "Edit Team Settings", fontWeight = FontWeight.Medium)
        }

        if (showTeamActions) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditTeamColorsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Colors")
                }
                OutlinedButton(
                    onClick = { showRenameTeamDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rename")
                }
            }
        }

        Spacer(modifier = Modifier.height(primaryActionSpacing))

        OutlinedButton(
            onClick = { showInviteDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(primaryActionHeight)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Invite to team")
            Spacer(modifier = Modifier.width(10.dp))
            Text("Invite to Team", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(primaryActionSpacing))

        OutlinedButton(
            onClick = { showLeaveTeamDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(primaryActionHeight),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave team")
            Spacer(modifier = Modifier.width(10.dp))
            Text("Leave Team", fontWeight = FontWeight.Medium)
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

    if (showEditTeamColorsDialog) {
        if (selectedTeamMeta != null) {
            EditTeamColorsDialog(
                teamName = teamName,
                initialHomeColor = selectedTeamMeta.color,
                initialAwayColor = selectedTeamMeta.awayColor,
                initialHomeJerseyColor = selectedTeamMeta.homeJerseyColor,
                initialAwayJerseyColor = selectedTeamMeta.awayJerseyColor,
                onDismiss = {
                    showEditTeamColorsDialog = false
                },
                onSave = { newHome, newAway, newHomeJersey, newAwayJersey ->
                    teamViewModel.updateTeamColors(
                        teamName = teamName,
                        color = newHome,
                        awayColor = newAway,
                        homeJerseyColor = newHomeJersey,
                        awayJerseyColor = newAwayJersey
                    )
                    showEditTeamColorsDialog = false
                }
            )
        } else {
            showEditTeamColorsDialog = false
        }
    }

    if (showRenameTeamDialog) {
        RenameTeamDialog(
            teamName = teamName,
            onDismiss = {
                showRenameTeamDialog = false
            },
            onRename = { newName ->
                teamViewModel.renameTeam(teamName, newName)
                showRenameTeamDialog = false
            }
        )
    }

    if (showLeaveTeamDialog) {
        DeleteTeamDialog(
            teamName = teamName,
            onDismiss = {
                showLeaveTeamDialog = false
            },
            onDelete = {
                teamViewModel.unsubscribeFromTeam(teamName)
                showLeaveTeamDialog = false
                onClearTeam()
            }
        )
    }

    if (showInviteDialog) {
        InviteTeamDialog(
            teamName = teamName,
            onDismiss = { showInviteDialog = false }
        )
    }

}

@Composable
fun InviteTeamDialog(
    teamName: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 0 = options menu, 1 = QR code, 2 = NFC tap
    var screen by remember { mutableStateOf(0) }

    val inviteLink = "spotr://join?team=${java.net.URLEncoder.encode(teamName, "UTF-8")}"

    val qrBitmap = remember(inviteLink) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(inviteLink, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            val w = bitMatrix.width; val h = bitMatrix.height
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until w) for (y in 0 until h) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
            bmp
        } catch (e: Exception) { null }
    }

    // Check NFC availability
    val nfcAdapter = remember {
        android.nfc.NfcAdapter.getDefaultAdapter(context)
    }
    val nfcAvailable = nfcAdapter != null
    val nfcEnabled = nfcAdapter?.isEnabled == true

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PersonAdd, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (screen == 0) "Invite to $teamName"
                            else if (screen == 1) "Scan QR Code"
                            else "Tap to Share via NFC",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Check, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (screen) {
                    0 -> {
                        // Send via Text
                        OutlinedButton(
                            onClick = {
                                val smsBody = "Join my team \"$teamName\" on Spotr! $inviteLink"
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("smsto:")
                                    putExtra("sms_body", smsBody)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Send via Text", fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Code
                        OutlinedButton(
                            onClick = { screen = 1 },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Show QR Code", fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // NFC / Nearby Share
                        OutlinedButton(
                            onClick = {
                                if (nfcAvailable && nfcEnabled) {
                                    screen = 2
                                } else if (nfcAvailable && !nfcEnabled) {
                                    // Open NFC settings so user can enable it
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                                } else {
                                    // No NFC — fall back to system share sheet (includes Nearby Share)
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, inviteLink)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Join $teamName on Spotr")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Invite to $teamName"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = true
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    if (nfcAvailable) "Tap Phones (NFC)" else "Nearby Share",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (nfcAvailable && !nfcEnabled) {
                                    Text(
                                        "NFC is off — tap to enable",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // QR code screen
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (qrBitmap != null) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    androidx.compose.foundation.Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR code to join $teamName",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                                    Text("Could not generate QR code", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Have the other parent scan this to join \"$teamName\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { screen = 0 }) { Text("Back") }
                        }
                    }

                    2 -> {
                        // NFC tap screen — write NDEF record when phone is tapped
                        val activity = context as? android.app.Activity
                        DisposableEffect(Unit) {
                            val pendingIntent = android.app.PendingIntent.getActivity(
                                context, 0,
                                android.content.Intent(context, context.javaClass).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                            )
                            try {
                                activity?.let {
                                    nfcAdapter?.enableForegroundDispatch(it, pendingIntent, null, null)
                                }
                            } catch (_: Exception) {}
                            onDispose {
                                try { activity?.let { nfcAdapter?.disableForegroundDispatch(it) } } catch (_: Exception) {}
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Hold phones back-to-back",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "The other parent will receive a link to join \"$teamName\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { screen = 0 }) { Text("Cancel") }
                        }
                    }
                }
            }
        }
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
            context.getSharedPreferences("video_custom_names", android.content.Context.MODE_PRIVATE),
            context.getSharedPreferences("video_kid_names", android.content.Context.MODE_PRIVATE)
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
            val selectionPaths = listOf(
                "Movies/PlayerID/",
                "Movies/PlayerID",
                "Movies/Spotr/",
                "Movies/Spotr"
            )
            val selection = selectionPaths.joinToString(" OR ") { "${android.provider.MediaStore.MediaColumns.RELATIVE_PATH}=?" }
            val selectionArgs = selectionPaths.toTypedArray()
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