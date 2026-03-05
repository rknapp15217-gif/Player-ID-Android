package com.playerid.app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.data.AcademicYear
import com.playerid.app.data.Player
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerDialog(
    teamName: String? = null,
    onDismiss: () -> Unit,
    onAdd: (Player) -> Unit,
    availableTeams: List<String> = listOf("Red Team", "Blue Team", "Green Team", "Yellow Team"),
    currentUser: String = "Unknown"
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var team by remember { mutableStateOf(teamName ?: "Red Team") }
    var academicYear by remember { mutableStateOf(AcademicYear.FRESHMAN.displayName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (teamName != null) "Add New Player to $teamName" else "Add New Player",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Jersey Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Position") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (teamName == null) {
                    var teamExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = teamExpanded,
                        onExpandedChange = { teamExpanded = !teamExpanded }
                    ) {
                        OutlinedTextField(
                            value = team,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Team") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = teamExpanded,
                            onDismissRequest = { teamExpanded = false }
                        ) {
                            availableTeams.forEach { teamOption ->
                                DropdownMenuItem(
                                    text = { Text(teamOption) },
                                    onClick = {
                                        team = teamOption
                                        teamExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                var yearExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = !yearExpanded }
                ) {
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Academic Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        AcademicYear.values().forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.displayName) },
                                onClick = {
                                    academicYear = year.displayName
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (name.isNotBlank() && number.isNotBlank() && position.isNotBlank()) {
                                val player = Player(
                                    id = UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    number = number.trim(),
                                    position = position.trim(),
                                    team = team,
                                    academicYear = academicYear,
                                    addedBy = currentUser
                                )
                                onAdd(player)
                            }
                        },
                        enabled = name.isNotBlank() && number.isNotBlank() && position.isNotBlank()
                    ) {
                        Text("Add Player")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlayerDialog(
    player: Player,
    onDismiss: () -> Unit,
    onSave: (Player) -> Unit,
    hideTeamField: Boolean = false, // New parameter to hide team field in team context
    availableTeams: List<String> = listOf("Red Team", "Blue Team", "Green Team", "Yellow Team")
) {
    var name by remember { mutableStateOf(player.name) }
    var number by remember { mutableStateOf(player.number) }
    var position by remember { mutableStateOf(player.position) }
    var team by remember { mutableStateOf(player.team) }
    var academicYear by remember { mutableStateOf(player.academicYear) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Edit Player",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Jersey Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Position") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!hideTeamField) {
                    Spacer(modifier = Modifier.height(8.dp))

                    var teamExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = teamExpanded,
                        onExpandedChange = { teamExpanded = !teamExpanded }
                    ) {
                        OutlinedTextField(
                            value = team,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Team") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = teamExpanded,
                            onDismissRequest = { teamExpanded = false }
                        ) {
                            availableTeams.forEach { teamOption ->
                                DropdownMenuItem(
                                    text = { Text(teamOption) },
                                    onClick = {
                                        team = teamOption
                                        teamExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                var yearExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = !yearExpanded }
                ) {
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Academic Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        AcademicYear.values().forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.displayName) },
                                onClick = {
                                    academicYear = year.displayName
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank() && number.isNotBlank() && position.isNotBlank()) {
                                val updatedPlayer = player.copy(
                                    name = name.trim(),
                                    number = number.trim(),
                                    position = position.trim(),
                                    team = team,
                                    academicYear = academicYear
                                )
                                onSave(updatedPlayer)
                            }
                        },
                        enabled = name.isNotBlank() && number.isNotBlank() && position.isNotBlank()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun AddTeamDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    existingTeams: List<String> = emptyList()
) {
    var teamName by remember { mutableStateOf("") }
    val similarTeams = remember(teamName, existingTeams) {
        if (teamName.isNotBlank() && existingTeams.isNotEmpty()) {
            com.playerid.app.utils.TeamSimilarityUtil.findSimilarTeams(teamName, existingTeams)
        } else emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Team") },
        text = {
            Column {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    supportingText = {
                        if (similarTeams.isNotEmpty()) {
                            Text(
                                "⚠️ Similar teams found - check before creating",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    isError = similarTeams.isNotEmpty()
                )
                
                // Show similar teams warning
                if (similarTeams.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "Similar teams already exist:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            similarTeams.take(3).forEach { similarTeam ->
                                Text(
                                    "• ${similarTeam.name} (${(similarTeam.similarity * 100).toInt()}% match)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (similarTeams.size > 3) {
                                Text(
                                    "... and ${similarTeams.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (teamName.isNotBlank()) {
                        onAdd(teamName)
                    }
                },
                enabled = teamName.isNotBlank()
            ) {
                Text(if (similarTeams.isNotEmpty()) "Create Anyway" else "Create Team")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameTeamDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newTeamName by remember { mutableStateOf(teamName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Team") },
        text = {
            OutlinedTextField(
                value = newTeamName,
                onValueChange = { newTeamName = it },
                label = { Text("New Team Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTeamName.isNotBlank()) {
                        onRename(newTeamName)
                    }
                },
                enabled = newTeamName.isNotBlank()
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteTeamDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave Team") },
        text = { Text("Are you sure you want to leave the team '$teamName'? You can rejoin from Browse All Teams later.") },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Leave Team")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeletePlayerDialog(
    player: com.playerid.app.data.Player,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Player") },
        text = { 
            Column {
                Text("Are you sure you want to delete ${player.name} (#${player.number})?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ This will remove the player for ALL users who have subscribed to this team.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Player")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}