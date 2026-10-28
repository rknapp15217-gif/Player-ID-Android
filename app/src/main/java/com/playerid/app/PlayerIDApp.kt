package com.playerid.app
import android.net.Uri
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.collectAsState

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier // Keep only one import
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.playerid.app.ui.components.BottomNavItem
import com.playerid.app.domain.navigation.AppDestination
import com.playerid.app.domain.navigation.AppNavigationState
import com.playerid.app.ui.navigation.PlayerIDAppShell
import com.playerid.app.ui.screens.MorePage
import com.playerid.app.ui.screens.CameraScreen
import com.playerid.app.ui.screens.TeamScreen
import com.playerid.app.ui.screens.ReferralScreen
import com.playerid.app.ui.screens.SettingsScreen
import com.playerid.app.ui.screens.WebRosterImportScreen
import com.playerid.app.ui.screens.AppRosterImportScreen
import com.playerid.app.ui.screens.ClipsScreenRefactored
import com.playerid.app.ui.screens.ScheduleImportScreen
import androidx.compose.material.icons.filled.Search
import com.playerid.app.ui.screens.ContactImportScreen
import com.playerid.app.ui.screens.OnboardingScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.playerid.app.R
import com.playerid.app.memory.MemoryIngestionViewModel
import com.playerid.app.platform.AndroidAccount
import com.playerid.app.ui.screens.AccountPage

val LocalPlayerViewModel = compositionLocalOf<PlayerViewModel> { error("No PlayerViewModel provided") }
val LocalTeamViewModel = compositionLocalOf<TeamViewModel> { error("No TeamViewModel provided") }
val LocalAppContext = compositionLocalOf<Context> { error("No Context provided") }

