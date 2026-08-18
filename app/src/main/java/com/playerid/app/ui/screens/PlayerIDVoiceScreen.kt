
package com.playerid.app.ui.screens
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.playerid.app.BuildConfig
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerIDVoiceScreen(
    viewModel: PlayerViewModel,
    teamViewModel: TeamViewModel,
    cameraHandoffToken: Int = 0
) {
    val localContext = LocalContext.current
    val localView = LocalView.current
    val permissionState = remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                localContext,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingMicTap by rememberSaveable { mutableStateOf(false) }
    var listenAttempts by rememberSaveable { mutableIntStateOf(0) }
    var resultWindowsShown by rememberSaveable { mutableIntStateOf(0) }
    val teams by teamViewModel.subscribedTeams.collectAsState()
    val allPlayers by viewModel.allPlayers.collectAsState(initial = emptyList())
    val cameraTeam by teamViewModel.selectedTeam.collectAsState()
    // Local state per screen; Camera only hands off on explicit camera navigation exits.
    var localSelectedTeam by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(cameraHandoffToken) {
        localSelectedTeam = cameraTeam
    }
    
    // SpeechRecognizer integration setup
    val recognitionIntent = remember {
        android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Ask for a player number")
        }
    }
    var isSpeechActive by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<android.speech.SpeechRecognizer?>(null) }
    
    fun startListening() {
        listenAttempts += 1
        if (BuildConfig.DEBUG) {
            android.util.Log.i(
                "PlayerIDVoiceScreen",
                "startListening attempt #$listenAttempts"
            )
        }
        // Fire tactile feedback from the guaranteed listening entrypoint.
        try {
            localView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = localContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                localContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(32L, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(32L)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerIDVoiceScreen", "startListening haptic failed", e)
        }
        viewModel.clearVoiceResult()
        if (speechRecognizer == null || !android.speech.SpeechRecognizer.isRecognitionAvailable(localContext)) {
            viewModel.setListening(false)
            isSpeechActive = false
            viewModel.reportVoiceError("Speech recognition is not available on this device.")
            return
        }
        try {
            speechRecognizer?.startListening(recognitionIntent)
            viewModel.setListening(true)
            isSpeechActive = true
        } catch (e: Exception) {
            isSpeechActive = false
            viewModel.setListening(false)
            viewModel.reportVoiceError("Could not start listening. Please try again.")
            android.util.Log.e("PlayerIDVoiceScreen", "startListening failed", e)
        }
    }
    
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            permissionState.value = granted
            if (granted && pendingMicTap) {
                pendingMicTap = false
                startListening()
            } else {
                pendingMicTap = false
            }
        }
    
    DisposableEffect(Unit) {
        if (speechRecognizer == null && android.speech.SpeechRecognizer.isRecognitionAvailable(localContext)) {
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(localContext)
        }

        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle) {
                android.util.Log.d("PlayerIDVoiceScreen", "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                android.util.Log.d("PlayerIDVoiceScreen", "onBeginningOfSpeech")
                isSpeechActive = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                android.util.Log.d("PlayerIDVoiceScreen", "onRmsChanged: $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray) {
                android.util.Log.d("PlayerIDVoiceScreen", "onBufferReceived")
            }

            override fun onEndOfSpeech() {
                android.util.Log.d("PlayerIDVoiceScreen", "onEndOfSpeech")
                isSpeechActive = false
                viewModel.setListening(false)
            }

            override fun onError(error: Int) {
                android.util.Log.d("PlayerIDVoiceScreen", "onError: $error")
                isSpeechActive = false
                viewModel.setListening(false)
                val message = when (error) {
                    android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't match that to a player. Try again."
                    android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Try tapping and speaking again."
                    android.speech.SpeechRecognizer.ERROR_NETWORK,
                    android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue while listening. Please try again."
                    android.speech.SpeechRecognizer.ERROR_AUDIO,
                    android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    android.speech.SpeechRecognizer.ERROR_SERVER -> "Speech recognition failed. Please try again."
                    android.speech.SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted. Please try again."
                    else -> "Could not process speech. Please try again."
                }
                viewModel.reportVoiceError(message)
            }

            override fun onResults(p0: Bundle) {
                android.util.Log.d("PlayerIDVoiceScreen", "onResults: $p0")
                isSpeechActive = false
                viewModel.setListening(false)
                val results =
                    p0.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!results.isNullOrEmpty()) {
                    val numberRegex = Regex("\\b\\d{1,3}\\b")
                    // Prefer full phrase with 'number', else just the first number
                    val bestPhrase = results.find { it.contains("number", ignoreCase = true) }
                        ?: results.find { numberRegex.containsMatchIn(it) } ?: results.first()
                    val match = numberRegex.find(bestPhrase)
                    val phraseToSend = match?.value ?: bestPhrase
                    val orderedHypotheses = buildList {
                        add(phraseToSend)
                        addAll(results)
                    }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
                    android.util.Log.d(
                        "PlayerIDVoiceScreen",
                        "processVoiceCommandHypotheses (best): $phraseToSend from '$bestPhrase' total=${orderedHypotheses.size}"
                    )
                    viewModel.processVoiceCommandHypotheses(
                        spokenTexts = orderedHypotheses,
                        selectedTeamOverride = localSelectedTeam,
                        onTeamSwitched = { switchedTeam ->
                            localSelectedTeam = switchedTeam
                        }
                    )
                } else {
                    viewModel.reportVoiceError("I didn't catch a player number. Try again.")
                }
            }

            override fun onPartialResults(p0: Bundle) {
                android.util.Log.d("PlayerIDVoiceScreen", "onPartialResults: $p0")
                // Do not call processVoiceCommand for partials, only log for debugging
                val partials =
                    p0.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partials.isNullOrEmpty()) {
                    val numberRegex = Regex("\\b\\d{1,3}\\b")
                    val bestPhrase = partials.find { it.contains("number", ignoreCase = true) }
                        ?: partials.find { numberRegex.containsMatchIn(it) } ?: partials.first()
                    android.util.Log.d("PlayerIDVoiceScreen", "partial best: $bestPhrase")
                }
            }

            override fun onEvent(p0: Int, p1: Bundle) {
                android.util.Log.d("PlayerIDVoiceScreen", "onEvent: $p0 $p1")
            }
        }

        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    val isListening by viewModel.isListening.collectAsState()
    val voiceResult = viewModel.voiceResult.collectAsState().value
    val haptic = LocalHapticFeedback.current

    fun triggerMicTapFeedback() {
        android.util.Log.d("PlayerIDVoiceScreen", "triggerMicTapFeedback() called")
        try {
            localView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            android.util.Log.d("PlayerIDVoiceScreen", "View haptic feedback executed")
        } catch (e: Exception) {
            android.util.Log.e("PlayerIDVoiceScreen", "View haptic failed: ${e.message}")
        }
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            android.util.Log.d("PlayerIDVoiceScreen", "Compose haptic feedback executed")
        } catch (e: Exception) {
            android.util.Log.e("PlayerIDVoiceScreen", "Compose haptic failed: ${e.message}")
        }
        
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = localContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                localContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            android.util.Log.d("PlayerIDVoiceScreen", "Vibrator obtained: ${vibrator != null}")
            vibrator?.let {
                android.util.Log.d("PlayerIDVoiceScreen", "Vibrator hasVibrator: ${it.hasVibrator()}")
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(24L, VibrationEffect.DEFAULT_AMPLITUDE))
                        android.util.Log.d("PlayerIDVoiceScreen", "Vibrator API 26+ vibrate called")
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(24L)
                        android.util.Log.d("PlayerIDVoiceScreen", "Vibrator deprecated vibrate called")
                    }
                } else {
                    android.util.Log.d("PlayerIDVoiceScreen", "Device reports no vibrator support")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerIDVoiceScreen", "Vibrator failed: ${e.message}", e)
        }
    }

    val debugMessage = remember { mutableStateOf("") }
    val pulse = remember { Animatable(1f) }
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 700
    val horizontalPadding = if (isSmallScreen) 16.dp else 20.dp
    val verticalPadding = if (isSmallScreen) 16.dp else 24.dp
    val micButtonSize = if (isSmallScreen) 132.dp else 150.dp
    val micIconSize = if (isSmallScreen) 76.dp else 88.dp
    val cardVerticalPadding = if (isSmallScreen) 18.dp else 24.dp
    val titleStyle = if (isSmallScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall

    LaunchedEffect(isListening) {
        if (isListening) {
            while (true) {
                pulse.animateTo(1.2f, animationSpec = tween(600))
                pulse.animateTo(1f, animationSpec = tween(600))
            }
        } else {
            pulse.snapTo(1f)
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var showManualRoster by rememberSaveable { mutableStateOf(false) }
    var rosterQuery by rememberSaveable { mutableStateOf("") }
    var selectedPositionFilter by rememberSaveable { mutableStateOf("All Positions") }
    var selectedAcademicYearFilter by rememberSaveable { mutableStateOf("All Years") }
    var positionMenuExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    val selectedTeam = localSelectedTeam ?: ""
    val selectedTeamMeta = remember(teams, selectedTeam) {
        teams.firstOrNull { it.name == selectedTeam }
    }
    val teamPrimary = parsePlayerScreenColor(selectedTeamMeta?.color, Color(0xFF1976D2))
    val teamRoster = remember(allPlayers, selectedTeam) {
        allPlayers.filter { it.team == selectedTeam }
    }
    val normalizedRosterQuery = rosterQuery.trim()
    val availablePositions = remember(teamRoster, normalizedRosterQuery, selectedAcademicYearFilter) {
        teamRoster
            .asSequence()
            .filter { player ->
                val matchesQuery = normalizedRosterQuery.isEmpty() ||
                    player.name.contains(normalizedRosterQuery, ignoreCase = true) ||
                    player.number.contains(normalizedRosterQuery)
                val matchesYear = selectedAcademicYearFilter == "All Years" ||
                    player.academicYear.equals(selectedAcademicYearFilter, ignoreCase = true)
                matchesQuery && matchesYear
            }
            .map { it.position }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }
    val availableAcademicYears = remember(teamRoster, normalizedRosterQuery, selectedPositionFilter) {
        teamRoster
            .asSequence()
            .filter { player ->
                val matchesQuery = normalizedRosterQuery.isEmpty() ||
                    player.name.contains(normalizedRosterQuery, ignoreCase = true) ||
                    player.number.contains(normalizedRosterQuery)
                val matchesPosition = selectedPositionFilter == "All Positions" ||
                    player.position.equals(selectedPositionFilter, ignoreCase = true)
                matchesQuery && matchesPosition
            }
            .map { it.academicYear }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }
    val filteredRoster = remember(
        teamRoster,
        normalizedRosterQuery,
        selectedPositionFilter,
        selectedAcademicYearFilter
    ) {
        teamRoster.filter { player ->
            val matchesQuery = normalizedRosterQuery.isEmpty() ||
                player.name.contains(normalizedRosterQuery, ignoreCase = true) ||
                player.number.contains(normalizedRosterQuery)
            val matchesPosition = selectedPositionFilter == "All Positions" ||
                player.position.equals(selectedPositionFilter, ignoreCase = true)
            val matchesYear = selectedAcademicYearFilter == "All Years" ||
                player.academicYear.equals(selectedAcademicYearFilter, ignoreCase = true)
            matchesQuery && matchesPosition && matchesYear
        }.sortedWith(
            compareBy<com.playerid.app.data.Player> { it.number.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.number }
                .thenBy { it.name }
        )
    }

    LaunchedEffect(availablePositions, selectedPositionFilter) {
        if (selectedPositionFilter != "All Positions" && selectedPositionFilter !in availablePositions) {
            selectedPositionFilter = "All Positions"
        }
    }
    LaunchedEffect(availableAcademicYears, selectedAcademicYearFilter) {
        if (selectedAcademicYearFilter != "All Years" && selectedAcademicYearFilter !in availableAcademicYears) {
            selectedAcademicYearFilter = "All Years"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Team: ${selectedTeam.ifBlank { "Select" }}")
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        teams.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name) },
                                onClick = {
                                    localSelectedTeam = team.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF006064))),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    Column(
                        modifier = Modifier.padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isListening) Color(0xFFFF6F00).copy(alpha = 0.85f) else Color(0xFFFF6F00),
                            tonalElevation = 10.dp,
                            shadowElevation = 14.dp,
                            modifier = Modifier
                                .size(micButtonSize)
                                .clickable(enabled = !isListening) {
                                    android.util.Log.d("PlayerIDVoiceScreen", "Mic button clicked")
                                    if (!permissionState.value) {
                                        android.util.Log.d("PlayerIDVoiceScreen", "Permission not granted, launching permission request")
                                        pendingMicTap = true
                                        launcher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        android.util.Log.d("PlayerIDVoiceScreen", "Permission granted, calling startListening")
                                        pendingMicTap = false
                                        if (BuildConfig.DEBUG) {
                                            android.util.Log.d(
                                                "PlayerIDVoiceScreen",
                                                "Mic tapped, starting listening"
                                            )
                                        }
                                        startListening()
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = "Voice Player ID",
                                    modifier = Modifier.size(micIconSize),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isListening) "Listening..." else "Tap to ID Player",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isListening) {
                            Text(
                                text = "Try: \"Number 12\" or \"Who is 7?\"",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showManualRoster = !showManualRoster },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (showManualRoster) "Hide" else "Browse the Lineup")
                            Icon(
                                imageVector = if (showManualRoster) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    if (showManualRoster) {
                        if (selectedTeam.isBlank()) {
                            Text(
                                text = "Select a team first to browse roster.",
                                color = Color(0xFF607D8B),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            OutlinedTextField(
                                value = rosterQuery,
                                onValueChange = { rosterQuery = it },
                                label = { Text("Search number or name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { positionMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = selectedPositionFilter,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = positionMenuExpanded,
                                        onDismissRequest = { positionMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Positions") },
                                            onClick = {
                                                selectedPositionFilter = "All Positions"
                                                positionMenuExpanded = false
                                            }
                                        )
                                        availablePositions.forEach { position ->
                                            DropdownMenuItem(
                                                text = { Text(position) },
                                                onClick = {
                                                    selectedPositionFilter = position
                                                    positionMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { yearMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = selectedAcademicYearFilter,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = yearMenuExpanded,
                                        onDismissRequest = { yearMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Years") },
                                            onClick = {
                                                selectedAcademicYearFilter = "All Years"
                                                yearMenuExpanded = false
                                            }
                                        )
                                        availableAcademicYears.forEach { year ->
                                            DropdownMenuItem(
                                                text = { Text(year) },
                                                onClick = {
                                                    selectedAcademicYearFilter = year
                                                    yearMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (filteredRoster.isEmpty()) {
                                Text(
                                    text = "No matching players",
                                    color = Color(0xFF78909C),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredRoster, key = { it.id }) { player ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.width(44.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Text(
                                                        text = "#${player.number}",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = player.name,
                                                        color = Color(0xFF263238),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${player.position} • ${player.academicYear}",
                                                        color = Color(0xFF607D8B),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val successResult = voiceResult as? com.playerid.app.viewmodels.VoiceAssistantResult.Success
        val errorResult = voiceResult as? com.playerid.app.viewmodels.VoiceAssistantResult.Error
        val successPlayers = when {
            successResult == null -> emptyList()
            successResult.players.isNotEmpty() -> successResult.players
            successResult.player != null -> listOf(successResult.player)
            else -> emptyList()
        }
        val successPlayer = successPlayers.singleOrNull()

        LaunchedEffect(voiceResult) {
            if (voiceResult is com.playerid.app.viewmodels.VoiceAssistantResult.Error) {
                showManualRoster = true
            }
            if (voiceResult != null) {
                resultWindowsShown += 1
                if (BuildConfig.DEBUG) {
                    android.util.Log.i(
                        "PlayerIDVoiceScreen",
                        "result window shown #$resultWindowsShown with ${voiceResult::class.simpleName}"
                    )
                }
            }
        }

        if (voiceResult != null) {
            Dialog(onDismissRequest = { viewModel.clearVoiceResult() }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (errorResult != null) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = cardVerticalPadding)) {
                        if (successPlayer != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(end = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "#${successPlayer.number}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 46.sp,
                                        lineHeight = 46.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = successPlayer.name,
                                        color = Color(0xFF263238),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Position: ${successPlayer.position}",
                                        color = Color(0xFF455A64),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    if (!successPlayer.academicYear.isNullOrBlank()) {
                                        Text(
                                            text = "Year: ${successPlayer.academicYear}",
                                            color = Color(0xFF607D8B),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else if (successPlayers.isNotEmpty()) {
                            Text(
                                text = successResult?.message ?: "Multiple matches found",
                                color = Color(0xFF263238),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            successPlayers.forEach { player ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                        Text(
                                            text = "#${player.number} ${player.name}",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Position: ${player.position}",
                                            color = Color(0xFF455A64),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (!player.academicYear.isNullOrBlank()) {
                                            Text(
                                                text = "Year: ${player.academicYear}",
                                                color = Color(0xFF607D8B),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            if (errorResult != null) {
                                Text(
                                    text = "Could not find player",
                                    color = Color(0xFFE65100),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            val fallbackMessage = errorResult?.message ?: successResult?.message.orEmpty()
                            Text(
                                text = fallbackMessage,
                                color = Color(0xFF37474F),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                    }
                }
            }
        }

            if (isListening) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        tonalElevation = 8.dp,
                        modifier = Modifier.size((160 * pulse.value).dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = "Voice Player ID",
                                modifier = Modifier.size((96 * pulse.value).dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            if (debugMessage.value.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        debugMessage.value,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

private fun parsePlayerScreenColor(raw: String?, fallback: Color): Color {
    if (raw.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(raw))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}