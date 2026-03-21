package com.playerid.app
import android.net.Uri
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier // Keep only one import
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import com.playerid.app.ui.screens.PlayerIDVoiceScreen
import com.playerid.app.ui.screens.WebRosterImportScreen
import com.playerid.app.ui.screens.AppRosterImportScreen
import com.playerid.app.ui.screens.ClipsScreen
import androidx.compose.runtime.remember

val LocalPlayerViewModel = compositionLocalOf<PlayerViewModel> { error("No PlayerViewModel provided") }
val LocalTeamViewModel = compositionLocalOf<TeamViewModel> { error("No TeamViewModel provided") }
val LocalAppContext = compositionLocalOf<Context> { error("No Context provided") }

@Composable
fun PlayerIDApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel()
    val selectedTeamByPlayerVm by playerViewModel.selectedTeam.collectAsState()
    val selectedTeamByTeamVm by teamViewModel.selectedTeam.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "camera"
    val navItems = listOf(
        BottomNavItem("Camera", Icons.Filled.CameraAlt, "camera"),
        BottomNavItem("Player ID", Icons.Filled.Mic, "playeridvoice"),
        BottomNavItem("Clips", Icons.Filled.PlayArrow, "clips"),
        BottomNavItem("Teams", Icons.Filled.Group, "teams"),
        BottomNavItem("Settings", Icons.Filled.Settings, "settings")
    )
    val selectedIndex = navItems.indexOfFirst { routeItem ->
        currentRoute == routeItem.route || currentRoute.startsWith("${routeItem.route}/")
    }.coerceAtLeast(0)

    LaunchedEffect(selectedTeamByTeamVm, selectedTeamByPlayerVm) {
        when {
            !selectedTeamByTeamVm.isNullOrBlank() && selectedTeamByPlayerVm != selectedTeamByTeamVm -> {
                playerViewModel.setSelectedTeam(selectedTeamByTeamVm)
            }
            selectedTeamByTeamVm.isNullOrBlank() && !selectedTeamByPlayerVm.isNullOrBlank() -> {
                val fallbackTeam = selectedTeamByPlayerVm ?: return@LaunchedEffect
                teamViewModel.selectTeam(fallbackTeam)
            }
        }
    }

    CompositionLocalProvider(
        LocalPlayerViewModel provides playerViewModel,
        LocalAppContext provides playerViewModel.getApplication()
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
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
                                onNavigateToClips = { navController.navigate("clips") }
                            )
                        }
                        composable("playeridvoice") {
                            PlayerIDVoiceScreen(
                                viewModel = playerViewModel,
                                teamViewModel = teamViewModel
                            )
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
                                }
                            )
                        }
                        composable("clips") {
                            ClipsScreen(
                                playerViewModel = playerViewModel,
                                teamViewModel = teamViewModel,
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
                        composable("referral") {
                            ReferralScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(
                                teamViewModel = teamViewModel,
                                playerViewModel = playerViewModel,
                                onNavigateToReferral = { navController.navigate("referral") }
                            )
                        }
                    }
                }
                SpotrBottomNavigationBar(
                    items = navItems,
                    selectedIndex = selectedIndex,
                    onItemSelected = { idx ->
                        val route = navItems[idx].route
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
        }
    }
}