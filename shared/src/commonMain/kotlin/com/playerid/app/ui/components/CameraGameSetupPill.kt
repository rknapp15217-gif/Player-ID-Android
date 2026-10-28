package com.playerid.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraGameSetupPill(
    teamName: String?,
    opponent: String,
    player: String,
    jerseyColor: Color,
    hasOpened: Boolean,
    enabled: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "setupPillPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "setupPillProgress"
    )
    Row(
        modifier = Modifier
            .background(
                if (hasOpened) Color.Black.copy(alpha = 0.58f)
                else Color(0xFFC65D21).copy(alpha = 0.28f + pulse * 0.28f),
                RoundedCornerShape(50)
            )
            .border(
                1.dp,
                if (hasOpened) Color.White.copy(alpha = 0.2f)
                else Color(0xFFFFA062).copy(alpha = 0.35f + pulse * 0.45f),
                RoundedCornerShape(50)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (teamName != null) {
            Canvas(Modifier.size(if (compact) 22.dp else 26.dp).semantics { contentDescription = "Jersey color" }) {
                val jersey = Path().apply {
                    moveTo(8.5f, 2f)
                    cubicTo(8.5f, 2f, 7f, 4.5f, 4f, 5.5f)
                    lineTo(2f, 8f); lineTo(5f, 9.5f); lineTo(5f, 21f)
                    lineTo(19f, 21f); lineTo(19f, 9.5f); lineTo(22f, 8f); lineTo(20f, 5.5f)
                    cubicTo(17f, 4.5f, 15.5f, 2f, 15.5f, 2f)
                    cubicTo(15f, 3.5f, 13.5f, 4.5f, 12f, 4.5f)
                    cubicTo(10.5f, 4.5f, 9f, 3.5f, 8.5f, 2f)
                    close()
                }
                scale(size.width / 24f, size.height / 24f, Offset.Zero) {
                    drawPath(jersey, jerseyColor)
                    drawPath(jersey, Color.White, style = Stroke(0.7f))
                }
            }
        }
        Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(teamName ?: "Select Team", color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Medium, fontSize = if (compact) 13.sp else 15.sp,
                    lineHeight = if (compact) 15.sp else TextUnit.Unspecified, maxLines = 1)
                if (teamName != null && opponent.isNotBlank()) {
                    Box(Modifier.size(if (compact) 4.dp else 7.dp).background(Color.White.copy(alpha = 0.9f), CircleShape))
                    Text(opponent, color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium, fontSize = if (compact) 13.sp else 15.sp,
                        lineHeight = if (compact) 15.sp else TextUnit.Unspecified, maxLines = 1)
                }
            }
            if (teamName != null) Text("Player: $player", color = Color.White.copy(alpha = 0.78f),
                fontWeight = FontWeight.SemiBold, fontSize = if (compact) 10.sp else 12.sp,
                lineHeight = if (compact) 12.sp else TextUnit.Unspecified, maxLines = 1)
        }
        Icon(Icons.Default.ExpandMore, "Choose team and opponent",
            tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(if (compact) 24.dp else 36.dp))
    }
}

/** Large pulsing red "!" shown instead of the setup pill until Game setup has been opened. */
@Composable
fun CameraSetupAlertBadge(enabled: Boolean = true, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "setupAlertPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "setupAlertProgress"
    )
    Box(
        Modifier.size(64.dp).semantics { contentDescription = "Game setup needed" }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFE53935).copy(alpha = 0.35f * (1f - pulse)), radius = size.minDimension / 2f * (0.8f + 0.2f * pulse))
            drawCircle(Color(0xFFE53935), radius = size.minDimension / 2f * (0.68f + 0.06f * pulse))
            drawCircle(Color.White, radius = size.minDimension / 2f * (0.68f + 0.06f * pulse), style = Stroke(2.dp.toPx()))
        }
        Text("!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
    }
}
