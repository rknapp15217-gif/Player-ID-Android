package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.playerid.app.data.TeamWithPlayerCount
import com.playerid.app.ui.components.*
import com.playerid.app.ui.dialogs.AddTeamDialog
import com.playerid.app.ui.theme.*
import com.playerid.app.viewmodels.TeamViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrowdSourcedTeamsScreen(
    teamViewModel: TeamViewModel,
    onTeamSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val teamsWithStats by teamViewModel.teamsWithStats.collectAsState()
    val allTeamNames by teamViewModel.allTeamNames.collectAsState()
    val subscribedTeams by teamViewModel.subscribedTeams.collectAsState()
    var showAddTeamDialog by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf<TeamWithPlayerCount?>(null) }
    var showTeamDetails by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    // Debounce search query for better performance with thousands of teams
    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(300) // Wait 300ms after user stops typing
        debouncedSearchQuery = searchQuery
    }
    
    // Filter teams based on debounced search query - only show results when typing
    val filteredTeams = remember(teamsWithStats, debouncedSearchQuery) {
        if (debouncedSearchQuery.isBlank() || debouncedSearchQuery.length < 2) {
            emptyList() // Don't show any teams until user starts typing (min 2 chars)
        } else {
            teamsWithStats.filter { team ->
                team.name.contains(debouncedSearchQuery, ignoreCase = true) ||
                team.createdBy.contains(debouncedSearchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Modern header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "All Teams",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Browse crowd-sourced team rosters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                }
                .imePadding(), // Handle keyboard padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search bar for finding teams
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Type to search teams...") },
                placeholder = { Text("e.g. \"Nor\" → North Allegheny") },
                leadingIcon = { 
                    Icon(Icons.Default.Search, contentDescription = "Search") 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add new team button
            Button(
                onClick = { showAddTeamDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Team")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Statistics overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isBlank() || searchQuery.length < 2) "${teamsWithStats.size}" else "${filteredTeams.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpotrGreen
                        )
                        Text(
                            text = if (searchQuery.isBlank() || searchQuery.length < 2) "Total Teams" else "Found Teams",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isBlank() || searchQuery.length < 2) "${teamsWithStats.sumOf { it.playerCount }}" else "${filteredTeams.sumOf { it.playerCount }}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpotrBlue
                        )
                        Text(
                            text = if (searchQuery.isBlank() || searchQuery.length < 2) "Total Players" else "Players Found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Teams list
            if (filteredTeams.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                focusManager.clearFocus()
                            }
                        },
                    contentPadding = WindowInsets.ime.asPaddingValues()
                ) {
                    items(filteredTeams) { teamWithStats ->
                        val isSubscribed = subscribedTeams.any { it.name == teamWithStats.name }
                        TeamStatCard(
                            teamWithStats = teamWithStats,
                            isSubscribed = isSubscribed,
                            onSelectTeam = { onTeamSelected(teamWithStats.name) },
                            onSubscriptionToggle = { 
                                if (isSubscribed) {
                                    teamViewModel.unsubscribeFromTeam(teamWithStats.name)
                                } else {
                                    teamViewModel.subscribeToTeam(teamWithStats.name)
                                }
                            },
                            onViewDetails = { 
                                selectedTeam = teamWithStats
                                showTeamDetails = true
                            }
                        )
                    }
                }
            } else if (searchQuery.isNotBlank()) {
                // No search results state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No teams found for \"$debouncedSearchQuery\"",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try searching for:\n• Team name (e.g. \"North Allegheny\")\n• Creator name\n• Part of the team name",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { 
                        searchQuery = ""
                        focusManager.clearFocus()
                    }) {
                        Text("Clear Search")
                    }
                }
            } else {
                // Search prompt state (no search query entered)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Search for Teams",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start typing to find teams like \"North Allegheny\" or \"True Lacrosse\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "💡 Tip: Type at least 2 characters to search",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // Add team dialog with duplicate detection
    if (showAddTeamDialog) {
        AddTeamDialog(
            onDismiss = { showAddTeamDialog = false },
            onAdd = { teamName ->
                teamViewModel.addTeam(teamName, "Community contributed team")
                showAddTeamDialog = false
            },
            existingTeams = allTeamNames
        )
    }
    
    // Team details dialog
    if (showTeamDetails && selectedTeam != null) {
        val isSelectedTeamSubscribed = subscribedTeams.any { it.name == selectedTeam!!.name }
        TeamDetailsDialog(
            teamWithStats = selectedTeam!!,
            teamViewModel = teamViewModel,
            isSubscribed = isSelectedTeamSubscribed,
            onDismiss = { 
                showTeamDetails = false
                selectedTeam = null
            },
            onSelectTeam = { teamName ->
                onTeamSelected(teamName)
                showTeamDetails = false
                selectedTeam = null
            },
            onSubscriptionToggle = { 
                if (isSelectedTeamSubscribed) {
                    teamViewModel.unsubscribeFromTeam(selectedTeam!!.name)
                } else {
                    teamViewModel.subscribeToTeam(selectedTeam!!.name)
                }
            }
        )
    }
}

