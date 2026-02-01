package com.playerid.app

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.playerid.app.ui.screens.*
import com.playerid.app.ui.components.*
import com.playerid.app.viewmodels.*
import com.playerid.app.subscription.SubscriptionViewModel
import com.playerid.app.subscription.SubscriptionViewModelFactory
import com.playerid.app.data.teamsnap.TeamSnapRepository
import com.playerid.app.data.PlayerDatabase
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerIDApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    val playerViewModel: PlayerViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel(
        factory = TeamViewModelFactory(context.applicationContext as Application)
    )
    val subscriptionViewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModelFactory(context)
    )
    
    val database = PlayerDatabase.getDatabase(context)
    val teamSnapRepository = remember {
        TeamSnapRepository(context, database.playerDao())
    }
    
    val isPaywallVisible by subscriptionViewModel.isPaywallVisible.collectAsState()
    val voiceResult by playerViewModel.voiceResult.collectAsState()
    val isListening by playerViewModel.isListening.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Direct SpeechRecognizer API
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                playerViewModel.setListening(true)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                playerViewModel.setListening(false)
            }
            override fun onError(error: Int) {
                Log.e("VoiceAssistant", "Recognition Error: $error")
                playerViewModel.setListening(false)
                // Ensure session is cleared on error so recording resumes
                playerViewModel.clearVoiceResult()
                
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    playerViewModel.reportVoiceError("Speech recognition failed ($error).")
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { spokenText ->
                    playerViewModel.processVoiceCommand(spokenText)
                } ?: playerViewModel.clearVoiceResult()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    LaunchedEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(recognitionListener)
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer.destroy() }
    }

    val startListening = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
        }
        playerViewModel.stopRecordingForVoice()
        speechRecognizer.startListening(intent)
    }

    val cancelListening = {
        speechRecognizer.cancel()
        playerViewModel.setListening(false)
        playerViewModel.clearVoiceResult()
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
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
                    if (!isListening) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startListening()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        cancelListening()
                    }
                },
                containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isListening) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                if (isListening) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Mic, "Voice Assistant")
                }
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
                        VideoEditorScreen(
                            videoUri = Uri.parse(Uri.decode(videoUriString)),
                            roster = playerViewModel.allPlayers.collectAsState(initial = emptyList()).value,
                            onNavigateBack = { navController.popBackStack() },
                            onSaveVideo = { navController.navigate("camera") }
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
                composable("settings") {
                    SettingsScreen(
                        teamViewModel = teamViewModel,
                        playerViewModel = playerViewModel
                    )
                }
            }

            // Prominent Listening Overlay
            if (isListening) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // CANCEL when tapping outside the card
                            cancelListening()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.clickable(enabled = false) {}, // Don't cancel when clicking the card itself
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Mic, 
                                contentDescription = null, 
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Speak Now", style = MaterialTheme.typography.headlineSmall)
                            Text("Identifying player...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }

            // Result Overlay
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