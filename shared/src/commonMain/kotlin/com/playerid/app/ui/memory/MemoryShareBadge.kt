package com.playerid.app.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.playerid.app.ui.theme.SpotrHighlightOrange
import com.playerid.app.ui.theme.SpotrTeal

/** PlayerID share mark: a white paper plane on a teal-to-coral gradient badge. */
@Composable
fun MemoryShareBadge(contentDescription: String?, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Box(
        modifier
            .size(size)
            .shadow(3.dp, CircleShape)
            .background(Brush.linearGradient(listOf(SpotrTeal, SpotrHighlightOrange)), CircleShape)
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Send,
            contentDescription = contentDescription,
            tint = Color.White,
            // Tilt the plane upward and nudge it so it looks optically centred.
            modifier = Modifier.size(size * 0.58f).offset(x = size * 0.03f, y = -size * 0.02f).rotate(-35f)
        )
    }
}
