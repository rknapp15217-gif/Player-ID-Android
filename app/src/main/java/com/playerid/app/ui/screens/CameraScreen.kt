@file:OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.camera.core.ExperimentalGetImage::class
)
package com.playerid.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.playerid.app.ar.JerseyDetectionManager
import com.playerid.app.ui.composables.PlayerBubblesOverlay
import com.playerid.app.utils.RecordingManager
import com.playerid.app.utils.RecordingState
import com.playerid.app.viewmodels.PlayerViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    viewModel: PlayerViewModel,
    teamViewModel: com.playerid.app.viewmodels.TeamViewModel,
    onVideoSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )

    val trackedPlayersWithInfo by viewModel.detectedPlayersWithInfo.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    val currentRoster by viewModel.filteredPlayers.collectAsState()
    
    val recordingManager = remember { RecordingManager(context) }
    val recordingState by recordingManager.recordingState.collectAsState()

    // CRITICAL FIX: Remember detectionManager to avoid re-creating it on every frame (prevent crash)

    var processing by remember { mutableStateOf(false) }
    val detectionManager = remember {
        JerseyDetectionManager(
            context = context,
            onPlayersTracked = { tracked ->
                viewModel.updateTrackedPlayers(tracked)
                processing = false
            },
            onDetectionProcessing = {
                processing = true
            }
        )
    }

    LaunchedEffect(currentRoster) {
        val validNumbers = currentRoster.map { it.number }.toSet()
        detectionManager.setRosterFilter(validNumbers)
    }

    var arMode by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            recordingManager.stopAndDiscardRecording()
        }
    }

    if (cameraPermissionsState.allPermissionsGranted) {
        Scaffold(
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Record Button in Center
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
                            RecordingState.RECORDING -> Icon(Icons.Default.Stop, "Stop")
                            RecordingState.FINALIZING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            else -> Icon(Icons.Default.FiberManualRecord, "Record")
                        }
                    }

                    // Player ID Toggle on the Right, in line with Record Button
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
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { padding ->
            var camera: Camera? by remember { mutableStateOf(null) }
            var scaleFactor by remember { mutableStateOf(1f) }
            var minZoom by remember { mutableStateOf(1f) }
            var maxZoom by remember { mutableStateOf(8f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            camera?.let { cam ->
                                scaleFactor = (scaleFactor * zoomChange).coerceIn(minZoom, maxZoom)
                                cam.cameraControl.setZoomRatio(scaleFactor)
                            }
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { view ->
                            startCamera(ctx, lifecycleOwner, view, detectionManager, recordingManager) { cam ->
                                camera = cam
                                minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                                maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8f
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (arMode) {
                    PlayerBubblesOverlay(
                        trackedPlayers = trackedPlayersWithInfo,
                        processing = processing,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Team selection overlay: always visible
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        TeamSelectionDropdown(
                            selectedTeam = selectedTeam,
                            availableTeams = subscribedTeams,
                            onTeamSelected = { teamName ->
                                viewModel.setSelectedTeam(teamName)
                                teamViewModel.selectTeam(teamName ?: "")
                            }
                        )
                        // AR Info card: only show if there are visible players
                        val visiblePlayers = trackedPlayersWithInfo.filter { it.first.disappearedFrames == 0 }
                        if (visiblePlayers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
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
                }
            }
        }
    } else {
        CameraPermissionScreen { cameraPermissionsState.launchMultiplePermissionRequest() }
    }
}

@Composable
private fun CameraPermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Camera Access Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text("Grant Permissions") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSelectionDropdown(
    selectedTeam: String?,
    availableTeams: List<com.playerid.app.data.Team>,
    onTeamSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedTeam ?: "No team selected",
            onValueChange = {},
            readOnly = true,
            label = { Text("Active Team") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("🚫 No team (detect all)") }, onClick = { onTeamSelected(null); expanded = false })
            availableTeams.forEach { team ->
                DropdownMenuItem(text = { Text(team.name) }, onClick = { onTeamSelected(team.name); expanded = false })
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    analyzer: JerseyDetectionManager,
    recordingManager: RecordingManager,
    onCameraReady: (Camera) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
        val videoCapture = VideoCapture.withOutput(recorder)
        recordingManager.setVideoCapture(videoCapture)

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                videoCapture,
                imageAnalyzer
            )
            onCameraReady(camera)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}
