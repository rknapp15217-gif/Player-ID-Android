package com.playerid.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val StopSignIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StopSignIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(7f, 2f)
            lineTo(17f, 2f)
            lineTo(22f, 7f)
            lineTo(22f, 17f)
            lineTo(17f, 22f)
            lineTo(7f, 22f)
            lineTo(2f, 17f)
            lineTo(2f, 7f)
            close()
        }
    }.build()
}

val CrossedLacrosseSticksIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CrossedLacrosseSticksIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5.2f, 2f)
            curveTo(3.4f, 2f, 2f, 3.4f, 2f, 5.2f)
            curveTo(2f, 7.6f, 4.3f, 10.1f, 6.6f, 10.1f)
            lineTo(18.8f, 22.3f)
            lineTo(20.3f, 20.8f)
            lineTo(8.1f, 8.6f)
            curveTo(8.9f, 7.7f, 9.5f, 6.4f, 9.5f, 5.2f)
            curveTo(9.5f, 3.4f, 8.1f, 2f, 6.3f, 2f)
            close()

            moveTo(17.7f, 2f)
            curveTo(15.9f, 2f, 14.5f, 3.4f, 14.5f, 5.2f)
            curveTo(14.5f, 6.4f, 15.1f, 7.7f, 15.9f, 8.6f)
            lineTo(3.7f, 20.8f)
            lineTo(5.2f, 22.3f)
            lineTo(17.4f, 10.1f)
            curveTo(19.7f, 10.1f, 22f, 7.6f, 22f, 5.2f)
            curveTo(22f, 3.4f, 20.6f, 2f, 18.8f, 2f)
            close()
        }
    }.build()
}

val GroundBallIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "GroundBallIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(10.5f, 2f)
            lineTo(13.5f, 2f)
            lineTo(13.5f, 10f)
            lineTo(17f, 10f)
            lineTo(12f, 15f)
            lineTo(7f, 10f)
            lineTo(10.5f, 10f)
            close()

            moveTo(12f, 16f)
            curveTo(8.7f, 16f, 7f, 17.3f, 7f, 19f)
            curveTo(7f, 20.7f, 8.7f, 22f, 12f, 22f)
            curveTo(15.3f, 22f, 17f, 20.7f, 17f, 19f)
            curveTo(17f, 17.3f, 15.3f, 16f, 12f, 16f)
            close()
        }
    }.build()
}