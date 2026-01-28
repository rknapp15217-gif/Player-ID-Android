package com.playerid.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.playerid.app.referral.ReferralManager
import com.playerid.app.referral.ReferralData
import com.playerid.app.referral.ReferralProgress
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val referralManager = remember { ReferralManager(context) }
    
    val referralData by referralManager.referralData.collectAsState()
    val referralProgress by referralManager.referralProgress.collectAsState()
    
    var showQRCode by remember { mutableStateOf(false) }
    var showRewardClaim by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Invite Friends",
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
                    containerColor = ComposeColor(0xFF1976D2),
                    titleContentColor = ComposeColor.White,
                    navigationIconContentColor = ComposeColor.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Hero Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ComposeColor(0xFF1976D2)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = ComposeColor.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Get a FREE YEAR!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            "Invite 5 friends and unlock a full year of Spotr Premium - absolutely free!",
                            fontSize = 16.sp,
                            color = ComposeColor.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Progress Section
            item {
                ReferralProgressCard(
                    progress = referralProgress,
                    onClaimReward = {
                        if (referralManager.claimFreeYear()) {
                            showRewardClaim = true
                        }
                    }
                )
            }
            
            // Your Referral Code Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ComposeColor(0xFFF8F9FA)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your Referral Code",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = ComposeColor.White
                            )
                        ) {
                            Text(
                                referralData.userReferralCode,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = ComposeColor(0xFF1976D2),
                                modifier = Modifier.padding(20.dp),
                                letterSpacing = 4.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showQRCode = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("QR Code")
                            }
                            
                            Button(
                                onClick = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, referralManager.getReferralShareMessage())
                                        putExtra(Intent.EXTRA_SUBJECT, "Join me on Spotr!")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Referral"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ComposeColor(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
            
            // Rewards Timeline
            item {
                RewardsTimelineCard(
                    currentReferrals = referralData.totalReferrals,
                    nextMilestone = referralManager.getNextMilestoneReward()
                )
            }
            
            // How It Works Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ComposeColor(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "How It Works",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val steps = listOf(
                            "Share your referral code with friends & family",
                            "They sign up using your code",
                            "They subscribe to Spotr Premium",
                            "You get closer to your free year!",
                            "At 5 referrals, claim your FREE YEAR! 🎉"
                        )
                        
                        steps.forEachIndexed { index, step ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(ComposeColor(0xFF4CAF50), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        color = ComposeColor.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    step,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // QR Code Dialog
    if (showQRCode) {
        AlertDialog(
            onDismissRequest = { showQRCode = false },
            title = { Text("Your Referral QR Code") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QRCodeImage(
                        data = referralManager.getReferralLink(),
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Friends can scan this to join with your code!",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQRCode = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Reward Claim Dialog
    if (showRewardClaim) {
        AlertDialog(
            onDismissRequest = { showRewardClaim = false },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Celebration,
                        contentDescription = null,
                        tint = ComposeColor(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Congratulations!") 
                }
            },
            text = {
                Text(
                    "🎉 You've earned a FREE YEAR of Spotr Premium! Your subscription has been updated.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRewardClaim = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF4CAF50)
                    )
                ) {
                    Text("Awesome!")
                }
            }
        )
    }
}

@Composable
fun ReferralProgressCard(
    progress: ReferralProgress,
    onClaimReward: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Progress to Free Year",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    "${progress.currentReferrals}/${progress.targetReferrals}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor(0xFF1976D2)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = progress.progressPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ComposeColor(0xFF4CAF50),
                trackColor = ComposeColor(0xFFE0E0E0)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (progress.canClaimReward) {
                Button(
                    onClick = onClaimReward,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Claim Your FREE YEAR! 🎉")
                }
            } else {
                val remaining = progress.targetReferrals - progress.currentReferrals
                Text(
                    "Invite $remaining more friend${if (remaining == 1) "" else "s"} to earn your free year!",
                    fontSize = 14.sp,
                    color = ComposeColor.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun RewardsTimelineCard(
    currentReferrals: Int,
    nextMilestone: com.playerid.app.referral.MilestoneReward?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor(0xFFFFF8E1)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Reward Milestones",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val milestones = listOf(
                "Free Year Subscription" to 5,
                "Exclusive Spotr Merchandise" to 10,
                "Premium Feature Early Access" to 20,
                "Spotr Ambassador Status" to 50
            )
            
            milestones.forEach { (reward, required) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCompleted = currentReferrals >= required
                    val isCurrent = nextMilestone?.requiredReferrals == required
                    
                    Icon(
                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCompleted) ComposeColor(0xFF4CAF50) else 
                               if (isCurrent) ComposeColor(0xFF1976D2) else ComposeColor.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            reward,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCompleted) ComposeColor(0xFF4CAF50) else ComposeColor.Black
                        )
                        Text(
                            "$required referrals",
                            fontSize = 12.sp,
                            color = ComposeColor.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QRCodeImage(
    data: String,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(data) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    qrBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = modifier
        )
    }
}