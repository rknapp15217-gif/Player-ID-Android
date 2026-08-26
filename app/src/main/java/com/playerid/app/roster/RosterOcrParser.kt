package com.playerid.app.roster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.roundToInt

suspend fun extractRosterCandidates(context: Context, imageUri: Uri): RosterOcrResult {
    val bitmap = loadBitmapFromUri(context, imageUri)
    return extractRosterCandidates(scaleBitmap(bitmap, 2048))
}

suspend fun extractRosterCandidates(bitmap: Bitmap): RosterOcrResult {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val blockLines = result.textBlocks.map { block -> block.lines.map { it.text } }
        parseRosterText(
            lines = blockLines.flatten(),
            blockLines = blockLines
        )
    } finally {
        recognizer.close()
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)
    } ?: error("Unable to decode roster image: $uri")
}

private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val maxSide = max(bitmap.width, bitmap.height)
    if (maxSide <= maxDimension) return bitmap

    val scale = maxDimension.toFloat() / maxSide.toFloat()
    val targetWidth = (bitmap.width * scale).roundToInt()
    val targetHeight = (bitmap.height * scale).roundToInt()
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
