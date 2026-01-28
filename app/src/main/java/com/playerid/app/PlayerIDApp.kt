package com.playerid.app

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.playerid.app.ui.screens.*
import com.playerid.app.ui.components.*
import com.playerid.app.viewmodels.*
import com.playerid.app.subscription.SubscriptionViewModel
import com.playerid.app.subscription.SubscriptionViewModelFactory
import com.playerid.app.data.teamsnap.TeamSnapRepository
import com.playerid.app.data.PlayerDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerIDApp() {
    android.util.Log.i("PlayerIDApp", "🎯 PlayerIDApp composable starting")
    val context = LocalContext.current
    val navController = rememberNavController()
    android.util.Log.d("PlayerIDApp", "📱 Creating ViewModels")
    val playerViewModel: PlayerViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel(
        factory = TeamViewModelFactory(context.applicationContext as Application)
    )
    val authViewModel: AuthViewModel = viewModel()
    val subscriptionViewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModelFactory(context)
    )
    
    val database = PlayerDatabase.getDatabase(context)
    val teamSnapRepository = remember {
        TeamSnapRepository(context, database.playerDao())
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val isPaywallVisible by subscriptionViewModel.isPaywallVisible.collectAsState()

    Scaffold(
        bottomBar = {
            val navItems = listOf(
                BottomNavItem("Camera", Icons.Default.PhotoCamera, "camera"),
                BottomNavItem("Validate", Icons.Default.CloudDownload, "validate"),
                BottomNavItem("My Team", Icons.Default.Groups, "team"),
                BottomNavItem("Settings", Icons.Default.Settings, "settings")
            )
            
            SpotrBottomNavigationBar(
                items = navItems,
                selectedIndex = selectedTab,
                onItemSelected = { index ->
                    selectedTab = index
                    navController.navigate(navItems[index].route)
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "camera",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("camera") {
                android.util.Log.i("PlayerIDApp", "🎬 Navigating to CameraScreen")
                CameraScreen(
                    viewModel = playerViewModel,
                    teamViewModel = teamViewModel,
                    onVideoSaved = { videoUri ->
                        navController.navigate("video_editor/${Uri.encode(videoUri.toString())}")
                    }
                )
            }
            composable("validate") {
                JerseyValidationScreen()
            }
            composable("video_import") {
                VideoImportScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onVideoSelected = { videoUri ->
                        navController.navigate("video_editor/${Uri.encode(videoUri.toString())}")
                    }
                )
            }
            composable("video_editor/{videoUri}") { backStackEntry ->
                val videoUriString = backStackEntry.arguments?.getString("videoUri")
                if (videoUriString != null) {
                    VideoEditorScreen(
                        videoUri = Uri.parse(videoUriString),
                        roster = playerViewModel.allPlayers.collectAsState(initial = emptyList()).value,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onSaveVideo = { nameBubbles ->
                            navController.navigate("video_export")
                        }
                    )
                }
            }
            composable("team") {
                TeamScreen(
                    teamViewModel = teamViewModel,
                    playerViewModel = playerViewModel,
                    teamSnapRepository = teamSnapRepository,
                    onNavigateToCrowdSourced = {
                        navController.navigate("crowd_sourced_teams")
                    }
                )
            }
            composable("crowd_sourced_teams") {
                CrowdSourcedTeamsScreen(
                    teamViewModel = teamViewModel,
                    onTeamSelected = { teamName ->
                        teamViewModel.selectTeam(teamName)
                        playerViewModel.setSelectedTeam(teamName)
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("referral") {
                ReferralScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    teamViewModel = teamViewModel,
                    playerViewModel = playerViewModel
                )
            }
        }
    }
    
    SubscriptionBanner(
        subscriptionViewModel = subscriptionViewModel,
        onUpgrade = { subscriptionViewModel.showPaywall() }
    )
    
    if (isPaywallVisible) {
        PaywallScreen(
            subscriptionViewModel = subscriptionViewModel,
            onDismiss = { subscriptionViewModel.hidePaywall() }
        )
    }
}