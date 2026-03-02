package com.playerid.app.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as Media3Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

/**
 * Post-Recording Moment Capture Screen
 * 
 * Celebratory screen for tagging and saving youth sports moments.
 * This is NOT file management - it's memory capture.
 */
@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun PostRecordingScreen(
    videoUri: Uri,
    onEdit: () -> Unit,
    onSaveToLibrary: (selectedTag: MomentTag?) -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var selectedTag by remember { mutableStateOf<MomentTag?>(null) }
    var showCelebration by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            repeatMode = Media3Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // Celebration animation when tag is selected
    LaunchedEffect(selectedTag) {
        if (selectedTag != null) {
            showCelebration = true
            delay(1500)
            showCelebration = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Video Preview with Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            keepScreenOn = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay preview when tag is selected
                selectedTag?.let { tag ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .background(
                                Color.Black.copy(alpha = 0.8f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                tag.emoji,
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                tag.displayName.uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }

            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.8f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Celebration Header
                    Text(
                        "Moment Captured 🎉",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // What Happened Section
                    Text(
                        "What happened?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Moment Tag Buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MomentTagButton(
                                tag = MomentTag.GOAL,
                                isSelected = selectedTag == MomentTag.GOAL,
                                onClick = {
                                    selectedTag = MomentTag.GOAL
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MomentTagButton(
                                tag = MomentTag.ASSIST,
                                isSelected = selectedTag == MomentTag.ASSIST,
                                onClick = {
                                    selectedTag = MomentTag.ASSIST
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MomentTagButton(
                                tag = MomentTag.SAVE,
                                isSelected = selectedTag == MomentTag.SAVE,
                                onClick = {
                                    selectedTag = MomentTag.SAVE
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MomentTagButton(
                                tag = MomentTag.BIG_PLAY,
                                isSelected = selectedTag == MomentTag.BIG_PLAY,
                                onClick = {
                                    selectedTag = MomentTag.BIG_PLAY
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Button
                    Button(
                        onClick = { onSaveToLibrary(selectedTag) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTag != null) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (selectedTag != null) "Add to Season Highlights" else "Save Without Tag",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Secondary Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDelete
                        ) {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                        }
                        
                        if (selectedTag != null) {
                            TextButton(
                                onClick = { selectedTag = null }
                            ) {
                                Text(
                                    "Clear Tag",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Celebration Confetti Effect
        if (showCelebration) {
            ConfettiEffect()
        }
    }
}

@Composable
fun MomentTagButton(
    tag: MomentTag,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tagButtonScale"
    )
    
    Card(
        modifier = modifier
            .height(80.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelected) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    tag.emoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    tag.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ConfettiEffect() {
    // Simple confetti celebration animation
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "✨",
            fontSize = 120.sp,
            modifier = Modifier.offset(y = (-100).dp)
        )
    }
}

enum class MomentTag(
    val displayName: String,
    val emoji: String,
    val overlayText: String
) {
    GOAL("Goal", "⚽", "GOAL"),
    ASSIST("Assist", "🎯", "ASSIST"),
    SAVE("Save", "🧤", "SAVE"),
    BIG_PLAY("Big Play", "⭐", "BIG PLAY")
}
