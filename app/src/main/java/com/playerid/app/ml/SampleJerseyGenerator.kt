package com.playerid.app.ml

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * 🎨 Creates sample jersey images for bundled validation
 * Generates placeholder jersey photos with numbers for offline training
 */
class SampleJerseyGenerator(private val context: Context) {
    
    companion object {
        private const val TAG = "SampleJerseyGenerator"
        private const val IMAGE_WIDTH = 800
        private const val IMAGE_HEIGHT = 600
    }
    
    /**
     * 🏈 Generate sample jersey image with number
     */
    fun generateJerseyImage(
        number: Int,
        jerseyColor: Int = Color.BLUE,
        numberColor: Int = Color.WHITE,
        sport: String = "soccer"
    ): Bitmap {
        
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background (field/court)
        val backgroundPaint = Paint().apply {
            color = when(sport) {
                "soccer" -> Color.rgb(34, 139, 34) // Green grass
                "basketball" -> Color.rgb(139, 69, 19) // Wood court
                "football" -> Color.rgb(34, 139, 34) // Green field
                else -> Color.GRAY
            }
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat(), backgroundPaint)
        
        // Jersey body (simplified rectangle)
        val jerseyPaint = Paint().apply {
            color = jerseyColor
            isAntiAlias = true
        }
        
        val jerseyRect = RectF(
            IMAGE_WIDTH * 0.25f,
            IMAGE_HEIGHT * 0.2f,
            IMAGE_WIDTH * 0.75f,
            IMAGE_HEIGHT * 0.8f
        )
        
        canvas.drawRoundRect(jerseyRect, 20f, 20f, jerseyPaint)
        
        // Jersey number
        val numberPaint = Paint().apply {
            color = numberColor
            textSize = 120f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        val numberText = number.toString()
        val textBounds = Rect()
        numberPaint.getTextBounds(numberText, 0, numberText.length, textBounds)
        
        val centerX = IMAGE_WIDTH / 2f
        val centerY = IMAGE_HEIGHT / 2f + textBounds.height() / 2f
        
        // Add shadow for better visibility
        val shadowPaint = Paint().apply {
            color = Color.BLACK
            textSize = 120f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText(numberText, centerX + 3f, centerY + 3f, shadowPaint)
        canvas.drawText(numberText, centerX, centerY, numberPaint)
        
        // Add some jersey details (stripes, logos, etc.)
        addJerseyDetails(canvas, jerseyRect, sport)
        
        Log.d(TAG, "✅ Generated jersey image for #$number ($sport)")
        return bitmap
    }
    
    /**
     * 🎨 Add sport-specific jersey details
     */
    private fun addJerseyDetails(canvas: Canvas, jerseyRect: RectF, sport: String) {
        val detailPaint = Paint().apply {
            color = Color.WHITE
            alpha = 100
            strokeWidth = 4f
        }
        
        when(sport) {
            "soccer" -> {
                // Add collar
                canvas.drawLine(
                    jerseyRect.left + 50f, jerseyRect.top,
                    jerseyRect.right - 50f, jerseyRect.top,
                    detailPaint
                )
                
                // Add side stripes
                canvas.drawLine(
                    jerseyRect.left, jerseyRect.top + 50f,
                    jerseyRect.left, jerseyRect.bottom - 50f,
                    detailPaint
                )
                canvas.drawLine(
                    jerseyRect.right, jerseyRect.top + 50f,
                    jerseyRect.right, jerseyRect.bottom - 50f,
                    detailPaint
                )
            }
            
            "basketball" -> {
                // Add tank top style
                detailPaint.strokeWidth = 8f
                canvas.drawLine(
                    jerseyRect.left + 30f, jerseyRect.top,
                    jerseyRect.left + 30f, jerseyRect.top + 100f,
                    detailPaint
                )
                canvas.drawLine(
                    jerseyRect.right - 30f, jerseyRect.top,
                    jerseyRect.right - 30f, jerseyRect.top + 100f,
                    detailPaint
                )
            }
            
            "football" -> {
                // Add shoulder pads outline
                detailPaint.strokeWidth = 6f
                canvas.drawArc(
                    jerseyRect.left - 20f, jerseyRect.top,
                    jerseyRect.left + 80f, jerseyRect.top + 100f,
                    0f, 180f, false, detailPaint
                )
                canvas.drawArc(
                    jerseyRect.right - 80f, jerseyRect.top,
                    jerseyRect.right + 20f, jerseyRect.top + 100f,
                    0f, 180f, false, detailPaint
                )
            }
        }
    }
    
    /**
     * 📦 Generate all sample jersey images and save to assets directory
     */
    fun generateAllSampleImages(): List<String> {
        val generatedFiles = mutableListOf<String>()
        val assetsDir = File(context.getExternalFilesDir(null), "sample_jerseys")
        assetsDir.mkdirs()
        
        // Jersey configurations
        val jerseyConfigs = listOf(
            // Soccer jerseys
            Triple(10, Color.rgb(0, 100, 200), "soccer"), // Messi style
            Triple(7, Color.rgb(139, 0, 0), "soccer"), // Ronaldo style
            Triple(11, Color.rgb(255, 215, 0), "soccer"), // Brazil style
            Triple(9, Color.rgb(0, 0, 139), "soccer"), // France style
            
            // Basketball jerseys
            Triple(23, Color.rgb(85, 37, 130), "basketball"), // Lakers style
            Triple(30, Color.rgb(0, 107, 182), "basketball"), // Warriors style
            Triple(35, Color.rgb(0, 0, 0), "basketball"), // Nets style
            
            // Football jerseys
            Triple(12, Color.rgb(0, 34, 68), "football"), // Patriots style
            Triple(12, Color.rgb(24, 48, 40), "football"), // Packers style
            
            // Test cases
            Triple(1, Color.RED, "soccer"),
            Triple(5, Color.GREEN, "basketball"),
            Triple(9, Color.BLUE, "football"),
            Triple(42, Color.MAGENTA, "football"),
            Triple(88, Color.CYAN, "football"),
            Triple(99, Color.YELLOW, "soccer")
        )
        
        jerseyConfigs.forEach { (number, color, sport) ->
            try {
                val bitmap = generateJerseyImage(number, color, Color.WHITE, sport)
                val filename = "jersey_${number}_${sport}.jpg"
                val file = File(assetsDir, filename)
                
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()
                
                generatedFiles.add(file.absolutePath)
                Log.d(TAG, "✅ Saved: $filename")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to generate $number: ${e.message}")
            }
        }
        
        return generatedFiles
    }
    
    /**
     * 🔀 Create variation of jersey (different angles, lighting, etc.)
     */
    fun generateJerseyVariation(
        baseNumber: Int,
        variation: String, // "angled", "dark", "blurry", "partial"
        sport: String = "soccer"
    ): Bitmap {
        val baseBitmap = generateJerseyImage(baseNumber, Color.BLUE, Color.WHITE, sport)
        
        return when(variation) {
            "dark" -> applyDarkening(baseBitmap)
            "blurry" -> applyBlur(baseBitmap)
            "angled" -> applyAngle(baseBitmap)
            "partial" -> applyPartialOcclusion(baseBitmap)
            else -> baseBitmap
        }
    }
    
    private fun applyDarkening(bitmap: Bitmap): Bitmap {
        val darkBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(darkBitmap)
        val darkPaint = Paint().apply {
            color = Color.BLACK
            alpha = 100
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), darkPaint)
        return darkBitmap
    }
    
    private fun applyBlur(bitmap: Bitmap): Bitmap {
        // Simple blur effect by scaling down and up
        val smallBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 4, bitmap.height / 4, true)
        return Bitmap.createScaledBitmap(smallBitmap, bitmap.width, bitmap.height, true)
    }
    
    private fun applyAngle(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            setRotate(15f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun applyPartialOcclusion(bitmap: Bitmap): Bitmap {
        val occludedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(occludedBitmap)
        val occlusionPaint = Paint().apply {
            color = Color.GRAY
            alpha = 150
        }
        
        // Add partial occlusion rectangle
        canvas.drawRect(
            bitmap.width * 0.6f, 
            bitmap.height * 0.1f,
            bitmap.width * 0.9f, 
            bitmap.height * 0.4f,
            occlusionPaint
        )
        
        return occludedBitmap
    }
}