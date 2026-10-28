package com.playerid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

@Composable
fun PlayerIDLogo() {
    // Original Android adaptive-icon paths and gradient, not the legacy raster icon.
    val foreground = remember {
        PathParser().parsePathString(
            "M137,318 L137,136 C137,99 159,79 194,79 L237,79 C301,79 341,116 341,173 " +
                "C341,231 301,267 238,267 L198,267 L198,318 C198,343 185,356 167,356 " +
                "C149,356 137,343 137,318 Z M198,139 L198,207 L260,173 Z"
        ).toPath().apply { fillType = PathFillType.EvenOdd }
    }
    val play = remember { PathParser().parsePathString("M205,139 L205,207 L263,173 Z").toPath() }
    Canvas(Modifier.size(112.dp).clip(RoundedCornerShape(26.dp))) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2AA8F7), Color(0xFF0F6CF0), Color(0xFF0A2FA6))))
        withTransform({ scale(size.width / 432f, size.height / 432f, pivot = Offset.Zero) }) {
            drawPath(foreground, Color.White)
            drawPath(play, Color(0xFF145FE5))
        }
    }
}
