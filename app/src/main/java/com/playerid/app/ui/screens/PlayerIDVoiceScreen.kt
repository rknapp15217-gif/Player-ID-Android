
package com.playerid.app.ui.screens
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
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
    val selectedTeamState = viewModel.selectedTeam.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = selectedTeamState.value ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (voiceResult != null) {
                    showResult = false
                    viewModel.clearVoiceResult()
                }
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedTeam,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Team") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor().width(220.dp)
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
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2196F3).copy(alpha = 0.85f),
                        tonalElevation = 8.dp,
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = "Voice Player ID",
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clickable(enabled = !isListening) {
                                            if (!isListening) {
                                                if (!permissionState.value) {
                                                    launcher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    android.util.Log.d(
                                                        "PlayerIDVoiceScreen",
                                                        "Mic tapped, starting listening"
                                                    )
                                                    startListening()
                                                }
                                            }
                                        },
                                    tint = if (!isListening) Color.White else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap to speak",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                val resultMessage = when (val result = voiceResult) {
                    is com.playerid.app.viewmodels.VoiceAssistantResult.Error -> result.message
                    is com.playerid.app.viewmodels.VoiceAssistantResult.Success -> {
                        val player = result.player
                        if (player != null) {
                            "#${player.number} ${player.name}\nPosition: ${player.position}\n${player.academicYear.orEmpty()}"
                        } else {
                            result.message
                        }
                    }
                    else -> null
                }
                // Reset showResult when listening starts
                LaunchedEffect(isListening) {
                    if (isListening) showResult = false
                }
                LaunchedEffect(voiceResult) {
                    if (voiceResult != null) {
                        showResult = true
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
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3),
                                        Color(0xFF1976D2)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .shadow(12.dp, RoundedCornerShape(32.dp))
                                    .clickable {
                                        showResult = false
                                        viewModel.clearVoiceResult()
                                    }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.15f)),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when {
                                resultMessage != null -> {
                                    Column(modifier = Modifier.padding(32.dp)) {
                                        Text(
                                            text = resultMessage,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = {
                                            showResult = false
                                            viewModel.clearVoiceResult()
                                        }) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                                voiceResult is com.playerid.app.viewmodels.VoiceAssistantResult.Success -> {
                                    val player = voiceResult.player
                                    val gradYear = try {
                                        val grad = player?.javaClass?.getMethod("getGraduationYear")?.invoke(player) as? String
                                        if (!grad.isNullOrBlank()) grad else null
                                    } catch (_: Exception) {
                                        null
                                    } ?: try {
                                        val acad = player?.javaClass?.getMethod("getAcademicYear")?.invoke(player) as? String
                                        if (!acad.isNullOrBlank()) acad else null
                                    } catch (_: Exception) {
                                        null
                                    }
                                    Row(
                                        modifier = Modifier.padding(32.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = player?.name ?: "",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 24.sp,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(48.dp)
                                                        .height(48.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "#${player?.number ?: ""}",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 28.sp,
                                                        color = Color(0xFFFFC107),
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                                                        softWrap = false
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(32.dp))
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.Start
                                                ) {
                                                    Text(
                                                        text = "Position: ${player?.position ?: ""}",
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 16.sp,
                                                        color = Color.White.copy(alpha = 0.85f),
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                    if (gradYear != null) {
                                                        Text(
                                                            text = "Grad Year: $gradYear",
                                                            fontWeight = FontWeight.Normal,
                                                            fontSize = 16.sp,
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = {
                                        showResult = false
                                        viewModel.clearVoiceResult()
                                    }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
                            // Show results window after speech input
                            // Only show results window after speech input completes (onResults or onError)
                            // Removed duplicate LaunchedEffect for showResult
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
                        color = Color(0xFF2196F3).copy(alpha = 0.85f),
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