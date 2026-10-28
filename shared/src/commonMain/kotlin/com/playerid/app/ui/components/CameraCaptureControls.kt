package com.playerid.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CameraModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unavailable: Boolean = false,
    compact: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (!selected) Color.White.copy(alpha = 0.16f)
                else if (compact) Color.Black.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = if (compact) 0.24f else 0.14f),
            disabledContentColor = Color.White.copy(alpha = 0.55f)
        ),
        contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 18.dp, vertical = if (compact) 4.dp else 12.dp),
        modifier = modifier.alpha(if (enabled) 1f else if (unavailable) 0.7f else 0.35f)
            .defaultMinSize(minWidth = if (compact) 0.dp else 136.dp, minHeight = if (compact) 56.dp else 80.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (compact) 18.sp else 20.sp, maxLines = 1)
            if (unavailable) {
                Text("Unavailable", color = Color.White, fontSize = 10.sp, maxLines = 1)
            } else if (selected) {
                Box(Modifier.padding(top = 5.dp).size(6.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
            } else {
                Spacer(Modifier.height(11.dp))
            }
        }
    }
}

@Composable
fun CameraShutterVisual(photo: Boolean, rolling: Boolean, recording: Boolean, enabled: Boolean, compact: Boolean = false) {
    val sizeScale = if (compact) 0.875f else 1f
    val innerSize by animateDpAsState(
        targetValue = when { photo -> 34.dp; recording -> 28.dp; else -> 42.dp } * sizeScale,
        animationSpec = tween(durationMillis = 200),
        label = "recordInnerSize"
    )
    val spin by rememberInfiniteTransition(label = "rollingCaptureSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1800, easing = LinearEasing)),
        label = "rollingCaptureSpinAngle"
    )
    Box(Modifier.size(64.dp * sizeScale).alpha(if (enabled) 1f else 0.45f), contentAlignment = Alignment.Center) {
        Box(Modifier.size(64.dp * sizeScale).border(2.dp, Color.White, CircleShape))
        if (rolling) {
            Canvas(Modifier.size(68.dp * sizeScale)) {
                val strokeWidth = 5.dp.toPx()
                val radius = size.minDimension / 2f - strokeWidth / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val cometColor = Color(0xFFB8FF6A)
                for (i in 0..5) {
                    drawArc(
                        color = cometColor.copy(alpha = (1f - i * 0.16f).coerceAtLeast(0.08f)),
                        startAngle = spin - i * 11f,
                        sweepAngle = 9f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                val angle = spin * kotlin.math.PI / 180.0
                drawCircle(
                    cometColor, radius = 3.2.dp.toPx(),
                    center = Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
                )
            }
        }
        Box(Modifier.size(innerSize).background(
            when { photo -> Color.White; rolling -> MaterialTheme.colorScheme.secondaryContainer; else -> Color.Red },
            CircleShape
        ))
    }
}