fun reelVideoRelativePathsForRestoredClips(): List<String> = listOf(
    "Movies/PlayerID/",
    "Movies/PlayerID",
    "Movies/Spotr/",
    "Movies/Spotr"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerIDApp() {
    val context = LocalContext.current
    val account = remember(context.applicationContext) { AndroidAccount(context.applicationContext) }
    LaunchedEffect(account) { account.auth.restore() }
    val onboardingPrefs = remember(context) {
        context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    }
    var onboardingCompleted by rememberSaveable {
        mutableStateOf(onboardingPrefs.getBoolean("sections_1_2a_completed", false))
    }
    var postOnboardingRoute by remember { mutableStateOf<String?>(null) }
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel()
    val memoryIngestionViewModel: MemoryIngestionViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "camera"
    val pendingMemoryPrompt by memoryIngestionViewModel.pendingPrompt.collectAsState()
    val requestedRoute by AppNavigationCallback.route.collectAsState()
    val navItems = listOf(
        BottomNavItem(stringResource(R.string.clips_tab), Icons.Filled.PhotoLibrary, "clips"),
        BottomNavItem(stringResource(R.string.camera_tab), Icons.Filled.CameraAlt, "camera"),
        BottomNavItem("Go", Icons.Filled.Search, "go")
    )
    val selectedIndex = when {
        currentRoute == "clips" -> 0
        currentRoute == "camera" -> 1
        currentRoute == "go" -> 2
        else -> -1
    }
    var previousRoute by remember { mutableStateOf(currentRoute) }
    var cameraHandoffToken by remember { mutableStateOf(0) }
    var goOpenGeneration by remember { mutableStateOf(0) }
    val goRecentRequests = androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(emptyList<String>())
    }

    // Camera is master only when leaving Camera to any other screen.
    LaunchedEffect(currentRoute) {
        if (previousRoute == "camera" && currentRoute != "camera") {
            cameraHandoffToken += 1
        }
        previousRoute = currentRoute
    }

    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted) memoryIngestionViewModel.scanForNewMemoriesOnLaunch()
    }

    LaunchedEffect(requestedRoute) {
        requestedRoute?.let { route ->
            navController.navigate(route) { launchSingleTop = true }
            AppNavigationCallback.clear()
        }
    }

    // Handle permission grant and retry scan
    val permissionGranted by AppNavigationCallback.permissionsGranted.collectAsState()
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            AppNavigationCallback.clearPermissionNotification()
            memoryIngestionViewModel.retryScanAfterPermission()
        }
    }

    CompositionLocalProvider(
        LocalPlayerViewModel provides playerViewModel,
        LocalAppContext provides playerViewModel.getApplication()
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            if (!onboardingCompleted) {
                OnboardingScreen(
                    teamViewModel = teamViewModel,
                    playerViewModel = playerViewModel,
                    onComplete = { route ->
                        onboardingPrefs.edit().putBoolean("sections_1_2a_completed", true).apply()
                        postOnboardingRoute = route
                        onboardingCompleted = true
                    }
                )
                return@Surface
            }
            val permissionDenied by memoryIngestionViewModel.permissionDenied.collectAsState()
            
            if (permissionDenied) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { },
                    title = { androidx.compose.material3.Text("Media access needed") },
                    text = {
                        androidx.compose.material3.Text(
                            "PlayerID needs access to your photos and videos to automatically organize your memories by game. " +
                            "Please grant media permissions to continue."
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { 
                                memoryIngestionViewModel.retryScanAfterPermission()
                            }
                        ) {
                            androidx.compose.material3.Text("Grant Permissions")
                        }
                    }
                )
            }
            
            if (pendingMemoryPrompt != null) {
                val prompt = pendingMemoryPrompt!!
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { memoryIngestionViewModel.skipPendingMemories() },
                    title = { androidx.compose.material3.Text("New memories found") },
                    text = {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = "Found ${prompt.totalCount} new memories that match your uploaded schedule."
                            )
                            prompt.groups.forEach { group ->
                                androidx.compose.material3.Text("${group.count} from ${group.label}")
                            }
                            androidx.compose.material3.Text(
                                text = "Would you like to include these memories?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { memoryIngestionViewModel.acceptPendingMemories() }
                        ) {
                            androidx.compose.material3.Text("Include")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { memoryIngestionViewModel.skipPendingMemories() }
                        ) {
                            androidx.compose.material3.Text("Not now")
                        }
                    }
                )
            }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize()
            ) {
                PlayerIDAppShell(
                    state = AppNavigationState(
                        destination = when (selectedIndex) {
                            0 -> AppDestination.Memories
                            1 -> AppDestination.Camera
                            2 -> AppDestination.Go
                            else -> AppDestination.More
                        }
                    ),
                    memoriesLabel = navItems[0].label,
                    cameraLabel = navItems[1].label,
                    onTabSelected = { idx ->
                        val targetRoute = navItems[idx].route
                        if (targetRoute == "go") goOpenGeneration++
                        val returnedToCamera = targetRoute == "camera" &&
                            navController.popBackStack(route = "camera", inclusive = false)
                        if (!returnedToCamera && currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = false
                                    saveState = targetRoute != "camera"
                                }
                                launchSingleTop = true
                                restoreState = targetRoute != "camera"
                            }
                        }
                    }
                ) {
                        NavHost(
                            navController = navController,
                            startDestination = postOnboardingRoute ?: "camera"
                        ) {
                            composable("camera") {
                                CameraScreen(
                                    viewModel = playerViewModel,
                                    teamViewModel = teamViewModel,
                                    onNavigateToTeams = { navController.navigate("teams") }
                                )
                            }
                            composable("more") {
                                MoreScreen(
                                    onNavigateToAccount = { navController.navigate("account") },
                                    onNavigateToSettings = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = false
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            composable("account") {
                                AccountPage(account.auth, onBack = { navController.popBackStack() })
                            }
                            composable("teams") {
                                TeamScreen(
                                    teamViewModel = teamViewModel,
                                    playerViewModel = playerViewModel,
                                    onNavigateToWebImport = { teamName ->
                                        navController.navigate("webRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToAppImport = { teamName, _ ->
                                        navController.navigate("appRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToScheduleImport = { teamName, source ->
                                        if (source == "app") {
                                            com.playerid.app.capture.AppRosterCaptureRepository.clearSchedule()
                                        }
                                        navController.navigate("scheduleImport/${Uri.encode(teamName)}/$source")
                                    },
                                    onNavigateToContactImport = { teamName, source ->
                                        navController.navigate("contactImport/${Uri.encode(teamName)}/$source")
                                    }
                                )
                            }
                            composable("teams/create") {
                                TeamScreen(
                                    teamViewModel = teamViewModel,
                                    playerViewModel = playerViewModel,
                                    startCreateTeamInitially = true,
                                    openRosterAfterCreate = true,
                                    onNavigateToWebImport = { teamName ->
                                        navController.navigate("webRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToAppImport = { teamName, _ ->
                                        navController.navigate("appRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToScheduleImport = { teamName, source ->
                                        if (source == "app") {
                                            com.playerid.app.capture.AppRosterCaptureRepository.clearSchedule()
                                        }
                                        navController.navigate("scheduleImport/${Uri.encode(teamName)}/$source")
                                    },
                                    onNavigateToContactImport = { teamName, source ->
                                        navController.navigate("contactImport/${Uri.encode(teamName)}/$source")
                                    }
                                )
                            }
                            composable(
                                route = "teams/{page}/{teamName}",
                                arguments = listOf(
                                    navArgument("teamName") { type = NavType.StringType },
                                    navArgument("page") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                TeamScreen(
                                    teamViewModel = teamViewModel,
                                    playerViewModel = playerViewModel,
                                    initialTeamName = backStackEntry.arguments?.getString("teamName"),
                                    openRosterInitially = backStackEntry.arguments?.getString("page") != "schedule",
                                    initialPage = if (backStackEntry.arguments?.getString("page") == "schedule")
                                        com.playerid.app.domain.team.TeamDetailPage.Schedule else null,
                                    onNavigateToWebImport = { teamName ->
                                        navController.navigate("webRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToAppImport = { teamName, _ ->
                                        navController.navigate("appRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToScheduleImport = { teamName, source ->
                                        if (source == "app") {
                                            com.playerid.app.capture.AppRosterCaptureRepository.clearSchedule()
                                        }
                                        navController.navigate("scheduleImport/${Uri.encode(teamName)}/$source")
                                    },
                                    onNavigateToContactImport = { teamName, source ->
                                        navController.navigate("contactImport/${Uri.encode(teamName)}/$source")
                                    }
                                )
                            }
                            listOf("clips", "go").forEach { libraryRoute ->
                            composable(libraryRoute) {
                                androidx.compose.runtime.key(if (libraryRoute == "go") goOpenGeneration else 0) {
                                ClipsScreenRefactored(
                                    teamViewModel = teamViewModel,
                                    playerViewModel = playerViewModel,
                                    cameraHandoffToken = cameraHandoffToken,
                                    goMode = libraryRoute == "go",
                                    goRecentRequests = if (libraryRoute == "go") goRecentRequests else null,
                                    onOpenMore = { navController.navigate("more") },
                                    onNavigateToTeams = { navController.navigate("teams") },
                                    onNavigate = { request ->
                                        when (request.destination) {
                                            com.playerid.app.domain.team.LibraryDestination.ROSTER ->
                                                navController.navigate("teams/roster/${Uri.encode(request.teamName)}")
                                            com.playerid.app.domain.team.LibraryDestination.SCHEDULE ->
                                                navController.navigate("teams/schedule/${Uri.encode(request.teamName)}")
                                            com.playerid.app.domain.team.LibraryDestination.TEAMS -> navController.navigate("teams")
                                            com.playerid.app.domain.team.LibraryDestination.CAMERA -> navController.navigate("camera")
                                            else -> Unit
                                        }
                                    }
                                )
                                }
                            }
                            }
                            composable(
                                route = "webRosterImport/{teamName}",
                                arguments = listOf(navArgument("teamName") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val teamName = backStackEntry.arguments?.getString("teamName") ?: ""
                                WebRosterImportScreen(
                                    teamName = teamName,
                                    onBack = { navController.popBackStack() },
                                    onImport = { candidates ->
                                        playerViewModel.importRosterCandidates(
                                            teamName = teamName,
                                            candidates = candidates,
                                            addedBy = "web_import"
                                        )
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable(
                                route = "appRosterImport/{teamName}",
                                arguments = listOf(navArgument("teamName") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val teamName = backStackEntry.arguments?.getString("teamName") ?: ""
                                AppRosterImportScreen(
                                    teamName = teamName,
                                    onBack = { navController.popBackStack() },
                                    onImport = { candidates ->
                                        playerViewModel.importRosterCandidates(
                                            teamName = teamName,
                                            candidates = candidates,
                                            addedBy = "app_capture"
                                        )
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable(
                                route = "scheduleImport/{teamName}/{source}",
                                arguments = listOf(
                                    navArgument("teamName") { type = NavType.StringType },
                                    navArgument("source") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val teamName = backStackEntry.arguments?.getString("teamName") ?: ""
                                val source = backStackEntry.arguments?.getString("source") ?: "screenshot"
                                ScheduleImportScreen(
                                    teamName = teamName,
                                    source = source,
                                    onBack = { navController.popBackStack() },
                                    onImport = { entries ->
                                        memoryIngestionViewModel.importSchedule(teamName, entries) {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                            composable(
                                route = "contactImport/{teamName}/{source}",
                                arguments = listOf(
                                    navArgument("teamName") { type = NavType.StringType },
                                    navArgument("source") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                ContactImportScreen(
                                    teamName = backStackEntry.arguments?.getString("teamName") ?: "",
                                    source = backStackEntry.arguments?.getString("source") ?: "screenshot",
                                    playerViewModel = playerViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("referral") {
                                ReferralScreen(onNavigateBack = { navController.popBackStack() })
                            }
                            composable("settings") {
                                SettingsScreen(
                                    teamViewModel = teamViewModel,
                                    playerViewModel = playerViewModel,
                                    onNavigateToReferral = { navController.navigate("referral") },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    MorePage(
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAccount = onNavigateToAccount,
        settingsLabel = stringResource(R.string.settings_title),
        settingsSubtitle = stringResource(R.string.account_referrals_and_settings),
        openDestinationLabel = stringResource(R.string.open_destination)
    )
}