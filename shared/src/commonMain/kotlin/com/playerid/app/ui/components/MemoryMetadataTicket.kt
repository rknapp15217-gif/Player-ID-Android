package com.playerid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MemoryMetadataTicket(
    tag: String?,
    opponent: String?,
    date: String?,
    modifier: Modifier = Modifier
) {
    if (tag == null && opponent == null && date == null) return
    Surface(
        modifier = modifier.widthIn(max = 360.dp),
        color = Color.Black.copy(alpha = 0.52f),
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            tag?.let {
                Text(it, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            if (tag != null && (opponent != null || date != null)) {
                Box(Modifier.size(4.dp).background(Color.White, CircleShape))
            }
            opponent?.let {
                Text("vs $it", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            if (opponent != null && date != null) {
                Box(Modifier.size(4.dp).background(Color.White, CircleShape))
            }
            date?.let {
                Text(it, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
