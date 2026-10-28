package com.playerid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.team.*
import com.playerid.app.domain.voice.VoiceCommandDecision
import com.playerid.app.domain.voice.VoiceCommandProcessor
import com.playerid.app.platform.SpeechRecognitionService
import com.playerid.app.platform.SpeechSynthesisService
import com.playerid.app.ui.components.CameraRosterPlayerDetails
import com.playerid.app.ui.components.CameraRosterPlayerRow
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import kotlinx.coroutines.launch

@Composable
fun CameraRosterHandle(enabled: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.width(96.dp).height(40.dp)
            .clickable(enabled = enabled, onClick = onOpen)
            .pointerInput(enabled) {
                if (enabled) {
                    var drag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = { if (drag < -25f) onOpen() },
                        onVerticalDrag = { change, amount -> change.consume(); drag += amount }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.KeyboardArrowUp, "Swipe up to view roster",
                tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(22.dp))
            Box(Modifier.width(50.dp).height(5.dp)
                .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
fun CameraRosterDrawer(
    repository: LocalTeamRepository,
    isFull: Boolean,
    height: Dp,
    canSelect: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onNavigateToTeams: () -> Unit,
    onDismiss: () -> Unit,
    voiceSearch: SpeechRecognitionService? = null,
    speaker: SpeechSynthesisService? = null
) {
    val snapshot by repository.snapshot.collectAsState()
    val teamName = snapshot.cameraOrganization?.teamName
    val players = snapshot.players.filter { it.teamName == teamName && it.isActive }
        .sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.number }, { it.name }))
    var inspectedId by remember(teamName) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var voiceMessage by remember(teamName) { mutableStateOf<String?>(null) }
    val voiceProcessor = remember { VoiceCommandProcessor() }
    val displayed = players
    val inspected = players.firstOrNull { it.id == inspectedId }
    val currentPlayerId = snapshot.linkedCameraOrganization()?.playerId
    val listState = rememberLazyListState()
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    val scope = rememberCoroutineScope()

    fun toggleVoiceLookup() {
        val recognizer = voiceSearch ?: return
        if (listening) { recognizer.cancel(); return }
        speaker?.stop()
        dismissKeyboard()
        listening = true
        voiceMessage = null
        scope.launch {
            try {
                val heard = recognizer.recognizeOnce() ?: return@launch
                val decision = voiceProcessor.process(listOf(heard), emptyList(), teamName, players)
                val spoken = when (decision) {
                    is VoiceCommandDecision.Answer -> {
                        inspectedId = null
                        voiceMessage = decision.message
                        decision.speech
                    }
                    is VoiceCommandDecision.Match -> {
                        // Like Android, the full roster stays listed; a single match opens its details card on top.
                        inspectedId = decision.players.singleOrNull()?.id
                        voiceMessage = decision.message
                        decision.speech
                    }
                    is VoiceCommandDecision.Error -> decision.message
                    else -> "Say a player's name or number."
                }
                if (decision !is VoiceCommandDecision.Match && decision !is VoiceCommandDecision.Answer) {
                    voiceMessage = "\u201C$heard\u201D \u2013 $spoken"
                }
                speaker?.speak(spoken.replace("#", "number "))
            } catch (failure: CameraCaptureException) {
                voiceMessage = failure.message ?: "Voice lookup isn't available right now."
            } finally {
                listening = false
            }
        }
    }

    LaunchedEffect(inspectedId) {
        if (inspectedId != null) listState.animateScrollToItem(0)
    }
    DisposableEffect(Unit) {
        onDispose { dismissKeyboard(); voiceSearch?.cancel() }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        if (isFull) Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (!busy) { dismissKeyboard(); onExpandChange(false) }
                }
        )
        Surface(
            modifier = Modifier.fillMaxWidth().imePadding().height(height),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF10101E).copy(alpha = 0.96f),
            tonalElevation = 16.dp,
            shadowElevation = 16.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)
                        .pointerInput(isFull, busy) {
                            var drag = 0f
                            detectVerticalDragGestures(
                                onDragStart = { drag = 0f },
                                onDragEnd = {
                                    if (!busy) {
                                        dismissKeyboard()
                                        when {
                                            drag > 80f && isFull -> onExpandChange(false)
                                            drag > 80f -> onDismiss()
                                            drag < -80f && !isFull -> onExpandChange(true)
                                        }
                                    }
                                },
                                onVerticalDrag = { change, amount -> change.consume(); drag += amount }
                            )
                        }
                ) {
                    IconButton(
                        enabled = !busy, onClick = { dismissKeyboard(); onDismiss() },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close roster", tint = Color.White.copy(alpha = 0.65f))
                    }
                    Box(Modifier.width(40.dp).height(4.dp).align(Alignment.Center)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                    IconButton(
                        enabled = !busy, onClick = { dismissKeyboard(); onExpandChange(!isFull) },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(48.dp)
                    ) {
                        Icon(if (isFull) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            if (isFull) "Collapse roster" else "Expand roster",
                            tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(32.dp))
                    }
                }
                if (players.isNotEmpty() && voiceSearch != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(enabled = !busy && (canSelect || listening), onClick = ::toggleVoiceLookup) {
                            Box(
                                Modifier.size(40.dp).background(
                                    if (listening) Color(0xFFFF453A) else Color.White.copy(alpha = 0.12f), CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, if (listening) "Stop listening" else "Ask a roster question",
                                    tint = Color.White.copy(alpha = if (canSelect || listening) 0.9f else 0.35f),
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                        Text(if (listening) "Listening\u2026" else "Ask about players, positions or class years",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f), state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    item(key = "capture-player") {
                        if (!listening) voiceMessage?.let { message ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(message, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    inspected?.let { player ->
                        item(key = "details") {
                            CameraRosterPlayerDetails(player, expanded = true, onToggle = { inspectedId = null })
                        }
                    }
                    if (players.isEmpty()) {
                        item(key = "empty") {
                            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No roster loaded", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                Text(if (teamName == null) "Choose a team in Game setup to view its roster."
                                    else "Go to Teams to upload a roster for this team.",
                                    color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp)
                                TextButton(enabled = !busy, onClick = { dismissKeyboard(); onNavigateToTeams() }) {
                                    Text("Teams", color = Color(0xFF7CC6FF))
                                }
                            }
                        }
                    }
                    items(displayed, key = { "player-${it.id}" }) { player ->
                        CameraRosterPlayerRow(player, player.id == inspectedId, player.id == currentPlayerId, onClick = {
                            inspectedId = player.id
                            dismissKeyboard()
                        })
                        if (isFull) Divider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
