package com.playerid.app.ui.team

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playerid.app.domain.team.ScheduleGameItem
import com.playerid.app.domain.team.ScheduleListState

@Composable
fun SchedulePage(
    state: ScheduleListState,
    totalCount: Int,
    nowMs: Long,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    backIcon: @Composable () -> Unit,
    addIcon: @Composable () -> Unit,
    searchIcon: @Composable () -> Unit,
    importIcon: @Composable () -> Unit,
    gameTrailingIcon: @Composable (ScheduleGameItem) -> Unit
) {
    val upcomingGames = state.upcomingGames(nowMs)
    val pastGames = state.pastGames(nowMs)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            SchedulePageHeader(
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
                placeholder = { Text("Search games") },
                leadingIcon = searchIcon,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$totalCount games", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onImport) {
                    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                        importIcon()
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Import schedule", color = ScheduleActionBlue)
                }
            }
        }
        if (upcomingGames.isNotEmpty()) {
            item { ScheduleSectionLabel("UPCOMING") }
        }
        items(upcomingGames, key = { it.id }) { game ->
            ScheduleGameRow(game, gameTrailingIcon)
        }
        if (pastGames.isNotEmpty()) {
            item { ScheduleSectionLabel("PAST") }
        }
        items(pastGames, key = { it.id }) { game ->
            ScheduleGameRow(game, gameTrailingIcon)
        }
        if (state.visibleGames.isEmpty()) {
            item {
                Text(
                    "No games found",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SchedulePageHeader(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    backIcon: @Composable () -> Unit,
    addIcon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            backIcon()
            Spacer(Modifier.width(4.dp))
            Text("Back")
        }
        Text(
            "Schedule",
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

@Composable
private fun ScheduleSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun ScheduleGameRow(
    game: ScheduleGameItem,
    trailingIcon: @Composable (ScheduleGameItem) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(game.dateLabel, modifier = Modifier.width(54.dp), lineHeight = 18.sp)
        Column(Modifier.weight(1f)) {
            Text(game.title, fontWeight = FontWeight.Medium)
            Text(
                game.detailLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingIcon(game)
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
}

private val ScheduleActionBlue = Color(0xFF0A66FF)