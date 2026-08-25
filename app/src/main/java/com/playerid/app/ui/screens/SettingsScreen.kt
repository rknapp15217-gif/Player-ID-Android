package com.playerid.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playerid.app.R
import com.playerid.app.ui.theme.SpotrPrimaryBlue
import com.playerid.app.ui.theme.SpotrSurfaceAlpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    teamViewModel: com.playerid.app.viewmodels.TeamViewModel? = null,
    playerViewModel: com.playerid.app.viewmodels.PlayerViewModel? = null,
    onNavigateToReferral: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    BackHandler(enabled = true) {
        onNavigateBack()
    }
    val selectedTeamName by (teamViewModel?.selectedTeam?.collectAsState() ?: remember { mutableStateOf(null) })
    val subscribedTeams by (teamViewModel?.subscribedTeams?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val selectedTeam = remember(subscribedTeams, selectedTeamName) {
        subscribedTeams.firstOrNull { it.name == selectedTeamName }
    }
    val teamPrimary = parseSettingsColor(selectedTeam?.color, SpotrPrimaryBlue)
    val teamSecondary = parseSettingsColor(selectedTeam?.awayColor, Color(0xFFE3F2FD))
    val onTeamPrimary = if (teamPrimary.luminance() > 0.55f) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 760.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        teamSecondary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = teamPrimary)
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = teamPrimary
            )
        }
        
        // Camera Settings
        SettingsSection(
            title = "Camera & Detection",
            teamPrimary = teamPrimary,
            teamSecondary = teamSecondary
        ) {
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
                onCheckedChange = { showDebugInfo = it },
                teamPrimary = teamPrimary
            )
        }
        
        // AR Settings
        SettingsSection(
            title = "Augmented Reality",
            teamPrimary = teamPrimary,
            teamSecondary = teamSecondary
        ) {
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
                onCheckedChange = { persistentBubbles = it },
                teamPrimary = teamPrimary
            )
        }
        
        // Team Settings
        SettingsSection(
            title = "Team Management",
            teamPrimary = teamPrimary,
            teamSecondary = teamSecondary
        ) {
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
        SettingsSection(
            title = "Data & Privacy",
            teamPrimary = teamPrimary,
            teamSecondary = teamSecondary
        ) {
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
        SettingsSection(
            title = "About",
            teamPrimary = teamPrimary,
            teamSecondary = teamSecondary
        ) {
            SettingsItem(
                title = "Refer a Friend",
                subtitle = "Invite others to try PlayerID",
                icon = Icons.Default.PersonAdd,
                onClick = onNavigateToReferral
            )

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
                containerColor = teamSecondary.copy(alpha = 0.24f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_built_with_android),
                    style = MaterialTheme.typography.titleMedium,
                    color = teamPrimary
                )
                Text(
                    text = stringResource(R.string.settings_stack_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = onTeamPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    teamPrimary: Color,
    teamSecondary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = teamPrimary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = teamSecondary.copy(alpha = SpotrSurfaceAlpha)
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
    onClick: () -> Unit,
    teamPrimary: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = teamPrimary
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
            contentDescription = stringResource(R.string.open),
            tint = teamPrimary.copy(alpha = 0.75f)
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    teamPrimary: Color = MaterialTheme.colorScheme.primary
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
            tint = teamPrimary
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
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = teamPrimary,
                checkedTrackColor = teamPrimary.copy(alpha = 0.45f)
            )
        )
    }
}

private fun parseSettingsColor(raw: String?, fallback: Color): Color {
    if (raw.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(raw))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}