package com.playerid.app.ui.screens

import com.playerid.app.platform.SpeechRecognitionService
import com.playerid.app.platform.SpeechSynthesisService

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.playerid.app.domain.camera.*
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.ui.components.CameraModeButton
import com.playerid.app.ui.components.CameraShutterVisual
import com.playerid.app.ui.components.CameraGameSetupPill
import com.playerid.app.ui.components.CameraSetupAlertBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

@Composable
fun CameraCapturePage(
    state: CameraCaptureState,
    preview: @Composable (Modifier, Boolean, (Float) -> Unit) -> Unit,
    onMode: (CaptureMode) -> Unit,
    onCapture: () -> Unit,
    onShutterPress: () -> Unit,
    onRetry: () -> Unit,
    teamName: String?,
    opponent: String,
    player: String,
    jerseyColor: Color,
    hasOpenedSetup: Boolean,
    showRollingHint: Boolean,
    deletionMessage: String?,
    canRetryDelete: Boolean,
    onRetryDelete: () -> Unit,
    setupOpen: Boolean,
    onSetup: () -> Unit,
    repository: LocalTeamRepository,
    onNavigateToTeams: () -> Unit,
    onZoom: (Float) -> Unit,
    zoomError: String?,
    onDismissZoomError: () -> Unit,
    voiceSearch: SpeechRecognitionService? = null,
    speaker: SpeechSynthesisService? = null
) {
    var rosterOpen by remember { mutableStateOf(false) }
    var rosterFull by remember { mutableStateOf(false) }
    var rosterBusy by remember { mutableStateOf(false) }
    val shutterInteractions = remember { MutableInteractionSource() }
    val shutterPress by rememberUpdatedState(onShutterPress)
    val shutterEnabled by rememberUpdatedState(state.canCapture && !rosterBusy)
    LaunchedEffect(shutterInteractions) {
        shutterInteractions.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press && shutterEnabled) shutterPress()
        }
    }
    fun closeRoster() { rosterOpen = false; rosterFull = false }
    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(state.phase) {
        if (state.phase == CapturePhase.RECORDING) {
            elapsedSeconds = 0
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }
    var setupPrompt by remember { mutableStateOf(false) }
    if (setupPrompt) AlertDialog(
        onDismissRequest = { setupPrompt = false },
        title = { Text("Set up your game") },
        text = {
            Text("Before you record, choose your team, opponent and player. " +
                "Your clips will be organized automatically." +
                (teamName?.let { "\n\nCurrent: $it" + (if (opponent.isNotBlank()) " vs $opponent" else "") } ?: ""))
        },
        confirmButton = {
            TextButton(onClick = { setupPrompt = false; closeRoster(); onSetup() }) { Text("Set up game") }
        },
        dismissButton = { TextButton(onClick = { setupPrompt = false }) { Text("Not now") } }
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val drawerHeight by animateDpAsState(
            if (rosterFull) maxHeight * 0.75f else maxHeight * 0.35f,
            animationSpec = tween(300), label = "drawerHeight"
        )
        val viewportPadding by animateDpAsState(
            if (!rosterOpen) 0.dp else if (rosterFull) maxHeight * 0.75f else maxHeight * 0.35f,
            animationSpec = tween(220), label = "cameraViewportBottomPadding"
        )
        preview(
            Modifier.fillMaxSize().padding(bottom = viewportPadding),
            state.canCapture && !setupOpen && !rosterBusy,
            onZoom
        )
        Column(
            Modifier.align(Alignment.TopStart).padding(top = 14.dp, start = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Until Game setup is opened, a pulsing red "!" replaces the pill; tapping it explains what to set up.
            if (hasOpenedSetup) CameraGameSetupPill(teamName, opponent, player, jerseyColor, hasOpenedSetup,
                enabled = state.canChangeMode && !rosterBusy,
                compact = true,
                onClick = { closeRoster(); onSetup() })
            else CameraSetupAlertBadge(enabled = state.canChangeMode && !rosterBusy, onClick = { setupPrompt = true })
            zoomError?.let { message ->
                Surface(shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(12.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onDismissZoomError) { Text("Dismiss") }
                    }
                    deletionMessage?.let { message ->
                        Surface(shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(12.dp)) {
                                Text(message)
                                if (canRetryDelete) TextButton(onClick = onRetryDelete) { Text("Retry delete") }
                            }
                        }
                    }
                }
            }
            if (state.phase == CapturePhase.RECORDING) {
                val minutes = (elapsedSeconds / 60).toString().padStart(2, '0')
                val seconds = (elapsedSeconds % 60).toString().padStart(2, '0')
                Row(
                    Modifier.background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.size(7.dp).background(Color.Red, CircleShape))
                    Text("$minutes:$seconds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        if (state.phase == CapturePhase.STARTING || state.phase == CapturePhase.CAPTURING || state.phase == CapturePhase.SAVING) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        // A plain STOPPED state is the normal moment before the camera opens, so only show Resume for real failures.
        if (state.phase == CapturePhase.FAILED || (state.phase == CapturePhase.STOPPED && state.error != null)) {
            Surface(Modifier.align(Alignment.Center).padding(24.dp), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Camera paused.")
                    TextButton(onClick = onRetry) { Text("Resume camera") }
                }
            }
        }
        if (!setupOpen) {
            @Composable
            fun ModeButtons(buttonModifier: Modifier) {
                CameraModeButton("Photo", state.mode == CaptureMode.PHOTO, state.canChangeMode && !rosterBusy,
                    { onMode(CaptureMode.PHOTO) }, buttonModifier, compact = true)
                CameraModeButton("Rolling", state.mode == CaptureMode.ROLLING, state.canChangeMode && !rosterBusy,
                    { onMode(CaptureMode.ROLLING) }, buttonModifier, compact = true)
                CameraModeButton("Video", state.mode == CaptureMode.VIDEO, state.canChangeMode && !rosterBusy,
                    { onMode(CaptureMode.VIDEO) }, buttonModifier, compact = true)
            }
            // In landscape the modes stack on the right edge instead of spanning the short bottom edge.
            if (maxWidth > maxHeight) Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, bottom = viewportPadding).width(104.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModeButtons(Modifier.fillMaxWidth())
            } else Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 104.dp + viewportPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModeButtons(Modifier.weight(1f))
            }
            FloatingActionButton(
                interactionSource = shutterInteractions,
                onClick = { if (state.canCapture && !rosterBusy) onCapture() },
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp + viewportPadding).size(56.dp)
                    .semantics {
                        contentDescription = when {
                            state.phase == CapturePhase.RECORDING -> "Stop recording"
                            state.mode == CaptureMode.ROLLING -> "Save last 30 seconds"
                            state.mode == CaptureMode.PHOTO -> "Take photo"
                            else -> "Start recording"
                        }
                    }
            ) {
                CameraShutterVisual(
                    photo = state.mode == CaptureMode.PHOTO,
                    rolling = state.mode == CaptureMode.ROLLING,
                    recording = state.phase == CapturePhase.RECORDING,
                    enabled = state.canCapture && !rosterBusy,
                    compact = true
                )
            }
            if (showRollingHint && state.phase == CapturePhase.BUFFERING) {
                Text("Rolling \u00b7 Tap to save up to 30 seconds",
                    color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        // Landscape keeps the roster handle right above the shutter, so the hint sits above the handle.
                        .padding(bottom = (if (maxWidth > maxHeight) 124.dp else 78.dp) + viewportPadding)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
        if (!setupOpen && !rosterOpen) {
            CameraRosterHandle(
                enabled = state.canCapture,
                onOpen = { rosterOpen = true; rosterFull = true },
                // In landscape there is no mode row above the shutter, so the handle sits directly above it.
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (maxWidth > maxHeight) 78.dp else 168.dp)
            )
        }
        if (rosterOpen && !setupOpen) CameraRosterDrawer(
            repository = repository,
            isFull = rosterFull,
            height = drawerHeight,
            canSelect = state.canChangeMode,
            onExpandChange = { rosterFull = it },
            onNavigateToTeams = { closeRoster(); onNavigateToTeams() },
            onDismiss = { closeRoster() },
            voiceSearch = voiceSearch,
            speaker = speaker
        )
    }
}
