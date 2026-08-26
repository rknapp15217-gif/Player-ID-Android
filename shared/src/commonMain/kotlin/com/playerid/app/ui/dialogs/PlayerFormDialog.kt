package com.playerid.app.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.domain.team.PlayerFormEvent
import com.playerid.app.domain.team.PlayerFormOptions
import com.playerid.app.domain.team.PlayerFormState
import com.playerid.app.domain.team.PlayerFormSubmission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFormDialog(
    title: String,
    submitLabel: String,
    formState: PlayerFormState,
    onEvent: (PlayerFormEvent) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (PlayerFormSubmission) -> Unit,
    showTeamField: Boolean,
    availableTeams: List<String>
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { onEvent(PlayerFormEvent.NameChanged(it)) },
                    label = { Text("Player Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.number,
                    onValueChange = { onEvent(PlayerFormEvent.NumberChanged(it)) },
                    label = { Text("Jersey Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.position,
                    onValueChange = { onEvent(PlayerFormEvent.PositionChanged(it)) },
                    label = { Text("Position") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showTeamField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PlayerFormDropdown(
                        label = "Team",
                        value = formState.teamName,
                        options = availableTeams,
                        onSelected = { onEvent(PlayerFormEvent.TeamSelected(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PlayerFormDropdown(
                    label = "Academic Year",
                    value = formState.academicYear,
                    options = PlayerFormOptions.academicYears,
                    onSelected = { onEvent(PlayerFormEvent.AcademicYearSelected(it)) }
                )

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
                        onClick = { formState.submission()?.let(onSubmit) },
                        enabled = formState.canSubmit
                    ) {
                        Text(submitLabel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerFormDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}