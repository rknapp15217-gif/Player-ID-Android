package com.playerid.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 📸 Jersey Number Annotation Dialog
 * 
 * Used for manually annotating jersey numbers during data collection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JerseyAnnotationDialog(
    onDismiss: () -> Unit,
    onAnnotate: (jerseyNumber: Int, lightingCondition: String, distance: String, angle: String) -> Unit
) {
    var jerseyNumber by remember { mutableStateOf("") }
    var lightingCondition by remember { mutableStateOf("normal") }
    var distance by remember { mutableStateOf("medium") }
    var angle by remember { mutableStateOf("front") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📸 Annotate Jersey Number",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                // Jersey Number Input
                OutlinedTextField(
                    value = jerseyNumber,
                    onValueChange = { 
                        if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                            jerseyNumber = it
                        }
                    },
                    label = { Text("Jersey Number (0-99)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Lighting Condition
                Column {
                    Text("Lighting Condition:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("bright", "normal", "dark").forEach { condition ->
                            FilterChip(
                                onClick = { lightingCondition = condition },
                                label = { Text(condition.capitalize()) },
                                selected = lightingCondition == condition
                            )
                        }
                    }
                }
                
                // Distance
                Column {
                    Text("Distance:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("close", "medium", "far").forEach { dist ->
                            FilterChip(
                                onClick = { distance = dist },
                                label = { Text(dist.capitalize()) },
                                selected = distance == dist
                            )
                        }
                    }
                }
                
                // Angle  
                Column {
                    Text("Camera Angle:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("front", "side", "angled").forEach { ang ->
                            FilterChip(
                                onClick = { angle = ang },
                                label = { Text(ang.capitalize()) },
                                selected = angle == ang
                            )
                        }
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
                    
                    Button(
                        onClick = {
                            val number = jerseyNumber.toIntOrNull()
                            if (number != null && number in 0..99) {
                                onAnnotate(number, lightingCondition, distance, angle)
                            }
                        },
                        enabled = jerseyNumber.toIntOrNull()?.let { it in 0..99 } == true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📸 Collect")
                    }
                }
            }
        }
    }
}