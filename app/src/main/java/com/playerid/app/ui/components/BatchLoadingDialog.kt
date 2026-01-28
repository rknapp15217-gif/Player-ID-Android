package com.playerid.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.playerid.app.ml.BundledJerseyPhotos
import com.playerid.app.ml.ImportedJerseyData
import com.playerid.app.ml.SampleJerseyGenerator
import kotlinx.coroutines.launch

/**
 * 📦 Batch Loading Dialog for Bundled Jersey Photos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchLoadingDialog(
    onDismiss: () -> Unit,
    onPhotosLoaded: (List<ImportedJerseyData>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var loadingText by remember { mutableStateOf("") }
    
    val bundledPhotos = remember { BundledJerseyPhotos(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📦 Load Training Dataset")
        },
        text = {
            Column {
                Text(
                    text = "Choose dataset size for jersey validation:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (isLoading) {
                    Column {
                        LinearProgressIndicator(
                            progress = loadingProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Start - 25 photos
                        Button(
                            onClick = {
                                scope.launch {
                                    loadPhotoBatch(
                                        BundledJerseyPhotos.BatchSize.SMALL,
                                        context,
                                        bundledPhotos,
                                        onProgressUpdate = { progress, text ->
                                            loadingProgress = progress
                                            loadingText = text
                                            isLoading = progress < 1f
                                        },
                                        onComplete = onPhotosLoaded
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Quick Start (25 photos)")
                                Text(
                                    "Strategic samples - good for testing",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        
                        // Medium Training - 50 photos
                        Button(
                            onClick = {
                                scope.launch {
                                    loadPhotoBatch(
                                        BundledJerseyPhotos.BatchSize.MEDIUM,
                                        context,
                                        bundledPhotos,
                                        onProgressUpdate = { progress, text ->
                                            loadingProgress = progress
                                            loadingText = text
                                            isLoading = progress < 1f
                                        },
                                        onComplete = onPhotosLoaded
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.School, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Training Set (250 photos)")
                                Text(
                                    "Balanced coverage - recommended for ML training",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        
                        // Full Dataset - 1000+ photos
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    loadPhotoBatch(
                                        BundledJerseyPhotos.BatchSize.FULL,
                                        context,
                                        bundledPhotos,
                                        onProgressUpdate = { progress, text ->
                                            loadingProgress = progress
                                            loadingText = text
                                            isLoading = progress < 1f
                                        },
                                        onComplete = onPhotosLoaded
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Dataset, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Full Dataset (1000+ photos)")
                                Text(
                                    "Complete coverage - maximum training data",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * 📥 Load photo batch with progress tracking
 */
private suspend fun loadPhotoBatch(
    batchSize: BundledJerseyPhotos.BatchSize,
    context: android.content.Context,
    bundledPhotos: BundledJerseyPhotos,
    onProgressUpdate: (Float, String) -> Unit,
    onComplete: (List<ImportedJerseyData>) -> Unit
) {
    onProgressUpdate(0f, "Preparing dataset...")
    
    val bundledPhotoList = bundledPhotos.getPhotosBatch(batchSize)
    val loadedPhotos = mutableListOf<ImportedJerseyData>()
    val generator = SampleJerseyGenerator(context)
    
    val jerseyColors = mapOf(
        "soccer" to android.graphics.Color.rgb(0, 100, 200),
        "basketball" to android.graphics.Color.rgb(85, 37, 130), 
        "football" to android.graphics.Color.rgb(0, 34, 68)
    )
    
    bundledPhotoList.forEachIndexed { index, bundledPhoto ->
        val progress = (index + 1).toFloat() / bundledPhotoList.size
        onProgressUpdate(progress, "Generating jersey ${index + 1}/${bundledPhotoList.size}")
        
        bundledPhoto.expectedNumber?.let { number ->
            val color = jerseyColors[bundledPhoto.sport] ?: android.graphics.Color.BLUE
            
            // Generate multiple variations for comprehensive training
            val variations = when(batchSize) {
                BundledJerseyPhotos.BatchSize.SMALL -> listOf("normal")
                BundledJerseyPhotos.BatchSize.MEDIUM -> listOf("normal", "angled") 
                BundledJerseyPhotos.BatchSize.LARGE -> listOf("normal", "angled", "dark")
                BundledJerseyPhotos.BatchSize.FULL -> listOf("normal", "angled", "dark", "blurry")
            }
            
            variations.forEach { variation ->
                val bitmap = when(variation) {
                    "normal" -> generator.generateJerseyImage(number, color, android.graphics.Color.WHITE, bundledPhoto.sport)
                    "angled" -> generator.generateJerseyVariation(number, "angled", bundledPhoto.sport)
                    "dark" -> generator.generateJerseyVariation(number, "dark", bundledPhoto.sport)
                    "blurry" -> generator.generateJerseyVariation(number, "blurry", bundledPhoto.sport)
                    else -> generator.generateJerseyImage(number, color, android.graphics.Color.WHITE, bundledPhoto.sport)
                }
                
                loadedPhotos.add(
                    ImportedJerseyData(
                        originalUrl = "bundled://${bundledPhoto.filename}_$variation",
                        bitmap = bitmap,
                        suggestedNumber = bundledPhoto.expectedNumber,
                        teamName = bundledPhoto.team,
                        sport = bundledPhoto.sport,
                        downloadTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    onProgressUpdate(1f, "Loaded ${loadedPhotos.size} photos!")
    onComplete(loadedPhotos)
}