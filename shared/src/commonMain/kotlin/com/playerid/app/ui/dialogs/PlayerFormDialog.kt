package com.playerid.app.ui.dialogs

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.domain.team.PlayerFormEvent
import com.playerid.app.domain.team.PlayerFormOptions
import com.playerid.app.domain.team.PlayerFormState
import com.playerid.app.domain.team.PlayerFormSubmission

@Composable
internal expect fun rememberSoftwareKeyboardDismissHandler(): () -> Unit

@Composable
internal fun rememberDialogKeyboardDismissHandler(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val hideSoftwareKeyboard = rememberSoftwareKeyboardDismissHandler()
    return {
        focusManager.clearFocus(force = true)
        hideSoftwareKeyboard()
    }
}

@Composable
fun PlayerFormDialog(
    title: String,
    submitLabel: String,
    formState: PlayerFormState,
    onEvent: (PlayerFormEvent) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (PlayerFormSubmission) -> Unit,
    showTeamField: Boolean,
    availableTeams: List<String>,
    showContactFields: Boolean = false,
    contactPhone: String = "",
    contactEmail: String = "",
    onContactPhoneChange: (String) -> Unit = {},
    onContactEmailChange: (String) -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        val dismissKeyboard = rememberDialogKeyboardDismissHandler()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete player", color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        dismissKeyboard()
                        onDismiss()
                    }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            dismissKeyboard()
                            formState.submission()?.let(onSubmit)
                        },
                        enabled = formState.canSubmit
                    ) {
                        Text(submitLabel)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {

                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { onEvent(PlayerFormEvent.NameChanged(it)) },
                    label = { Text("Player Name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.number,
                    onValueChange = { onEvent(PlayerFormEvent.NumberChanged(it)) },
                    label = { Text("Jersey Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.position,
                    onValueChange = { onEvent(PlayerFormEvent.PositionChanged(it)) },
                    label = { Text("Position") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                    singleLine = true,
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

                if (showContactFields) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = onContactPhoneChange,
                        label = { Text("Contact Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = onContactEmailChange,
                        label = { Text("Contact Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                }

            }
        }
    }
}

@Composable
private fun PlayerFormDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label: $value")
    }
    if (expanded) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
            options.forEach { option ->
                TextButton(
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(option)
                }
            }
        }
    }
    }
}