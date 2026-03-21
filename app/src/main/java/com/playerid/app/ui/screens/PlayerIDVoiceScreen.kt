
package com.playerid.app.ui.screens
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
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
fun PlayerIDVoiceScreen(viewModel: PlayerViewModel, teamViewModel: TeamViewModel) {
    val localContext = LocalContext.current
    val permissionState = remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                localContext,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            permissionState.value = granted
        }
    // ...existing code...
    // ...existing code...
    // ...existing code...
    // SpeechRecognizer integration
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
    var showResult by remember { mutableStateOf(false) }
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
                    android.util.Log.d(
                        "PlayerIDVoiceScreen",
                        "processVoiceCommand (best): $phraseToSend from '$bestPhrase'"
                    )
                    viewModel.processVoiceCommand(phraseToSend)
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

    var listenAttempts by rememberSaveable { mutableIntStateOf(0) }
    var resultWindowsShown by rememberSaveable { mutableIntStateOf(0) }

    fun startListening() {
        listenAttempts += 1
        if (BuildConfig.DEBUG) {
            android.util.Log.i(
                "PlayerIDVoiceScreen",
                "startListening attempt #$listenAttempts"
            )
        }
        viewModel.clearVoiceResult()
        showResult = false
        viewModel.setListening(true)
        isSpeechActive = true
        try {
            speechRecognizer?.startListening(recognitionIntent)
        } catch (e: Exception) {
            isSpeechActive = false
            viewModel.setListening(false)
            android.util.Log.e("PlayerIDVoiceScreen", "startListening failed", e)
        }
    }
    val isListening by viewModel.isListening.collectAsState()
    val voiceResult = viewModel.voiceResult.collectAsState().value
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

    val teams by teamViewModel.subscribedTeams.collectAsState()
    val allPlayers by viewModel.allPlayers.collectAsState(initial = emptyList())
    val selectedTeamState = viewModel.selectedTeam.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var showManualRoster by rememberSaveable { mutableStateOf(false) }
    var rosterQuery by rememberSaveable { mutableStateOf("") }
    var selectedPositionFilter by rememberSaveable { mutableStateOf("All Positions") }
    var selectedAcademicYearFilter by rememberSaveable { mutableStateOf("All Years") }
    var positionMenuExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    val selectedTeam = selectedTeamState.value ?: ""
    val selectedTeamMeta = remember(teams, selectedTeam) {
        teams.firstOrNull { it.name == selectedTeam }
    }
    val teamPrimary = parsePlayerScreenColor(selectedTeamMeta?.color, Color(0xFF1976D2))
    val teamSecondary = parsePlayerScreenColor(selectedTeamMeta?.awayColor, Color(0xFFE3F2FD))
    val onTeamPrimary = if (teamPrimary.luminance() > 0.55f) Color.Black else Color.White
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
        }
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        teamSecondary.copy(alpha = 0.22f),
                        Color(0xFFF3F6FA)
                    )
                )
            )
            .clickable {
                if (voiceResult != null) {
                    showResult = false
                    viewModel.clearVoiceResult()
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Player ID",
                color = teamPrimary,
                style = titleStyle,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Voice-first roster lookup",
                color = teamPrimary.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Team",
                        style = MaterialTheme.typography.labelLarge,
                        color = teamPrimary.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        TextField(
                            value = selectedTeam,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select Team") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = teamSecondary.copy(alpha = 0.22f),
                                unfocusedContainerColor = teamSecondary.copy(alpha = 0.14f),
                                focusedIndicatorColor = teamPrimary,
                                unfocusedIndicatorColor = teamPrimary.copy(alpha = 0.35f),
                                focusedTextColor = Color(0xFF263238),
                                unfocusedTextColor = Color(0xFF263238),
                                focusedTrailingIconColor = teamPrimary,
                                unfocusedTrailingIconColor = teamPrimary.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            teams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text(team.name) },
                                    onClick = {
                                        viewModel.setSelectedTeam(team.name)
                                        teamViewModel.selectTeam(team.name)
                                        expanded = false
                                    }
                                )
                            }
                        }
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
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isListening) teamSecondary else teamPrimary,
                            tonalElevation = 10.dp,
                            shadowElevation = 10.dp,
                            modifier = Modifier.size(micButtonSize)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = "Voice Player ID",
                                    modifier = Modifier
                                        .size(micIconSize)
                                        .clickable(enabled = !isListening) {
                                            if (!isListening) {
                                                if (!permissionState.value) {
                                                    launcher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    if (BuildConfig.DEBUG) {
                                                        android.util.Log.d(
                                                            "PlayerIDVoiceScreen",
                                                            "Mic tapped, starting listening"
                                                        )
                                                    }
                                                    startListening()
                                                }
                                            }
                                        },
                                    tint = onTeamPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isListening) "Listening..." else "Tap to Speak",
                            color = teamPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Can't find by voice?",
                            color = teamPrimary.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { showManualRoster = !showManualRoster },
                            colors = ButtonDefaults.textButtonColors(contentColor = teamPrimary)
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
                                            color = teamSecondary.copy(alpha = 0.18f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showManualRoster = false
                                                    rosterQuery = ""
                                                    viewModel.processVoiceCommand(player.number)
                                                }
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
                                                        color = teamPrimary,
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
        val successPlayer = successResult?.player

        LaunchedEffect(isListening) {
            if (isListening) showResult = false
        }
        LaunchedEffect(voiceResult) {
            if (voiceResult != null) {
                showResult = true
                if (voiceResult is com.playerid.app.viewmodels.VoiceAssistantResult.Error) {
                    showManualRoster = true
                }
                resultWindowsShown += 1
                if (BuildConfig.DEBUG) {
                    android.util.Log.i(
                        "PlayerIDVoiceScreen",
                        "result window shown #$resultWindowsShown with ${voiceResult::class.simpleName}"
                    )
                }
            }
        }

        if (showResult && voiceResult != null && !isListening) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (errorResult != null) Color(0xFFFF9800) else teamPrimary
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
                                        color = teamPrimary,
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
                // Reset showResult safely using a setter
                LaunchedEffect(isListening) {
                    if (isListening) showResult = false
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = teamPrimary.copy(alpha = 0.85f),
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

private fun parsePlayerScreenColor(raw: String?, fallback: Color): Color {
    if (raw.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(raw))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}