package com.playerid.app.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.playerid.app.viewmodels.VoiceAssistantResult
import kotlinx.coroutines.delay

@Composable
fun VoiceResultCard(result: VoiceAssistantResult, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(result) {
        delay(4000)
        onDismiss()
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is VoiceAssistantResult.Success -> MaterialTheme.colorScheme.primaryContainer
                is VoiceAssistantResult.Error -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (result) {
                    is VoiceAssistantResult.Success -> Icons.Default.RecordVoiceOver
                    is VoiceAssistantResult.Error -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (result) {
                    is VoiceAssistantResult.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                    is VoiceAssistantResult.Error -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (result) {
                        is VoiceAssistantResult.Success -> "Voice ID Found"
                        is VoiceAssistantResult.Error -> "Try Again"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (result) {
                        is VoiceAssistantResult.Success -> result.message
                        is VoiceAssistantResult.Error -> result.message
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Dismiss") }
        }
    }
}
