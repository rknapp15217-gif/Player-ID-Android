package com.playerid.app

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.text.font.FontWeight
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
import com.playerid.app.data.Player
import com.playerid.app.data.VideoClip
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
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
    
    // Listen for navigation callbacks from external sources
    val navRoute by AppNavigationCallback.route.collectAsState()
    LaunchedEffect(navRoute) {
        navRoute?.let { route ->
            navController.navigate(route)
            AppNavigationCallback.clear()
        }
    }
    
    // Listen for TeamSnap OAuth redirect
    val oauthRedirect by com.playerid.app.data.teamsnap.TeamSnapAuthCallback.redirectUri.collectAsState()
    LaunchedEffect(oauthRedirect) {
        oauthRedirect?.let { uri ->
            teamSnapRepository.handleAuthRedirect(uri)
            com.playerid.app.data.teamsnap.TeamSnapAuthCallback.clear()
        }
    }
    
    val isPaywallVisible by subscriptionViewModel.isPaywallVisible.collectAsState()
    val voiceResult by playerViewModel.voiceResult.collectAsState()
    val isListening by playerViewModel.isListening.collectAsState()
    val selectedTeam by playerViewModel.selectedTeam.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var teamMenuExpanded by remember { mutableStateOf(false) }
    val teamMenuWidth by animateDpAsState(
        targetValue = if (teamMenuExpanded) 280.dp else 180.dp,
        label = "teamMenuWidth"
    )

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
                
                // IGNORE ERROR_CLIENT (5) which triggers on manual cancel, 
                // and typical timeouts/no-matches.
                if (error != SpeechRecognizer.ERROR_NO_MATCH && 
                    error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                    error != SpeechRecognizer.ERROR_CLIENT) {
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

    val navItems = listOf(
        BottomNavItem("Camera", Icons.Default.PhotoCamera, "camera"),
        BottomNavItem("Validate", Icons.Default.CloudDownload, "validate"),
        BottomNavItem("My Team", Icons.Default.Groups, "team"),
        BottomNavItem("Settings", Icons.Default.Settings, "settings")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .height(IntrinsicSize.Min)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                navItems.forEach { item ->
                    val isSelected = currentRoute?.startsWith(item.route) == true
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "PlayerID")
                            Spacer(modifier = Modifier.width(12.dp))
                            ExposedDropdownMenuBox(
                                expanded = teamMenuExpanded,
                                onExpandedChange = { teamMenuExpanded = !teamMenuExpanded }
                            ) {
                                TextButton(
                                    onClick = { teamMenuExpanded = true },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .width(teamMenuWidth)
                                ) {
                                    val isTeamSelected = selectedTeam != null
                                    val labelText = selectedTeam ?: "Select team"
                                    val labelColor = if (isTeamSelected) {
                                        MaterialTheme.colorScheme.onBackground
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    val labelWeight = if (isTeamSelected) FontWeight.Medium else FontWeight.SemiBold

                                    Text(
                                        text = labelText,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = labelColor,
                                        fontWeight = labelWeight
                                    )
                                    if (!isTeamSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "Required",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                ExposedDropdownMenu(
                                    expanded = teamMenuExpanded,
                                    onDismissRequest = { teamMenuExpanded = false },
                                    modifier = Modifier.width(teamMenuWidth)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                        onClick = {
                                            teamMenuExpanded = false
                                            playerViewModel.setSelectedTeam(null)
                                            teamViewModel.clearTeamSelection()
                                        }
                                    )
                                    if (subscribedTeams.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No teams", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                            onClick = { teamMenuExpanded = false },
                                            enabled = false
                                        )
                                    } else {
                                        subscribedTeams.forEach { team ->
                                            DropdownMenuItem(
                                                text = { Text(team.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                                onClick = {
                                                    teamMenuExpanded = false
                                                    playerViewModel.setSelectedTeam(team.name)
                                                    teamViewModel.selectTeam(team.name)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {}
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
                            showVoiceId = playerViewModel.selectedTeam.collectAsState().value != null,
                            isVoiceListening = playerViewModel.isListening.collectAsState().value,
                            onVoiceIdToggle = {
                                val isListeningCurrent = playerViewModel.isListening.value
                                if (!isListeningCurrent) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        startListening()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    cancelListening()
                                }
                            },
                            onVideoSaved = { videoUri ->
                                val encodedUri = Uri.encode(videoUri.toString())
                                navController.navigate("post_recording?videoUri=$encodedUri") {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToVideoLibrary = {
                                val selectedTeam = playerViewModel.selectedTeam.value
                                if (selectedTeam != null) {
                                    val encodedTeamName = Uri.encode(selectedTeam)
                                    navController.navigate("video_library/$encodedTeamName")
                                }
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
                                onNavigateBack = { navController.popBackStack("camera", false) },
                                onSaveVideo = { navController.navigate("camera") }
                            )
                        }
                    }
                    composable(
                        route = "post_recording?videoUri={videoUri}",
                        arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val videoUriString = backStackEntry.arguments?.getString("videoUri")
                        if (videoUriString != null) {
                            PostRecordingScreen(
                                videoUri = Uri.parse(Uri.decode(videoUriString)),
                                onEdit = { 
                                    navController.navigate("video_editor?videoUri=${backStackEntry.arguments?.getString("videoUri")}") {
                                        popUpTo("post_recording") { inclusive = true }
                                    }
                                },
                                onSaveToLibrary = { 
                                    // TODO: Save video to library database
                                    navController.navigate("camera") {
                                        popUpTo("post_recording") { inclusive = true }
                                    }
                                },
                                onDiscard = { 
                                    navController.navigate("camera") {
                                        popUpTo("post_recording") { inclusive = true }
                                    }
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
                            },
                            onNavigateToWebImport = { teamName ->
                                val encodedTeamName = Uri.encode(teamName)
                                navController.navigate("web_roster_import/$encodedTeamName")
                            },
                            onNavigateToAppImport = { teamName, _ ->
                                val encodedTeamName = Uri.encode(teamName)
                                navController.navigate("app_roster_import/$encodedTeamName")
                            },
                            onNavigateToVideoLibrary = { teamName ->
                                val encodedTeamName = Uri.encode(teamName)
                                navController.navigate("video_library/$encodedTeamName")
                            }
                        )
                    }
                    composable(
                        route = "app_roster_import/{teamName}",
                        arguments = listOf(navArgument("teamName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val teamName = backStackEntry.arguments?.getString("teamName")?.let { Uri.decode(it) }
                        if (teamName != null) {
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
                    }
                    composable(
                        route = "web_roster_import/{teamName}",
                        arguments = listOf(navArgument("teamName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val teamName = backStackEntry.arguments?.getString("teamName")?.let { Uri.decode(it) }
                        if (teamName != null) {
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
                    }
                    composable("video_library/{teamName}",
                        arguments = listOf(navArgument("teamName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val teamName = backStackEntry.arguments?.getString("teamName")?.let { Uri.decode(it) }
                        if (teamName != null) {
                            val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
                            val rosterPlayers = remember(allPlayers, teamName) {
                                allPlayers.filter { it.team == teamName }
                            }
                            val scope = rememberCoroutineScope()
                            var isLoading by remember(teamName) { mutableStateOf(true) }
                            var videos by remember(teamName) { mutableStateOf(emptyList<VideoClip>()) }
                            var refreshKey by remember(teamName) { mutableStateOf(0) }
                            var lastRefreshedLabel by remember(teamName) { mutableStateOf("") }
                            var lastRefreshedAtMillis by remember(teamName) { mutableStateOf(0L) }
                            LaunchedEffect(teamName, refreshKey) {
                                isLoading = true
                                videos = loadRecordedVideos(context)
                                isLoading = false
                                val now = java.util.Date()
                                val nowMillis = now.time
                                lastRefreshedAtMillis = nowMillis
                                lastRefreshedLabel = formatLastRefreshedLabel(lastRefreshedAtMillis)
                            }
                            LaunchedEffect(teamName, lastRefreshedAtMillis) {
                                if (lastRefreshedAtMillis == 0L) return@LaunchedEffect
                                while (true) {
                                    val updatedLabel = formatLastRefreshedLabel(lastRefreshedAtMillis)
                                    if (updatedLabel != lastRefreshedLabel) {
                                        lastRefreshedLabel = updatedLabel
                                    }
                                    delay(60_000L)
                                }
                            }
                            
                            VideoLibraryScreen(
                                teamName = teamName,
                                videos = videos,
                                rosterPlayers = rosterPlayers,
                                isLoading = isLoading,
                                lastRefreshedLabel = lastRefreshedLabel,
                                onNavigateBack = { navController.popBackStack() },
                                onRefresh = { refreshKey += 1 },
                                onVideoSelected = { videoUri, players ->
                                    val encodedUri = Uri.encode(videoUri.toString())
                                    navController.navigate("video_playback/$encodedUri") {
                                        launchSingleTop = true
                                    }
                                },
                                onVideoEdit = { videoUri ->
                                    val encodedUri = Uri.encode(videoUri.toString())
                                    navController.navigate("video_editor?videoUri=$encodedUri") {
                                        launchSingleTop = true
                                    }
                                },
                                onVideoDelete = { video ->
                                    val videoUri = Uri.parse(video.filePath)
                                    videos = videos.filterNot { it.id == video.id }
                                    scope.launch {
                                        deleteRecordedVideo(context, videoUri)
                                        refreshKey += 1
                                    }
                                }
                            )
                        }
                    }
                    composable("video_playback/{videoUri}",
                        arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val videoUriString = backStackEntry.arguments?.getString("videoUri")?.let { Uri.decode(it) }
                        if (videoUriString != null) {
                            val videoUri = Uri.parse(videoUriString)
                            // TODO: Get detected players from video metadata
                            val detectedPlayers = emptyList<Player>()
                            
                            VideoPlaybackScreen(
                                videoUri = videoUri,
                                detectedPlayers = detectedPlayers,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
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
                    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 0.25f,
                        targetValue = 0.75f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
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
                                    Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "Speak Now",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
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
    }
    
    if (isPaywallVisible) {
        PaywallScreen(
            subscriptionViewModel = subscriptionViewModel,
            onDismiss = { subscriptionViewModel.hidePaywall() }
        )
    }
}

private suspend fun loadRecordedVideos(context: Context): List<VideoClip> = withContext(Dispatchers.IO) {
    val videos = mutableListOf<VideoClip>()
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.RELATIVE_PATH
    )
    val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("Movies/PlayerID%")
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

    context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val displayName = cursor.getString(nameColumn) ?: "Game Video"
            val dateAddedSeconds = cursor.getLong(dateColumn)
            val duration = cursor.getLong(durationColumn)
            val contentUri = ContentUris.withAppendedId(collection, id)

            val gameDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(dateAddedSeconds * 1000))

            videos.add(
                VideoClip(
                    id = id.toString(),
                    filePath = contentUri.toString(),
                    duration = duration,
                    createdAt = dateAddedSeconds * 1000,
                    gameDate = gameDate,
                    gameTitle = displayName.substringBeforeLast(".")
                )
            )
        }
    }

    videos
}

private suspend fun deleteRecordedVideo(context: Context, videoUri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        val deleted = context.contentResolver.delete(videoUri, null, null)
        deleted > 0
    } catch (_: Exception) {
        false
    }
}

private fun formatLastRefreshedLabel(lastRefreshedAtMillis: Long): String {
    if (lastRefreshedAtMillis == 0L) {
        return ""
    }

    val nowMillis = System.currentTimeMillis()
    val dayMillis = 24L * 60L * 60L * 1000L
    val format = if (nowMillis - lastRefreshedAtMillis >= dayMillis) {
        "MMM d, h:mm a"
    } else {
        "h:mm a"
    }
    val formatter = SimpleDateFormat(format, Locale.getDefault())
    return "Last refreshed: " + formatter.format(Date(lastRefreshedAtMillis))
}