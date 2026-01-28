package com.playerid.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.playerid.app.ml.*

/**
 * 📥 Batch Import Dialog
 * 
 * Import multiple jersey photos from URLs or suggested sources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<String>) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf("soccer") }
    var selectedTeam by remember { mutableStateOf<String?>(null) }
    var showSuggestions by remember { mutableStateOf(true) }
    
    val jerseyPhotoSources = remember { JerseyPhotoSources.getRealJerseySources() }
    val teamSources = remember { JerseyPhotoSources.getTeamJerseySources() }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📥 Batch Import Jersey Photos",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab Selector
                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("🔗 URLs", "🏆 Teams", "⚡ Quick")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        FilterChip(
                            onClick = { selectedTab = index },
                            label = { Text(tab) },
                            selected = selectedTab == index,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content based on selected tab
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> URLImportTab(
                            urlText = urlText,
                            onUrlTextChange = { urlText = it },
                            onImport = { urls -> onImport(urls) }
                        )
                        
                        1 -> TeamImportTab(
                            selectedSport = selectedSport,
                            onSportChange = { selectedSport = it },
                            selectedTeam = selectedTeam,
                            onTeamChange = { selectedTeam = it },
                            teamSources = teamSources,
                            onImport = { urls -> onImport(urls) }
                        )
                        
                        2 -> QuickImportTab(
                            onImport = { urls -> onImport(urls) }
                        )
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun URLImportTab(
    urlText: String,
    onUrlTextChange: (String) -> Unit,
    onImport: (List<String>) -> Unit
) {
    Column {
        Text(
            text = "🔗 Import from URLs",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Paste multiple URLs (one per line):",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = urlText,
            onValueChange = onUrlTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = {
                Text(
                    text = "https://example.com/jersey1.jpg\nhttps://example.com/jersey2.jpg\nhttps://example.com/jersey3.jpg",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            maxLines = 10
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // URL Validation Info
        val urls = urlText.lines().filter { it.isNotBlank() }
        val validUrls = urls.filter { JerseyPhotoSources.isValidJerseyPhotoUrl(it) }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "📊 URL Validation",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Total URLs: ${urls.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Valid URLs: ${validUrls.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (validUrls.size > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onImport(validUrls) },
            enabled = validUrls.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import ${validUrls.size} Photos")
        }
    }
}

@Composable
fun TeamImportTab(
    selectedSport: String,
    onSportChange: (String) -> Unit,
    selectedTeam: String?,
    onTeamChange: (String?) -> Unit,
    teamSources: Map<String, List<TeamJerseyInfo>>,
    onImport: (List<String>) -> Unit
) {
    Column {
        Text(
            text = "🏆 Import Team Jerseys",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Sport Selection
        Text("Select Sport:", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            listOf("soccer", "basketball", "football").forEach { sport ->
                if (selectedSport == sport) {
                    Button(
                        onClick = { 
                            onSportChange(sport)
                            onTeamChange(null) // Reset team selection
                        }
                    ) { Text(sport.capitalize()) }
                } else {
                    OutlinedButton(
                        onClick = { 
                            onSportChange(sport)
                            onTeamChange(null) // Reset team selection
                        }
                    ) { Text(sport.capitalize()) }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Team Selection
        val teamsForSport = teamSources[selectedSport] ?: emptyList()
        
        if (teamsForSport.isNotEmpty()) {
            Text("Select Team:", style = MaterialTheme.typography.labelMedium)
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                teamsForSport.forEach { team ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onTeamChange(if (selectedTeam == team.teamName) null else team.teamName) 
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedTeam == team.teamName) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = team.teamName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Colors: ${team.jerseyColors}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Numbers: ${team.commonNumbers.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (selectedTeam == team.teamName) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    selectedTeam?.let { teamName ->
                        // Generate search URLs for this team
                        val searchUrls = JerseyPhotoSources.generateJerseySearchUrls(selectedSport, teamName)
                        onImport(searchUrls)
                    }
                },
                enabled = selectedTeam != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Team Jerseys")
            }
        }
    }
}

@Composable
fun QuickImportTab(
    onImport: (List<String>) -> Unit
) {
    Column {
        Text(
            text = "⚡ Quick Import Sources",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Import from curated jersey databases:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val quickSources = JerseyPhotoSources.getQuickImportUrls()
        
        quickSources.forEach { (sport, urls) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onImport(urls) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${sport.capitalize()} Jerseys",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${urls.size} curated photos available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Import",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sample Jersey Numbers Info
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💡 Training Tips",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "• Import 50+ photos per jersey number for best accuracy\n" +
                          "• Include different lighting conditions and angles\n" +
                          "• Validate each photo to ensure correct number detection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}