package com.playerid.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * 📦 Manages bundled jersey photos for offline validation
 * Provides sample jersey images embedded in the app for training data collection
 */
class BundledJerseyPhotos(private val context: Context) {
    
    companion object {
        private const val TAG = "BundledJerseyPhotos"
        private const val ASSETS_FOLDER = "jersey_photos"
    }
    
    data class BundledJerseyPhoto(
        val filename: String,
        val expectedNumber: Int?,
        val sport: String,
        val team: String,
        val description: String,
        val difficultyLevel: String = "medium" // easy, medium, hard
    )
    
    /**
     * 🏈 Generate comprehensive training dataset (1000+ samples)
     */
    fun generateComprehensiveDataset(): List<BundledJerseyPhoto> {
        val photos = mutableListOf<BundledJerseyPhoto>()
        
        // Generate samples for ALL jersey numbers 0-99
        for (number in 0..99) {
            // Multiple sports variations for each number
            val sports = listOf("soccer", "basketball", "football")
            val difficulties = listOf("easy", "medium", "hard")
            val variations = listOf("normal", "angled", "dark", "blurry", "partial")
            
            sports.forEach { sport ->
                difficulties.forEach { difficulty ->
                    variations.forEach { variation ->
                        val filename = "jersey_${number}_${sport}_${difficulty}_${variation}.jpg"
                        photos.add(
                            BundledJerseyPhoto(
                                filename = filename,
                                expectedNumber = number,
                                sport = sport,
                                team = "${sport.capitalize()} Team $number",
                                description = "Jersey #$number ($sport, $difficulty, $variation)",
                                difficultyLevel = difficulty
                            )
                        )
                    }
                }
            }
        }
        
        // Add edge cases and challenging scenarios
        val edgeCases = listOf(
            BundledJerseyPhoto("unclear_number_1.jpg", null, "soccer", "Test", "Unclear number case 1", "hard"),
            BundledJerseyPhoto("unclear_number_2.jpg", null, "basketball", "Test", "Unclear number case 2", "hard"),
            BundledJerseyPhoto("multiple_players_1.jpg", null, "football", "Test", "Multiple players case 1", "hard"),
            BundledJerseyPhoto("multiple_players_2.jpg", null, "soccer", "Test", "Multiple players case 2", "hard"),
            BundledJerseyPhoto("damaged_jersey_1.jpg", null, "basketball", "Test", "Damaged/torn jersey", "hard"),
            BundledJerseyPhoto("folded_jersey_1.jpg", null, "football", "Test", "Folded jersey number", "hard"),
            BundledJerseyPhoto("shadow_occlusion_1.jpg", null, "soccer", "Test", "Shadow covering number", "hard"),
            BundledJerseyPhoto("motion_blur_1.jpg", null, "basketball", "Test", "Motion blur case", "hard")
        )
        
        photos.addAll(edgeCases)
        
        Log.d(TAG, "📊 Generated ${photos.size} comprehensive training samples")
        return photos
    }
    
