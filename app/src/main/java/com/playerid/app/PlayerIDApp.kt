package com.playerid.app
import com.playerid.app.ui.screens.CameraScreen
import com.playerid.app.ui.screens.JerseyValidationScreen
import com.playerid.app.ui.screens.VideoEditorScreen
import com.playerid.app.ui.screens.TeamScreen
import com.playerid.app.ui.screens.MyTeamScreen
import com.playerid.app.ui.screens.CrowdSourcedTeamsScreen
import com.playerid.app.ui.screens.ReferralScreen
import com.playerid.app.ui.screens.SettingsScreen
import com.playerid.app.ui.screens.VoiceResultCard
import com.playerid.app.ui.screens.PaywallScreen
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// ...existing code...
import com.playerid.app.ui.components.*
import com.playerid.app.viewmodels.*
import com.playerid.app.subscription.SubscriptionViewModel
import com.playerid.app.subscription.SubscriptionViewModelFactory
import com.playerid.app.data.teamsnap.TeamSnapRepository
import com.playerid.app.data.PlayerDatabase
// Removed duplicate non-@Composable PlayerIDApp function
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerIDApp() {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModelFactory(context.applicationContext as Application)
    )
    val teamViewModel: TeamViewModel = viewModel(
        factory = TeamViewModelFactory(context.applicationContext as Application)
    )
    val navController = rememberNavController()
    // Removed invalid Scaffold usage with factory parameter
    val subscriptionViewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModelFactory(context)
    )
    
    val database = PlayerDatabase.getDatabase(context)
    val teamSnapRepository = remember {
        TeamSnapRepository(context, database.playerDao())
    }
    
    val isPaywallVisible by subscriptionViewModel.isPaywallVisible.collectAsState()
    val voiceResult by playerViewModel.voiceResult.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            data?.firstOrNull()?.let { spokenText ->
                playerViewModel.processVoiceCommand(spokenText)
            }
        }
    }

    Scaffold(
        bottomBar = {
            val navItems = listOf(
                BottomNavItem("Camera", Icons.Default.PhotoCamera, "camera"),
                BottomNavItem("Validate", Icons.Default.CloudDownload, "validate"),
                BottomNavItem("My Team", Icons.Default.Groups, "team"),
                BottomNavItem("Settings", Icons.Default.Settings, "settings")
            )
            val selectedIndex = navItems.indexOfFirst { item ->
                currentRoute?.startsWith(item.route) == true
            }.coerceAtLeast(0)
            SpotrBottomNavigationBar(
                items = navItems,
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    navController.navigate(navItems[index].route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Identify a player or say 'Capture'...")
                    }
                    speechLauncher.launch(intent)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Mic, "Voice Assistant")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = "camera"
            ) {
                composable("camera") {
                    CameraScreen(
                        viewModel = playerViewModel,
                        teamViewModel = teamViewModel,
                        onVideoSaved = { videoUri ->
                            val encodedUri = Uri.encode(videoUri.toString())
                            navController.navigate("video_editor?videoUri=$encodedUri")
                        }
                    )
                }
                composable("validate") {
                    JerseyValidationScreen()
                }
                composable(
                    route = "video_editor?videoUri={videoUri}",
                    arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val videoUriString = backStackEntry.arguments?.getString("videoUri")
                    if (videoUriString != null) {
                        val rosterState = playerViewModel.allPlayers.collectAsState(initial = emptyList())
                        VideoEditorScreen(
                            videoUri = Uri.parse(Uri.decode(videoUriString)),
                            roster = rosterState.value,
                            onNavigateBack = { navController.popBackStack() },
                            onSaveVideo = { navController.navigate("camera") }
                        )
                    }
                }
                composable("team") {
                    val teamName = teamViewModel.selectedTeam.collectAsState().value ?: ""
                    MyTeamScreen(
                        teamName = teamName,
                        rosterId = "", // Not available, pass empty string
                        videoClips = emptyList(), // Not available, pass empty list
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        onInviteSent = { /* TODO: handle invite sent */ }
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
            voiceResult?.let { result ->
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                    VoiceResultCard(
                        result = result,
                        onDismiss = { playerViewModel.clearVoiceResult() }
                    )
                }
            }
        }
    }
    if (isPaywallVisible) {
        PaywallScreen(
            subscriptionViewModel = subscriptionViewModel,
            onDismiss = { subscriptionViewModel.hidePaywall() }
        )
    }
}