package com.playerid.app.ui.memory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.ChildProfileRecord
import com.playerid.app.domain.team.GameScheduleProfile
import com.playerid.app.domain.team.MemoryBrowsingState
import com.playerid.app.domain.team.MemoryItemProfile
import com.playerid.app.domain.team.SportSeasonProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryBrowsingPage(
    state: MemoryBrowsingState,
    gameDateLabel: (GameScheduleProfile) -> String,
    onSelectChild: (ChildProfileRecord) -> Unit,
    onSelectSeason: (SportSeasonProfile) -> Unit,
    onSelectGame: (GameScheduleProfile) -> Unit,
    onBackToChildren: () -> Unit,
    onBackToSeasons: () -> Unit,
    onBackToGames: () -> Unit,
    onExit: () -> Unit,
    backIcon: @Composable () -> Unit,
    memoryThumbnail: @Composable (MemoryItemProfile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = when {
                        state.selectedGame != null -> "Photos & Videos"
                        state.selectedSeason != null -> state.selectedSeason.sportName
                        state.selectedChild != null -> state.selectedChild.displayName
                        else -> "My Memories"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = when {
                        state.selectedGame != null -> onBackToGames
                        state.selectedSeason != null -> onBackToSeasons
                        state.selectedChild != null -> onBackToChildren
                        else -> onExit
                    }
                ) { backIcon() }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        AnimatedContent(
            targetState = when {
                state.selectedGame != null -> 3
                state.selectedSeason != null -> 2
                state.selectedChild != null -> 1
                else -> 0
            },
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
            },
            modifier = Modifier.fillMaxSize()
        ) { level ->
            when (level) {
                0 -> ItemList(
                    items = state.children,
                    emptyTitle = "No children yet",
                    emptyMessage = "Add a child profile to start organizing memories.",
                    onClick = onSelectChild
                ) { child -> Text(child.displayName, style = MaterialTheme.typography.titleMedium) }
                1 -> ItemList(
                    items = state.seasons,
                    emptyTitle = "No seasons yet",
                    emptyMessage = "Upload a schedule to start organizing memories by game.",
                    onClick = onSelectSeason
                ) { season ->
                    Text(season.sportName, style = MaterialTheme.typography.titleMedium)
                    val detail = listOf(season.seasonLabel, season.teamName)
                        .filter { it.isNotBlank() }.joinToString(" • ")
                    if (detail.isNotEmpty()) DetailText(detail)
                }
                2 -> ItemList(
                    items = state.games,
                    emptyTitle = "No games yet",
                    emptyMessage = "No scheduled games found for this season.",
                    onClick = onSelectGame
                ) { game ->
                    Text(gameTitle(game), style = MaterialTheme.typography.titleMedium)
                    DetailText(
                        listOfNotNull(
                            gameDateLabel(game),
                            game.locationName?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                    )
                }
                else -> MemoryGrid(state.memories, memoryThumbnail)
            }
        }
    }
}

@Composable
private fun <T> ItemList(
    items: List<T>,
    emptyTitle: String,
    emptyMessage: String,
    onClick: (T) -> Unit,
    content: @Composable ColumnScope.(T) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(emptyTitle, emptyMessage)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onClick(item) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) { content(item) }
            }
        }
    }
}

@Composable
private fun DetailText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MemoryGrid(
    memories: List<MemoryItemProfile>,
    thumbnail: @Composable (MemoryItemProfile) -> Unit
) {
    if (memories.isEmpty()) {
        EmptyState("No photos or videos", "This game doesn't have any memories yet.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(memories, key = MemoryItemProfile::id) { memory -> thumbnail(memory) }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun gameTitle(game: GameScheduleProfile): String = buildString {
    append("vs ")
    append(game.opponentName)
    if (game.gameLabel.isNotBlank()) {
        append(" • ")
        append(game.gameLabel)
    }
}