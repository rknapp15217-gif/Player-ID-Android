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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.playerid.app.ui.components.SpotrBottomNavigationBar
import com.playerid.app.ui.screens.CameraScreen
import com.playerid.app.ui.screens.TeamScreen
import com.playerid.app.ui.screens.ReferralScreen
import com.playerid.app.ui.screens.SettingsScreen
import com.playerid.app.ui.screens.WebRosterImportScreen
import com.playerid.app.ui.screens.AppRosterImportScreen
import com.playerid.app.ui.screens.ClipsScreenRefactored
import com.playerid.app.ui.screens.ScheduleImportScreen
import com.playerid.app.ui.screens.MemoryBrowsingScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.playerid.app.R
import com.playerid.app.memory.MemoryIngestionViewModel

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
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel()
    val memoryIngestionViewModel: MemoryIngestionViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "camera"
    val pendingMemoryPrompt by memoryIngestionViewModel.pendingPrompt.collectAsState()
    val navItems = listOf(
        BottomNavItem(stringResource(R.string.clips_tab), Icons.Filled.PhotoLibrary, "clips"),
        BottomNavItem(stringResource(R.string.camera_tab), Icons.Filled.CameraAlt, "camera"),
        BottomNavItem(stringResource(R.string.settings_title), Icons.Filled.Settings, "more")
    )
    val selectedIndex = when {
        currentRoute == "clips" -> 0
        currentRoute == "camera" -> 1
        currentRoute == "more" || currentRoute == "settings" -> 2
        else -> 2
    }
    var previousRoute by remember { mutableStateOf(currentRoute) }
    var cameraHandoffToken by remember { mutableStateOf(0) }

    // Camera is master only when leaving Camera to any other screen.
    LaunchedEffect(currentRoute) {
        if (previousRoute == "camera" && currentRoute != "camera") {
            cameraHandoffToken += 1
        }
        previousRoute = currentRoute
    }

    LaunchedEffect(Unit) {
        memoryIngestionViewModel.scanForNewMemoriesOnLaunch()
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
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize()
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    ) {
                        NavHost(navController = navController, startDestination = "camera") {
                            composable("camera") {
                                CameraScreen(
                                    viewModel = playerViewModel,
                                    teamViewModel = teamViewModel,
                                    onNavigateToClips = { navController.navigate("clips") },
                                    onNavigateToTeams = { navController.navigate("teams") }
                                )
                            }
                            composable("more") {
                                MoreScreen(
                                    onNavigateToTeams = {
                                        navController.navigate("teams") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                inclusive = false
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
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
                            composable("teams") {
                                TeamScreen(
                                    teamViewModel = teamViewModel,
                                    playerViewModel = playerViewModel,
                                    cameraHandoffToken = cameraHandoffToken,
                                    onNavigateToWebImport = { teamName ->
                                        navController.navigate("webRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToAppImport = { teamName, _ ->
                                        navController.navigate("appRosterImport/${Uri.encode(teamName)}")
                                    },
                                    onNavigateToScheduleImport = { teamName ->
                                        navController.navigate("scheduleImport/${Uri.encode(teamName)}")
                                    }
                                )
                            }
                            composable("clips") {
                                ClipsScreenRefactored(
                                    teamViewModel = teamViewModel,
                                    cameraHandoffToken = cameraHandoffToken,
                                    onNavigateToTeams = { navController.navigate("teams") }
                                )
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
                                route = "scheduleImport/{teamName}",
                                arguments = listOf(navArgument("teamName") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val teamName = backStackEntry.arguments?.getString("teamName") ?: ""
                                ScheduleImportScreen(
                                    teamName = teamName,
                                    onBack = { navController.popBackStack() },
                                    onImport = { entries ->
                                        memoryIngestionViewModel.importSchedule(teamName, entries) {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                            composable("memoryBrowsing") {
                                MemoryBrowsingScreen(
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
                    SpotrBottomNavigationBar(
                        items = navItems,
                        selectedIndex = selectedIndex,
                        onItemSelected = { idx ->
                            navController.navigate(navItems[idx].route) {
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
            }
        }
    }
}

@Composable
private fun MoreScreen(
    onNavigateToTeams: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.more),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            MoreDestinationRow(
                icon = Icons.Filled.Group,
                title = stringResource(R.string.teams),
                subtitle = stringResource(R.string.manage_teams_and_rosters),
                onClick = onNavigateToTeams
            )

            MoreDestinationRow(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.account_referrals_and_settings),
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoreDestinationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.open_destination),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .width(30.dp)
                    .height(30.dp)
            )
        }
    }
}