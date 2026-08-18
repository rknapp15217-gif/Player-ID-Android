package com.playerid.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GoatIcon: ImageVector
    get() {
        if (_goatIcon != null) return _goatIcon!!

        _goatIcon = ImageVector.Builder(
            name = "GoatIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(7.0f, 3.2f)
                curveTo(5.7f, 3.4f, 4.8f, 4.4f, 4.7f, 5.8f)
                curveTo(4.6f, 7.1f, 5.3f, 8.2f, 6.3f, 8.9f)
                lineTo(7.4f, 9.6f)
                lineTo(7.2f, 8.3f)
                curveTo(7.0f, 7.1f, 7.3f, 6.1f, 8.0f, 5.3f)
                curveTo(8.7f, 4.5f, 9.1f, 3.8f, 9.2f, 3.0f)
                curveTo(8.5f, 3.0f, 7.7f, 3.1f, 7.0f, 3.2f)
                close()

                moveTo(17.0f, 3.2f)
                curveTo(16.3f, 3.1f, 15.5f, 3.0f, 14.8f, 3.0f)
                curveTo(14.9f, 3.8f, 15.3f, 4.5f, 16.0f, 5.3f)
                curveTo(16.7f, 6.1f, 17.0f, 7.1f, 16.8f, 8.3f)
                lineTo(16.6f, 9.6f)
                lineTo(17.7f, 8.9f)
                curveTo(18.7f, 8.2f, 19.4f, 7.1f, 19.3f, 5.8f)
                curveTo(19.2f, 4.4f, 18.3f, 3.4f, 17.0f, 3.2f)
                close()

                moveTo(8.7f, 7.2f)
                curveTo(7.5f, 8.1f, 6.8f, 9.6f, 6.8f, 11.2f)
                curveTo(6.8f, 13.2f, 7.8f, 15.0f, 9.6f, 16.0f)
                lineTo(9.6f, 18.2f)
                curveTo(9.6f, 19.4f, 8.9f, 20.4f, 7.8f, 20.8f)
                lineTo(7.0f, 21.1f)
                lineTo(7.0f, 22.0f)
                lineTo(10.1f, 22.0f)
                lineTo(11.2f, 18.4f)
                lineTo(12.8f, 18.4f)
                lineTo(13.9f, 22.0f)
                lineTo(17.0f, 22.0f)
                lineTo(17.0f, 21.1f)
                lineTo(16.2f, 20.8f)
                curveTo(15.1f, 20.4f, 14.4f, 19.4f, 14.4f, 18.2f)
                lineTo(14.4f, 16.0f)
                curveTo(16.2f, 15.0f, 17.2f, 13.2f, 17.2f, 11.2f)
                curveTo(17.2f, 9.6f, 16.5f, 8.1f, 15.3f, 7.2f)
                curveTo(14.6f, 8.0f, 13.4f, 8.5f, 12.0f, 8.5f)
                curveTo(10.6f, 8.5f, 9.4f, 8.0f, 8.7f, 7.2f)
                close()

                moveTo(10.2f, 11.0f)
                curveTo(10.6f, 11.0f, 11.0f, 11.4f, 11.0f, 11.8f)
                curveTo(11.0f, 12.2f, 10.6f, 12.6f, 10.2f, 12.6f)
                curveTo(9.8f, 12.6f, 9.4f, 12.2f, 9.4f, 11.8f)
                curveTo(9.4f, 11.4f, 9.8f, 11.0f, 10.2f, 11.0f)
                close()

                moveTo(13.8f, 11.0f)
                curveTo(14.2f, 11.0f, 14.6f, 11.4f, 14.6f, 11.8f)
                curveTo(14.6f, 12.2f, 14.2f, 12.6f, 13.8f, 12.6f)
                curveTo(13.4f, 12.6f, 13.0f, 12.2f, 13.0f, 11.8f)
                curveTo(13.0f, 11.4f, 13.4f, 11.0f, 13.8f, 11.0f)
                close()

                moveTo(10.7f, 14.4f)
                lineTo(13.3f, 14.4f)
                curveTo(13.0f, 15.2f, 12.6f, 15.8f, 12.0f, 16.2f)
                curveTo(11.4f, 15.8f, 11.0f, 15.2f, 10.7f, 14.4f)
                close()
            }
        }.build()

        return _goatIcon!!
    }

private var _goatIcon: ImageVector? = null