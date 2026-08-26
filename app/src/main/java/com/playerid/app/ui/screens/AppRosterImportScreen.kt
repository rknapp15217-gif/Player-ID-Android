package com.playerid.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.playerid.app.capture.AppRosterCaptureRepository
import com.playerid.app.capture.RosterAppDetector
import com.playerid.app.capture.ScreenCaptureService
import com.playerid.app.roster.RosterCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRosterImportScreen(
    teamName: String,
    onBack: () -> Unit,
    onImport: (List<RosterCandidate>) -> Unit
) {
    val context = LocalContext.current
    val candidates by AppRosterCaptureRepository.candidates.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isWaiting by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var showOverwriteWarning by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf(0) }
    var importedCandidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }
    val autoRemind = false
    var installedRosterApps by remember { mutableStateOf(emptyList<com.playerid.app.capture.RosterApp>()) }
    var isLoadingRosterApps by remember { mutableStateOf(true) }
    var selectedApp by remember { mutableStateOf<com.playerid.app.capture.RosterApp?>(null) }

    LaunchedEffect(teamName) {
        AppRosterCaptureRepository.setActiveTeamName(teamName)
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            // Cache the permission for reuse
            AppRosterCaptureRepository.cacheProjection(result.resultCode, data)
            
            startCaptureService(context, result.resultCode, data, autoRemind)
            isWaiting = true
            errorMessage = null
            // Launch the selected app
            selectedApp?.let { app ->
                if (RosterAppDetector.launchApp(context, app.packageName)) {
                    setOverlayVisible(context, true)
                } else {
                    errorMessage = "Could not open ${app.name}"
                }
            }
            selectedApp = null
        } else {
            errorMessage = "Screen capture permission denied."
            selectedApp = null
            AppRosterCaptureRepository.clearCachedProjection()
        }
    }

    // Show overlay only when actively capturing
    LaunchedEffect(isWaiting) {
        if (!isWaiting) {
            setOverlayVisible(context, false)
        }
    }

    LaunchedEffect(Unit) {
        isLoadingRosterApps = true
        installedRosterApps = withContext(Dispatchers.IO) {
            RosterAppDetector.getInstalledRosterApps(context)
        }
        isLoadingRosterApps = false
    }

    LaunchedEffect(showConfirmation, importedCandidates) {
        if (showConfirmation && importedCandidates.isNotEmpty()) {
            delay(1400)
            onImport(importedCandidates)
            AppRosterCaptureRepository.clear()
            showConfirmation = false
            onBack()
        }
    }


    // Handle back button - stop capture if running
    BackHandler {
        if (isWaiting) {
            stopCaptureService(context)
            isWaiting = false
        }
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from App") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isWaiting) {
                            stopCaptureService(context)
                            isWaiting = false
                        }
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Team: $teamName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Show roster apps list
            if (!isWaiting && candidates.isEmpty()) {
                Text(
                    "Select roster app",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (isLoadingRosterApps) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Loading apps...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (installedRosterApps.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("No roster apps found.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            installedRosterApps.forEach { app ->
                                OutlinedButton(
                                    onClick = {
                                        selectedApp = app
                                        
                                        // Check overlay permission first
                                        if (!Settings.canDrawOverlays(context)) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                android.net.Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                            errorMessage = "Enable Display over other apps, then try again."
                                            selectedApp = null
                                            return@OutlinedButton
                                        }
                                        
                                        // If we have cached permission, use it; otherwise request permission
                                        val cachedProjection = AppRosterCaptureRepository.getCachedProjection()
                                        if (cachedProjection != null) {
                                            // Reuse cached permission
                                            val (resultCode, resultData) = cachedProjection
                                            startCaptureService(context, resultCode, resultData, autoRemind)
                                            isWaiting = true
                                            errorMessage = null
                                            if (RosterAppDetector.launchApp(context, app.packageName)) {
                                                setOverlayVisible(context, true)
                                            } else {
                                                errorMessage = "Could not open ${app.name}"
                                            }
                                            selectedApp = null
                                        } else {
                                            // Request screen capture permission for the first time
                                            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                            projectionLauncher.launch(manager.createScreenCaptureIntent())
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val iconBitmap = remember(app.icon) {
                                        app.icon?.toBitmap(48, 48)?.asImageBitmap()
                                    }
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = "Open app")
                                    }
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(app.name)
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (candidates.isNotEmpty()) {
                Text(
                    "${candidates.size} players detected",
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(candidates) { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "#${candidate.number}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        candidate.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!candidate.position.isNullOrBlank()) {
                                        Text(
                                            candidate.position,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    candidate.academicYear?.takeIf(String::isNotBlank)?.let { academicYear ->
                                        Text(
                                            academicYear,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    AppRosterCaptureRepository.setCandidates(candidates.filterNot { it == candidate })
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showOverwriteWarning = true
                        }
                    ) {
                        Text("Import")
                    }
                    TextButton(
                        onClick = { AppRosterCaptureRepository.clear() }
                    ) {
                        Text("Clear")

                if (showOverwriteWarning) {
                    AlertDialog(
                        onDismissRequest = { showOverwriteWarning = false },
                        title = {
                            Text(
                                "Overwrite Existing Roster?",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            Text(
                                "Importing this roster will overwrite existing roster data for $teamName. Continue?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    importedCandidates = candidates.toList()
                                    importedCount = importedCandidates.size
                                    showOverwriteWarning = false
                                    showConfirmation = true
                                }
                            ) {
                                Text("Continue")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOverwriteWarning = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                    }
                }
            }
        }
    }

    // Import Confirmation Dialog
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { /* Do nothing - prevent dismissal */ },
            title = {
                Text(
                    "Import Complete",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✅",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        "Successfully imported $importedCount players",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "to team $teamName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Closing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

private fun startCaptureService(
    context: Context,
    resultCode: Int,
    data: Intent,
    autoRemind: Boolean
) {
    val intent = Intent(context, ScreenCaptureService::class.java).apply {
        action = ScreenCaptureService.ACTION_START
        putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
        putExtra(ScreenCaptureService.EXTRA_DATA, data)
        putExtra(ScreenCaptureService.EXTRA_AUTO_REMIND, autoRemind)
    }
    context.startForegroundService(intent)
}

private fun stopCaptureService(context: Context) {
    val intent = Intent(context, ScreenCaptureService::class.java).apply {
        action = ScreenCaptureService.ACTION_STOP
    }
    context.startService(intent)
}

private fun setOverlayVisible(context: Context, visible: Boolean) {
    val intent = Intent(context, ScreenCaptureService::class.java).apply {
        action = if (visible) {
            ScreenCaptureService.ACTION_SHOW_OVERLAY
        } else {
            ScreenCaptureService.ACTION_HIDE_OVERLAY
        }
    }
    context.startService(intent)
}
