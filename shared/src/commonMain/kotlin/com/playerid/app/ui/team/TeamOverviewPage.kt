package com.playerid.app.ui.team

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.PlayerProfile

enum class TeamOverviewDestination {
    Roster,
    Schedule,
    Invite,
    Settings
}

@Composable
fun TeamOverviewPage(
    teamName: String,
    seasonLabel: String,
    homeColor: Color,
    assignedKid: String,
    assignedPlayer: PlayerProfile?,
    playerCount: Int,
    gameCount: Int,
    onBack: () -> Unit,
    onAssignedKidClick: () -> Unit,
    onRoster: () -> Unit,
    onImportRoster: () -> Unit,
    onSchedule: () -> Unit,
    onImportSchedule: () -> Unit,
    onInvite: () -> Unit,
    onSettings: () -> Unit,
    onLeave: () -> Unit,
    backIcon: @Composable () -> Unit,
    kidPicker: @Composable () -> Unit,
    assignedPlayerTrailingIcon: @Composable () -> Unit,
    destinationIcon: @Composable (TeamOverviewDestination) -> Unit,
    destinationTrailingIcon: @Composable (TeamOverviewDestination) -> Unit,
    leaveIcon: @Composable () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TeamOverviewHeader(
                onBack = onBack,
                backIcon = backIcon
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(homeColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamInitials(teamName),
                        color = homeColor.contrastingTextColor(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(teamName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (seasonLabel.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(seasonLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { TeamOverviewSectionLabel("MY PLAYER") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAssignedKidClick),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamOverviewAvatar(
                        number = assignedPlayer?.number ?: assignedKid.take(1),
                        color = homeColor
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            assignedPlayer?.name ?: assignedKid,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (assignedPlayer == null) {
                                "Assigned to this team"
                            } else {
                                "#${assignedPlayer.number}  ${assignedPlayer.position}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    assignedPlayerTrailingIcon()
                    kidPicker()
                }
            }
        }
        item { TeamOverviewSectionLabel("TEAM") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                TeamOverviewDestinationRow(
                    destination = TeamOverviewDestination.Roster,
                    title = "Roster",
                    subtitle = "$playerCount players",
                    onClick = onRoster,
                    destinationIcon = destinationIcon,
                    trailingIcon = destinationTrailingIcon
                )
                TextButton(onClick = onImportRoster, modifier = Modifier.padding(start = 52.dp)) {
                    Text("Import roster", color = TeamOverviewActionBlue)
                }
                Divider()
                TeamOverviewDestinationRow(
                    destination = TeamOverviewDestination.Schedule,
                    title = "Schedule",
                    subtitle = "$gameCount games",
                    onClick = onSchedule,
                    destinationIcon = destinationIcon,
                    trailingIcon = destinationTrailingIcon
                )
                TextButton(onClick = onImportSchedule, modifier = Modifier.padding(start = 52.dp)) {
                    Text("Import schedule", color = TeamOverviewActionBlue)
                }
            }
        }
        item { TeamOverviewSectionLabel("SHARING") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                TeamOverviewDestinationRow(
                    destination = TeamOverviewDestination.Invite,
                    title = "Invite parents & coaches",
                    subtitle = "Give others access to this team",
                    onClick = onInvite,
                    destinationIcon = destinationIcon,
                    trailingIcon = destinationTrailingIcon
                )
            }
        }
        item { TeamOverviewSectionLabel("MANAGEMENT") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                TeamOverviewDestinationRow(
                    destination = TeamOverviewDestination.Settings,
                    title = "Team settings",
                    subtitle = "Team name, colors and notifications",
                    onClick = onSettings,
                    destinationIcon = destinationIcon,
                    trailingIcon = destinationTrailingIcon
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onLeave),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    leaveIcon()
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Leave Team", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "You will no longer have access to the shared roster, schedule and team content.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamOverviewHeader(
    onBack: () -> Unit,
    backIcon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            backIcon()
            Spacer(Modifier.width(4.dp))
            Text("Teams")
        }
        Text(
            "Teams",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun TeamOverviewSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun TeamOverviewDestinationRow(
    destination: TeamOverviewDestination,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destinationIcon: @Composable (TeamOverviewDestination) -> Unit,
    trailingIcon: @Composable (TeamOverviewDestination) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            destinationIcon(destination)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingIcon(destination)
    }
}

@Composable
private fun TeamOverviewAvatar(number: String, color: Color) {
    Box(
        modifier = Modifier.size(48.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, fontWeight = FontWeight.Bold, color = color.contrastingTextColor())
    }
}

private fun teamInitials(teamName: String): String {
    return teamName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
}

private fun Color.contrastingTextColor(): Color {
    return if (luminance() > 0.5f) Color.Black else Color.White
}

private val TeamOverviewActionBlue = Color(0xFF0A66FF)