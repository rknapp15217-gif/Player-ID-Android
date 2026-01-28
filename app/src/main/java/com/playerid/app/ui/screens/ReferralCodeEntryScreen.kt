package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.referral.ReferralManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralCodeEntryScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val referralManager = remember { ReferralManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var referralCode by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val validateAndApplyReferralCode = { 
        if (referralCode.length != 6) {
            showError = true
            errorMessage = "Referral code must be 6 characters"
        } else {
            isValidating = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(1000) // Simulate network call
                val isValid = referralManager.processReferralSignup(referralCode)
                
                isValidating = false
                if (isValid) {
                    showSuccess = true
                } else {
                    showError = true
                    errorMessage = "Invalid referral code or cannot refer yourself"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Spotr!") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Hero section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Got a Referral Code?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        "Enter your friend's referral code to unlock special bonuses for both of you!",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Referral code input
            OutlinedTextField(
                value = referralCode,
                onValueChange = { 
                    referralCode = it.uppercase()
                    showError = false
                },
                label = { Text("Referral Code") },
                placeholder = { Text("Enter 6-character code") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (referralCode.isNotEmpty()) {
                            validateAndApplyReferralCode()
                        }
                    }
                ),
                isError = showError,
                supportingText = if (showError) {
                    { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                } else null,
                trailingIcon = if (referralCode.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { referralCode = "" }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                } else null
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    enabled = !isValidating
                ) {
                    Text("Skip for Now")
                }
                
                Button(
                    onClick = {
                        if (referralCode.isNotEmpty()) {
                            validateAndApplyReferralCode()
                        } else {
                            onContinue()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isValidating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text(if (referralCode.isEmpty()) "Continue" else "Apply Code")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Benefits section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "Referral Benefits",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val benefits = listOf(
                        "🎁 Both you and your friend get bonus features",
                        "⭐ Extended trial period", 
                        "🏆 Help them earn their free year faster",
                        "🚀 Access to exclusive referral rewards"
                    )
                    
                    benefits.forEach { benefit ->
                        Text(
                            benefit,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer note
            Text(
                "Don't have a code? No worries! You can always add one later in Settings.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Success dialog
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { 
                showSuccess = false
                onContinue()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Code Applied!")
                }
            },
            text = {
                Text(
                    "🎉 Great! Your referral code has been applied. You and your friend will both receive special bonuses!",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showSuccess = false
                        onContinue()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Continue")
                }
            }
        )
    }
}

// Extension to add referral code entry to existing onboarding flow
@Composable
fun OnboardingWithReferral(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    when (currentStep) {
        0 -> ReferralCodeEntryScreen(
            onContinue = { currentStep = 1 },
            onSkip = { currentStep = 1 }
        )
        1 -> {
            // Continue with existing onboarding flow
            // This would connect to your existing welcome/tutorial screens
            LaunchedEffect(Unit) {
                onComplete()
            }
        }
    }
}