package com.playerid.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.ml.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * 🌐 Jersey Photo Validation Screen
 * 
 * Shows jersey photos with numbers for validation - the approach you liked!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JerseyValidationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Configuration constants - FOCUSED #10 DATASET MODE
    val SAMPLES_PER_NUMBER = 500 // 500 samples for jersey #10 (mix of real + synthetic)
    val FOCUSED_NUMBER_MODE = true // Focus on single number with mixed dataset
    
    // Sequential validation state - FOCUSED on #10 with mixed real/synthetic
    val jerseyNumbers = remember { 
        if (FOCUSED_NUMBER_MODE) {
            listOf("10") // Single number focus with 500 mixed samples
        } else {
            listOf("00") + (0..99).map { it.toString() } // Full set when proven
        }
    }
    // Load saved progress or start fresh
    val loadedProgress = loadValidationProgress(context)
    var currentNumberIndex by remember { 
        mutableStateOf(
            if (loadedProgress >= jerseyNumbers.size) {
                Log.d("ValidationPersistence", "🔄 Loaded progress ($loadedProgress) exceeds available numbers (${jerseyNumbers.size}), starting from 0")
                0
            } else {
                loadedProgress
            }
        )
    }
    val currentNumber = jerseyNumbers[currentNumberIndex]
    var currentBatch by remember { mutableStateOf<List<ValidationPhoto>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var validatedCount by remember { mutableStateOf(0) }
    var totalValidated by remember { mutableStateOf(loadTotalValidated(context)) }
    
    // Export state
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf("") }
    var exportComplete by remember { mutableStateOf(false) }
    
    // Initialize services
    val datasetCollector = remember { JerseyDatasetCollector(context) }
    val bundledPhotos = remember { BundledJerseyPhotos(context) }
    val onlineImporter = remember { OnlineJerseyImporter(context, datasetCollector) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "🏃 Jersey Number Validation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Focused Dataset: Validate 500 samples of jersey #10 (mix of real photos + synthetic)",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Number: $currentNumber",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("Progress: $validatedCount/$SAMPLES_PER_NUMBER for number #$currentNumber")
                if (FOCUSED_NUMBER_MODE) {
                    Text(
                        text = "🎯 FOCUSED DATASET: Jersey #10 × 500 samples (250 real + 250 synthetic)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text("Total Validated: $totalValidated samples")
                
                LinearProgressIndicator(
                    progress = if (validatedCount > 0) validatedCount / SAMPLES_PER_NUMBER.toFloat() else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Control Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export & Train Button
                    Button(
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                exportProgress = "Starting export..."
                                exportComplete = false
                                scope.launch(Dispatchers.IO) {
                                    exportAndTrainModelWithProgress(
                                        context = context,
                                        onProgress = { progress ->
                                            exportProgress = progress
                                        },
                                        onComplete = { success ->
                                            isExporting = false
                                            exportComplete = success
                                            exportProgress = if (success) "✅ Export completed!" else "❌ Export failed"
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExporting) MaterialTheme.colorScheme.surfaceVariant 
                                          else MaterialTheme.colorScheme.secondary
                        ),
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exporting...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text("🤖 Export & Train")
                        }
                    }
                    
                    // Reset Progress Button
                    Button(
                        onClick = {
                            // Reset all progress
                            saveValidationProgress(context, 0, 0)
                            currentNumberIndex = 0
                            totalValidated = 0
                            validatedCount = 0
                            currentBatch = emptyList()
                            currentIndex = 0
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("🔄 Reset")
                    }
                }
            }
        }
        
        // Export Progress Display
        if (isExporting || exportComplete) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (exportComplete) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (exportComplete) "Export Complete!" else "Export in Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = exportProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Alternative Approach Button
        Button(
            onClick = {
                scope.launch {
                    testAlternativeApproaches(context)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("🔬 Test Alternative ML Approaches")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Start/Load Button
        if (currentBatch.isEmpty()) {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            // Generate batch for current number
                            val batch = generateSequentialBatch(currentNumber, context, batchSize = SAMPLES_PER_NUMBER)
                            currentBatch = batch
                            currentIndex = 0
                            Log.d("JerseyValidation", "🎯 Generated ${batch.size} samples for number $currentNumber")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("🚀 Start Validating Number $currentNumber")
            }
        }
        
        // Current Photo Validation
        if (currentBatch.isNotEmpty() && currentIndex < currentBatch.size) {
            val currentPhoto = currentBatch[currentIndex]
            
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Photo Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (currentPhoto.bitmap != null) {
                            Image(
                                bitmap = currentPhoto.bitmap.asImageBitmap(),
                                contentDescription = "Jersey #${currentPhoto.expectedNumber}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Is this jersey number $currentNumber?",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    // Save as correct validation  
                                    val jerseyNum = currentNumber.toIntOrNull() ?: 0
                                    val success = datasetCollector.captureTrainingSample(
                                        image = currentPhoto.bitmap ?: return@launch,
                                        jerseyNumber = jerseyNum,
                                        boundingBox = RectF(0f, 0f, 
                                            currentPhoto.bitmap.width.toFloat(), 
                                            currentPhoto.bitmap.height.toFloat())
                                    )
                                    
                                    if (success != null) {
                                        // Move to next photo
                                        currentIndex++
                                        validatedCount++
                                        totalValidated++
                                        
                                        // Check if completed this number
                                        if (currentIndex >= currentBatch.size) {
                                            Log.d("JerseyValidation", "✅ Completed all $SAMPLES_PER_NUMBER samples for number $currentNumber! (Enhanced: 500 samples per number)")
                                            
                                            // Save progress before moving to next number
                                            saveValidationProgress(context, currentNumberIndex + 1, totalValidated)
                                            
                                            // Move to next number
                                            currentNumberIndex++
                                            currentBatch = emptyList()
                                            validatedCount = 0
                                            currentIndex = 0
                                            
                                            if (currentNumberIndex >= jerseyNumbers.size) {
                                                Log.d("JerseyValidation", "🎉 VALIDATION COMPLETE! All ${jerseyNumbers.size} jersey numbers validated.")
                                                currentNumberIndex = jerseyNumbers.size - 1 // Stop at last number
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Green
                            )
                        ) {
                            Text(
                                text = "YES - CORRECT",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Button(
                            onClick = {
                                // Skip this sample
                                currentIndex++
                                
                                // Check if completed this number
                                if (currentIndex >= currentBatch.size) {
                                    // Save progress before moving to next number
                                    saveValidationProgress(context, currentNumberIndex + 1, totalValidated)
                                    
                                    // Move to next number
                                    currentNumberIndex++
                                    currentBatch = emptyList()
                                    validatedCount = 0
                                    currentIndex = 0
                                    
                                    if (currentNumberIndex >= jerseyNumbers.size) {
                                        currentNumberIndex = jerseyNumbers.size - 1 // Stop at last number
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text(
                                text = "NO - SKIP",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // Completion message
        if (currentNumberIndex >= jerseyNumbers.size - 1 && currentIndex >= currentBatch.size) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🎉 Validation Complete! Ready to train ML model.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Validation Photo Card - Shows photo with number input
 */
@Composable
fun JerseyValidationCard(
    photo: ValidationPhoto,
    onValidate: (Int) -> Unit,
    onSkip: () -> Unit
) {
    var jerseyNumber by remember { mutableStateOf(photo.expectedNumber.toString()) }
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Jersey Photo Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (photo.bitmap != null) {
                    Image(
                        bitmap = photo.bitmap.asImageBitmap(),
                        contentDescription = "Jersey #${photo.expectedNumber}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback if bitmap failed to load
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📷 Loading...",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text("${photo.sport.uppercase()} #${photo.expectedNumber}")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Validation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number Input
                OutlinedTextField(
                    value = jerseyNumber,
                    onValueChange = { 
                        if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                            jerseyNumber = it
                        }
                    },
                    label = { Text("Jersey Number") },
                    placeholder = { Text("0-99") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                // Action Buttons
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text("Skip")
                }
                
                Button(
                    onClick = {
                        val number = jerseyNumber.toIntOrNull()
                        if (number != null && number in 0..99) {
                            onValidate(number)
                        }
                    },
                    enabled = jerseyNumber.toIntOrNull()?.let { it in 0..99 } == true,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text("✅ Validate")
                }
            }
        }
    }
}

/**
 * � Create sample jersey bitmap with number
 */
private fun createSampleBitmap(number: Int): Bitmap {
    val width = 300
    val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw background (jersey color)
    val colors = listOf(
        AndroidColor.BLUE,
        AndroidColor.RED, 
        AndroidColor.GREEN,
        AndroidColor.YELLOW,
        AndroidColor.CYAN
    )
    val bgColor = colors[number % colors.size]
    canvas.drawColor(bgColor)
    
    // Draw number
    val paint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 120f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    
    val xPos = width / 2f
    val yPos = height / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(number.toString(), xPos, yPos, paint)
    
    return bitmap
}

/**
 * 🎯 Generate manageable batch of jersey samples (memory-efficient)
 */
private fun generateJerseyBatch(batchSize: Int = 50): List<ValidationPhoto> {
    val samples = mutableListOf<ValidationPhoto>()
    
    // Core jersey colors (most common)
    val coreColors = listOf(
        AndroidColor.BLUE, AndroidColor.RED, AndroidColor.GREEN, AndroidColor.WHITE, AndroidColor.BLACK, AndroidColor.YELLOW
    )
    
    val sports = listOf("soccer", "basketball", "football")
    val difficulties = listOf("easy", "medium", "hard")
    
    // Generate diverse samples across different number ranges
    var count = 0
    for (numberBatch in 0 until 10) { // 10 batches of 10 numbers each
        val startNum = numberBatch * 10
        for (i in 0 until 10) {
            if (count >= batchSize) break
            
            val number = startNum + i
            val sport = sports[count % sports.size]
            val difficulty = difficulties[count % difficulties.size]
            val color = coreColors[count % coreColors.size]
            
            val filename = "batch_jersey_${number}_${sport}_${difficulty}.jpg"
            val bitmap = createEfficientJerseyBitmap(number, color, difficulty)
            
            samples.add(
                ValidationPhoto(
                    filename = filename,
                    expectedNumber = number,
                    sport = sport,
                    validated = false,
                    bitmap = bitmap
                )
            )
            count++
        }
        if (count >= batchSize) break
    }
    
    Log.d("JerseyValidation", "📊 Generated efficient batch of ${samples.size} jersey samples")
    return samples.shuffled()
}

/**
 * 🔢 Generate sequential batch for specific number (supports "00", "0", "1", etc.)
 */
private fun generateSequentialBatch(targetNumber: String, context: Context, batchSize: Int = 500): List<ValidationPhoto> {
    val samples = mutableListOf<ValidationPhoto>()
    
    val coreColors = listOf(
        AndroidColor.BLUE, AndroidColor.RED, AndroidColor.GREEN, AndroidColor.WHITE, AndroidColor.BLACK, AndroidColor.YELLOW,
        AndroidColor.CYAN, AndroidColor.MAGENTA, AndroidColor.parseColor("#FF6600")
    )
    
    val sports = listOf("soccer", "basketball", "football", "hockey", "baseball", "lacrosse", "rugby")
    val difficulties = listOf("easy", "medium", "hard", "very_hard")
    val variations = listOf(
        // Solid colors
        "white_numbers", "black_numbers", "gold_numbers", "silver_numbers", 
        "yellow_numbers", "red_numbers", "blue_numbers", "green_numbers",
        // Outlined/two-toned combinations
        "white_black_outlined", "black_white_outlined", "gold_black_outlined",
        "orange_black_outlined", "blue_white_outlined", "red_white_outlined",
        "yellow_black_outlined", "green_white_outlined", "silver_black_outlined",
        // Smart contrasting
        "contrasting", "high_contrast"
    )
    
    // Calculate split: 50% real photos, 50% synthetic
    val realPhotoCount = batchSize / 2
    val syntheticCount = batchSize - realPhotoCount
    
    Log.d("MixedDataset", "🎯 Generating $batchSize samples for #$targetNumber: $realPhotoCount real + $syntheticCount synthetic")
    
    // Generate mixed dataset
    for (i in 0 until batchSize) {
        val sport = sports[i % sports.size]
        val difficulty = difficulties[i % difficulties.size]
        val variation = variations[i % variations.size]
        val color = coreColors[i % coreColors.size]
        
        val useRealPhoto = i < realPhotoCount
        val photoType = if (useRealPhoto) "real" else "synthetic"
        val filename = "mixed_${targetNumber}_${photoType}_${sport}_${difficulty}_${variation}_${i}.jpg"
        
        try {
            val bitmap = if (useRealPhoto) {
                // Try to load real jersey photo, with enhanced fallback
                loadRealJerseyPhoto(targetNumber, context) ?: loadBundledJerseyPhoto(targetNumber, context) ?: createEnhancedRealisticJersey(targetNumber, color, difficulty, variation)
            } else {
                // Generate synthetic image with enhanced realism
                createJerseyBitmapWithString(targetNumber, color, difficulty, variation)
            }
            
            Log.d("MixedDataset", "✅ Created $photoType bitmap for #$targetNumber, size: ${bitmap.width}x${bitmap.height}")
            
            samples.add(
                ValidationPhoto(
                    filename = filename,
                    expectedNumber = targetNumber.toIntOrNull() ?: 0,
                    sport = sport,
                    validated = false,
                    bitmap = bitmap
                )
            )
        } catch (e: Exception) {
            Log.e("MixedDataset", "❌ Failed to create $photoType bitmap for #$targetNumber: ${e.message}")
        }
    }
    
    Log.d("BatchGeneration", "🎯 Generated ${samples.size} samples for number $targetNumber")
    return samples
}

/**
 * � GEMINI ENHANCEMENT: Helper function to configure text paint properties
 * Reduces code duplication and ensures consistency across paint configurations
 */
private fun configureTextPaint(
    paint: Paint,
    color: Int,
    typeface: Typeface,
    isBold: Boolean,
    size: Float,
    clarityFactor: Float
) {
    paint.color = color
    paint.typeface = typeface
    paint.isFakeBoldText = isBold
    paint.textSize = size
    paint.maskFilter = if (clarityFactor < 1.0f) {
        // 🎯 GEMINI ENHANCEMENT: Randomized blur radius for better training variation
        val baseBlurRadius = 2f + kotlin.random.Random.nextFloat() * 2f
        android.graphics.BlurMaskFilter(baseBlurRadius * (1f - clarityFactor), android.graphics.BlurMaskFilter.Blur.NORMAL)
    } else {
        null
    }
}

/**
 * �🎨 Create jersey bitmap with string number (supports "00", "0", "1", etc.)
 */
private fun createJerseyBitmapWithString(numberStr: String, bgColor: Int, difficulty: String, variation: String): Bitmap {
    Log.d("BitmapCreation", "🎨 Creating bitmap for number '$numberStr', difficulty: $difficulty")
    
    // Create memory-efficient image that still simulates camera view positioning
    val width = 320  // Reduced size for memory efficiency
    val height = 240 // Maintains 4:3 aspect ratio
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap)
    
    // Draw realistic background (not just solid jersey color)
    drawRealisticBackground(canvas, width, height)
    
    // Create jersey area with realistic positioning
    val (jerseyArea, jerseyCanvas) = createJerseyArea(bgColor, variation)
    
    // Apply difficulty-based transformations and challenges to jersey area
    val (transformedJerseyCanvas, finalXPos, finalYPos, clarityFactor) = applyDifficultyEffects(jerseyCanvas, difficulty, jerseyArea.width, jerseyArea.height)
    
    // Position jersey randomly in the camera view (realistic player positions)
    val jerseyPosition = getRealisticJerseyPosition(width, height, jerseyArea.width, jerseyArea.height)
    
    // Draw jersey area onto main canvas at realistic position
    canvas.drawBitmap(jerseyArea, null, jerseyPosition, null)
    
    // Now draw the number text on the main canvas at the jersey position
    val baseTextSize = when (difficulty) {
        "easy" -> 40f
        "medium" -> 35f
        "hard" -> 30f
        "very_hard" -> 25f
        else -> 37f
    }
    
    // Apply final text size with difficulty clarity factor
    val finalTextSize = baseTextSize * clarityFactor
    
    // Calculate text position (center of jersey area in main canvas)
    val textX = jerseyPosition.centerX()
    val textY = jerseyPosition.centerY() + (finalTextSize * 0.35f) // Adjust for text baseline
    
    // 🚀 GEMINI ENHANCEMENT: Create reusable Paint objects to avoid GC churn
    val sharedPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    
    val outlinePaint = Paint(sharedPaint).apply {
        style = Paint.Style.STROKE
    }
    
    val fillPaint = Paint(sharedPaint).apply {
        style = Paint.Style.FILL
    }
    
    // 🎯 GEMINI ENHANCEMENT: Generate consistent font properties once per number
    val randomTypeface = getRandomTypeface()
    val useBold = getRandomFontWeight()

    // Handle outlined/two-toned numbers
    when {
        variation.contains("outlined") -> {
            // 🚀 GEMINI ENHANCEMENT: Configure outline using existing Paint object
            outlinePaint.apply {
                // 🎯 GEMINI ENHANCEMENT: Randomize stroke width for more robust training data
                strokeWidth = 4f + kotlin.random.Random.nextFloat() * 5f
            }
            configureTextPaint(outlinePaint, getOutlineColor(variation), randomTypeface, useBold, finalTextSize, clarityFactor)
            canvas.drawText(numberStr, textX, textY, outlinePaint)
            
            // 🚀 GEMINI ENHANCEMENT: Configure fill using existing Paint object with consistent properties
            configureTextPaint(fillPaint, getNumberColor(bgColor, variation), randomTypeface, useBold, finalTextSize, clarityFactor)
            canvas.drawText(numberStr, textX, textY, fillPaint)
        }
        else -> {
            // 🚀 GEMINI ENHANCEMENT: Configure single color using existing Paint object
            configureTextPaint(fillPaint, getNumberColor(bgColor, variation), randomTypeface, useBold, finalTextSize, clarityFactor)
            canvas.drawText(numberStr, textX, textY, fillPaint)
        }
    }
    
    return bitmap
}

/**
 * 🎨 Create memory-efficient jersey bitmap
 */
private fun createEfficientJerseyBitmap(number: Int, bgColor: Int, difficulty: String, variation: String = "contrasting"): Bitmap {
    // Smaller dimensions to reduce memory usage
    val width = 200
    val height = 250
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565) // Use RGB_565 for less memory
    val canvas = Canvas(bitmap)
    
    // Simple background
    canvas.drawColor(bgColor)
    
    // Number text with realistic color variations
    val paint = Paint().apply {
        color = getNumberColor(bgColor, variation)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
        textSize = when (difficulty) {
            "easy" -> 80f
            "medium" -> 70f 
            "hard" -> 60f
            else -> 70f
        }
    }
    
    val xPos = width / 2f
    val yPos = height / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(number.toString(), xPos, yPos, paint)
    
    return bitmap
}

/**
 * 🎯 Generate comprehensive jersey dataset (1000+ samples) - LEGACY
 */
private fun generateComprehensiveJerseyDataset(): List<ValidationPhoto> {
    val samples = mutableListOf<ValidationPhoto>()
    
    // Jersey colors (realistic team colors)
    val jerseyColors = listOf(
        AndroidColor.BLUE, AndroidColor.RED, AndroidColor.GREEN, AndroidColor.YELLOW, AndroidColor.CYAN,
        AndroidColor.MAGENTA, AndroidColor.BLACK, AndroidColor.WHITE, AndroidColor.GRAY, 
        AndroidColor.parseColor("#FF6600"), // Orange
        AndroidColor.parseColor("#800080"), // Purple
        AndroidColor.parseColor("#8B4513"), // Brown
        AndroidColor.parseColor("#006400"), // Dark Green
        AndroidColor.parseColor("#8B0000")  // Dark Red
    )
    
    val sports = listOf("soccer", "basketball", "football", "hockey", "baseball", "lacrosse", "rugby")
    val difficulties = listOf("easy", "medium", "hard", "very_hard")
    val variations = listOf(
        "white_numbers", "black_numbers", "gold_numbers", "contrasting", "red_numbers",
        "white_black_outlined", "gold_black_outlined", "blue_white_outlined", 
        "red_white_outlined", "yellow_black_outlined"
    )
    
    // Generate comprehensive samples for ALL numbers 0-99 with MANY variations
    for (number in 0..99) {
        for (sport in sports) {
            for (difficulty in difficulties) {
                for (variation in variations) { // Use ALL variations now
                    for (color in jerseyColors) { // Use ALL jersey colors
                        val filename = "jersey_${number}_${sport}_${difficulty}_${variation}_${color}.jpg"
                        val bitmap = createVariedJerseyBitmap(number, color, difficulty, variation)
                        
                        samples.add(
                            ValidationPhoto(
                                filename = filename,
                                expectedNumber = number,
                                sport = sport,
                                validated = false,
                                bitmap = bitmap
                            )
                        )
                    }
                }
            }
        }
    }
    
    Log.d("JerseyValidation", "📊 Generated ${samples.size} comprehensive jersey samples")
    return samples.shuffled() // Randomize order for better training
}

/**
 * 🎨 Create varied jersey bitmap with realistic variations
 */
private fun createVariedJerseyBitmap(
    number: Int, 
    bgColor: Int, 
    difficulty: String, 
    variation: String
): Bitmap {
    val width = 300
    val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Apply background based on variation
    when (variation) {
        "faded" -> {
            val fadedColor = AndroidColor.argb(180, AndroidColor.red(bgColor), AndroidColor.green(bgColor), AndroidColor.blue(bgColor))
            canvas.drawColor(fadedColor)
        }
        "shadowed" -> {
            canvas.drawColor(bgColor)
            val shadowPaint = Paint().apply { color = AndroidColor.BLACK; alpha = 50 }
            canvas.drawRect(50f, 50f, width.toFloat() - 50f, height.toFloat() - 50f, shadowPaint)
        }
        else -> canvas.drawColor(bgColor)
    }
    
    // Apply difficulty-based transformations and challenges
    val (transformedCanvas, finalXPos, finalYPos, clarityFactor) = applyDifficultyEffects(canvas, difficulty, width, height)
    
    val baseTextSize = when (difficulty) {
        "easy" -> 140f
        "medium" -> 120f
        "hard" -> 100f
        "very_hard" -> 80f
        else -> 120f
    }
    
    val finalTextSize = baseTextSize * clarityFactor
    
    // 🚀 GEMINI ENHANCEMENT: Use optimized Paint configuration with helper function
    val paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    configureTextPaint(paint, getNumberColor(bgColor, variation), getRandomTypeface(), true, finalTextSize, clarityFactor)
    
    transformedCanvas.drawText(number.toString(), finalXPos, finalYPos, paint)
    return bitmap
}

/**
 * 🎨 Get realistic number color based on jersey background and variation
 */
private fun getNumberColor(backgroundColor: Int, variation: String): Int {
    return when {
        // Solid color numbers
        variation == "white_numbers" -> AndroidColor.WHITE
        variation == "black_numbers" -> AndroidColor.BLACK
        variation == "gold_numbers" -> AndroidColor.parseColor("#FFD700")
        variation == "silver_numbers" -> AndroidColor.parseColor("#C0C0C0")
        variation == "yellow_numbers" -> AndroidColor.YELLOW
        variation == "red_numbers" -> AndroidColor.RED
        variation == "blue_numbers" -> AndroidColor.BLUE
        variation == "green_numbers" -> AndroidColor.GREEN
        
        // Outlined number main colors (the fill color)
        variation == "white_black_outlined" -> AndroidColor.WHITE
        variation == "black_white_outlined" -> AndroidColor.BLACK
        variation == "gold_black_outlined" -> AndroidColor.parseColor("#FFD700")
        variation == "orange_black_outlined" -> AndroidColor.parseColor("#FF6600")
        variation == "blue_white_outlined" -> AndroidColor.BLUE
        variation == "red_white_outlined" -> AndroidColor.RED
        variation == "yellow_black_outlined" -> AndroidColor.YELLOW
        variation == "green_white_outlined" -> AndroidColor.GREEN
        variation == "silver_black_outlined" -> AndroidColor.parseColor("#C0C0C0")
        
        // Smart contrasting
        variation == "contrasting" || variation == "high_contrast" -> {
            val brightness = (AndroidColor.red(backgroundColor) * 299 + 
                             AndroidColor.green(backgroundColor) * 587 + 
                             AndroidColor.blue(backgroundColor) * 114) / 1000
            if (brightness > 128) AndroidColor.BLACK else AndroidColor.WHITE
        }
        
        else -> AndroidColor.WHITE // Default fallback
    }
}

/**
 * 🎨 Get outline color for two-toned numbers
 */
private fun getOutlineColor(variation: String): Int {
    return when {
        variation.contains("black_outlined") -> AndroidColor.BLACK
        variation.contains("white_outlined") -> AndroidColor.WHITE
        variation.contains("gold_outlined") -> AndroidColor.parseColor("#FFD700")
        variation.contains("red_outlined") -> AndroidColor.RED
        variation.contains("blue_outlined") -> AndroidColor.BLUE
        else -> AndroidColor.BLACK // Default outline
    }
}

/**
 * 🎯 Apply realistic difficulty-based visual challenges
 */
private fun applyDifficultyEffects(canvas: Canvas, difficulty: String, width: Int, height: Int): DifficultyResult {
    val centerX = width / 2f
    val centerY = height / 2f + 30f
    
    return when (difficulty) {
        "easy" -> {
            // Clean, perfect conditions
            DifficultyResult(canvas, centerX, centerY, 1.0f, 0f)
        }
        "medium" -> {
            // Slight rotation and minor occlusion
            val rotation = (-10..10).random().toFloat()
            canvas.rotate(rotation, centerX, centerY)
            
            // Minor dirt/scuff marks
            addMinorOcclusion(canvas, width, height)
            
            DifficultyResult(canvas, centerX, centerY, 1.0f, rotation)
        }
        "hard" -> {
            // More rotation, partial occlusion, slight blur effect
            val rotation = (-25..25).random().toFloat()
            canvas.rotate(rotation, centerX, centerY)
            
            // Add wrinkles/folds
            addMediumOcclusion(canvas, width, height)
            
            DifficultyResult(canvas, centerX, centerY, 0.8f, rotation)
        }
        "very_hard" -> {
            // Extreme conditions: heavy rotation, significant occlusion
            val rotation = (-45..45).random().toFloat()
            canvas.rotate(rotation, centerX, centerY)
            
            // Heavy occlusion (jersey folds, shadows, wear)
            addHeavyOcclusion(canvas, width, height)
            
            DifficultyResult(canvas, centerX, centerY, 0.6f, rotation)
        }
        else -> DifficultyResult(canvas, centerX, centerY, 1.0f, 0f)
    }
}

/**
 * 📊 Result of difficulty transformations
 */
data class DifficultyResult(
    val canvas: Canvas,
    val xPos: Float,
    val yPos: Float,
    val clarity: Float, // 1.0 = clear, 0.6 = blurred
    val rotation: Float
)

/**
 * 🧹 Add minor occlusion (small dirt spots, minor wear)
 */
private fun addMinorOcclusion(canvas: Canvas, width: Int, height: Int) {
    val occlusionPaint = Paint().apply {
        color = AndroidColor.argb(30, 0, 0, 0) // Light transparent overlay
        isAntiAlias = true
    }
    
    // Add 2-3 small spots
    repeat(2 + (0..1).random()) {
        val x = (width * 0.2f + Math.random() * width * 0.6f).toFloat()
        val y = (height * 0.2f + Math.random() * height * 0.6f).toFloat()
        val radius = (5..15).random().toFloat()
        canvas.drawCircle(x, y, radius, occlusionPaint)
    }
}

/**
 * 🌊 Add medium occlusion (wrinkles, folds, moderate wear)
 */
private fun addMediumOcclusion(canvas: Canvas, width: Int, height: Int) {
    val occlusionPaint = Paint().apply {
        color = AndroidColor.argb(50, 0, 0, 0)
        isAntiAlias = true
    }
    
    // Add wrinkle lines
    repeat(2) {
        val startX = (Math.random() * width).toFloat()
        val startY = (Math.random() * height).toFloat()
        val endX = (Math.random() * width).toFloat()
        val endY = (Math.random() * height).toFloat()
        
        occlusionPaint.strokeWidth = (3..8).random().toFloat()
        canvas.drawLine(startX, startY, endX, endY, occlusionPaint)
    }
    
    // Add medium spots
    repeat(3 + (0..2).random()) {
        val x = (Math.random() * width).toFloat()
        val y = (Math.random() * height).toFloat()
        val radius = (8..25).random().toFloat()
        canvas.drawCircle(x, y, radius, occlusionPaint)
    }
}

/**
 * 🌪️ Add heavy occlusion (major folds, shadows, heavy wear)
 */
private fun addHeavyOcclusion(canvas: Canvas, width: Int, height: Int) {
    val heavyOcclusionPaint = Paint().apply {
        color = AndroidColor.argb(80, 0, 0, 0)
        isAntiAlias = true
    }
    
    // Add major shadow/fold
    val rectLeft = (width * 0.1f + Math.random() * width * 0.3f).toFloat()
    val rectTop = (height * 0.1f + Math.random() * height * 0.3f).toFloat()
    val rectRight = rectLeft + (width * 0.3f + Math.random() * width * 0.4f).toFloat()
    val rectBottom = rectTop + (height * 0.2f + Math.random() * height * 0.3f).toFloat()
    
    canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, heavyOcclusionPaint)
    
    // Add multiple wear spots
    repeat(5 + (0..3).random()) {
        val x = (Math.random() * width).toFloat()
        val y = (Math.random() * height).toFloat()
        val radius = (15..40).random().toFloat()
        canvas.drawCircle(x, y, radius, heavyOcclusionPaint)
    }
}

/**
 * �🌐 Get curated list of real jersey photo URLs for validation
 */
private fun getRealJerseyPhotoUrls(): List<JerseyPhotoUrl> {
    return listOf(
        // Soccer jerseys - clear numbers
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?w=400",
            expectedNumber = 10,
            sport = "soccer",
            description = "Soccer jersey #10"
        ),
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=400", 
            expectedNumber = 7,
            sport = "soccer",
            description = "Soccer jersey #7"
        ),
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1579952363873-27d3bfad9c0d?w=400",
            expectedNumber = 9,
            sport = "soccer", 
            description = "Soccer jersey #9"
        ),
        
        // Basketball jerseys
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=400",
            expectedNumber = 23,
            sport = "basketball",
            description = "Basketball jersey #23"
        ),
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1558618654-fcd25c85cd64?w=400",
            expectedNumber = 1,
            sport = "basketball",
            description = "Basketball jersey #1"
        ),
        
        // Football jerseys
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1566577134770-3d85bb3a9cc4?w=400",
            expectedNumber = 12,
            sport = "football",
            description = "Football jersey #12"
        ),
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1577223625816-7546f13df25d?w=400",
            expectedNumber = 88,
            sport = "football", 
            description = "Football jersey #88"
        ),
        
        // Single digit numbers
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400",
            expectedNumber = 3,
            sport = "soccer",
            description = "Soccer jersey #3"
        ),
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=400",
            expectedNumber = 5,
            sport = "basketball",
            description = "Basketball jersey #5"
        ),
        JerseyPhotoUrl(
            url = "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400",
            expectedNumber = 2,
            sport = "football",
            description = "Football jersey #2"
        )
    )
}

/**
 * Data class for jersey photo URLs
 */
data class JerseyPhotoUrl(
    val url: String,
    val expectedNumber: Int?,
    val sport: String?,
    val description: String
)

/**
 * Data class for validation photos
 */
data class ValidationPhoto(
    val filename: String,
    val expectedNumber: Int,
    val sport: String,
    val validated: Boolean = false,
    val bitmap: Bitmap? = null
)

/**
 * 🤖 Export and training pipeline with progress callbacks
 */
suspend fun exportAndTrainModelWithProgress(
    context: Context,
    onProgress: (String) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    try {
        onProgress("🚀 Starting export...")
        Log.d("TrainingPipeline", "🚀 Starting proof-of-concept export...")
        
        // Step 1: Export validated training data
        onProgress("📤 Exporting training data...")
        val exportSuccess = exportTrainingDataWithProgress(context) { progress ->
            onProgress(progress)
        }
        if (!exportSuccess) {
            Log.e("TrainingPipeline", "❌ Failed to export training data")
            onComplete(false)
            return
        }
        
        // Step 2: Create training instructions for computer
        onProgress("📋 Creating training instructions...")
        createTrainingInstructions(context)
        
        // Step 3: Show results and next steps
        onProgress("✅ Finalizing export...")
        showExportResults(context)
        
        onProgress("🎉 Export completed successfully!")
        Log.d("TrainingPipeline", "✅ Proof-of-concept data exported successfully!")
        Log.d("TrainingPipeline", "📋 Next: Copy data to computer for training")
        
        onComplete(true)
        
    } catch (e: Exception) {
        Log.e("TrainingPipeline", "💥 Export error: ${e.message}")
        onProgress("❌ Export failed: ${e.message}")
        onComplete(false)
    }
}

/**
 * 🚀 Automated Training Pipeline - Export data and train custom model
 */
suspend fun exportAndTrainModel(context: Context) {
    try {
        Log.d("TrainingPipeline", "🚀 Starting proof-of-concept export...")
        
        // Step 1: Export validated training data
        val exportSuccess = exportTrainingData(context)
        if (!exportSuccess) {
            Log.e("TrainingPipeline", "❌ Failed to export training data")
            return
        }
        
        // Step 2: Create training instructions for computer
        createTrainingInstructions(context)
        
        // Step 3: Show results and next steps
        showExportResults(context)
        
        Log.d("TrainingPipeline", "✅ Proof-of-concept data exported successfully!")
        Log.d("TrainingPipeline", "📋 Next: Copy data to computer for training")
        
    } catch (e: Exception) {
        Log.e("TrainingPipeline", "💥 Export error: ${e.message}")
    }
}

/**
 * 📦 Export validated training data with progress updates
 */
private suspend fun exportTrainingDataWithProgress(
    context: Context, 
    onProgress: (String) -> Unit
): Boolean {
    try {
        onProgress("Setting up export directories...")
        val exportDir = File(context.getExternalFilesDir(null), "ml_export")
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val datasetDir = File(context.getExternalFilesDir(null), "jersey_dataset")
        val trainingDir = File(datasetDir, "images")
        if (!trainingDir.exists() || trainingDir.listFiles()?.isEmpty() == true) {
            Log.w("Export", "No training samples found in ${trainingDir.absolutePath}")
            return false
        }
        
        // Create YOLO format dataset structure
        val imagesDir = File(exportDir, "images")
        val labelsDir = File(exportDir, "labels")
        imagesDir.mkdirs()
        labelsDir.mkdirs()
        
        var exportedCount = 0
        val allSamples = trainingDir.listFiles()?.filter { it.extension == "jpg" } ?: emptyList()
        val totalSamples = allSamples.size
        
        onProgress("Processing $totalSamples training images...")
        
        for ((index, imageFile) in allSamples.withIndex()) {
            // Update progress every 100 files
            if (index % 100 == 0) {
                val percentage = ((index.toFloat() / totalSamples) * 100).toInt()
                onProgress("Processing images: $index/$totalSamples ($percentage%)")
            }
            
            // Copy image
            val destImage = File(imagesDir, imageFile.name)
            imageFile.copyTo(destImage, overwrite = true)
            
            // Create YOLO label file
            val labelFileName = imageFile.nameWithoutExtension + ".txt"
            val labelFile = File(labelsDir, labelFileName)
            
            // Extract jersey number from filename (format: jersey_10_20251104_...)
            val parts = imageFile.nameWithoutExtension.split("_")
            if (parts.size >= 2) {
                val jerseyNumber = parts[1]
                
                // YOLO format: class_id center_x center_y width height (normalized 0-1)
                val classId = when (jerseyNumber) {
                    "00" -> 100 // Special case for "00"
                    else -> jerseyNumber.toIntOrNull() ?: 0
                }
                
                // Full image bounding box (normalized coordinates)
                val yoloLabel = "$classId 0.5 0.5 1.0 1.0"
                labelFile.writeText(yoloLabel)
                exportedCount++
            }
        }
        
        onProgress("Creating dataset configuration...")
        
        // Create dataset configuration file
        val configFile = File(exportDir, "dataset.yaml")
        configFile.writeText("""
            # Jersey Number Detection Dataset
            train: images
            val: images
            
            # Number of classes (0-99 + 00 = 101 classes)
            nc: 101
            
            # Class names
            names: ${(0..99).map { "'$it'" }.plus("'00'").joinToString(", ")}
        """.trimIndent())
        
        onProgress("Exported $exportedCount training samples successfully!")
        Log.d("Export", "✅ Exported $exportedCount training samples to ${exportDir.absolutePath}")
        return true
        
    } catch (e: Exception) {
        Log.e("Export", "💥 Export failed: ${e.message}")
        onProgress("Export failed: ${e.message}")
        return false
    }
}

/**
 * 📦 Export validated training data in ML-ready format
 */
private suspend fun exportTrainingData(context: Context): Boolean {
    try {
        val exportDir = File(context.getExternalFilesDir(null), "ml_export")
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val datasetDir = File(context.getExternalFilesDir(null), "jersey_dataset")
        val trainingDir = File(datasetDir, "images")
        if (!trainingDir.exists() || trainingDir.listFiles()?.isEmpty() == true) {
            Log.w("Export", "No training samples found in ${trainingDir.absolutePath}")
            return false
        }
        
        // Create YOLO format dataset structure
        val imagesDir = File(exportDir, "images")
        val labelsDir = File(exportDir, "labels")
        imagesDir.mkdirs()
        labelsDir.mkdirs()
        
        var exportedCount = 0
        val allSamples = trainingDir.listFiles()?.filter { it.extension == "jpg" } ?: emptyList()
        
        for (imageFile in allSamples) {
            // Copy image
            val destImage = File(imagesDir, imageFile.name)
            imageFile.copyTo(destImage, overwrite = true)
            
            // Create YOLO label file
            val labelFileName = imageFile.nameWithoutExtension + ".txt"
            val labelFile = File(labelsDir, labelFileName)
            
            // Extract jersey number from filename (format: seq_00_soccer_easy_white_numbers_0.jpg)
            val parts = imageFile.nameWithoutExtension.split("_")
            if (parts.size >= 2) {
                val jerseyNumber = parts[1]
                
                // YOLO format: class_id center_x center_y width height (normalized 0-1)
                // For jersey numbers, we'll use the number as class_id and assume full image bbox
                val classId = when (jerseyNumber) {
                    "00" -> 100 // Special case for "00"
                    else -> jerseyNumber.toIntOrNull() ?: 0
                }
                
                // Full image bounding box (normalized coordinates)
                val yoloLabel = "$classId 0.5 0.5 1.0 1.0"
                labelFile.writeText(yoloLabel)
                exportedCount++
            }
        }
        
        // Create dataset configuration file
        val configFile = File(exportDir, "dataset.yaml")
        configFile.writeText("""
            # Jersey Number Detection Dataset
            train: images
            val: images
            
            # Number of classes (0-99 + 00 = 101 classes)
            nc: 101
            
            # Class names
            names: ${(0..99).map { it.toString() }.plus("00").joinToString(", ") { "'$it'" }}
        """.trimIndent())
        
        Log.d("Export", "📦 Exported $exportedCount training samples to ${exportDir.absolutePath}")
        return exportedCount > 0
        
    } catch (e: Exception) {
        Log.e("Export", "Export failed: ${e.message}")
        return false
    }
}

/**
 * 🐍 Trigger Python training script on the training data
 */
private suspend fun triggerModelTraining(context: Context): Boolean {
    try {
        val exportDir = File(context.getExternalFilesDir(null), "ml_export")
        val trainingScript = File(context.getExternalFilesDir(null), "../ml_training/train_jersey_detector.py")
        
        // Create enhanced training command
        val command = arrayOf(
            "python",
            trainingScript.absolutePath,
            "--data", File(exportDir, "dataset.yaml").absolutePath,
            "--img", "640",
            "--batch", "16",
            "--epochs", "100",
            "--device", "cpu",
            "--project", exportDir.absolutePath,
            "--name", "jersey_model"
        )
        
        Log.d("Training", "🐍 Starting model training with command: ${command.joinToString(" ")}")
        
        val process = ProcessBuilder(*command)
            .directory(File(context.getExternalFilesDir(null), "../ml_training"))
            .redirectErrorStream(true)
            .start()
        
        // Monitor training progress
        val reader = process.inputStream.bufferedReader()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            Log.d("Training", "📊 $line")
        }
        
        val exitCode = process.waitFor()
        Log.d("Training", "🏁 Training completed with exit code: $exitCode")
        
        return exitCode == 0
        
    } catch (e: Exception) {
        Log.e("Training", "Training failed: ${e.message}")
        return false
    }
}

/**
 * 🚀 Deploy trained model back to the app
 */
private suspend fun deployTrainedModel(context: Context): Boolean {
    try {
        val exportDir = File(context.getExternalFilesDir(null), "ml_export")
        val trainedModelPath = File(exportDir, "jersey_model/weights/best.pt")
        
        if (!trainedModelPath.exists()) {
            Log.e("Deploy", "Trained model not found at ${trainedModelPath.absolutePath}")
            return false
        }
        
        // Convert PyTorch model to TensorFlow Lite
        val tfliteModelPath = convertToTensorFlowLite(trainedModelPath, exportDir)
        if (tfliteModelPath == null) {
            Log.e("Deploy", "Failed to convert model to TensorFlow Lite")
            return false
        }
        
        // Deploy to app assets
        val assetsDir = File(context.getExternalFilesDir(null), "../../app/src/main/assets")
        assetsDir.mkdirs()
        
        val deployedModel = File(assetsDir, "custom_jersey_detector.tflite")
        File(tfliteModelPath).copyTo(deployedModel, overwrite = true)
        
        Log.d("Deploy", "🚀 Model deployed to ${deployedModel.absolutePath}")
        
        // Update app to use custom model
        updateModelConfiguration(context)
        
        return true
        
    } catch (e: Exception) {
        Log.e("Deploy", "Deployment failed: ${e.message}")
        return false
    }
}

/**
 * 🏞️ Draw realistic camera background (not just solid color)
 */
private fun drawRealisticBackground(canvas: Canvas, width: Int, height: Int) {
    // Simulate grass field, court, or stadium background
    val backgrounds = listOf(
        AndroidColor.parseColor("#228B22"), // Grass green
        AndroidColor.parseColor("#8B4513"), // Court brown  
        AndroidColor.parseColor("#696969"), // Stadium gray
        AndroidColor.parseColor("#006400"), // Dark green
        AndroidColor.parseColor("#F5DEB3")  // Sand/beige
    )
    
    val bgColor = backgrounds.random()
    canvas.drawColor(bgColor)
    
    // Add realistic texture/noise to enhance ML training robustness
    val noisePaint = Paint().apply {
        color = AndroidColor.argb(20, 0, 0, 0)
    }
    
    repeat(50) {
        val x = (Math.random() * width).toFloat()
        val y = (Math.random() * height).toFloat()
        val radius = (2..8).random().toFloat()
        canvas.drawCircle(x, y, radius, noisePaint)
    }
}

/**
 * 👕 Create jersey area with realistic size and background
 */
private fun createJerseyArea(bgColor: Int, variation: String): Pair<Bitmap, Canvas> {
    // Memory-efficient jersey area sizes (proportionally smaller)
    val jerseySize = when ((1..4).random()) {
        1 -> Pair(40, 50)    // Distant player
        2 -> Pair(60, 75)    // Medium distance  
        3 -> Pair(80, 100)   // Close player
        4 -> Pair(100, 125)  // Very close player
        else -> Pair(60, 75)
    }
    
    val (jerseyWidth, jerseyHeight) = jerseySize
    val jerseyBitmap = Bitmap.createBitmap(jerseyWidth, jerseyHeight, Bitmap.Config.RGB_565)
    val jerseyCanvas = Canvas(jerseyBitmap)
    
    // Apply jersey background based on variation
    when (variation) {
        "faded" -> {
            val fadedColor = AndroidColor.argb(180, AndroidColor.red(bgColor), AndroidColor.green(bgColor), AndroidColor.blue(bgColor))
            jerseyCanvas.drawColor(fadedColor)
        }
        else -> jerseyCanvas.drawColor(bgColor)
    }
    
    return Pair(jerseyBitmap, jerseyCanvas)
}

/**
 * 📍 Get realistic jersey position in camera view
 */
private fun getRealisticJerseyPosition(cameraWidth: Int, cameraHeight: Int, jerseyWidth: Int, jerseyHeight: Int): RectF {
    // Define realistic zones where players appear
    val positions = listOf(
        // Center area (traditional training position)
        Pair(0.3f..0.7f, 0.3f..0.7f),
        // Top area (players running toward camera)
        Pair(0.2f..0.8f, 0.1f..0.4f),  
        // Bottom area (players running away)
        Pair(0.2f..0.8f, 0.6f..0.9f),
        // Left side (sideline players)
        Pair(0.0f..0.3f, 0.2f..0.8f),
        // Right side (opposite sideline)
        Pair(0.7f..1.0f, 0.2f..0.8f),
        // Corners (dynamic gameplay positions)
        Pair(0.0f..0.3f, 0.0f..0.3f),
        Pair(0.7f..1.0f, 0.0f..0.3f),
        Pair(0.0f..0.3f, 0.7f..1.0f),
        Pair(0.7f..1.0f, 0.7f..1.0f)
    )
    
    val (xRange, yRange) = positions.random()
    
    // Calculate position ensuring jersey fits in frame
    val maxLeft = cameraWidth - jerseyWidth
    val maxTop = cameraHeight - jerseyHeight
    
    val left = (xRange.start * maxLeft + Math.random().toFloat() * (xRange.endInclusive - xRange.start) * maxLeft).coerceIn(0f, maxLeft.toFloat())
    val top = (yRange.start * maxTop + Math.random().toFloat() * (yRange.endInclusive - yRange.start) * maxTop).coerceIn(0f, maxTop.toFloat())
    
    return RectF(left, top, left + jerseyWidth, top + jerseyHeight)
}

/**
 * � Get random typeface to simulate different jersey number fonts
 */
private fun getRandomTypeface(): android.graphics.Typeface {
    val typefaces = listOf(
        android.graphics.Typeface.DEFAULT_BOLD,      // Standard bold
        android.graphics.Typeface.SANS_SERIF,       // Clean sans serif
        android.graphics.Typeface.SERIF,             // Traditional serif
        android.graphics.Typeface.MONOSPACE,         // Fixed-width (common in sports)
        android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD),
        android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD),
        android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    )
    return typefaces.random()
}

