package com.playerid.app.ui.team

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.TeamSelectionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSelectionPage(
    teams: List<TeamSelectionItem>,
    teamSnapImportAvailable: Boolean,
    onSelectTeam: (String) -> Unit,
    onJoinTeam: () -> Unit,
    onCreateTeam: () -> Unit,
    onImportTeamSnap: () -> Unit,
    joinIcon: @Composable () -> Unit,
    createIcon: @Composable () -> Unit,
    selectIcon: @Composable () -> Unit,
    emptyIcon: @Composable () -> Unit,
    importIcon: @Composable () -> Unit,
    joinTeamLabel: String = "Join Team",
    createTeamLabel: String = "Create Team",
    createNewTeamLabel: String = "Create New Team",
    importTeamSnapLabel: String = "Import from TeamSnap"
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            OutlinedButton(
                onClick = onJoinTeam,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    joinIcon()
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(joinTeamLabel)
            }
            OutlinedButton(
                onClick = onCreateTeam,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    createIcon()
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(createTeamLabel)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (teams.isNotEmpty()) {
                items(teams, key = { it.name }) { team ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectTeam(team.name) },
                        colors = CardDefaults.cardColors(
                            containerColor = teamColor(team.homeColorHex, 0x1976D2L).copy(alpha = 0.10f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    team.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TeamColorDot("Home", team.homeColorHex)
                                    TeamColorDot("Away", team.awayColorHex)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                team.playerCount?.let { count ->
                                    Text(
                                        "$count players",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                selectIcon()
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                emptyIcon()
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Teams Yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Join an existing team or create your own new team",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onCreateTeam) {
                                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    createIcon()
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(createNewTeamLabel)
                            }
                        }
                    }
                }
            }
        }

        if (teamSnapImportAvailable) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onImportTeamSnap,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    importIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(importTeamSnapLabel)
                }
            }
        }
    }
}

@Composable
private fun TeamColorDot(label: String, colorHex: String) {
    val color = teamColor(colorHex, 0x9E9E9EL)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun teamColor(hex: String, fallbackRgb: Long): Color {
    val rgb = hex.removePrefix("#").takeIf { value ->
        value.length == 6 && value.all { it.digitToIntOrNull(16) != null }
    }?.toLong(16) ?: fallbackRgb
    return Color(0xFF000000L or rgb)
}