    /**
     * 🎯 Get strategic subset for initial validation (manageable size)
     */
    private val strategicSamplePhotos = listOf(
        // Core numbers (commonly used in sports)
        BundledJerseyPhoto("messi_10.jpg", 10, "soccer", "Argentina", "Messi #10 Argentina jersey", "easy"),
        BundledJerseyPhoto("ronaldo_7.jpg", 7, "soccer", "Portugal", "Ronaldo #7 Portugal jersey", "easy"),
        BundledJerseyPhoto("lebron_23.jpg", 23, "basketball", "Lakers", "LeBron #23 Lakers jersey", "easy"),
        BundledJerseyPhoto("brady_12.jpg", 12, "football", "Patriots", "Brady #12 Patriots jersey", "medium"),
        
        // Single digit numbers (0-9) - critical for detection
        BundledJerseyPhoto("jersey_00.jpg", 0, "soccer", "Goalkeeper", "Jersey #0 goalkeeper", "easy"),
        BundledJerseyPhoto("jersey_01.jpg", 1, "basketball", "Point Guard", "Jersey #1 point guard", "easy"),
        BundledJerseyPhoto("jersey_02.jpg", 2, "football", "Kicker", "Jersey #2 kicker", "easy"),
        BundledJerseyPhoto("jersey_03.jpg", 3, "soccer", "Defender", "Jersey #3 defender", "medium"),
        BundledJerseyPhoto("jersey_04.jpg", 4, "basketball", "Forward", "Jersey #4 forward", "medium"),
        BundledJerseyPhoto("jersey_05.jpg", 5, "football", "Quarterback", "Jersey #5 quarterback", "medium"),
        BundledJerseyPhoto("jersey_06.jpg", 6, "soccer", "Midfielder", "Jersey #6 midfielder", "medium"),
        BundledJerseyPhoto("jersey_07.jpg", 7, "basketball", "Guard", "Jersey #7 guard", "hard"),
        BundledJerseyPhoto("jersey_08.jpg", 8, "football", "Receiver", "Jersey #8 receiver", "hard"),
        BundledJerseyPhoto("jersey_09.jpg", 9, "soccer", "Striker", "Jersey #9 striker", "hard"),
        
        // Teen numbers (10-19) - challenging due to two digits
        BundledJerseyPhoto("jersey_10.jpg", 10, "basketball", "Center", "Jersey #10 center", "medium"),
        BundledJerseyPhoto("jersey_11.jpg", 11, "football", "Wide Receiver", "Jersey #11 receiver", "medium"),
        BundledJerseyPhoto("jersey_13.jpg", 13, "soccer", "Winger", "Jersey #13 winger", "hard"),
        BundledJerseyPhoto("jersey_15.jpg", 15, "basketball", "Forward", "Jersey #15 forward", "hard"),
        BundledJerseyPhoto("jersey_17.jpg", 17, "football", "Quarterback", "Jersey #17 quarterback", "hard"),
        
        // High numbers (common in football)
        BundledJerseyPhoto("jersey_42.jpg", 42, "football", "Running Back", "Jersey #42 running back", "medium"),
        BundledJerseyPhoto("jersey_55.jpg", 55, "football", "Linebacker", "Jersey #55 linebacker", "hard"),
        BundledJerseyPhoto("jersey_88.jpg", 88, "football", "Tight End", "Jersey #88 tight end", "hard"),
        BundledJerseyPhoto("jersey_99.jpg", 99, "football", "Defensive End", "Jersey #99 defensive end", "hard"),
        
        // Edge Cases
        BundledJerseyPhoto("jersey_unclear.jpg", null, "soccer", "Test Team", "Unclear number - validation test", "hard"),
        BundledJerseyPhoto("jersey_multiple.jpg", null, "basketball", "Test Team", "Multiple players - validation test", "hard")
    )
    
    /**
     * 📋 Get list of bundled photos (strategic sample for validation)
     */
    fun getBundledPhotos(): List<BundledJerseyPhoto> = strategicSamplePhotos
    
    /**
     * 🎯 Get comprehensive training dataset (1000+ samples)
     */
    fun getComprehensiveTrainingSet(): List<BundledJerseyPhoto> = generateComprehensiveDataset()
    
    /**
     * 📊 Get batch of photos by size preference
     */
    fun getPhotosBatch(batchSize: BatchSize): List<BundledJerseyPhoto> {
        return when(batchSize) {
            BatchSize.SMALL -> strategicSamplePhotos.take(25) // Quick validation
            BatchSize.MEDIUM -> strategicSamplePhotos // ~25 strategic samples
            BatchSize.LARGE -> generateComprehensiveDataset().take(250) // Manageable subset
            BatchSize.FULL -> generateComprehensiveDataset() // Full 1000+ dataset
        }
    }
    
