package com.playerid.app.ui.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.TeamCreationEvent
import com.playerid.app.domain.team.TeamCreationFormState
import com.playerid.app.domain.team.TeamCreationOptions
import com.playerid.app.domain.team.TeamCreationSubmission
import com.playerid.app.domain.team.teamHexToHue
import com.playerid.app.domain.team.teamHueToHex
import kotlin.math.PI
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCreationDialog(
    existingTeams: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (TeamCreationSubmission) -> Unit
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
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(teamComposeColor(target.color(formState)), CircleShape)
                                    .border(
                                        width = if (activeColorTarget == target) 3.dp else 1.dp,
                                        color = if (activeColorTarget == target) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        activeColorTarget = if (activeColorTarget == target) null else target
                                    }
                            )
                        }
                    }
                }

                activeColorTarget?.let { target ->
                    Spacer(modifier = Modifier.height(8.dp))
                    TeamColorPickerField(
                        label = "Select ${target.label} Color",
                        selectedColorHex = target.color(formState),
                        onColorSelected = { selectedHex ->
                            formState = formState.reduce(target.colorEvent(selectedHex))
                            activeColorTarget = null
                        },
                        presetColors = TeamCreationOptions.presetColors,
                        showHexValue = false
                    )
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

@Composable
fun TeamColorPickerField(
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
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(teamComposeColor(hex), CircleShape)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                        color = if (selectedColorHex == hex) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
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
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    var hue by remember(selectedColorHex) { mutableStateOf(teamHexToHue(selectedColorHex)) }
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
    val selectedColor = remember(hue) { teamComposeColor(teamHueToHex(hue)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val position = change.position
                        val angle = atan2(
                            position.y - center.y,
                            position.x - center.x
                        ) * 180f / PI.toFloat()
                        hue = (angle + 450f) % 360f
                        onColorSelected(teamHueToHex(hue))
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
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

private fun teamComposeColor(hex: String): Color {
    val rgb = hex.removePrefix("#").toLongOrNull(16) ?: 0L
    return Color(0xFF000000L or rgb)
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