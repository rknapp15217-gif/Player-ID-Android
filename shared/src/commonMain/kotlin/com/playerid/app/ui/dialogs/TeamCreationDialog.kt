package com.playerid.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.TeamCreationEvent
import com.playerid.app.domain.team.TeamCreationFormState
import com.playerid.app.domain.team.TeamCreationOptions
import com.playerid.app.domain.team.TeamCreationSubmission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCreationDialog(
    existingTeams: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (TeamCreationSubmission) -> Unit,
    colorSwatch: @Composable (
        colorHex: String,
        selected: Boolean,
        onClick: () -> Unit
    ) -> Unit,
    colorPicker: @Composable (
        label: String,
        selectedColorHex: String,
        onColorSelected: (String) -> Unit
    ) -> Unit
) {
    var formState by remember { mutableStateOf(TeamCreationFormState()) }
    var activeColorTarget by remember { mutableStateOf<TeamColorTarget?>(null) }
    var sportDropdownExpanded by remember { mutableStateOf(false) }
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
                        formState = formState.reduce(TeamCreationEvent.TeamNameChanged(it))
                    },
                    label = { Text("Team Name") },
                    supportingText = {
                        if (similarTeams.isNotEmpty()) {
                            Text(
                                "\u26A0\uFE0F Similar teams found - check before creating",
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
                        TeamCreationOptions.sports.forEach { sport ->
                            DropdownMenuItem(
                                text = { Text(sport) },
                                onClick = {
                                    formState = formState.reduce(TeamCreationEvent.SportSelected(sport))
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
                    TeamColorTarget.values().forEach { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(target.label)
                            colorSwatch(
                                target.color(formState),
                                activeColorTarget == target
                            ) {
                                activeColorTarget = if (activeColorTarget == target) null else target
                            }
                        }
                    }
                }

                activeColorTarget?.let { target ->
                    Spacer(modifier = Modifier.height(8.dp))
                    colorPicker(
                        "Select ${target.label} Color",
                        target.color(formState)
                    ) { selectedHex ->
                        formState = formState.reduce(target.colorEvent(selectedHex))
                        activeColorTarget = null
                    }
                }

                if (similarTeams.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Similar teams already exist:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            similarTeams.take(3).forEach { similarTeam ->
                                Text(
                                    "\u2022 ${similarTeam.name} (${(similarTeam.similarity * 100).toInt()}% match)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (similarTeams.size > 3) {
                                Text(
                                    "... and ${similarTeams.size - 3} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { formState.submission()?.let(onSubmit) },
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

private enum class TeamColorTarget(val label: String) {
    Home("Home Jersey"),
    Away("Away Jersey");

    fun color(state: TeamCreationFormState): String = when (this) {
        Home -> state.homeJerseyColor
        Away -> state.awayJerseyColor
    }

    fun colorEvent(colorHex: String): TeamCreationEvent = when (this) {
        Home -> TeamCreationEvent.HomeJerseyColorSelected(colorHex)
        Away -> TeamCreationEvent.AwayJerseyColorSelected(colorHex)
    }
}