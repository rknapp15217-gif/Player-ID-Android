package com.playerid.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playerid.app.data.*
import com.playerid.app.viewmodels.VideoViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoExportScreen(
    videoClip: VideoClip,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: VideoViewModel = viewModel()

    var selectedBubbles by remember { mutableStateOf(setOf<String>()) }
    var selectedOverlays by remember { mutableStateOf(listOf<Overlay>()) }
    var includeWatermark by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    
    val availableBubbles by viewModel.getBubblesForVideo(videoClip.id).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Highlight") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Video Preview
            VideoPreviewCard(videoClip = videoClip)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Player Selection
            Text(
                text = "Select Players to Include",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableBubbles) { bubble ->
                    PlayerSelectionCard(
                        bubble = bubble,
                        isSelected = selectedBubbles.contains(bubble.id),
                        onSelectionChanged = { isSelected ->
                            selectedBubbles = if (isSelected) {
                                selectedBubbles + bubble.id
                            } else {
                                selectedBubbles - bubble.id
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Overlay Selection
            Text(
                text = "Add Celebration Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OverlaySelectionSection(
                selectedOverlays = selectedOverlays,
                onOverlaysChanged = { selectedOverlays = it },
                videoDuration = videoClip.duration
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Export Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Include Spotr Watermark",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = includeWatermark,
                    onCheckedChange = { includeWatermark = it }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Export Progress
            if (isExporting) {
                Column {
                    Text(
                        text = "Exporting highlight... ${exportProgress.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = exportProgress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Export Button
            Button(
                onClick = {
                    if (!isExporting) {
                        isExporting = true
                        exportProgress = 0f
                        
                        viewModel.exportVideo(
                            videoClip = videoClip,
                            selectedBubbles = selectedBubbles.toList(),
                            selectedOverlays = selectedOverlays,
                            includeWatermark = includeWatermark,
                            onProgress = { progress -> exportProgress = progress },
                            onComplete = { exportedPath ->
                                isExporting = false
                                // Show sharing options
                                shareVideo(context, exportedPath)
                            },
                            onError = { error ->
                                isExporting = false
                                // Show error
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedBubbles.isNotEmpty() && !isExporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isExporting) "Exporting..." else "Export & Share")
            }
        }
    }
}

@Composable
fun VideoPreviewCard(videoClip: VideoClip) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = videoClip.gameTitle.ifEmpty { "Game Highlight" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Duration: ${formatDuration(videoClip.duration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = videoClip.gameDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionCard(
    bubble: NameBubble,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        onClick = { onSelectionChanged(!isSelected) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChanged
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Jersey number
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bubble.jerseyNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = bubble.playerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Visible for ${formatDuration(1000)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OverlaySelectionSection(
    selectedOverlays: List<Overlay>,
    onOverlaysChanged: (List<Overlay>) -> Unit,
    videoDuration: Long
) {
    val availableOverlays = OverlayType.values().map { it.displayName }
    var showAddOverlay by remember { mutableStateOf(false) }
    
    Column {
        // Selected overlays
        selectedOverlays.forEach { overlay ->
            OverlayCard(
                overlay = overlay,
                onRemove = {
                    onOverlaysChanged(selectedOverlays - overlay)
                }
            )
        }
        
        // Add overlay button
        OutlinedButton(
            onClick = { showAddOverlay = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Celebration Tag")
        }
    }
    
    if (showAddOverlay) {
        AddOverlayDialog(
            videoDuration = videoDuration,
            onDismiss = { showAddOverlay = false },
            onAdd = { overlay ->
                onOverlaysChanged(selectedOverlays + overlay)
                showAddOverlay = false
            }
        )
    }
}

@Composable
fun OverlayCard(
    overlay: Overlay,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = overlay.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "At ${formatTimestamp(overlay.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddOverlayDialog(
    videoDuration: Long,
    onDismiss: () -> Unit,
    onAdd: (Overlay) -> Unit
) {
    var selectedType by remember { mutableStateOf(OverlayType.WINNING_GOAL) }
    var customText by remember { mutableStateOf("") }
    var timestamp by remember { mutableLongStateOf(0L) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Celebration Tag") },
        text = {
            Column {
                // Overlay type selection
                Text("Choose tag type:")
                // Simplified - in production would be a dropdown
                
                if (selectedType == OverlayType.CUSTOM) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("Custom text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Show at timestamp: ${formatTimestamp(timestamp)}")
                // Simplified timestamp selector
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val overlay = Overlay(
                        id = UUID.randomUUID().toString(),
                        type = selectedType,
                        text = if (selectedType == OverlayType.CUSTOM) customText else selectedType.displayName,
                        timestamp = timestamp
                    )
                    onAdd(overlay)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun shareVideo(context: android.content.Context, videoPath: String) {
    val videoUri = Uri.parse(videoPath)
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, videoUri)
        putExtra(Intent.EXTRA_TEXT, "Created with Spotr — download here: [app store link]")
        putExtra(Intent.EXTRA_SUBJECT, "Check out this highlight!")
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Share highlight"))
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes}:${String.format("%02d", remainingSeconds)}"
}

private fun formatTimestamp(milliseconds: Long): String {
    return formatDuration(milliseconds)
}
