package com.playerid.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import android.content.Intent
import android.graphics.BitmapFactory
import com.playerid.app.R
import com.playerid.app.data.Player
import com.playerid.app.data.GameSchedule
import com.playerid.app.data.repositories.toProfile
import com.playerid.app.domain.team.RosterListEvent
import com.playerid.app.domain.team.RosterListState
import com.playerid.app.domain.team.ScheduleLabelPolicy
import com.playerid.app.domain.team.ScheduleListEvent
import com.playerid.app.domain.team.ScheduleListState
import com.playerid.app.domain.team.scheduleGameItem
import com.playerid.app.domain.team.JoinTeamItem
import com.playerid.app.domain.team.TeamSelectionDialog
import com.playerid.app.domain.team.TeamSelectionEvent
import com.playerid.app.domain.team.TeamSelectionItem
import com.playerid.app.domain.team.TeamSelectionState
import com.playerid.app.domain.team.TeamImportSource
import com.playerid.app.domain.team.TeamManagementDialog
import com.playerid.app.domain.team.TeamManagementEvent
import com.playerid.app.domain.team.TeamManagementState
import com.playerid.app.domain.team.initialTeamSelectionState
import com.playerid.app.domain.team.TeamDetailNavigationEvent
import com.playerid.app.domain.team.TeamDetailPage
import com.playerid.app.domain.team.initialTeamDetailPage
import com.playerid.app.domain.team.reduce
import com.playerid.app.ui.dialogs.AddPlayerDialog
import com.playerid.app.ui.dialogs.AddTeamDialog
import com.playerid.app.ui.dialogs.DeleteTeamDialog
import com.playerid.app.ui.dialogs.DeletePlayerDialog
import com.playerid.app.ui.dialogs.EditPlayerDialog
import com.playerid.app.ui.dialogs.EditTeamSettingsDialog
import com.playerid.app.ui.components.*
import com.playerid.app.ui.roster.RosterPage
import com.playerid.app.ui.team.SchedulePage
import com.playerid.app.ui.team.JoinTeamDialog as SharedJoinTeamDialog
import com.playerid.app.ui.team.InviteTeamDialog as SharedInviteTeamDialog
import com.playerid.app.ui.team.TeamOverviewDestination
import com.playerid.app.ui.team.TeamOverviewPage
import com.playerid.app.ui.team.TeamSelectionPage
import com.playerid.app.ui.team.TeamImportOptionsDialog
import com.playerid.app.ui.theme.*
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel
import com.playerid.app.data.teamsnap.TeamSnapRepository
import com.playerid.app.roster.RosterCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    initialTeamName: String? = null,
    openRosterInitially: Boolean = false,
    startCreateTeamInitially: Boolean = false,
    openRosterAfterCreate: Boolean = false,
    teamSnapRepository: TeamSnapRepository? = null,
    onNavigateToWebImport: (String) -> Unit = {},
    onNavigateToAppImport: (String, Boolean) -> Unit = { _, _ -> },
    onNavigateToScheduleImport: (String, String) -> Unit = { _, _ -> }
) {
    TeamSelectionView(
        teamViewModel = teamViewModel,
        playerViewModel = playerViewModel,
        initialTeamName = initialTeamName,
        openRosterInitially = openRosterInitially,
        startCreateTeamInitially = startCreateTeamInitially,
        openRosterAfterCreate = openRosterAfterCreate,
        teamSnapRepository = teamSnapRepository,
        onNavigateToWebImport = onNavigateToWebImport,
        onNavigateToAppImport = onNavigateToAppImport,
        onNavigateToScheduleImport = onNavigateToScheduleImport
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamSelectionView(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    initialTeamName: String? = null,
    openRosterInitially: Boolean = false,
    startCreateTeamInitially: Boolean = false,
    openRosterAfterCreate: Boolean = false,
    teamSnapRepository: TeamSnapRepository? = null,
    onNavigateToWebImport: (String) -> Unit,
    onNavigateToAppImport: (String, Boolean) -> Unit,
    onNavigateToScheduleImport: (String, String) -> Unit
) {
    val context = LocalContext.current
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val subscribedTeamsWithStats by teamViewModel.subscribedTeamsWithStats.collectAsState()
    val playerCounts = remember(subscribedTeamsWithStats) {
        subscribedTeamsWithStats.associate { it.name to it.playerCount }
    }
    val selectionItems = remember(subscribedTeams, playerCounts) {
        subscribedTeams.map { team ->
            TeamSelectionItem(
                name = team.name,
                homeColorHex = team.color,
                awayColorHex = team.awayColor,
                playerCount = playerCounts[team.name]
            )
        }
    }
    
    var selectionState by rememberSaveable(
        initialTeamName,
        startCreateTeamInitially,
        stateSaver = Saver(
            save = { state ->
                listOf(
                    state.selectedTeamName.orEmpty(),
                    state.createdTeamName.orEmpty(),
                    state.activeDialog?.name.orEmpty()
                )
            },
            restore = { saved ->
                TeamSelectionState(
                    selectedTeamName = saved[0].ifBlank { null },
                    createdTeamName = saved[1].ifBlank { null },
                    activeDialog = saved[2].ifBlank { null }?.let(TeamSelectionDialog::valueOf)
                )
            }
        )
    ) {
        mutableStateOf(
            initialTeamSelectionState(
                initialTeamName = initialTeamName,
                startCreateTeamInitially = startCreateTeamInitially
            )
        )
    }
    
    if (selectionState.selectedTeamName != null) {
        TeamManagementView(
            teamName = selectionState.selectedTeamName!!,
            playerViewModel = playerViewModel,
            teamViewModel = teamViewModel,
            onClearTeam = {
                selectionState = selectionState.reduce(TeamSelectionEvent.TeamCleared)
            },
            onNavigateToWebImport = onNavigateToWebImport,
            onNavigateToAppImport = onNavigateToAppImport,
            onNavigateToScheduleImport = onNavigateToScheduleImport,
            openRosterInitially = selectionState.shouldOpenRoster(openRosterInitially)
        )
    } else {
        TeamSelectionPage(
            teams = selectionItems,
            teamSnapImportAvailable = teamSnapRepository != null,
            onSelectTeam = { teamName ->
                selectionState = selectionState.reduce(TeamSelectionEvent.TeamSelected(teamName))
            },
            onJoinTeam = {
                selectionState = selectionState.reduce(
                    TeamSelectionEvent.DialogRequested(TeamSelectionDialog.JoinTeam)
                )
            },
            onCreateTeam = {
                selectionState = selectionState.reduce(
                    TeamSelectionEvent.DialogRequested(TeamSelectionDialog.CreateTeam)
                )
            },
            onImportTeamSnap = {
                selectionState = selectionState.reduce(
                    TeamSelectionEvent.DialogRequested(TeamSelectionDialog.TeamSnapImport)
                )
            },
            joinIcon = { Icon(Icons.Default.Group, contentDescription = null) },
            createIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            selectIcon = {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.select_team))
            },
            emptyIcon = {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            importIcon = {
                Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.import_roster))
            },
            joinTeamLabel = stringResource(R.string.join_team),
            createTeamLabel = stringResource(R.string.create_team),
            createNewTeamLabel = stringResource(R.string.create_new_team),
            importTeamSnapLabel = stringResource(R.string.import_from_teamsnap)
        )
    }

    // Team Management Dialogs
    if (selectionState.activeDialog == TeamSelectionDialog.CreateTeam) {
        AddTeamDialog(
            onDismiss = {
                selectionState = selectionState.reduce(TeamSelectionEvent.DialogDismissed)
            },
            onAdd = { teamName, sport, homeColor, awayColor, homeJerseyColor, awayJerseyColor ->
                teamViewModel.addTeam(
                    teamName = teamName,
                    sport = sport,
                    color = homeColor,
                    awayColor = awayColor,
                    homeJerseyColor = homeJerseyColor,
                    awayJerseyColor = awayJerseyColor
                )
                selectionState = selectionState.reduce(
                    TeamSelectionEvent.TeamCreated(teamName, openRosterAfterCreate)
                )
                if (openRosterAfterCreate) {
                    teamViewModel.replaceSubscriptionsWithTeam(teamName)
                    playerViewModel.setSelectedTeam(teamName)
                }
            }
        )
    }
    
    // Join Team Dialog
    if (selectionState.activeDialog == TeamSelectionDialog.JoinTeam) {
        JoinTeamDialog(
            teamViewModel = teamViewModel,
            subscribedTeams = subscribedTeams,
            onDismiss = {
                selectionState = selectionState.reduce(TeamSelectionEvent.DialogDismissed)
            }
        )
    }

    // TeamSnap Import Dialog
    if (selectionState.activeDialog == TeamSelectionDialog.TeamSnapImport && teamSnapRepository != null) {
        TeamSnapImportDialog(
            teamSnapRepository = teamSnapRepository,
            onDismiss = {
                selectionState = selectionState.reduce(TeamSelectionEvent.DialogDismissed)
            },
            onImportComplete = { result ->
                selectionState = selectionState.reduce(
                    TeamSelectionEvent.TeamImported(result.localTeamName)
                )
                Toast.makeText(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.teamsnap_import_success,
                        result.importedCount,
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
    val context = LocalContext.current
    val availableTeams by teamViewModel.availableTeams.collectAsState()
    val teamsWithStats by teamViewModel.teamsWithStats.collectAsState()
    val playerCounts = remember(teamsWithStats) { teamsWithStats.associate { it.name to it.playerCount } }
    val joinTeamItems = remember(availableTeams, playerCounts) {
        availableTeams.map { team ->
            JoinTeamItem(
                name = team.name,
                colorHex = team.color,
                playerCount = playerCounts[team.name]
            )
        }
    }
    SharedJoinTeamDialog(
        teams = joinTeamItems,
        subscribedTeamNames = subscribedTeams.map { it.name }.toSet(),
        onDismiss = onDismiss,
        onJoin = { teamName ->
            teamViewModel.subscribeToTeam(teamName)
            onDismiss()
        },
        closeIcon = {
            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.close), modifier = Modifier.size(18.dp))
        },
        searchIcon = {
            Icon(Icons.Default.PeopleAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        title = stringResource(R.string.join_a_team),
        searchPlaceholder = stringResource(R.string.search_teams),
        noOtherTeamsText = stringResource(R.string.no_other_teams_available),
        noTeamsMatchText = { query -> context.getString(R.string.no_teams_match, query) },
        joinLabel = stringResource(R.string.join)
    )
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
    onNavigateToScheduleImport: (String, String) -> Unit,
    openRosterInitially: Boolean = false
) {
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val selectedTeamMeta = remember(subscribedTeams, teamName) {
        subscribedTeams.firstOrNull { it.name == teamName }
    }
    val homeColor = parseTeamColor(selectedTeamMeta?.color, fallback = Color(0xFF1976D2))

    val teamPlayers by remember(teamName) { playerViewModel.observeTeamRoster(teamName) }
        .collectAsState(initial = emptyList())
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
    val displayPlayersById = remember(displayPlayers) { displayPlayers.associateBy { it.id } }
    val context = LocalContext.current
    val playerPhotoPrefs = remember { context.getSharedPreferences("player_photos", android.content.Context.MODE_PRIVATE) }
    var playerPhotoUris by remember(teamName, displayPlayers) {
        mutableStateOf(displayPlayers.mapNotNull { player ->
            playerPhotoPrefs.getString(player.id, null)?.let { player.id to it }
        }.toMap())
    }
    
    var managementState by remember(teamName) { mutableStateOf(TeamManagementState()) }
    var showDeletePlayerDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showOcrImportDialog by remember { mutableStateOf(false) }
    var detailPage by rememberSaveable(teamName, openRosterInitially) {
        mutableStateOf(initialTeamDetailPage(openRosterInitially))
    }
    var rosterSearch by rememberSaveable(teamName) { mutableStateOf("") }
    var scheduleSearch by rememberSaveable(teamName) { mutableStateOf("") }
    var favoritePlayerIds by rememberSaveable(teamName) { mutableStateOf(setOf<String>()) }
    val rosterListState = remember(teamName, displayPlayers, rosterSearch, favoritePlayerIds) {
        RosterListState(
            teamName = teamName,
            players = displayPlayers.map { it.toProfile() },
            searchQuery = rosterSearch,
            favoritePlayerIds = favoritePlayerIds
        )
    }
    val teamGames by remember(teamName) { teamViewModel.getGamesForTeam(teamName) }
        .collectAsState(initial = emptyList())
    var ocrImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val rosterImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            ocrImageUri = uri
            showOcrImportDialog = true
        }
    }

    if (managementState.activeDialog == TeamManagementDialog.ImportRoster) {
        TeamImportOptionsDialog(
            title = stringResource(R.string.import_roster),
            helpText = stringResource(R.string.import_roster_help),
            sourceLabel = { source -> importSourceLabel(source) },
            sourceIcon = { source -> ImportSourceIcon(source) },
            onSourceSelected = { source ->
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
                when (source) {
                    TeamImportSource.Screenshot -> rosterImagePicker.launch("image/*")
                    TeamImportSource.App -> onNavigateToAppImport(teamName, true)
                    TeamImportSource.Website -> onNavigateToWebImport(teamName)
                }
            },
            onDismiss = {
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
            },
            closeLabel = stringResource(R.string.close)
        )
    }

    if (managementState.activeDialog == TeamManagementDialog.ImportSchedule) {
        TeamImportOptionsDialog(
            title = "Import schedule",
            helpText = "Choose where to import the team schedule from.",
            sourceLabel = { source -> importSourceLabel(source) },
            sourceIcon = { source -> ImportSourceIcon(source) },
            onSourceSelected = { source ->
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
                onNavigateToScheduleImport(teamName, source.routeKey)
            },
            onDismiss = {
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
            },
            closeLabel = stringResource(R.string.close)
        )
    }

    when (detailPage) {
        TeamDetailPage.Overview -> TeamOverviewPage(
            teamName = teamName,
            seasonLabel = selectedTeamMeta?.description?.takeIf { it.isNotBlank() } ?: selectedTeamMeta?.sport.orEmpty(),
            homeColor = homeColor,
            assignedKid = assignedKid,
            assignedPlayer = teamPlayers
                .firstOrNull { it.name.contains(assignedKid, ignoreCase = true) }
                ?.toProfile(),
            playerCount = teamPlayers.size,
            gameCount = teamGames.size,
            onBack = onClearTeam,
            onAssignedKidClick = { kidExpanded = true },
            kidPicker = {
                ExposedDropdownMenuBox(expanded = kidExpanded, onExpandedChange = { kidExpanded = it }) {
                    Box(modifier = Modifier.size(1.dp).menuAnchor())
                    androidx.compose.material3.DropdownMenu(expanded = kidExpanded, onDismissRequest = { kidExpanded = false }) {
                        kidOptions.forEach { kidName ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text(kidName) }, onClick = {
                                assignedKid = kidName
                                teamViewModel.assignKidToTeam(teamName, kidName)
                                kidExpanded = false
                            })
                        }
                    }
                }
            },
            onRoster = { detailPage = detailPage.reduce(TeamDetailNavigationEvent.RosterSelected) },
            onImportRoster = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.ImportRoster)
                )
            },
            onSchedule = { detailPage = detailPage.reduce(TeamDetailNavigationEvent.ScheduleSelected) },
            onImportSchedule = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.ImportSchedule)
                )
            },
            onInvite = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.InviteTeam)
                )
            },
            onSettings = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.TeamSettings)
                )
            },
            onLeave = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.LeaveTeam)
                )
            },
            backIcon = {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            },
            assignedPlayerTrailingIcon = {
                Icon(Icons.Default.ChevronRight, contentDescription = "Change assigned player")
            },
            destinationIcon = { destination ->
                val icon = when (destination) {
                    TeamOverviewDestination.Roster -> Icons.Default.Groups
                    TeamOverviewDestination.Schedule -> Icons.Default.CalendarMonth
                    TeamOverviewDestination.Invite -> Icons.Default.Share
                    TeamOverviewDestination.Settings -> Icons.Default.Settings
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            destinationTrailingIcon = { destination ->
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open ${destination.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leaveIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
        TeamDetailPage.Roster -> TeamRosterPage(
            players = rosterListState.visiblePlayers.mapNotNull { visiblePlayer ->
                displayPlayersById[visiblePlayer.id]
            },
            totalCount = teamPlayers.size,
            search = rosterListState.searchQuery,
            favoritePlayerIds = rosterListState.favoritePlayerIds,
            onSearchChange = { query ->
                rosterSearch = rosterListState
                    .reduce(RosterListEvent.SearchQueryChanged(query))
                    .searchQuery
            },
            onBack = { detailPage = detailPage.reduce(TeamDetailNavigationEvent.OverviewSelected) },
            onAdd = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.AddPlayer)
                )
            },
            onImport = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.ImportRoster)
                )
            },
            playerPhotoUris = playerPhotoUris,
            onPhotoSelected = { player, uri ->
                playerPhotoPrefs.edit().putString(player.id, uri.toString()).apply()
                playerPhotoUris = playerPhotoUris + (player.id to uri.toString())
            },
            onEdit = { player ->
                managementState = managementState.reduce(
                    TeamManagementEvent.EditPlayerRequested(player.id)
                )
            },
            onToggleFavorite = { player ->
                favoritePlayerIds = rosterListState
                    .reduce(RosterListEvent.FavoriteToggled(player.id))
                    .favoritePlayerIds
            }
        )
        TeamDetailPage.Schedule -> TeamSchedulePage(
            games = teamGames,
            totalCount = teamGames.size,
            search = scheduleSearch,
            onSearchChange = { scheduleSearch = it },
            onBack = { detailPage = detailPage.reduce(TeamDetailNavigationEvent.OverviewSelected) },
            onAdd = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.ImportSchedule)
                )
            },
            onImport = {
                managementState = managementState.reduce(
                    TeamManagementEvent.DialogRequested(TeamManagementDialog.ImportSchedule)
                )
            }
        )
    }

    if (managementState.activeDialog == TeamManagementDialog.TeamSettings) {
        selectedTeamMeta?.let { team ->
            EditTeamSettingsDialog(
                teamName = teamName,
                initialHomeColor = team.color,
                initialAwayColor = team.awayColor,
                initialHomeJerseyColor = team.homeJerseyColor,
                initialAwayJerseyColor = team.awayJerseyColor,
                onDismiss = {
                    managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
                },
                onSave = { newName, newHome, newAway, newHomeJersey, newAwayJersey ->
                    teamViewModel.updateTeamSettings(
                        currentName = teamName,
                        newName = newName,
                        color = newHome,
                        awayColor = newAway,
                        homeJerseyColor = newHomeJersey,
                        awayJerseyColor = newAwayJersey
                    )
                    managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
                }
            )
        }
    }

    // Dialogs
    if (managementState.activeDialog == TeamManagementDialog.AddPlayer) {
        val availableTeams by teamViewModel.availableTeams.collectAsState()
        AddPlayerDialog(
            teamName = teamName,
            onDismiss = {
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
            },
            onAdd = { player ->
                playerViewModel.addPlayer(player, teamViewModel.getCurrentUser())
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
            },
            availableTeams = availableTeams.map { it.name },
            currentUser = teamViewModel.getCurrentUser()
        )
    }
    
    managementState.editingPlayerId?.let(displayPlayersById::get)?.let { player ->
        val availableTeams by teamViewModel.availableTeams.collectAsState()
        EditPlayerDialog(
            player = player,
            hideTeamField = true, // Hide team field since we're in team context
            onDismiss = {
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
            },
            onSave = { updatedPlayer ->
                playerViewModel.updatePlayer(updatedPlayer)
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
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

    if (managementState.activeDialog == TeamManagementDialog.LeaveTeam) {
        DeleteTeamDialog(
            teamName = teamName,
            onDismiss = {
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
            },
            onDelete = {
                teamViewModel.unsubscribeFromTeam(teamName)
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
                onClearTeam()
            }
        )
    }

    if (managementState.activeDialog == TeamManagementDialog.InviteTeam) {
        InviteTeamDialog(
            teamName = teamName,
            onDismiss = {
                managementState = managementState.reduce(TeamManagementEvent.DialogDismissed)
            }
        )
    }

}

@Composable
private fun TeamRosterPage(
    players: List<Player>,
    totalCount: Int,
    search: String,
    favoritePlayerIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    playerPhotoUris: Map<String, String>,
    onPhotoSelected: (Player, android.net.Uri) -> Unit,
    onEdit: (Player) -> Unit,
    onToggleFavorite: (Player) -> Unit
) {
    val context = LocalContext.current
    var photoPlayer by remember { mutableStateOf<Player?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val player = photoPlayer
        if (uri != null && player != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onPhotoSelected(player, uri)
        }
        photoPlayer = null
    }
    val playersById = remember(players) { players.associateBy { it.id } }
    RosterPage(
        state = RosterListState(
            teamName = players.firstOrNull()?.team.orEmpty(),
            players = players.map { it.toProfile() },
            searchQuery = search,
            favoritePlayerIds = favoritePlayerIds
        ),
        totalCount = totalCount,
        onSearchChange = onSearchChange,
        onBack = onBack,
        onAdd = onAdd,
        onImport = onImport,
        onPlayerClick = { profile -> playersById[profile.id]?.let(onEdit) },
        backIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
        addIcon = { Icon(Icons.Default.Add, contentDescription = "Add") },
        searchIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        importIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TeamActionBlue) },
        addPlayerIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
        playerLeadingContent = { profile ->
            playersById[profile.id]?.let { player ->
                PlayerPhotoAvatar(
                    photoUri = playerPhotoUris[player.id],
                    contentDescription = "Upload photo for ${player.name}",
                    onClick = {
                        photoPlayer = player
                        photoPicker.launch(arrayOf("image/*"))
                    }
                )
            }
        },
        playerTrailingContent = { profile ->
            playersById[profile.id]?.let { player ->
                    IconButton(onClick = { onToggleFavorite(player) }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite player",
                            tint = if (player.id in favoritePlayerIds) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                        )
                    }
            }
        }
    )
}

