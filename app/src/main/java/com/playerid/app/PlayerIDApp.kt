
package com.playerid.app

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.playerid.app.ui.components.BottomNavItem
import com.playerid.app.ui.components.SpotrBottomNavigationBar
import com.playerid.app.ui.screens.CameraScreen
import com.playerid.app.ui.screens.TeamScreen
import com.playerid.app.ui.screens.ReferralScreen
import com.playerid.app.ui.screens.SettingsScreen

val LocalPlayerViewModel = compositionLocalOf<PlayerViewModel> { error("No PlayerViewModel provided") }
val LocalTeamViewModel = compositionLocalOf<TeamViewModel> { error("No TeamViewModel provided") }
val LocalAppContext = compositionLocalOf<Context> { error("No Context provided") }

@Composable
fun PlayerIDApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "camera"

    val navItems = listOf(
        BottomNavItem("Camera", Icons.Default.CameraAlt, "camera"),
        BottomNavItem("Teams", Icons.Default.Group, "teams"),
        BottomNavItem("Referral", Icons.Default.PersonAdd, "referral"),
        BottomNavItem("Settings", Icons.Default.Settings, "settings")
    )
    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    CompositionLocalProvider(
        LocalPlayerViewModel provides playerViewModel,
        LocalTeamViewModel provides teamViewModel,
        LocalAppContext provides playerViewModel.getApplication()
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    NavHost(navController = navController, startDestination = "camera") {
                        composable("camera") {
                            CameraScreen(
                                viewModel = playerViewModel,
                                teamViewModel = teamViewModel,
                                showVoiceId = true,
                                isVoiceListening = false,
                                onVoiceIdToggle = {},
                                onVideoSaved = { _, _ -> },
                                onNavigateToVideoLibrary = {}
                            )
                        }
                        composable("teams") {
                            TeamScreen(
                                teamViewModel = teamViewModel,
                                playerViewModel = playerViewModel
                            )
                        }
                        composable("referral") {
                            ReferralScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(
                                teamViewModel = teamViewModel,
                                playerViewModel = playerViewModel
                            )
                        }
                    }
                }
                SpotrBottomNavigationBar(
                    items = navItems,
                    selectedIndex = selectedIndex,
                    onItemSelected = { idx ->
                        val route = navItems[idx].route
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}