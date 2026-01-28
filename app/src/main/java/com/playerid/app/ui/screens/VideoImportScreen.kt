package com.playerid.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.data.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoImportScreen(
    onNavigateBack: () -> Unit,
    onVideoSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var processingStatus by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            selectedVideo = it
            processingStatus = "Video selected: ${uri.lastPathSegment}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Import Video",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Video Selection Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF1976D2)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Select Game Video",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "Choose a video from your gallery to add player name bubbles",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        )
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Video File")
                    }
                }
            }

            // Processing Status
            if (processingStatus.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProcessing) Color(0xFFFFF3E0) else Color(0xFFE8F5E8)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFFF9800)
                            )
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            processingStatus,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Feature Options
            if (selectedVideo != null) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Processing Options",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(
                        listOf(
                            ProcessingOption(
                                title = "Auto-Detect Players",
                                description = "Automatically detect jersey numbers and add name bubbles",
                                icon = Icons.Default.AutoAwesome,
                                action = "auto_detect"
                            ),
                            ProcessingOption(
                                title = "Manual Placement",
                                description = "Manually place name bubbles on specific players",
                                icon = Icons.Default.TouchApp,
                                action = "manual_placement"
                            ),
                            ProcessingOption(
                                title = "Hybrid Mode",
                                description = "Auto-detect first, then manually adjust positions",
                                icon = Icons.Default.Tune,
                                action = "hybrid"
                            )
                        )
                    ) { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF1976D2)
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        option.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        option.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        isProcessing = true
                                        processingStatus = "Processing video with ${option.title.lowercase()}..."
                                        
                                        // Navigate to video editor with selected mode
                                        selectedVideo?.let { uri ->
                                            onVideoSelected(uri)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1976D2)
                                    )
                                ) {
                                    Text("Start", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ProcessingOption(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: String
)