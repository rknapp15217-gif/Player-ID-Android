package com.playerid.app.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.playerid.app.data.Team

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGameInfoSheet(
    teams: List<Team>,
    initialTeam: Team?,
    initialColor: String?,
    initialOpponent: String?,
    onSave: (Team, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTeam by remember { mutableStateOf(initialTeam) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var opponent by remember { mutableStateOf(initialOpponent ?: "") }
    var teamDropdownExpanded by remember { mutableStateOf(false) }
    var showOpponentField by remember { mutableStateOf(opponent.isNotBlank()) }

    val handleDismiss = {
        if (selectedTeam != null && selectedColor != null) {
            onSave(selectedTeam!!, selectedColor!!, opponent)
        }
        onDismiss()
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 200.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Today's Game", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            // Team selection row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = teamDropdownExpanded,
                    onExpandedChange = { teamDropdownExpanded = it }
                ) {
                    TextField(
                        value = selectedTeam?.name ?: "Select team",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Team") },
                        modifier = Modifier
                            .width(180.dp)
                            .menuAnchor()
                            .clickable { teamDropdownExpanded = true }
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = teamDropdownExpanded,
                        onDismissRequest = { teamDropdownExpanded = false }
                    ) {
                        teams.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name) },
                                onClick = {
                                    selectedTeam = team
                                    selectedColor = team.color
                                    teamDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                if (selectedTeam != null) {
                    Spacer(Modifier.width(12.dp))
                    val colorOptions = listOf(selectedTeam!!.color, "#FFFFFF")
                    Row {
                        colorOptions.forEach { colorHex ->
                            val colorInt = android.graphics.Color.parseColor(colorHex)
                            val isSelected = selectedColor == colorHex
                            val borderColor = when {
                                isSelected && colorHex.equals("#1976D2", ignoreCase = true) -> Color.Black
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(colorInt), CircleShape)
                                    .clickable {
                                        selectedColor = colorHex
                                    }
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = borderColor,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Add Opponent button and field below team selection, left-aligned
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (!showOpponentField) {
                    TextButton(
                        onClick = { showOpponentField = true }
                    ) {
                        Text("Add Opponent")
                    }
                }
            }
            if (showOpponentField) {
                OutlinedTextField(
                    value = opponent,
                    onValueChange = { opponent = it },
                    label = { Text("Opponent (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    DisposableEffect(Unit) {
        onDispose { handleDismiss() }
    }
}
