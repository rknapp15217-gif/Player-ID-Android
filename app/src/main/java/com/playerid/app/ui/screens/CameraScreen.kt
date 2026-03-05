package com.playerid.app.ui.screens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.ui.screens.VoiceResultCard
import com.playerid.app.ui.composables.EditGameInfoSheet
import com.playerid.app.ui.screens.CameraPermissionScreen
import com.playerid.app.ui.screens.startCamera
import com.playerid.app.ui.composables.PlayerBubblesOverlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.camera.core.Camera
import com.playerid.app.utils.RecordingState
// ...existing code...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: PlayerViewModel,
    teamViewModel: com.playerid.app.viewmodels.TeamViewModel,
    onVideoSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isStandby by remember { mutableStateOf(false) }

    // Managers and states
    val recordingManager = remember { com.playerid.app.utils.RecordingManager(context) }
    val detectionManager = remember {
        com.playerid.app.ar.JerseyDetectionManager(
            context,
            onPlayersTracked = { trackedPlayers ->
                // Update trackedPlayersWithInfo or handle tracked players as needed
            }
        )
    }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val cameraPermissionsState = remember { object { val allPermissionsGranted = true; fun launchMultiplePermissionRequest() {} } }
    var recordingState by remember { mutableStateOf(com.playerid.app.utils.RecordingState.IDLE) }
    var voiceResult by remember { mutableStateOf<String?>(null) }
    val trackedPlayersWithInfo = remember { mutableListOf<Pair<Any, Any>>() }
    var processing by remember { mutableStateOf(false) }
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    // ...existing code...
    DisposableEffect(isStandby) {
        if (activity != null) {
            activity.window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                attributes = attributes?.apply { screenBrightness = if (isStandby) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
            }
        }
        onDispose {}
    }

    var arMode by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { recordingManager.stopAndDiscardRecording() }
    }

    var showGameInfoSheet by remember { mutableStateOf(false) }
    val opponent by viewModel.opponent.collectAsState()
    val jerseyColor by viewModel.jerseyColor.collectAsState()

    if (cameraPermissionsState.allPermissionsGranted) {
        Scaffold(
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { isStandby = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, "Standby")
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { 
                                if (recordingState == RecordingState.RECORDING) {
                                    recordingManager.stopRecording()
                                } else if (recordingState == RecordingState.IDLE) {
                                    recordingManager.startRecording { uri ->
                                        uri?.let { onVideoSaved(it) }
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.Center),
                            containerColor = if (recordingState == RecordingState.RECORDING) Color.Red else MaterialTheme.colorScheme.primary
                        ) {
                            when (recordingState) {
                                RecordingState.RECORDING -> Icon(Icons.Default.Stop, "Capture")
                                RecordingState.FINALIZING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                else -> Icon(Icons.Default.FiberManualRecord, "Record")
                            }
                        }

                        Surface(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ID", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = arMode,
                                    onCheckedChange = { arMode = it },
                                    modifier = Modifier.scale(0.7f),
                                    thumbContent = if (arMode) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            content = { padding ->
                var camera: Camera? by remember { mutableStateOf(null) }
                var scaleFactor by remember { mutableStateOf(1f) }
                var minZoom by remember { mutableStateOf(1f) }
                var maxZoom by remember { mutableStateOf(8f) }

                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            if (!isStandby) {
                                camera?.let { cam ->
                                    scaleFactor = (scaleFactor * zoomChange).coerceIn(minZoom, maxZoom)
                                    cam.cameraControl.setZoomRatio(scaleFactor)
                                }
                            }
                        }
                    }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { view ->
                                startCamera(
                                    ctx,
                                    lifecycleOwner,
                                    view,
                                    detectionManager,
                                    recordingManager,
                                    { cam ->
                                        camera = cam
                                        minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                                        maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8f
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (arMode && !isStandby) {
                        PlayerBubblesOverlay(
                            trackedPlayers = trackedPlayersWithInfo as List<Pair<com.playerid.app.data.TrackedPlayer, com.playerid.app.data.Player?>>, // Cast to match expected type
                            processing = processing,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                tonalElevation = 4.dp,
                                modifier = Modifier
                                    .clickable { showGameInfoSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    // Color dot for team color
                                    if (jerseyColor.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    color = try { Color(android.graphics.Color.parseColor(jerseyColor)) } catch (e: Exception) { Color.Gray },
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    // Team name
                                    Text(
                                        text = selectedTeam ?: "Select Team",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    // Opponent name
                                    if (opponent.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "vs $opponent",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        if (showGameInfoSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showGameInfoSheet = false },
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            ) {
                                EditGameInfoSheet(
                                    teams = subscribedTeams,
                                    initialTeam = subscribedTeams.find { it.name == selectedTeam },
                                    initialColor = jerseyColor,
                                    initialOpponent = opponent,
                                    onSave = { team, color, opp ->
                                        viewModel.setSelectedTeam(team.name)
                                        viewModel.setJerseyColor(color)
                                        viewModel.setOpponent(opp)
                                        teamViewModel.selectTeam(team.name)
                                        showGameInfoSheet = false
                                    },
                                    onDismiss = { showGameInfoSheet = false }
                                )
                            }
                        }

                        val visiblePlayers = trackedPlayersWithInfo.filter { it.first.disappearedFrames == 0 }
                        if (visiblePlayers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(48.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Spotr AR Active", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Tracking: ${visiblePlayers.size} players", style = MaterialTheme.typography.bodySmall)
                                        val firstPlayer = visiblePlayers.first().first
                                        val frozenW = firstPlayer.initialBox.width().toInt()
                                        val frozenH = firstPlayer.initialBox.height().toInt()
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("| Size: ${frozenW}x${frozenH}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    visiblePlayers.firstOrNull()?.let { (tracked, player) ->
                                        val displayText = player?.let { "${it.name} #${it.number}" } ?: "Unknown #${tracked.jerseyNumber}"
                                        val textColor = if (player != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        Text(displayText, style = MaterialTheme.typography.bodyMedium, color = textColor)
                                    }
                                }
                            }
                        }
                    }

                    if (isStandby) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.98f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { isStandby = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FlashOn, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("SPOTR STANDBY", color = Color.White.copy(alpha = 0.2f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Recording past moments... Tap to wake UI", color = Color.White.copy(alpha = 0.1f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (!isStandby) {
                        (voiceResult as? com.playerid.app.viewmodels.VoiceAssistantResult)?.let { result ->
                            VoiceResultCard(
                                result = result,
                                onDismiss = { viewModel.clearVoiceResult() },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                            )
                        }
                    }
                }
            }
        )
    } else {
        CameraPermissionScreen { cameraPermissionsState.launchMultiplePermissionRequest() }
    }
}



