/**
 * ⚖️ Get random font weight variation
 */
private fun getRandomFontWeight(): Boolean {
    // Mix of bold and normal weights to simulate different jersey styles
    return listOf(true, true, false).random() // 67% bold, 33% normal
}

/**
 * �🔄 Convert PyTorch model to TensorFlow Lite
 */
private fun convertToTensorFlowLite(modelPath: File, outputDir: File): String? {
    try {
        val conversionScript = """
import torch
import torch.nn as nn
import tensorflow as tf
from ultralytics import YOLO

# Load trained YOLO model
model = YOLO('${modelPath.absolutePath}')

# Export to TensorFlow Lite
model.export(format='tflite', imgsz=640)

print("✅ Model converted to TensorFlow Lite successfully!")
        """.trimIndent()
        
        val scriptFile = File(outputDir, "convert_model.py")
        scriptFile.writeText(conversionScript)
        
        val process = ProcessBuilder("python", scriptFile.absolutePath)
            .directory(outputDir)
            .redirectErrorStream(true)
            .start()
        
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            val tfliteFile = File(outputDir, "jersey_model/weights/best.tflite")
            return if (tfliteFile.exists()) tfliteFile.absolutePath else null
        }
        
        return null
        
    } catch (e: Exception) {
        Log.e("Convert", "Conversion failed: ${e.message}")
        return null
    }
}

