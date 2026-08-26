package com.playerid.app.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.playerid.app.domain.team.JoinTeamEvent
import com.playerid.app.domain.team.JoinTeamItem
import com.playerid.app.domain.team.JoinTeamState

@Composable
fun JoinTeamDialog(
    teams: List<JoinTeamItem>,
    subscribedTeamNames: Set<String>,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    closeIcon: @Composable () -> Unit,
    searchIcon: @Composable () -> Unit,
    title: String = "Join a Team",
    searchPlaceholder: String = "Search teams",
    noOtherTeamsText: String = "No other teams available",
    noTeamsMatchText: (String) -> String = { query -> "No teams match \"$query\"" },
    joinLabel: String = "Join"
) {
    var state by remember {
        mutableStateOf(
            JoinTeamState(
                teams = teams,
                subscribedTeamNames = subscribedTeamNames
            )
        )
    }
    LaunchedEffect(teams) {
        state = state.reduce(JoinTeamEvent.TeamsChanged(teams))
    }
    LaunchedEffect(subscribedTeamNames) {
        state = state.reduce(JoinTeamEvent.SubscriptionsChanged(subscribedTeamNames))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        closeIcon()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { query ->
                        state = state.reduce(JoinTeamEvent.SearchQueryChanged(query))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(searchPlaceholder) },
                    leadingIcon = searchIcon,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (state.visibleTeams.isEmpty()) {
                    Text(
                        text = if (state.searchQuery.isEmpty()) {
                            noOtherTeamsText
                        } else {
                            noTeamsMatchText(state.searchQuery)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.visibleTeams, key = { it.name }) { team ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = teamColor(team.colorHex).copy(alpha = 0.10f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            team.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        team.playerCount?.let { playerCount ->
                                            Text(
                                                "$playerCount players",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Button(onClick = { onJoin(team.name) }) {
                                        Text(joinLabel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun teamColor(hex: String): Color {
    val rgb = hex.removePrefix("#").takeIf { value ->
        value.length == 6 && value.all { it.digitToIntOrNull(16) != null }
    }?.toLong(16) ?: 0x1976D2L
    return Color(0xFF000000L or rgb)
}