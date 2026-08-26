package com.playerid.app.ui.roster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.RosterListState

@Composable
fun RosterPage(
    state: RosterListState,
    totalCount: Int,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onPlayerClick: (PlayerProfile) -> Unit,
    backIcon: @Composable () -> Unit,
    addIcon: @Composable () -> Unit,
    searchIcon: @Composable () -> Unit,
    importIcon: @Composable () -> Unit,
    addPlayerIcon: @Composable () -> Unit,
    playerLeadingContent: @Composable (PlayerProfile) -> Unit,
    playerTrailingContent: @Composable (PlayerProfile) -> Unit
) {
    val visiblePlayers = state.visiblePlayers
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            RosterPageHeader(
                onBack = onBack,
                onAdd = onAdd,
                backIcon = backIcon,
                addIcon = addIcon
            )
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search players") },
                leadingIcon = searchIcon,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalCount players",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onImport) {
                    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                        importIcon()
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Import roster")
                }
            }
        }
        items(visiblePlayers, key = { it.id }) { player ->
            RosterPlayerRow(
                player = player,
                onClick = { onPlayerClick(player) },
                leadingContent = { playerLeadingContent(player) },
                trailingContent = { playerTrailingContent(player) }
            )
        }
        if (visiblePlayers.isEmpty()) {
            item {
                Text(
                    text = "No players found",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            TextButton(onClick = onAdd, modifier = Modifier.padding(vertical = 8.dp)) {
                addPlayerIcon()
                Spacer(Modifier.width(8.dp))
                Text("Add player")
            }
        }
    }
}

@Composable
private fun RosterPageHeader(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    backIcon: @Composable () -> Unit,
    addIcon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            backIcon()
            Spacer(Modifier.width(4.dp))
            Text("Back")
        }
        Text(
            text = "Roster",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onAdd) {
            addIcon()
        }
    }
}