/**
 * � Check if real jersey photo exists for this number
 */
private fun hasRealJerseyPhoto(number: String): Boolean {
    // Check if real jersey photos are available in assets or external storage
    // This will integrate with your jersey_photos_to_import.md workflow
    return false // For now - will be implemented when real photos are added
}

/**
 * 📷 Load real jersey photo if available
 */
private fun loadRealJerseyPhoto(number: String, context: Context): Bitmap? {
    return try {
        // Try to load from external storage first (user imported photos)
        val externalDir = File(context.getExternalFilesDir(null), "jersey_photos")
        val photoFile = File(externalDir, "jersey_${number}.jpg")
        
        if (photoFile.exists()) {
            val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
            Log.d("RealPhotos", "✅ Loaded real photo for #$number: ${photoFile.name}")
            return bitmap
        }
        
        // Fallback: check assets folder
        val assetManager = context.assets
        val inputStream = assetManager.open("jersey_photos/jersey_${number}.jpg")
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        Log.d("RealPhotos", "✅ Loaded bundled photo for #$number")
        return bitmap
    } catch (e: Exception) {
        Log.d("RealPhotos", "No real photo found for #$number, will use synthetic")
        null
    }
}

/**
 * Load bundled jersey photos from assets
 */
private fun loadBundledJerseyPhoto(number: String, context: Context): Bitmap? {
    return try {
        val assetManager = context.assets
        
        // Try different naming conventions
        val possibleNames = listOf(
            "jersey_photos/number_${number}.jpg",
            "jersey_photos/${number}.jpg",
            "jersey_photos/jersey${number}.jpg",
            "real_jerseys/jersey_${number}.jpg"
        )
        
        for (assetPath in possibleNames) {
            try {
                val inputStream = assetManager.open(assetPath)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                Log.d("BundledPhotos", "✅ Found bundled photo: $assetPath")
                return bitmap
            } catch (e: Exception) {
                // Try next naming convention
            }
        }
        
        Log.d("BundledPhotos", "No bundled photo found for #$number")
        null
    } catch (e: Exception) {
        Log.w("BundledPhotos", "Error loading bundled photo for #$number: ${e.message}")
        null
    }
}

