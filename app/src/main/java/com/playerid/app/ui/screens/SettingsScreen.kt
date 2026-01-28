package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    teamViewModel: com.playerid.app.viewmodels.TeamViewModel? = null,
    playerViewModel: com.playerid.app.viewmodels.PlayerViewModel? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "⚙️ Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Camera Settings
        SettingsSection(title = "Camera & Detection") {
            SettingsItem(
                title = "Detection Sensitivity",
                subtitle = "Adjust text detection sensitivity",
                icon = Icons.Default.Tune,
                onClick = { /* TODO */ }
            )
            
            SettingsItem(
                title = "Camera Quality",
                subtitle = "High quality for better detection",
                icon = Icons.Default.HighQuality,
                onClick = { /* TODO */ }
            )
            
            var showDebugInfo by remember { mutableStateOf(false) }
            SettingsToggleItem(
                title = "Show Debug Info",
                subtitle = "Display detection confidence and timing",
                icon = Icons.Default.BugReport,
                checked = showDebugInfo,
                onCheckedChange = { showDebugInfo = it }
            )
        }
        
        // AR Settings
        SettingsSection(title = "Augmented Reality") {
            SettingsItem(
                title = "Bubble Style",
                subtitle = "Customize player name bubbles",
                icon = Icons.Default.Style,
                onClick = { /* TODO */ }
            )
            
            SettingsItem(
                title = "Text Size",
                subtitle = "Adjust AR text size",
                icon = Icons.Default.TextFields,
                onClick = { /* TODO */ }
            )
            
            var persistentBubbles by remember { mutableStateOf(true) }
            SettingsToggleItem(
                title = "Persistent Bubbles",
                subtitle = "Keep bubbles visible when not detected",
                icon = Icons.Default.PushPin,
                checked = persistentBubbles,
                onCheckedChange = { persistentBubbles = it }
            )
        }
        
        // Team Settings
        SettingsSection(title = "Team Management") {
            SettingsItem(
                title = "Auto Team Learning",
                subtitle = "Automatically learn team colors",
                icon = Icons.Default.AutoMode,
                onClick = { /* TODO */ }
            )
            
            SettingsItem(
                title = "Collaboration Settings",
                subtitle = "Manage team sharing permissions",
                icon = Icons.Default.Share,
                onClick = { /* TODO */ }
            )
            
            SettingsItem(
                title = "Clean Up Inactive Teams",
                subtitle = "Archive old teams with no activity (6+ months)",
                icon = Icons.Default.CleaningServices,
                onClick = { 
                    // TODO: Implement team archival functionality
                    teamViewModel?.let {
                        // For now, show a placeholder message
                        // In future: it.archiveInactiveTeams()
                    }
                }
            )
        }
        
        // Data Settings
        SettingsSection(title = "Data & Privacy") {
            SettingsItem(
                title = "Export Database",
                subtitle = "Export player data to JSON/CSV",
                icon = Icons.Default.Download,
                onClick = { 
                    playerViewModel?.exportDatabase()
                }
            )
            
            SettingsItem(
                title = "Import Database",
                subtitle = "Import player data from file",
                icon = Icons.Default.Upload,
                onClick = { 
                    playerViewModel?.importDatabase()
                }
            )
            
            SettingsItem(
                title = "Clear Cache",
                subtitle = "Clear app cache and temporary data",
                icon = Icons.Default.Delete,
                onClick = { 
                    playerViewModel?.clearCache()
                }
            )
        }
        
        // App Info
        SettingsSection(title = "About") {
            SettingsItem(
                title = "App Version",
                subtitle = "PlayerID v1.0",
                icon = Icons.Default.Info,
                onClick = { /* TODO */ }
            )
            
            SettingsItem(
                title = "Privacy Policy",
                subtitle = "How we handle your data",
                icon = Icons.Default.PrivacyTip,
                onClick = { /* TODO */ }
            )
            
            SettingsItem(
                title = "Open Source Licenses",
                subtitle = "Third-party software licenses",
                icon = Icons.Default.Code,
                onClick = { /* TODO */ }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Credits
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📱 Built with Android",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Using ARCore, ML Kit, CameraX, and Jetpack Compose",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                content()
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}