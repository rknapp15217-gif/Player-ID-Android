package com.playerid.app.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import com.playerid.app.data.Team

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSelectionDropdown(selectedTeam: String?, availableTeams: List<Team>, onTeamSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selectedTeam ?: "No team selected", onValueChange = {}, readOnly = true, label = { Text("Active Team") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("🚫 No team (detect all)") }, onClick = { onTeamSelected(null); expanded = false })
            availableTeams.forEach { team -> DropdownMenuItem(text = { Text(team.name) }, onClick = { onTeamSelected(team.name); expanded = false }) }
        }
    }
}