@Composable
fun TeamStatCard(
    teamWithStats: TeamWithPlayerCount,
    isSubscribed: Boolean,
    onSelectTeam: () -> Unit,
    onSubscriptionToggle: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teamWithStats.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (teamWithStats.description.isNotBlank()) {
                        Text(
                            text = teamWithStats.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SpotrBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${teamWithStats.playerCount} players",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SpotrGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "by ${teamWithStats.createdBy}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Verification badge
                        if (teamWithStats.isVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified Team",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Verified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // Community feedback indicator
                        if (teamWithStats.reportCount > 0 || teamWithStats.verificationCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (teamWithStats.verificationCount > teamWithStats.reportCount) 
                                        Icons.Default.ThumbUp 
                                    else 
                                        Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (teamWithStats.verificationCount > teamWithStats.reportCount) 
                                        SpotrGreen 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${teamWithStats.verificationCount}✓ ${teamWithStats.reportCount}⚠",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Team color indicator
                if (teamWithStats.color.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                        // Note: You'd need to parse the color string to show actual color
                    ) {
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = "Team color",
                            tint = SpotrOrange // Placeholder - would parse actual color
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Details")
                }
                
                if (isSubscribed) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Button(
                            onClick = onSelectTeam,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select")
                        }
                        
                        OutlinedButton(
                            onClick = onSubscriptionToggle,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onSubscriptionToggle,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Join Team")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailsDialog(
    teamWithStats: TeamWithPlayerCount,
    teamViewModel: TeamViewModel,
    isSubscribed: Boolean,
    onDismiss: () -> Unit,
    onSelectTeam: (String) -> Unit,
    onSubscriptionToggle: () -> Unit
) {
    var contributors by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(teamWithStats.name) {
        contributors = teamViewModel.getTeamContributors(teamWithStats.name)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = teamWithStats.name,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                if (teamWithStats.description.isNotBlank()) {
                    Text(
                        text = teamWithStats.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Players",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${teamWithStats.playerCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SpotrBlue
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Contributors",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${contributors.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SpotrGreen
                        )
                    }
                }
                
                if (contributors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Team Contributors:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    contributors.forEach { contributor ->
                        Text(
                            text = "• $contributor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Created by: ${teamWithStats.createdBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Community verification info
                if (teamWithStats.reportCount > 0 || teamWithStats.verificationCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Community Feedback: ${teamWithStats.verificationCount} verifications, ${teamWithStats.reportCount} reports",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Community action buttons
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            teamViewModel.verifyTeam(teamWithStats.name)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify")
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            teamViewModel.reportTeamAsDuplicate(teamWithStats.name)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report")
                    }
                }
            }
        },
        confirmButton = {
            if (isSubscribed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSelectTeam(teamWithStats.name) }
                    ) {
                        Text("Select Team")
                    }
                    OutlinedButton(
                        onClick = onSubscriptionToggle,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Leave Team")
                    }
                }
            } else {
                Button(
                    onClick = onSubscriptionToggle
                ) {
                    Text("Join Team")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}