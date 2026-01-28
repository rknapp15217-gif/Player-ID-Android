package com.playerid.app.roster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

@Composable
fun RosterSharingScreen(
    teamName: String,
    rosterId: String,
    onBack: () -> Unit,
    onInviteSent: () -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    
    LaunchedEffect(rosterId) {
        isGenerating = true
        qrBitmap = generateQRCode(rosterId)
        isGenerating = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Invite Team",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Team info card
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
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Team",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Invite other parents to join your team roster",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // QR Code section
        Text(
            text = "Scan QR Code to Join",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.size(250.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    CircularProgressIndicator()
                } else if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                } else {
                    Text(
                        text = "Failed to generate QR code",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How to join:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val steps = listOf(
                    "Download Spotr app from App Store/Play Store",
                    "Open the app and tap 'Join Team'",
                    "Scan this QR code with your camera",
                    "Add your child to the roster"
                )
                
                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Share buttons
        Button(
            onClick = {
                shareRosterInvite(context, teamName, rosterId)
                onInviteSent()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share Team Invite")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = {
                shareQRCode(context, qrBitmap, teamName)
                onInviteSent()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = qrBitmap != null
        ) {
            Icon(Icons.Default.QrCode, contentDescription = "Share QR")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share QR Code")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Privacy note
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Privacy",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Privacy First",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Parents can opt out or remove their child anytime. All data stays private to your team.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private suspend fun generateQRCode(data: String): Bitmap? = withContext(Dispatchers.IO) {
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
        e.printStackTrace()
        null
    }
}

private fun shareRosterInvite(context: Context, teamName: String, rosterId: String) {
    val shareText = """
        🏃‍♂️ Join our $teamName roster on Spotr!
        
        Never miss your child's best moments with AR player tracking and instant highlights.
        
        Team Code: $rosterId
        
        Download Spotr:
        📱 iOS: [App Store Link]
        📱 Android: [Play Store Link]
        
        Or scan the QR code in the app!
    """.trimIndent()
    
    val shareIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Join our $teamName on Spotr!")
    }
    
    context.startActivity(android.content.Intent.createChooser(shareIntent, "Invite Team"))
}

private fun shareQRCode(context: Context, bitmap: Bitmap?, teamName: String) {
    if (bitmap == null) return
    
    // In production, save bitmap to cache and share file URI
    val shareText = "Scan this QR code to join our $teamName roster on Spotr!"
    
    val shareIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
    }
    
    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share QR Code"))
}