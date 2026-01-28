package com.playerid.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.subscription.SubscriptionStatus
import com.playerid.app.subscription.SubscriptionViewModel

@Composable
fun PaywallScreen(
    subscriptionViewModel: SubscriptionViewModel,
    onDismiss: () -> Unit
) {
    val subscriptionStatus by subscriptionViewModel.subscriptionStatus.collectAsState()
    val trialDaysRemaining by subscriptionViewModel.trialDaysRemaining.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spotr",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Hero section
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = "Spotr",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (subscriptionStatus == SubscriptionStatus.EXPIRED) {
                "Your free trial has ended"
            } else {
                "Unlock Full Spotr Experience"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (subscriptionStatus == SubscriptionStatus.FREE_TRIAL) {
                subscriptionViewModel.getTrialMessage()
            } else {
                "Continue recording and sharing your child's best moments"
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Features list
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Premium Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val features = listOf(
                    "🎥 Unlimited video recording" to "Record as many highlights as you want",
                    "🏷️ Custom celebration tags" to "Add personalized overlays to videos", 
                    "📤 Easy sharing via text/email" to "Share directly from the app",
                    "🔍 Advanced player tracking" to "Better AR detection and accuracy",
                    "☁️ Cloud backup coming soon" to "Never lose your precious memories",
                    "🤖 AI highlights coming soon" to "Automatically detect great plays"
                )
                
                features.forEach { (title, description) ->
                    FeatureRow(title = title, description = description)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Referral Incentive
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E8)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF4CAF50)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "💡 Want Premium for FREE?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                
                Text(
                    text = subscriptionViewModel.getReferralStatus(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { 
                        // Navigate to referral screen
                        // This would need to be passed as a parameter
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Referring Friends")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pricing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subscriptionViewModel.getSubscriptionPrice(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Less than $1 per month!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Cancel anytime • No commitment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Subscribe button
        Button(
            onClick = { subscriptionViewModel.startSubscription() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (subscriptionStatus == SubscriptionStatus.FREE_TRIAL) {
                    "Continue with Premium"
                } else {
                    "Start Premium Subscription"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Restore purchases
        TextButton(
            onClick = { subscriptionViewModel.restorePurchases() }
        ) {
            Text("Restore Previous Purchase")
        }
        
        if (subscriptionStatus == SubscriptionStatus.FREE_TRIAL && trialDaysRemaining > 0) {
            TextButton(onClick = onDismiss) {
                Text("Continue Free Trial ($trialDaysRemaining days left)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Fine print
        Text(
            text = "Your subscription will automatically renew unless cancelled at least 24 hours before the end of the current period.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeatureRow(title: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(120.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SubscriptionBanner(
    subscriptionViewModel: SubscriptionViewModel,
    onUpgrade: () -> Unit
) {
    val subscriptionStatus by subscriptionViewModel.subscriptionStatus.collectAsState()
    val trialDaysRemaining by subscriptionViewModel.trialDaysRemaining.collectAsState()
    
    if (subscriptionStatus == SubscriptionStatus.FREE_TRIAL && trialDaysRemaining <= 7) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (trialDaysRemaining <= 3) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Trial",
                    tint = if (trialDaysRemaining <= 3) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscriptionViewModel.getTrialMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Upgrade to continue recording highlights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Upgrade")
                }
            }
        }
    }
}