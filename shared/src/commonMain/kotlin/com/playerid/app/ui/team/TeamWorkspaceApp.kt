package com.playerid.app.ui.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.playerid.app.domain.team.playerMemoryCounts
import com.playerid.app.domain.team.TeamCreationSubmission
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import com.playerid.app.domain.memory.GuardianShareContact
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.domain.team.GameScheduleProfile
import com.playerid.app.domain.team.JoinTeamItem
import com.playerid.app.domain.team.LOCAL_TEAM_USER
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.PlayerFormState
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.RosterListState
import com.playerid.app.domain.team.ScheduleListState
import com.playerid.app.domain.team.TeamCreationFormState
import com.playerid.app.domain.team.TeamDetailNavigationEvent
import com.playerid.app.domain.team.TeamDetailPage
import com.playerid.app.domain.team.TeamProfile
import com.playerid.app.domain.team.TeamSelectionDialog
import com.playerid.app.domain.team.TeamSelectionEvent
import com.playerid.app.domain.team.TeamSelectionItem
import com.playerid.app.domain.team.TeamSelectionState
import com.playerid.app.domain.team.TeamSnapshotStorage
import com.playerid.app.domain.team.TeamStorageException
import com.playerid.app.domain.team.TeamWorkspace
import com.playerid.app.domain.team.initialTeamDetailPage
import com.playerid.app.domain.team.reduce
import com.playerid.app.domain.team.scheduleGameItem
import com.playerid.app.ui.dialogs.PlayerFormDialog
import com.playerid.app.ui.dialogs.TeamCreationDialog
import com.playerid.app.ui.dialogs.teamComposeColor
import com.playerid.app.ui.roster.RosterPage
import com.playerid.app.roster.RosterCandidate
import com.playerid.app.roster.distinctRosterCandidates
import com.playerid.app.domain.team.TeamImportSource
import com.playerid.app.ui.theme.PlayerIDBrandTheme
import com.playerid.app.domain.navigation.AppDestination
import com.playerid.app.domain.navigation.AppNavigationState
import com.playerid.app.domain.navigation.OnboardingDestination
import com.playerid.app.ui.components.PlayerIDLogo
import com.playerid.app.ui.navigation.PlayerIDAppShell
import com.playerid.app.ui.navigation.PendingNativeFeaturePage
import com.playerid.app.ui.screens.MorePage
import com.playerid.app.ui.screens.AccountPage
import com.playerid.app.domain.auth.EmailAuth
import com.playerid.app.ui.screens.OnboardingFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