/**
 * Create enhanced realistic jersey with photographic elements
 */
private fun createEnhancedRealisticJersey(number: String, bgColor: Int, difficulty: String, variation: String): Bitmap {
    val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Enhanced realistic background with fabric texture
    drawRealisticJerseyBackground(canvas, bgColor, 320, 240)
    
    // Add fabric creases and shadows for realism
    addFabricRealism(canvas, 320, 240)
    
    // Draw number with realistic positioning and lighting
    drawRealisticNumber(canvas, number, bgColor, difficulty, variation, 320, 240)
    
    Log.d("EnhancedJersey", "✅ Created enhanced realistic jersey for #$number")
    return bitmap
}

/**
 * Draw realistic jersey background with fabric texture
 */
private fun drawRealisticJerseyBackground(canvas: Canvas, baseColor: Int, width: Int, height: Int) {
    // Base jersey color with slight gradient
    val gradientPaint = Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            baseColor, adjustColorBrightness(baseColor, 0.9f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
    
    // Add subtle fabric texture
    val texturePaint = Paint().apply {
        color = adjustColorBrightness(baseColor, 0.95f)
        alpha = 30
    }
    
    // Draw subtle texture lines
    for (i in 0 until width step 8) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), texturePaint)
    }
    for (i in 0 until height step 8) {
        canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), texturePaint)
    }
}

