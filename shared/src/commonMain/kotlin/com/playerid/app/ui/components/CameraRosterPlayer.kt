package com.playerid.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.domain.team.PlayerProfile

@Composable
fun CameraRosterPlayerRow(
    player: PlayerProfile,
    selected: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit = {}
) {
    val rowBg by animateColorAsState(
        targetValue = when {
            selected -> Color(0xFF2E7D32).copy(alpha = 0.42f)
            highlighted -> Color(0xFF1565C0).copy(alpha = 0.40f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "rowBg_${player.id}"
    )
    Row(
        Modifier.fillMaxWidth().background(rowBg, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#${player.number}", color = Color.White, fontWeight = FontWeight.Bold,
            fontSize = 16.sp, modifier = Modifier.width(52.dp))
        Text(player.name, color = Color.White.copy(alpha = 0.88f),
            fontSize = 16.sp, modifier = Modifier.weight(1f))
        trailingContent()
    }
}

@Composable
fun CameraRosterPlayerDetails(
    player: PlayerProfile,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A223D).copy(alpha = 0.95f)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#${player.number}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(player.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Position: ${player.position.ifBlank { "Unknown" }}",
                    color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                Text("Grad Year: ${player.academicYear.ifBlank { "Unknown" }}",
                    color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                if (expanded) Text("Team: ${player.teamName.ifBlank { "Unknown" }}",
                    color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Collapse details" else "Expand details",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