@Composable
private fun TeamSchedulePage(
    games: List<GameSchedule>,
    totalCount: Int,
    search: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit
) {
    val labelPolicy = remember {
        val locale = Locale.getDefault()
        val symbols = DateFormatSymbols.getInstance(locale)
        ScheduleLabelPolicy(
            monthLabels = symbols.shortMonths.take(12).map { it.uppercase(locale) },
            amLabel = symbols.amPmStrings[0],
            pmLabel = symbols.amPmStrings[1]
        )
    }
    val scheduleItems = remember(games, labelPolicy) {
        val timeZone = java.util.TimeZone.getDefault()
        games.map { game ->
            scheduleGameItem(
                id = game.id,
                opponentName = game.opponentName,
                gameLabel = game.gameLabel,
                scheduledStartMs = game.scheduledStartMs,
                locationName = game.locationName,
                labelPolicy = labelPolicy,
                utcOffsetMinutes = timeZone.getOffset(game.scheduledStartMs) / 60_000
            )
        }
    }
    val scheduleState = ScheduleListState(games = scheduleItems, searchQuery = search)
    SchedulePage(
        state = scheduleState,
        totalCount = totalCount,
        nowMs = System.currentTimeMillis(),
        onSearchChange = { query ->
            onSearchChange(
                scheduleState.reduce(ScheduleListEvent.SearchQueryChanged(query)).searchQuery
            )
        },
        onBack = onBack,
        onAdd = onAdd,
        onImport = onImport,
        backIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
        addIcon = { Icon(Icons.Default.Add, contentDescription = "Add") },
        searchIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        importIcon = {
            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TeamActionBlue)
        },
        gameTrailingIcon = {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Game options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun InviteTeamDialog(
    teamName: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    SharedInviteTeamDialog(
        teamName = teamName,
        nfcAvailable = nfcAvailable,
        nfcEnabled = nfcEnabled,
        onDismiss = onDismiss,
        onSendText = {
            val smsBody = "Join my team \"$teamName\" on Spotr! $inviteLink"
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:")
                putExtra("sms_body", smsBody)
            }
            context.startActivity(intent)
        },
        onOpenNfcSettings = {
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
        },
        onShareNearby = {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, inviteLink)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Join $teamName on Spotr")
            }
            context.startActivity(
                android.content.Intent.createChooser(shareIntent, "Invite to $teamName")
            )
        },
        headerIcon = {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        },
        closeIcon = {
            Icon(Icons.Default.Check, contentDescription = "Close", modifier = Modifier.size(18.dp))
        },
        textIcon = {
            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
        },
        qrIcon = {
            Icon(Icons.Default.QrCode, contentDescription = null)
        },
        proximityIcon = {
            Icon(Icons.Default.Wifi, contentDescription = null)
        },
        nfcHeroIcon = {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        qrCodeContent = {
            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
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
        },
        nfcSession = {
            val activity = context as? android.app.Activity
            DisposableEffect(Unit) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    0,
                    android.content.Intent(context, context.javaClass)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                try {
                    activity?.let {
                        nfcAdapter?.enableForegroundDispatch(it, pendingIntent, null, null)
                    }
                } catch (_: Exception) {}
                onDispose {
                    try {
                        activity?.let { nfcAdapter?.disableForegroundDispatch(it) }
                    } catch (_: Exception) {}
                }
            }
        }
    )
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

private fun parseTeamColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
private fun importSourceLabel(source: TeamImportSource): String = when (source) {
    TeamImportSource.Screenshot -> stringResource(R.string.from_screenshot)
    TeamImportSource.App -> stringResource(R.string.from_app)
    TeamImportSource.Website -> stringResource(R.string.from_website)
}

@Composable
private fun ImportSourceIcon(source: TeamImportSource) {
    val icon = when (source) {
        TeamImportSource.Screenshot -> Icons.Default.CloudDownload
        TeamImportSource.App -> Icons.Default.PhoneAndroid
        TeamImportSource.Website -> Icons.Default.Language
    }
    Icon(icon, contentDescription = importSourceLabel(source))
}

private val TeamActionBlue = Color(0xFF0A66FF)

@Composable
private fun PlayerPhotoAvatar(
    photoUri: String?,
    contentDescription: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(photoUri) {
        bitmap = if (photoUri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val input = context.contentResolver.openInputStream(android.net.Uri.parse(photoUri))
                    ?: return@runCatching null
                input.use { stream -> BitmapFactory.decodeStream(stream) }
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}