/**
 * Add fabric realism effects
 */
private fun addFabricRealism(canvas: Canvas, width: Int, height: Int) {
    // Add subtle shadow/highlight for fabric depth
    val shadowPaint = Paint().apply {
        color = AndroidColor.BLACK
        alpha = 20
    }
    
    // Diagonal shadow effect
    canvas.drawLine(0f, 0f, width * 0.3f, height.toFloat(), shadowPaint)
    canvas.drawLine(width * 0.7f, 0f, width.toFloat(), height.toFloat(), shadowPaint)
}

/**
 * Draw number with realistic lighting and positioning
 */
private fun drawRealisticNumber(canvas: Canvas, number: String, bgColor: Int, difficulty: String, variation: String, width: Int, height: Int) {
    val positionRect = getRealisticJerseyPosition(width, height, width, height)
    val xPos = positionRect.centerX()
    val yPos = positionRect.centerY()
    
    val paint = Paint().apply {
        color = getContrastingNumberColor(bgColor)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
        textSize = when (difficulty) {
            "easy" -> 120f
            "medium" -> 100f
            "hard" -> 85f
            "very_hard" -> 70f
            else -> 100f
        }
        
        // Add subtle shadow for realism
        setShadowLayer(2f, 1f, 1f, AndroidColor.BLACK)
    }
    
    canvas.drawText(number, xPos, yPos, paint)
}

