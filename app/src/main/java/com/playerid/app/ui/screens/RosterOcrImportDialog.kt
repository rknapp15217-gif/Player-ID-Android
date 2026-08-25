package com.playerid.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.roster.RosterCandidate
import com.playerid.app.roster.extractRosterCandidates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun RosterOcrImportDialog(
    teamName: String,
    imageUri: Uri,
    onDismiss: () -> Unit,
    onImport: (List<RosterCandidate>) -> Unit
) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(true) }
    var candidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    var showOverwriteWarning by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf(0) }
    var importedCandidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }

    LaunchedEffect(imageUri) {
        isProcessing = true
        errorMessage = null
        candidates = emptyList()

        try {
            val result = withContext(Dispatchers.Default) {
                extractRosterCandidates(context, imageUri)
            }
            candidates = result.candidates
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to read roster"
        } finally {
            isProcessing = false
        }
    }

    LaunchedEffect(showConfirmation, importedCandidates) {
        if (showConfirmation && importedCandidates.isNotEmpty()) {
            delay(1400)
            onImport(importedCandidates)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = if (showConfirmation) {{ /* prevent dismissal during confirmation */ }} else onDismiss,
        title = {
            Column {
                Text(
                    if (showConfirmation) "Import Complete" else "Import from Screenshot",
                    style = MaterialTheme.typography.titleLarge
                )
                if (!showConfirmation) {
                    Text(
                        "Team: $teamName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    showConfirmation -> {
                        // Confirmation state
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
                    isProcessing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(12.dp))
                            Text("Reading roster...")
                        }
                    }
                    errorMessage != null -> {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    candidates.isEmpty() -> {
                        Text(
                            "No players detected. Try a clearer screenshot of the roster table.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        Text(
                            "${candidates.size} players detected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(candidates) { candidate ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "#${candidate.number}",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                candidate.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (!candidate.academicYear.isNullOrBlank()) {
                                                Text(
                                                    candidate.academicYear,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            candidates = candidates.filterNot { it == candidate }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showConfirmation) {
                Button(
                    onClick = {
                        showOverwriteWarning = true
                    },
                    enabled = candidates.isNotEmpty() && !isProcessing
                ) {
                    Text("Import Players")
                }
            }
        },
        dismissButton = {
            if (!showConfirmation) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showOverwriteWarning) {
        AlertDialog(
            onDismissRequest = { showOverwriteWarning = false },
            title = { Text("Overwrite Existing Roster?") },
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
