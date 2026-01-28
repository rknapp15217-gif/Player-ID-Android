package com.playerid.app.ui.composables

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun DrawScope.drawPlayerOverlay(
    playerName: String,
    jerseyNumber: String,
    position: Offset,
    isSelected: Boolean,
    debugWidth: Int,
    debugHeight: Int,
    pulseFraction: Float = 0f // 0f = no pulse, 1f = max pulse
) {
    // Show size prominently in the same bar
    val sizeText = "(${debugWidth}x${debugHeight})"
    val fullText = "$playerName $sizeText"

    val textPaint = android.text.TextPaint().apply {
        color = android.graphics.Color.WHITE
        textSize = 16.sp.toPx()
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        // Make text bold to be more visible
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val numberPaint = android.text.TextPaint().apply {
        color = android.graphics.Color.WHITE
        textSize = 20.sp.toPx()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    // 1. Calculate dimensions
    val textWidth = textPaint.measureText(fullText)
    val textBounds = Rect()
    textPaint.getTextBounds(fullText, 0, fullText.length, textBounds)
    val textHeight = textBounds.height()

    val numberWidth = numberPaint.measureText(jerseyNumber)
    val numberBounds = Rect()
    numberPaint.getTextBounds(jerseyNumber, 0, jerseyNumber.length, numberBounds)

    val padding = 10.dp.toPx()
    val numberCircleRadius = (numberWidth.coerceAtLeast(numberBounds.height().toFloat()) / 2f) + padding

    val totalWidth = (numberCircleRadius * 2) + padding + textWidth + padding
    val totalHeight = (numberCircleRadius * 2).coerceAtLeast(textHeight.toFloat() + padding * 2)

    // 2. Define the drawing area
    val overlayRect = RectF(
        position.x - totalWidth / 2,
        position.y - totalHeight / 2,
        position.x + totalWidth / 2,
        position.y + totalHeight / 2
    )

    // 3. Draw the background
    val baseColor = if (isSelected) Color(0xFFFF9800) else Color(0xFF1976D2)
    // Pulse effect: interpolate between baseColor and a lighter color
    val pulseColor = Color(0xFFFFF176) // Light yellow for pulse
    val bgColor = Color(
        red = baseColor.red + (pulseColor.red - baseColor.red) * pulseFraction,
        green = baseColor.green + (pulseColor.green - baseColor.green) * pulseFraction,
        blue = baseColor.blue + (pulseColor.blue - baseColor.blue) * pulseFraction,
        alpha = 0.9f
    )
    val paint = Paint().apply {
        color = bgColor.toArgb()
        isAntiAlias = true
    }

    drawIntoCanvas { canvas ->
        // Draw the main rounded rectangle bar
        canvas.nativeCanvas.drawRoundRect(
            overlayRect.left,
            overlayRect.top,
            overlayRect.right,
            overlayRect.bottom,
            25f,
            25f,
            paint
        )

        // 4. Draw the text
        val circleCenterX = overlayRect.left + numberCircleRadius + padding/4
        

        // Draw white box around number if processing
        if (pulseFraction > 0f) {
            // Draw a sharp blue rectangle tightly around the number for visibility
            val rectPadding = 6.dp.toPx() + 4f * pulseFraction
            val numberRect = RectF(
                circleCenterX - numberWidth / 2f - rectPadding,
                overlayRect.centerY() - numberBounds.height() / 2f - rectPadding,
                circleCenterX + numberWidth / 2f + rectPadding,
                overlayRect.centerY() + numberBounds.height() / 2f + rectPadding
            )
            canvas.nativeCanvas.drawRect(
                numberRect,
                Paint().apply {
                    color = Color(0xFF1976D2).toArgb() // Blue
                    style = Paint.Style.STROKE
                    strokeWidth = 5f + 2f * pulseFraction
                    isAntiAlias = true
                }
            )
        }

        // Draw jersey number circle outline
        canvas.nativeCanvas.drawCircle(
            circleCenterX,
            overlayRect.centerY(),
            numberCircleRadius - 4.dp.toPx(),
            Paint().apply { 
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f 
                isAntiAlias = true
            }
        )

        // Draw jersey number
        canvas.nativeCanvas.drawText(
            jerseyNumber,
            circleCenterX,
            overlayRect.centerY() + numberBounds.height() / 2,
            numberPaint
        )

        // Draw Player Name and SIZE prominently
        canvas.nativeCanvas.drawText(
            fullText,
            circleCenterX + numberCircleRadius + padding/2,
            overlayRect.centerY() + textHeight / 2,
            textPaint
        )
    }
}