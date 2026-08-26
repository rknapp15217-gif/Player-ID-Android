package com.playerid.app.ui.roster

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.PlayerProfile

@Composable
fun RosterPlayerRow(
    player: PlayerProfile,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent()
        Spacer(Modifier.width(12.dp))
        Text(
            text = player.number,
            modifier = Modifier.width(32.dp),
            fontWeight = FontWeight.SemiBold
        )
        Column(Modifier.weight(1f)) {
            Text(player.name, fontWeight = FontWeight.Medium)
            Text(
                text = player.position,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingContent()
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
}