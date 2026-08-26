package com.playerid.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.data.Player
import com.playerid.app.data.repositories.toProfile
import com.playerid.app.domain.team.PlayerFormEvent
import com.playerid.app.domain.team.PlayerFormOptions
import com.playerid.app.domain.team.PlayerFormState
import com.playerid.app.domain.team.TeamCreationEvent
import com.playerid.app.domain.team.TeamCreationFormState
import com.playerid.app.domain.team.TeamCreationOptions
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
    var formState by remember {
        mutableStateOf(PlayerFormState.forNewPlayer(teamName ?: "Red Team"))
    }

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
                    value = formState.name,
                    onValueChange = { formState = formState.reduce(PlayerFormEvent.NameChanged(it)) },
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = formState.number,
                    onValueChange = { formState = formState.reduce(PlayerFormEvent.NumberChanged(it)) },
                    label = { Text("Jersey Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = formState.position,
                    onValueChange = { formState = formState.reduce(PlayerFormEvent.PositionChanged(it)) },
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
                            value = formState.teamName,
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
                                        formState = formState.reduce(PlayerFormEvent.TeamSelected(teamOption))
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
                        value = formState.academicYear,
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
                        PlayerFormOptions.academicYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year) },
                                onClick = {
                                    formState = formState.reduce(PlayerFormEvent.AcademicYearSelected(year))
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
                            formState.submission()?.let { submission ->
                                val player = Player(
                                    id = UUID.randomUUID().toString(),
                                    name = submission.name,
                                    number = submission.number,
                                    position = submission.position,
                                    team = submission.teamName,
                                    academicYear = submission.academicYear,
                                    addedBy = currentUser
                                )
                                onAdd(player)
                            }
                        },
                        enabled = formState.canSubmit
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
    var formState by remember(player) {
        mutableStateOf(PlayerFormState.forEditing(player.toProfile()))
    }

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
                    value = formState.name,
                    onValueChange = { formState = formState.reduce(PlayerFormEvent.NameChanged(it)) },
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.number,
                    onValueChange = { formState = formState.reduce(PlayerFormEvent.NumberChanged(it)) },
                    label = { Text("Jersey Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.position,
                    onValueChange = { formState = formState.reduce(PlayerFormEvent.PositionChanged(it)) },
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
                            value = formState.teamName,
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
                                        formState = formState.reduce(PlayerFormEvent.TeamSelected(teamOption))
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
                        value = formState.academicYear,
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
                        PlayerFormOptions.academicYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year) },
                                onClick = {
                                    formState = formState.reduce(PlayerFormEvent.AcademicYearSelected(year))
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
                            formState.submission()?.let { submission ->
                                val updatedPlayer = player.copy(
                                    name = submission.name,
                                    number = submission.number,
                                    position = submission.position,
                                    team = submission.teamName,
                                    academicYear = submission.academicYear
                                )
                                onSave(updatedPlayer)
                            }
                        },
                        enabled = formState.canSubmit
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeamDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String) -> Unit,
    existingTeams: List<String> = emptyList()
) {
    var formState by remember { mutableStateOf(TeamCreationFormState()) }
    var activeColorTarget by remember { mutableStateOf<String?>(null) }
    var sportDropdownExpanded by remember { mutableStateOf(false) }
    val presetColors = TeamCreationOptions.presetColors
    val sports = TeamCreationOptions.sports
    
    val similarTeams = remember(formState.teamName, existingTeams) {
        formState.similarTeams(existingTeams)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Team") },
        text = {
            Column {
                OutlinedTextField(
                    value = formState.teamName,
                    onValueChange = {
                        formState = formState.reduce(
                            TeamCreationEvent.TeamNameChanged(it)
                        )
                    },
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ExposedDropdownMenuBox(
                    expanded = sportDropdownExpanded,
                    onExpandedChange = { sportDropdownExpanded = !sportDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = formState.sport,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sport") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportDropdownExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sportDropdownExpanded,
                        onDismissRequest = { sportDropdownExpanded = false }
                    ) {
                        sports.forEach { sport ->
                            DropdownMenuItem(
                                text = { Text(sport) },
                                onClick = {
                                    formState = formState.reduce(
                                        TeamCreationEvent.SportSelected(sport)
                                    )
                                    sportDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Colors",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Home Jersey" to formState.homeJerseyColor,
                        "Away Jersey" to formState.awayJerseyColor
                    ).forEach { (label, colorHex) ->
                        val swatchColor = Color(android.graphics.Color.parseColor(colorHex))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(swatchColor, CircleShape)
                                    .border(
                                        width = if (activeColorTarget == label) 3.dp else 1.dp,
                                        color = if (activeColorTarget == label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        activeColorTarget = if (activeColorTarget == label) null else label
                                    }
                            )
                        }
                    }
                }

                activeColorTarget?.let { target ->
                    Spacer(modifier = Modifier.height(8.dp))
                    TeamColorPickerField(
                        label = "Select $target Color",
                        selectedColorHex = if (target == "Home Jersey") {
                            formState.homeJerseyColor
                        } else {
                            formState.awayJerseyColor
                        },
                        onColorSelected = { selectedHex ->
                            formState = if (target == "Home Jersey") {
                                formState.reduce(
                                    TeamCreationEvent.HomeJerseyColorSelected(selectedHex)
                                )
                            } else {
                                formState.reduce(
                                    TeamCreationEvent.AwayJerseyColorSelected(selectedHex)
                                )
                            }
                            activeColorTarget = null
                        },
                        presetColors = presetColors,
                        showHexValue = false
                    )
                }
                
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
                    formState.submission()?.let { submission ->
                        onAdd(
                            submission.teamName,
                            submission.sport,
                            submission.homeJerseyColor,
                            submission.awayJerseyColor,
                            submission.homeJerseyColor,
                            submission.awayJerseyColor
                        )
                    }
                },
                enabled = formState.canSubmit
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
fun EditTeamSettingsDialog(
    teamName: String,
    initialHomeColor: String,
    initialAwayColor: String,
    initialHomeJerseyColor: String,
    initialAwayJerseyColor: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var newTeamName by remember(teamName) { mutableStateOf(teamName) }
    val jerseyColors = remember(teamName) {
        val prefs = context.getSharedPreferences("team_jersey_colors", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString(teamName, null)
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        mutableStateListOf(*(
            listOf(initialHomeJerseyColor, initialAwayJerseyColor)
                .plus(saved)
                .distinct()
                .toTypedArray()
        ))
    }
    var activeColorTarget by remember { mutableStateOf("Jersey 1") }

    val presetColors = listOf(
        "Navy" to "#0B3D91",
        "Royal" to "#1976D2",
        "Red" to "#E53E3E",
        "Maroon" to "#7A0019",
        "Green" to "#059669",
        "Black" to "#111827",
        "White" to "#FFFFFF",
        "Gray" to "#9CA3AF",
        "Gold" to "#D4AF37",
        "Orange" to "#EA580C",
        "Purple" to "#7C3AED",
        "Teal" to "#0D9488"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Team Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Team Name",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = newTeamName,
                    onValueChange = { newTeamName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                val activeColorIndex = activeColorTarget.removePrefix("Jersey ").toIntOrNull()?.minus(1) ?: 0
                val currentSelectedColor = jerseyColors.getOrElse(activeColorIndex) {
                    jerseyColors.firstOrNull() ?: "#FFFFFF"
                }

                Text("Primary Jersey Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                jerseyColors.forEachIndexed { index, colorHex ->
                    val label = "Jersey ${index + 1}"
                    val swatchColor = Color(android.graphics.Color.parseColor(colorHex))
                    OutlinedButton(
                        onClick = { activeColorTarget = label },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (activeColorTarget == label) 2.dp else 1.dp,
                            color = if (activeColorTarget == label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(swatchColor, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                if (jerseyColors.size > 1) {
                                    IconButton(
                                        onClick = {
                                            jerseyColors.removeAt(index)
                                            if (activeColorTarget == label || activeColorIndex !in jerseyColors.indices) {
                                                activeColorTarget = "Jersey 1"
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove $label",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = {
                        if (jerseyColors.size < 4) {
                            jerseyColors.add("#FFFFFF")
                            activeColorTarget = "Jersey ${jerseyColors.size}"
                        }
                    },
                    enabled = jerseyColors.size < 4
                ) {
                    Text("+ Jersey Color")
                }
                if (jerseyColors.size >= 4) {
                    Text(
                        text = "Maximum 4 jersey colors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TeamColorPickerField(
                    label = "Select Color for $activeColorTarget",
                    selectedColorHex = currentSelectedColor,
                    onColorSelected = { selectedHex ->
                        val index = activeColorTarget.removePrefix("Jersey ").toIntOrNull()?.minus(1) ?: -1
                        if (index in jerseyColors.indices) {
                            jerseyColors[index] = selectedHex
                        }
                    },
                    presetColors = presetColors,
                    showHexValue = false
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedJersey = if (jerseyColors.isEmpty()) listOf("#FFFFFF") else jerseyColors.toList()
                    val normalizedTeamName = newTeamName.trim()
                    context.getSharedPreferences("team_jersey_colors", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putString(normalizedTeamName, normalizedJersey.joinToString("|"))
                        .apply {
                            if (normalizedTeamName != teamName) remove(teamName)
                        }
                        .apply()

                    onSave(
                        normalizedTeamName,
                        initialHomeColor,
                        initialAwayColor,
                        normalizedJersey.getOrElse(0) { "#FFFFFF" },
                        normalizedJersey.getOrElse(1) { normalizedJersey.firstOrNull() ?: "#FFFFFF" }
                    )
                },
                enabled = newTeamName.isNotBlank()
            ) {
                Text("Save")
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

@Composable
private fun TeamColorPickerField(
    label: String,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    presetColors: List<Pair<String, String>>,
    showHexValue: Boolean = true
) {
    var showCustom by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { showCustom = !showCustom }) {
                Text(if (showCustom) "Use Presets" else "Custom")
            }
        }

        if (showCustom) {
            TeamColorWheelPicker(
                label = "",
                selectedColorHex = selectedColorHex,
                onColorSelected = onColorSelected
            )
        } else {
            presetColors.chunked(4).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowColors.forEach { (name, hex) ->
                        val swatchColor = Color(android.graphics.Color.parseColor(hex))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(swatchColor, CircleShape)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                        color = if (selectedColorHex == hex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelected(hex) }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (showHexValue) {
                Text(
                    text = selectedColorHex,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TeamColorWheelPicker(
    label: String,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    var hue by remember(selectedColorHex) { mutableStateOf(hexToHue(selectedColorHex)) }

    Column {
        if (label.isNotBlank()) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
        }

        val sweepColors = remember {
            listOf(
                Color(0xFFFF0000),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00FFFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000)
            )
        }

        val selectedColor = remember(hue) {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.95f)))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val p = change.position
                            val angle = Math.toDegrees(
                                kotlin.math.atan2((p.y - center.y).toDouble(), (p.x - center.x).toDouble())
                            ).toFloat()
                            hue = (angle + 360f + 90f) % 360f
                            onColorSelected(hueToHex(hue))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.sweepGradient(sweepColors),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = size.minDimension * 0.24f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(selectedColor, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(selectedColorHex, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun hueToHex(hue: Float): String {
    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.95f))
    return String.format("#%06X", 0xFFFFFF and colorInt)
}

private fun hexToHue(hex: String): Float {
    return try {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(hex), hsv)
        hsv[0]
    } catch (_: Exception) {
        210f
    }
}