    enum class BatchSize {
        SMALL,    // ~25 samples - quick validation
        MEDIUM,   // ~50 samples - strategic validation  
        LARGE,    // ~250 samples - substantial training
        FULL      // 1000+ samples - complete dataset
    }
    
    /**
     * 🖼️ Load bitmap from bundled assets
     */
    fun loadPhotoBitmap(filename: String): Bitmap? {
        return try {
            val inputStream = context.assets.open("$ASSETS_FOLDER/$filename")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            Log.d(TAG, "✅ Loaded bundled photo: $filename")
            bitmap
            
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to load bundled photo: $filename - ${e.message}")
            null
        }
    }
    
    /**
     * 📊 Get validation statistics for bundled photos
     */
    fun getValidationStats(): ValidationStats {
        val photos = strategicSamplePhotos
        val totalPhotos = photos.size
        val knownNumbers = photos.count { it.expectedNumber != null }
        val unknownNumbers = photos.count { it.expectedNumber == null }
        
        val sportBreakdown = photos.groupBy { it.sport }
            .mapValues { it.value.size }
        
        val difficultyBreakdown = photos.groupBy { it.difficultyLevel }
            .mapValues { it.value.size }
        
        return ValidationStats(
            totalPhotos = totalPhotos,
            knownNumbers = knownNumbers,
            unknownNumbers = unknownNumbers,
            sportBreakdown = sportBreakdown,
            difficultyBreakdown = difficultyBreakdown
        )
    }
    
    /**
     * 🎯 Get photos by difficulty level
     */
    fun getPhotosByDifficulty(difficulty: String): List<BundledJerseyPhoto> {
        return strategicSamplePhotos.filter { it.difficultyLevel == difficulty }
    }
    
    /**
     * 🏃 Get photos by sport
     */
    fun getPhotosBySport(sport: String): List<BundledJerseyPhoto> {
        return strategicSamplePhotos.filter { it.sport == sport }
    }
    
    /**
     * 🔍 Check if photo exists in assets
     */
    fun photoExists(filename: String): Boolean {
        return try {
            context.assets.open("$ASSETS_FOLDER/$filename").close()
            true
        } catch (e: IOException) {
            false
        }
    }
    
    /**
     * 📦 Create sample photos for testing (generates placeholder images)
     */
    fun createSamplePhotos() {
        Log.d(TAG, "📦 Creating sample jersey photos for bundled validation...")
        
        val generator = SampleJerseyGenerator(context)
        
        for (photo in strategicSamplePhotos) {
            if (!photoExists(photo.filename)) {
                Log.d(TAG, "📝 Creating: ${photo.filename} - ${photo.description}")
                
                // Generate sample image for this jersey
                photo.expectedNumber?.let { number ->
                    val jerseyColors = mapOf(
                        "soccer" to Color.rgb(0, 100, 200),
                        "basketball" to Color.rgb(85, 37, 130), 
                        "football" to Color.rgb(0, 34, 68)
                    )
                    
                    val color = jerseyColors[photo.sport] ?: Color.BLUE
                    val bitmap = generator.generateJerseyImage(number, color, Color.WHITE, photo.sport)
                    
                    // Save to assets directory (this would be done at build time normally)
                    saveBitmapToAssets(bitmap, photo.filename)
                }
            }
        }
    }
    
    /**
     * 💾 Save generated bitmap to assets (for development only)
     */
    private fun saveBitmapToAssets(bitmap: Bitmap, filename: String) {
        try {
            val assetsDir = File(context.getExternalFilesDir(null), "jersey_photos")
            assetsDir.mkdirs()
            
            val file = File(assetsDir, filename)
            val outputStream = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            Log.d(TAG, "✅ Saved sample image: $filename")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save sample image: ${e.message}")
        }
    }
}

/**
 * 📊 Validation statistics for bundled photos
 */
data class ValidationStats(
    val totalPhotos: Int,
    val knownNumbers: Int,
    val unknownNumbers: Int,
    val sportBreakdown: Map<String, Int>,
    val difficultyBreakdown: Map<String, Int>
)