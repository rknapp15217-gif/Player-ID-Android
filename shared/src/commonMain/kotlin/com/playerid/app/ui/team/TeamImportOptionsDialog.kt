package com.playerid.app.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.TeamImportSource

@Composable
fun TeamImportOptionsDialog(
    title: String,
    helpText: String,
    sourceLabel: @Composable (TeamImportSource) -> String,
    sourceIcon: @Composable (TeamImportSource) -> Unit,
    onSourceSelected: (TeamImportSource) -> Unit,
    onDismiss: () -> Unit,
    closeLabel: String = "Close"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = helpText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TeamImportSource.values().forEach { source ->
                    OutlinedButton(
                        onClick = { onSourceSelected(source) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            sourceIcon(source)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sourceLabel(source))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(closeLabel)
            }
        }
    )
}