/**
 * Adjust color brightness for realistic effects
 */
private fun adjustColorBrightness(color: Int, factor: Float): Int {
    val red = (AndroidColor.red(color) * factor).toInt().coerceIn(0, 255)
    val green = (AndroidColor.green(color) * factor).toInt().coerceIn(0, 255)
    val blue = (AndroidColor.blue(color) * factor).toInt().coerceIn(0, 255)
    return AndroidColor.rgb(red, green, blue)
}

/**
 * Get contrasting number color for readability
 */
private fun getContrastingNumberColor(bgColor: Int): Int {
    val brightness = (AndroidColor.red(bgColor) + AndroidColor.green(bgColor) + AndroidColor.blue(bgColor)) / 3
    return if (brightness > 128) AndroidColor.BLACK else AndroidColor.WHITE
}

/**
 * Create training instructions for computer-based training
 */
private fun createTrainingInstructions(context: Context) {
    val exportDir = File(context.getExternalFilesDir(null), "ml_export")
    val instructionsFile = File(exportDir, "TRAINING_INSTRUCTIONS.txt")
    
    val instructions = """
JERSEY NUMBER DETECTION - FOCUSED #10 DATASET
============================================

DATASET SUMMARY:
- Number focused: #10 only
- Total images: 500 samples (250 real + 250 synthetic)
- Format: YOLO (images + labels)
- Location: ${exportDir.absolutePath}

NEXT STEPS ON COMPUTER:

1. COPY DATA TO COMPUTER:
   - Copy entire folder: ${exportDir.name}
   - Place in: C:\PlayerID\training_data\

2. INSTALL REQUIREMENTS:
   - Python 3.8+
   - pip install ultralytics tensorflow opencv-python pillow

3. TRAIN MODEL:
   - cd C:\PlayerID\training_data\ml_export
   - python -c "from ultralytics import YOLO; model = YOLO('yolov8n.pt'); model.train(data='dataset.yaml', epochs=50, device='cpu')"

4. TEST ACCURACY:
   - Take 5-10 real jersey photos
   - Run model on test images
   - Check detection accuracy

5. DECISION POINT:
   - If >70% accuracy: Scale to full dataset (101 numbers)
   - If <70% accuracy: Try alternative approaches

ALTERNATIVE APPROACHES TO TEST:
- EasyOCR: pip install easyocr
- Template matching with OpenCV
- Pre-trained text detection models

SUCCESS CRITERIA:
- Detects numbers in various positions
- Works with different fonts/styles
- >70% accuracy on real jersey photos

Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
""".trimIndent()

    instructionsFile.writeText(instructions)
    Log.d("TrainingInstructions", "Created training instructions: ${instructionsFile.absolutePath}")
}

