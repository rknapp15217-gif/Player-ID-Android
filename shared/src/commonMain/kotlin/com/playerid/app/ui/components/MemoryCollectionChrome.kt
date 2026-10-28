package com.playerid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MemoryCollectionTab(
    label: String, highlighted: Boolean, underlined: Boolean, enabled: Boolean, onClick: () -> Unit,
    compact: Boolean = false
) {
    Box(
        Modifier.widthIn(min = if (compact) 56.dp else 72.dp).height(48.dp).padding(horizontal = if (compact) 4.dp else 8.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, softWrap = false)
        if (underlined) Box(
            Modifier.align(Alignment.BottomCenter).width(48.dp).height(3.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        )
    }
}

@Composable
fun MemoryCollectionGroupHeading(teamLabel: String?, title: String, subtitle: String) {
    val groupColor = Color(0xFF3676B8)
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
        teamLabel?.let {
            Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(44.dp).background(groupColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
        Box(Modifier.fillMaxWidth(0.9f).height(3.dp).background(groupColor))
    }
}
