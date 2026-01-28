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
                Text("Progress: $validatedCount/100 for this number")
                Text("Total Validated: $totalValidated samples")
                
                LinearProgressIndicator(
                    progress = if (validatedCount > 0) validatedCount / 100f else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
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
                            val batch = generateSequentialBatch(currentNumber, batchSize = 100)
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
                                    val success = datasetCollector.captureTrainingSample(
                                        image = currentPhoto.bitmap ?: return@launch,
                                        jerseyNumber = currentNumber,
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
                                            // Move to next number
                                            currentNumber++
                                            currentBatch = emptyList()
                                            validatedCount = 0
                                            currentIndex = 0
                                            
                                            if (currentNumber > 99) {
                                                currentNumber = 99 // Stop at 99
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("✅ Correct")
                        }
                        
                        Button(
                            onClick = {
                                // Skip this sample
                                currentIndex++
                                
                                // Check if completed this number
                                if (currentIndex >= currentBatch.size) {
                                    // Move to next number
                                    currentNumber++
                                    currentBatch = emptyList()
                                    validatedCount = 0
                                    currentIndex = 0
                                    
                                    if (currentNumber > 99) {
                                        currentNumber = 99 // Stop at 99
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("❌ Skip")
                        }
                    }
                }
            }
        }
        
        // Completion message
        if (currentNumber >= 99 && currentIndex >= currentBatch.size) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🎉 Validation Complete! Ready to train ML model.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }