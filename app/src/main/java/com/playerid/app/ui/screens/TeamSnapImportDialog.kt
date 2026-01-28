package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.data.teamsnap.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSnapImportDialog(
    teamSnapRepository: TeamSnapRepository,
    onDismiss: () -> Unit,
    onImportComplete: (TeamSnapImportResult) -> Unit
) {
    val scope = rememberCoroutineScope()
    val authState by teamSnapRepository.authState.collectAsState(initial = TeamSnapAuthState.NotAuthenticated)
    val availableTeams by teamSnapRepository.availableTeams.collectAsState(initial = emptyList())
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedTeam by remember { mutableStateOf<TeamSnapTeam?>(null) }
    var localTeamName by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import from TeamSnap")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (authState) {
                    is TeamSnapAuthState.NotAuthenticated -> {
                        // Authentication Step
                        Text(
                            "Sign in to your TeamSnap account to import team rosters",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        if (errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    is TeamSnapAuthState.Authenticating -> {
                        // Loading State
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Connecting to TeamSnap...")
                        }
                    }
                    
                    is TeamSnapAuthState.Authenticated -> {
                        // Team Selection Step
                        Text(
                            "Select a TeamSnap team to import:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (availableTeams.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No teams found in your TeamSnap account",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(availableTeams) { team ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { selectedTeam = team },
                                        colors = if (selectedTeam?.id == team.id) {
                                            CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        } else {
                                            CardDefaults.cardColors()
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = team.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (!team.seasonName.isNullOrBlank()) {
                                                Text(
                                                    text = team.seasonName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (selectedTeam != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = localTeamName,
                                    onValueChange = { localTeamName = it },
                                    label = { Text("Local Team Name") },
                                    placeholder = { Text(selectedTeam!!.name) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                Text(
                                    "This will be the team name used in your local roster",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (isImporting) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importing roster...")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (authState) {
                is TeamSnapAuthState.NotAuthenticated -> {
                    Button(
                        onClick = {
                            scope.launch {
                                errorMessage = null
                                val result = teamSnapRepository.authenticate(email, password)
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
                                }
                            }
                        },
                        enabled = email.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Sign In")
                    }
                }
                
                is TeamSnapAuthState.Authenticated -> {
                    Button(
                        onClick = {
                            if (selectedTeam != null) {
                                scope.launch {
                                    isImporting = true
                                    val teamName = localTeamName.ifBlank { selectedTeam!!.name }
                                    val result = teamSnapRepository.importTeamRoster(selectedTeam!!, teamName)
                                    isImporting = false
                                    
                                    if (result.isSuccess) {
                                        onImportComplete(result.getOrThrow())
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Import failed"
                                    }
                                }
                            }
                        },
                        enabled = selectedTeam != null && !isImporting
                    ) {
                        Text("Import Roster")
                    }
                }
                
                else -> {
                    // Authenticating state - no action button
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}