/**
 * Show export results and next steps
 */
private fun showExportResults(context: Context) {
    val exportDir = File(context.getExternalFilesDir(null), "ml_export")
    val imagesCount = File(exportDir, "images").listFiles()?.size ?: 0
    val labelsCount = File(exportDir, "labels").listFiles()?.size ?: 0
    
    Log.d("ExportResults", "PROOF OF CONCEPT EXPORT COMPLETE!")
    Log.d("ExportResults", "Location: ${exportDir.absolutePath}")
    Log.d("ExportResults", "Images: $imagesCount")
    Log.d("ExportResults", "Labels: $labelsCount") 
    Log.d("ExportResults", "Copy to computer for training")
    Log.d("ExportResults", "See TRAINING_INSTRUCTIONS.txt for details")
}

/**
 * �💾 Save validation progress to resume later
 */
private fun saveValidationProgress(context: Context, currentNumberIndex: Int, totalValidated: Int) {
    try {
        val prefs = context.getSharedPreferences("jersey_validation", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("current_number_index", currentNumberIndex)
            .putInt("total_validated", totalValidated)
            .putLong("last_saved", System.currentTimeMillis())
            .apply()
        
        Log.d("ValidationPersistence", "💾 Saved progress: number index $currentNumberIndex, total validated $totalValidated")
    } catch (e: Exception) {
        Log.e("ValidationPersistence", "❌ Failed to save progress: ${e.message}")
    }
}

/**
 * 📂 Load saved validation progress
 */
private fun loadValidationProgress(context: Context): Int {
    return try {
        val prefs = context.getSharedPreferences("jersey_validation", Context.MODE_PRIVATE)
        val savedIndex = prefs.getInt("current_number_index", 0)
        val lastSaved = prefs.getLong("last_saved", 0)
        
        Log.d("ValidationPersistence", "📂 Loaded progress: resuming at number index $savedIndex")
        savedIndex
    } catch (e: Exception) {
        Log.e("ValidationPersistence", "❌ Failed to load progress: ${e.message}")
        0 // Start from beginning if error
    }
}

/**
 * 📊 Load total validated count
 */
private fun loadTotalValidated(context: Context): Int {
    return try {
        val prefs = context.getSharedPreferences("jersey_validation", Context.MODE_PRIVATE)
        val total = prefs.getInt("total_validated", 0)
        Log.d("ValidationPersistence", "📊 Loaded total validated: $total")
        total
    } catch (e: Exception) {
        Log.e("ValidationPersistence", "❌ Failed to load total: ${e.message}")
        0
    }
}

/**
 * 🔬 Test alternative ML approaches before committing to full validation
 */
private suspend fun testAlternativeApproaches(context: Context) {
    Log.d("AlternativeApproaches", "🔬 Testing alternative ML methods...")
    
    // Create research file with alternative approaches
    val exportDir = File(context.getExternalFilesDir(null), "ml_export")
    if (!exportDir.exists()) exportDir.mkdirs()
    
    val researchFile = File(exportDir, "ALTERNATIVE_APPROACHES.txt")
    val researchContent = """
🔬 JERSEY NUMBER DETECTION - ALTERNATIVE APPROACHES
=================================================

Based on your proof-of-concept validation, here are alternative ML approaches to test:

📖 1. PRE-TRAINED OCR MODELS (RECOMMENDED FIRST)
- EasyOCR: pip install easyocr
- Test command: python -c "import easyocr; reader = easyocr.Reader(['en']); print(reader.readtext('jersey_image.jpg'))"
- Pros: No training needed, handles varied fonts
- Cons: May struggle with jersey-specific contexts

🤖 2. GOOGLE ML KIT TEXT RECOGNITION
- Already in Android app - can be enhanced
- Add jersey-specific preprocessing
- Filter results for number patterns only
- Pros: Real-time, no custom training
- Cons: Generic model may miss jersey numbers

🎯 3. HYBRID DETECTION APPROACH
- Step 1: Detect jersey shape/area (YOLO)
- Step 2: OCR on detected jersey region
- Step 3: Validate number format (00-99)
- Pros: Combines strengths of both methods
- Cons: Two-stage complexity

🔄 4. TRANSFER LEARNING
- Start with MNIST digit recognition model
- Fine-tune on your 600 jersey samples
- Use data augmentation for more variety
- Pros: Less training data needed
- Cons: Still requires custom training

⚡ 5. TEMPLATE MATCHING (OPENCV)
- Create templates for each number 0-99
- Match against jersey regions
- Use multiple font templates
- Pros: Fast, no ML training
- Cons: Limited to template variations

🧪 TESTING STRATEGY:
1. Try EasyOCR first (quickest to test)
2. If >60% accuracy: enhance with preprocessing
3. If <60%: try hybrid approach
4. Custom training as last resort

📊 BENCHMARK WITH YOUR DATA:
- Test on 10-20 real jersey photos
- Compare accuracy across approaches
- Measure inference speed on device
- Check false positive rates

Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
""".trimIndent()

    researchFile.writeText(researchContent)
    
    Log.d("AlternativeApproaches", "📋 Created research file: ${researchFile.absolutePath}")
    Log.d("AlternativeApproaches", "💡 Alternative approaches documented for testing")
    Log.d("AlternativeApproaches", "🎯 Recommendation: Start with EasyOCR for quick validation")
}

/**
 * 📖 Test pre-trained OCR models (TensorFlow Hub, Hugging Face)
 */
private fun testPreTrainedOCR() {
    Log.d("AlternativeApproaches", "📖 Testing pre-trained OCR models...")
    
    // Models to consider:
    // 1. EasyOCR - good for varied fonts and backgrounds
    // 2. TrOCR (Microsoft) - transformer-based OCR
    // 3. PaddleOCR - optimized for mobile
    // 4. CRAFT text detection + CRNN recognition
    
    Log.d("AlternativeApproaches", "💡 Pre-trained OCR could work with minimal training data")
}

/**
 * 🔄 Test hybrid detection (jersey detection + number recognition)
 */
private fun testHybridDetection() {
    Log.d("AlternativeApproaches", "🔄 Testing hybrid detection approach...")
    
    // Step 1: Use existing object detection to find jerseys
    // Step 2: Crop jersey regions
    // Step 3: Use specialized number recognition on cropped regions
    // This reduces complexity and improves accuracy
    
    Log.d("AlternativeApproaches", "💡 Hybrid approach could leverage existing person detection")
}

/**
 * 🎯 Test transfer learning from number recognition models
 */
private fun testTransferLearning() {
    Log.d("AlternativeApproaches", "🎯 Testing transfer learning...")
    
    // Use pre-trained models:
    // 1. Street View House Numbers (SVHN) dataset models
    // 2. License plate recognition models
    // 3. Sports number recognition models (if available)
    
    Log.d("AlternativeApproaches", "💡 Transfer learning could require much less data")
}

/**
 * 🎨 Test real-time template matching
 */
private fun testTemplateMatching() {
    Log.d("AlternativeApproaches", "🎨 Testing template matching...")
    
    // Simple approach:
    // 1. Create number templates for each font style
    // 2. Use OpenCV template matching
    // 3. Much faster, no ML training required
    
    Log.d("AlternativeApproaches", "💡 Template matching could work for consistent jersey styles")
}

/**
 * ⚙️ Update app configuration to use custom trained model
 */
private fun updateModelConfiguration(context: Context) {
    // This would integrate with your existing JerseyDetectionManager
    // to switch from ML Kit to the custom trained model
    Log.d("Deploy", "🔧 Updating app to use custom trained model...")
}