@Composable
fun TeamWorkspaceApp(
    storage: TeamSnapshotStorage,
    newId: () -> String,
    cameraContent: @Composable (TeamWorkspace, () -> Unit) -> Unit,
    memoriesContent: @Composable (TeamWorkspace, Boolean, Int, () -> Unit, (com.playerid.app.domain.team.LibraryNavigationRequest) -> Unit) -> Unit,
    rosterImport: RosterImportAdapter? = null,
    onRotationAllowed: (Boolean) -> Unit = {},
    pickContact: (suspend () -> GuardianShareContact?)? = null,
    account: EmailAuth? = null
) {
    LaunchedEffect(account) { account?.restore() }
    var workspace by remember { mutableStateOf<TeamWorkspace?>(null) }
    var loadAttempt by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(TeamSelectionState()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(loadAttempt) {
        try {
            workspace = TeamWorkspace(LocalTeamRepository.open(storage), newId)
        } catch (error: TeamStorageException) {
            errorMessage = error.message
        }
    }

    val runAction: (suspend () -> Unit) -> Unit = { action ->
        if (!busy) {
            busy = true
            scope.launch {
                try {
                    action()
                } catch (error: TeamStorageException) {
                    errorMessage = error.message
                } catch (error: IllegalArgumentException) {
                    errorMessage = error.message
                } finally {
                    busy = false
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        val current = workspace
        if (current == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (errorMessage == null) CircularProgressIndicator()
            }
        } else {
            WorkspaceRoot(
                workspace = current,
                selection = selection,
                onSelection = { selection = it },
                runAction = runAction,
                cameraContent = cameraContent,
                memoriesContent = memoriesContent,
                rosterImport = rosterImport,
                onRotationAllowed = onRotationAllowed,
                pickContact = pickContact,
                account = account,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (busy) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Saving changes...", modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { if (workspace != null) errorMessage = null },
            title = { Text(if (workspace == null) "Cannot open saved data" else "Changes were not saved") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    errorMessage = null
                    if (workspace == null) loadAttempt += 1
                }) { Text(if (workspace == null) "Retry" else "OK") }
            }
        )
    }
}

@Composable
private fun WorkspaceRoot(
    workspace: TeamWorkspace,
    selection: TeamSelectionState,
    onSelection: (TeamSelectionState) -> Unit,
    runAction: (suspend () -> Unit) -> Unit,
    cameraContent: @Composable (TeamWorkspace, () -> Unit) -> Unit,
    memoriesContent: @Composable (TeamWorkspace, Boolean, Int, () -> Unit, (com.playerid.app.domain.team.LibraryNavigationRequest) -> Unit) -> Unit,
    rosterImport: RosterImportAdapter?,
    onRotationAllowed: (Boolean) -> Unit,
    pickContact: (suspend () -> GuardianShareContact?)?,
    account: EmailAuth?,
    modifier: Modifier
) {
    val snapshot by workspace.repository.snapshot.collectAsState()
    var navigation by remember { mutableStateOf(AppNavigationState()) }
    var accountOpen by remember { mutableStateOf(false) }
    // Only the Camera rotates; Memories and the Settings tab (More, Teams, Settings) stay portrait.
    val rotationAllowed = !snapshot.requiresOnboarding && navigation.destination == AppDestination.Camera
    LaunchedEffect(rotationAllowed) { onRotationAllowed(rotationAllowed) }
    val stateHolder = rememberSaveableStateHolder()
    if (snapshot.requiresOnboarding) {
        Box(modifier) {
            OnboardingFlow(
                availableTeams = snapshot.teams.filter { it.isActive && !it.isArchived },
                allPlayers = snapshot.players.filter { it.isActive },
                logo = { PlayerIDLogo() },
                teamSearchNotice = "This build searches teams saved on this iPhone only. Cross-device team discovery is not connected yet.",
                onJoinTeam = { team, player, onJoined ->
                    runAction {
                        workspace.joinFromOnboarding(team, player)
                        onJoined()
                    }
                },
                onComplete = { destination ->
                    runAction {
                        workspace.repository.completeOnboarding()
                        when (destination) {
                            OnboardingDestination.Camera, OnboardingDestination.ContinueWithoutTeam ->
                                navigation = navigation.navigate(AppDestination.Camera)
                            OnboardingDestination.CreateTeam -> {
                                onSelection(TeamSelectionState(activeDialog = TeamSelectionDialog.CreateTeam))
                                navigation = navigation.navigate(AppDestination.Teams)
                            }
                            is OnboardingDestination.Roster -> {
                                onSelection(TeamSelectionState(
                                    selectedTeamName = destination.teamName,
                                    createdTeamName = destination.teamName
                                ))
                                navigation = navigation.navigate(AppDestination.Teams)
                            }
                        }
                    }
                }
            )
        }
    } else {
        Box(modifier) {
            PlayerIDAppShell(
                state = navigation,
                onTabSelected = { accountOpen = false; navigation = navigation.selectTab(it) }
            ) {
                Column(Modifier.fillMaxSize()) {
                if (!accountOpen && navigation.destination in listOf(
                        AppDestination.More, AppDestination.Teams, AppDestination.Settings
                    )) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navigation = navigation.back() }) {
                            Icon(Icons.Default.ArrowBack,
                                if (navigation.destination == AppDestination.More) "Back to Memories" else "Back to More")
                        }
                        Text(if (navigation.destination == AppDestination.More) "Settings and more" else "More",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
                Box(Modifier.weight(1f)) {
                if (accountOpen && account != null) {
                    AccountPage(account, onBack = { accountOpen = false })
                } else when (navigation.destination) {
                    AppDestination.More -> MorePage(
                        onNavigateToSettings = { navigation = navigation.navigate(AppDestination.Settings) },
                        onNavigateToAccount = if (account != null) ({ accountOpen = true }) else null
                    )
                    AppDestination.Teams -> stateHolder.SaveableStateProvider("teams") {
                        WorkspaceContent(workspace, selection, onSelection, runAction, rosterImport, pickContact, Modifier.fillMaxSize())
                    }
                    AppDestination.Camera -> PlayerIDBrandTheme(darkTheme = false) {
                        cameraContent(workspace) {
                            onSelection(TeamSelectionState(selectedTeamName = snapshot.cameraOrganization?.teamName))
                            navigation = navigation.navigate(AppDestination.Teams)
                        }
                    }
                    AppDestination.Memories, AppDestination.Go -> stateHolder.SaveableStateProvider(
                        if (navigation.destination == AppDestination.Go) "go" else "memories"
                    ) {
                        memoriesContent(workspace, navigation.destination == AppDestination.Go, navigation.goOpenGeneration,
                            { navigation = navigation.navigate(AppDestination.More) }) { request ->
                            when (request.destination) {
                                com.playerid.app.domain.team.LibraryDestination.ROSTER,
                                com.playerid.app.domain.team.LibraryDestination.SCHEDULE -> {
                                    onSelection(TeamSelectionState(
                                        selectedTeamName = request.teamName,
                                        initialPage = if (request.destination == com.playerid.app.domain.team.LibraryDestination.ROSTER)
                                            TeamDetailPage.Roster else TeamDetailPage.Schedule
                                    ))
                                    navigation = navigation.navigate(AppDestination.Teams)
                                }
                                com.playerid.app.domain.team.LibraryDestination.TEAMS -> {
                                    onSelection(TeamSelectionState())
                                    navigation = navigation.navigate(AppDestination.Teams)
                                }
                                com.playerid.app.domain.team.LibraryDestination.CAMERA ->
                                    navigation = navigation.navigate(AppDestination.Camera)
                                else -> Unit
                            }
                        }
                    }
                    AppDestination.Settings -> {
                        val title = when (navigation.destination) {
                            else -> "Settings"
                        }
                        val message = when (navigation.destination) {
                            else ->
                                "The existing Android settings workflow is being migrated. iPhone account, referral, and device settings are not connected in this build. Team sport and jersey colors are available under Teams."
                        }
                        PendingNativeFeaturePage(
                            title, message,
                            onTeams = { navigation = navigation.navigate(AppDestination.Teams) },
                            onMore = { navigation = navigation.navigate(AppDestination.More) }
                        )
                    }
                }
                }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceContent(
    workspace: TeamWorkspace,
    selection: TeamSelectionState,
    onSelection: (TeamSelectionState) -> Unit,
    runAction: (suspend () -> Unit) -> Unit,
    rosterImport: RosterImportAdapter?,
    pickContact: (suspend () -> GuardianShareContact?)?,
    modifier: Modifier
) {
    val repository = workspace.repository
    val snapshot by repository.snapshot.collectAsState()
    val teams by remember(repository) { repository.observeSubscribedTeams(LOCAL_TEAM_USER) }
        .collectAsState(emptyList())
    val selected = snapshot.teams.firstOrNull { it.name == selection.selectedTeamName }
    Box(modifier) {
        if (selected == null) {
            TeamSelectionPage(
                teams = teams.map { team ->
                    TeamSelectionItem(
                        team.name, team.color.ifBlank { team.homeJerseyColor }, team.awayColor,
                        snapshot.players.count { it.teamName == team.name && it.isActive }
                    )
                },
                teamSnapImportAvailable = false,
                onSelectTeam = { onSelection(selection.reduce(TeamSelectionEvent.TeamSelected(it))) },
                onJoinTeam = { onSelection(selection.reduce(TeamSelectionEvent.DialogRequested(TeamSelectionDialog.JoinTeam))) },
                onCreateTeam = { onSelection(selection.reduce(TeamSelectionEvent.DialogRequested(TeamSelectionDialog.CreateTeam))) },
                onImportTeamSnap = {},
                joinIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                createIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                selectIcon = { Icon(Icons.Default.PlayArrow, contentDescription = "Select team") },
                emptyIcon = {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                importIcon = {}
            )
        } else {
            // Keyed by ID so renaming keeps the current page open.
            key(selected.id) {
                WorkspaceTeamDetail(
                    team = selected,
                    workspace = workspace,
                    openRoster = selection.shouldOpenRoster(false),
                    initialPage = selection.initialPage,
                    onBack = { onSelection(selection.reduce(TeamSelectionEvent.TeamCleared)) },
                    onRenamed = { name -> onSelection(selection.copy(selectedTeamName = name)) },
                    runAction = runAction,
                    rosterImport = rosterImport,
                    pickContact = pickContact
                )
            }
        }
    }

    when (selection.activeDialog) {
        TeamSelectionDialog.CreateTeam -> TeamCreationDialog(
            existingTeams = snapshot.teams.map { it.name },
            onDismiss = { onSelection(selection.reduce(TeamSelectionEvent.DialogDismissed)) },
            onSubmit = { submission ->
                runAction {
                    val team = workspace.createTeam(submission)
                    onSelection(selection.reduce(TeamSelectionEvent.TeamCreated(team.name, true)))
                }
            }
        )
        TeamSelectionDialog.JoinTeam -> JoinTeamDialog(
            teams = snapshot.teams.filter { it.isActive && !it.isArchived }.map {
                JoinTeamItem(it.name, it.homeJerseyColor)
            },
            subscribedTeamNames = teams.map { it.name }.toSet(),
            onDismiss = { onSelection(selection.reduce(TeamSelectionEvent.DialogDismissed)) },
            onJoin = { teamName ->
                runAction {
                    workspace.joinTeam(teamName)
                    onSelection(selection.copy(selectedTeamName = teamName, activeDialog = null))
                }
            },
            closeIcon = { Icon(Icons.Default.Close, contentDescription = "Close") },
            searchIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            noOtherTeamsText = "No other teams saved on this iPhone. Teams from other devices are not synced."
        )
        else -> Unit
    }
}

private enum class WorkspaceDialog {
    AddPlayer, EditPlayer, DeletePlayer, RosterImport, ScheduleImport, AddGame,
    EditGame, DeleteGame, Settings, LeaveTeam, AssignPlayer
}

@Composable
private fun WorkspaceTeamDetail(
    team: TeamProfile,
    workspace: TeamWorkspace,
    openRoster: Boolean,
    initialPage: TeamDetailPage? = null,
    onBack: () -> Unit,
    onRenamed: (String) -> Unit,
    runAction: (suspend () -> Unit) -> Unit,
    rosterImport: RosterImportAdapter?,
    pickContact: (suspend () -> GuardianShareContact?)?
) {
    val repository = workspace.repository
    val snapshot by repository.snapshot.collectAsState()
    val players by remember(repository, team.name) { repository.observePlayers(team.name) }
        .collectAsState(emptyList())
    val games by remember(repository, team.name) { repository.observeGamesForTeam(team.name) }
        .collectAsState(emptyList())
    var page by rememberSaveable(
        team.id, initialPage, openRoster,
        stateSaver = Saver(
            save = { it.name },
            restore = { TeamDetailPage.valueOf(it) }
        )
    ) { mutableStateOf(initialPage ?: initialTeamDetailPage(openRoster)) }
    LaunchedEffect(initialPage) {
        initialPage?.let { page = it }
    }
    var dialog by remember { mutableStateOf<WorkspaceDialog?>(null) }
    var editingPlayer by remember { mutableStateOf<PlayerProfile?>(null) }
    var editingGame by remember { mutableStateOf<GameScheduleProfile?>(null) }
    var playerForm by remember { mutableStateOf(PlayerFormState.forNewPlayer(team.name)) }
    var rosterQuery by rememberSaveable { mutableStateOf("") }
    var scheduleQuery by rememberSaveable { mutableStateOf("") }
    val assignedPlayer = players.firstOrNull { it.id == snapshot.assignedPlayerIds[team.name] }
    val timeZone = TimeZone.currentSystemDefault()
    val memoryCounts = remember(snapshot, team.name) { playerMemoryCounts(snapshot, team.name) }
    fun addPlayer() {
        editingPlayer = null
        playerForm = PlayerFormState.forNewPlayer(team.name)
        dialog = WorkspaceDialog.AddPlayer
    }
    fun overview() {
        page = page.reduce(TeamDetailNavigationEvent.OverviewSelected)
    }
    var rosterImportPage by remember { mutableStateOf<TeamImportSource?>(null) }
    var scheduleImportSource by remember { mutableStateOf<TeamImportSource?>(null) }
    var screenshotReview by remember { mutableStateOf(false) }
    var screenshotProcessing by remember { mutableStateOf(false) }
    var screenshotError by remember { mutableStateOf<String?>(null) }
    var screenshotCandidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }
    val importScope = rememberCoroutineScope()
    fun importRoster(candidates: List<RosterCandidate>) {
        rosterImportPage = null
        screenshotReview = false
        dialog = null
        runAction {
            workspace.importRoster(team.name, candidates)
            page = page.reduce(TeamDetailNavigationEvent.RosterSelected)
        }
    }
    fun chooseRosterSource(source: TeamImportSource) {
        val adapter = rosterImport ?: return
        dialog = null
        when (source) {
            TeamImportSource.Screenshot -> importScope.launch {
                screenshotError = null
                screenshotCandidates = emptyList()
                try {
                    val result = adapter.recognizeRosterImages(multiple = false) {
                        screenshotReview = true
                        screenshotProcessing = true
                    }
                    if (result != null) screenshotCandidates = distinctRosterCandidates(result.candidates)
                } catch (error: Exception) {
                    screenshotReview = true
                    screenshotError = error.message ?: "Failed to read roster"
                } finally {
                    screenshotProcessing = false
                }
            }
            TeamImportSource.App, TeamImportSource.Website -> rosterImportPage = source
        }
    }
    val scheduleSource = scheduleImportSource
    if (rosterImport != null && scheduleSource != null) {
        ScheduleImportPage(
            teamName = team.name,
            source = scheduleSource,
            adapter = rosterImport,
            onBack = { scheduleImportSource = null },
            onImport = { entries ->
                scheduleImportSource = null
                runAction {
                    workspace.importSchedule(team, entries)
                    page = page.reduce(TeamDetailNavigationEvent.ScheduleSelected)
                }
            }
        )
        return
    }
    if (rosterImport != null && rosterImportPage != null) {
        when (rosterImportPage) {
            TeamImportSource.Website -> WebRosterImportPage(
                teamName = team.name,
                adapter = rosterImport,
                onBack = { rosterImportPage = null },
                onImport = ::importRoster
            )
            else -> AppRosterImportPage(
                teamName = team.name,
                adapter = rosterImport,
                onBack = { rosterImportPage = null },
                onImport = ::importRoster
            )
        }
        return
    }

    when (page) {
        TeamDetailPage.Overview -> TeamOverviewPage(
            teamName = team.name,
            seasonLabel = team.description.ifBlank { team.sport },
            homeColor = teamComposeColor(team.color.ifBlank { team.homeJerseyColor }),
            assignedKid = "Choose your player",
            assignedPlayer = assignedPlayer,
            playerCount = players.size,
            gameCount = games.size,
            onBack = onBack,
            onAssignedKidClick = { dialog = WorkspaceDialog.AssignPlayer },
            onRoster = { page = page.reduce(TeamDetailNavigationEvent.RosterSelected) },
            onImportRoster = { dialog = WorkspaceDialog.RosterImport },
            onSchedule = { page = page.reduce(TeamDetailNavigationEvent.ScheduleSelected) },
            onContacts = { page = page.reduce(TeamDetailNavigationEvent.ContactsSelected) },
            contactsSubtitle = teamContactsSubtitle(snapshot.teamShareContacts[team.name].orEmpty()),
            onImportSchedule = { dialog = WorkspaceDialog.ScheduleImport },
            onInvite = {},
            onSettings = { dialog = WorkspaceDialog.Settings },
            onLeave = { dialog = WorkspaceDialog.LeaveTeam },
            backIcon = { Icon(Icons.Default.ArrowBack, contentDescription = "Back") },
            kidPicker = {},
            assignedPlayerTrailingIcon = {
                Icon(Icons.Default.ChevronRight, contentDescription = "Change assigned player")
            },
            destinationIcon = { destination ->
                val icon = when (destination) {
                    TeamOverviewDestination.Roster -> Icons.Default.Groups
                    TeamOverviewDestination.Schedule -> Icons.Default.CalendarMonth
                    TeamOverviewDestination.Contacts -> Icons.Default.Contacts
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
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            sharingAvailable = false,
            settingsSubtitle = "Team name, sport and jersey colors",
            leaveDescription = "Hide this team from your list. Its data stays on this iPhone and you can rejoin it."
        )
        TeamDetailPage.Roster -> RosterPage(
            state = RosterListState(team.name, players, rosterQuery, snapshot.favoritePlayerIds),
            totalCount = players.size,
            onSearchChange = { rosterQuery = it },
            onBack = { overview() },
            onAdd = { addPlayer() },
            onImport = { dialog = WorkspaceDialog.RosterImport },
            onPlayerClick = {
                editingPlayer = it
                playerForm = PlayerFormState.forEditing(it)
                dialog = WorkspaceDialog.EditPlayer
            },
            backIcon = { Icon(Icons.Default.ArrowBack, contentDescription = "Back") },
            addIcon = { Icon(Icons.Default.Add, contentDescription = "Add") },
            searchIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            importIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF0A66FF)) },
            addPlayerIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
            playerLeadingContent = {
                Box(
                    modifier = Modifier.size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            playerTrailingContent = { player ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val memoryCount = memoryCounts[player.id] ?: 0
                    if (memoryCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "$memoryCount ${if (memoryCount == 1) "memory" else "memories"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                    IconButton(onClick = { runAction { repository.toggleFavorite(player.id) } }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = if (player.id in snapshot.favoritePlayerIds) "Unfavorite player" else "Favorite player",
                            tint = if (player.id in snapshot.favoritePlayerIds) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        )
        TeamDetailPage.Contacts -> TeamContactsPage(
            teamName = team.name,
            players = players,
            repository = repository,
            pickContact = pickContact,
            onBack = { page = page.reduce(TeamDetailNavigationEvent.OverviewSelected) }
        )
        TeamDetailPage.Schedule -> SchedulePage(
            state = ScheduleListState(
                games = games.map {
                    scheduleGameItem(
                        it.id, it.opponentName, it.gameLabel, it.scheduledStartMs, it.locationName,
                        utcOffsetMinutes = timeZone.offsetAt(Instant.fromEpochMilliseconds(it.scheduledStartMs)).totalSeconds / 60
                    )
                },
                searchQuery = scheduleQuery
            ),
            totalCount = games.size,
            nowMs = Clock.System.now().toEpochMilliseconds(),
            onSearchChange = { scheduleQuery = it },
            onBack = { overview() },
            onAdd = { editingGame = null; dialog = WorkspaceDialog.AddGame },
            onImport = { dialog = WorkspaceDialog.ScheduleImport },
            backIcon = { Icon(Icons.Default.ArrowBack, contentDescription = "Back") },
            addIcon = { Icon(Icons.Default.Add, contentDescription = "Add") },
            searchIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            importIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF0A66FF)) },
            gameTrailingIcon = { game ->
                IconButton(onClick = {
                    editingGame = games.first { it.id == game.id }
                    dialog = WorkspaceDialog.EditGame
                }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Game options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    when (dialog) {
        WorkspaceDialog.AddPlayer, WorkspaceDialog.EditPlayer -> PlayerFormDialog(
            title = if (editingPlayer == null) "Add Player" else "Edit Player",
            submitLabel = "Save",
            formState = playerForm,
            onEvent = { playerForm = playerForm.reduce(it) },
            onDismiss = { dialog = null },
            onSubmit = { submission ->
                runAction {
                    workspace.savePlayer(submission, editingPlayer)
                    dialog = null
                }
            },
            showTeamField = false,
            availableTeams = listOf(team.name),
            onDelete = if (editingPlayer == null) null else { { dialog = WorkspaceDialog.DeletePlayer } }
        )
        WorkspaceDialog.DeletePlayer -> WorkspaceConfirmation(
            title = "Delete player?",
            message = "${editingPlayer?.name} will be permanently removed from this iPhone's roster.",
            confirmLabel = "Delete",
            onDismiss = { dialog = WorkspaceDialog.EditPlayer },
            onConfirm = {
                editingPlayer?.let { player ->
                    runAction {
                        workspace.deletePlayer(player.id)
                        editingPlayer = null
                        dialog = null
                    }
                }
            }
        )
        WorkspaceDialog.RosterImport -> if (rosterImport != null) TeamImportOptionsDialog(
            title = "Import roster",
            helpText = "Import your roster so PlayerID can identify players and help you share the moments that matter.",
            sourceLabel = { importSourceLabel(it) },
            sourceIcon = { ImportSourceIcon(it) },
            onSourceSelected = ::chooseRosterSource,
            onDismiss = { dialog = null }
        ) else RosterTextImportDialog(
            onDismiss = { dialog = null },
            onImport = { candidates ->
                runAction {
                    workspace.importRoster(team.name, candidates)
                    page = page.reduce(TeamDetailNavigationEvent.RosterSelected)
                    dialog = null
                }
            }
        )
        WorkspaceDialog.ScheduleImport -> if (rosterImport != null) TeamImportOptionsDialog(
            title = "Import schedule",
            helpText = "Choose where to import the team schedule from.",
            sourceLabel = { importSourceLabel(it) },
            sourceIcon = { ImportSourceIcon(it) },
            onSourceSelected = { source ->
                dialog = null
                scheduleImportSource = source
            },
            onDismiss = { dialog = null }
        ) else ScheduleTextImportDialog(
            onDismiss = { dialog = null },
            onImport = { entries ->
                runAction {
                    workspace.importSchedule(team, entries)
                    page = page.reduce(TeamDetailNavigationEvent.ScheduleSelected)
                    dialog = null
                }
            }
        )
        WorkspaceDialog.AddGame, WorkspaceDialog.EditGame -> ScheduleGameDialog(
            onDismiss = { dialog = null },
            onSubmit = { form ->
                runAction {
                    workspace.saveGame(team, form, editingGame)
                    dialog = null
                }
            },
            existing = editingGame,
            onDelete = if (editingGame == null) null else { { dialog = WorkspaceDialog.DeleteGame } }
        )
        WorkspaceDialog.DeleteGame -> WorkspaceConfirmation(
            title = "Delete game?",
            message = "This game will be permanently removed from this iPhone's schedule.",
            confirmLabel = "Delete",
            onDismiss = { dialog = WorkspaceDialog.EditGame },
            onConfirm = {
                editingGame?.let { game ->
                    runAction {
                        repository.deleteGame(game.id)
                        editingGame = null
                        dialog = null
                    }
                }
            }
        )
        WorkspaceDialog.Settings -> TeamCreationDialog(
            existingTeams = snapshot.teams.filter { it.id != team.id }.map { it.name },
            onDismiss = { dialog = null },
            onSubmit = { submission ->
                runAction {
                    val saved = workspace.updateTeam(team, submission)
                    dialog = null
                    if (saved.name != team.name) onRenamed(saved.name)
                }
            },
            initialState = TeamCreationFormState(team.name, team.sport, team.homeJerseyColor, team.awayJerseyColor),
            title = "Team settings",
            submitLabel = "Save"
        )
        WorkspaceDialog.LeaveTeam -> WorkspaceConfirmation(
            title = "Leave ${team.name}?",
            message = "This hides the team, but does not delete its roster or schedule. Use Rejoin local team to restore it.",
            confirmLabel = "Leave team",
            onDismiss = { dialog = null },
            onConfirm = {
                runAction {
                    workspace.leaveTeam(team.name)
                    dialog = null
                    onBack()
                }
            }
        )
        WorkspaceDialog.AssignPlayer -> AssignedPlayerDialog(
            players = players,
            onDismiss = { dialog = null },
            onAssign = { playerId ->
                runAction {
                    repository.assignPlayer(team.name, playerId)
                    dialog = null
                }
            }
        )
        null -> Unit
    }
    if (screenshotReview) RosterImportReviewDialog(
        title = "Import from Screenshot",
        teamName = team.name,
        isProcessing = screenshotProcessing,
        errorMessage = screenshotError,
        candidates = screenshotCandidates,
        emptyMessage = "No players detected. Try a clearer screenshot of the roster table.",
        onRemove = { removed -> screenshotCandidates = screenshotCandidates.filterNot { it == removed } },
        onDismiss = { screenshotReview = false },
        onImport = ::importRoster
    )
}

@Composable
internal fun WorkspaceConfirmation(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun importSourceLabel(source: TeamImportSource): String = when (source) {
    TeamImportSource.Screenshot -> "From screenshot"
    TeamImportSource.App -> "From app"
    TeamImportSource.Website -> "From website"
}

@Composable
private fun ImportSourceIcon(source: TeamImportSource) {
    Icon(
        imageVector = when (source) {
            TeamImportSource.Screenshot -> Icons.Default.CloudDownload
            TeamImportSource.App -> Icons.Default.PhoneIphone
            TeamImportSource.Website -> Icons.Default.Language
        },
        contentDescription = null
    )
}


private fun teamContactsSubtitle(contacts: List<GuardianShareContact>): String {
    val linked = contacts.count { it.linkedPlayerIds.isNotEmpty() }
    val favorites = contacts.count { it.isFavorite }
    if (linked == 0 && favorites == 0) return "Add player contacts and favorites"
    return "$linked player ${if (linked == 1) "contact" else "contacts"} · $favorites ${if (favorites == 1) "favorite" else "favorites"}"
}
