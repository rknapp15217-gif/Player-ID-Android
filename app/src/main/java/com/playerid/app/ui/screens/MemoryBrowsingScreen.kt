package com.playerid.app.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playerid.app.data.ChildProfile
import com.playerid.app.data.GameSchedule
import com.playerid.app.data.MemoryItem
import com.playerid.app.data.SportSeason
import com.playerid.app.memory.MemoryBrowsingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryBrowsingScreen(
    onBack: () -> Unit,
    viewModel: MemoryBrowsingViewModel = viewModel()
) {
    val selectedChild by viewModel.selectedChild.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val selectedGame by viewModel.selectedGame.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    when {
                        selectedGame != null -> "Photos & Videos"
                        selectedSeason != null -> selectedSeason!!.sportName
                        selectedChild != null -> selectedChild!!.displayName
                        else -> "My Memories"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (selectedChild != null || selectedSeason != null || selectedGame != null) {
                    IconButton(
                        onClick = {
                            when {
                                selectedGame != null -> viewModel.goBackToGames()
                                selectedSeason != null -> viewModel.goBackToSeasons()
                                else -> viewModel.goBackToChildren()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        AnimatedContent(
            targetState = when {
                selectedGame != null -> 3
                selectedSeason != null -> 2
                selectedChild != null -> 1
                else -> 0
            },
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(targetOffsetX = { -it })
            },
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {
                0 -> ChildrenListView(viewModel = viewModel)
                1 -> SeasonsListView(viewModel = viewModel)
                2 -> GamesListView(viewModel = viewModel)
                3 -> MemoriesGridView(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ChildrenListView(viewModel: MemoryBrowsingViewModel) {
    val children by viewModel.children.collectAsState()

    if (children.isEmpty()) {
        EmptyStateView(
            title = "No children yet",
            message = "Add a child profile to start organizing memories."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(children) { child ->
                ChildItemCard(
                    child = child,
                    onClick = { viewModel.selectChild(child) }
                )
            }
        }
    }
}

@Composable
private fun ChildItemCard(
    child: ChildProfile,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = child.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SeasonsListView(viewModel: MemoryBrowsingViewModel) {
    val seasons by viewModel.seasons.collectAsState()

    if (seasons.isEmpty()) {
        EmptyStateView(
            title = "No seasons yet",
            message = "Upload a schedule to start organizing memories by game."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(seasons) { season ->
                SeasonItemCard(
                    season = season,
                    onClick = { viewModel.selectSeason(season) }
                )
            }
        }
    }
}

@Composable
private fun SeasonItemCard(
    season: SportSeason,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = season.sportName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildString {
                    if (season.seasonLabel.isNotBlank()) {
                        append(season.seasonLabel)
                        if (season.teamName.isNotBlank()) append(" • ")
                    }
                    if (season.teamName.isNotBlank()) {
                        append(season.teamName)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GamesListView(viewModel: MemoryBrowsingViewModel) {
    val games by viewModel.games.collectAsState()

    if (games.isEmpty()) {
        EmptyStateView(
            title = "No games yet",
            message = "No scheduled games found for this season."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games) { game ->
                GameItemCard(
                    game = game,
                    date = viewModel.getGameDate(game),
                    title = viewModel.getGameTitle(game),
                    onClick = { viewModel.selectGame(game) }
                )
            }
        }
    }
}

@Composable
private fun GameItemCard(
    game: GameSchedule,
    date: String,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildString {
                    append(date)
                    if (!game.locationName.isNullOrBlank()) {
                        append(" • ")
                        append(game.locationName)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemoriesGridView(viewModel: MemoryBrowsingViewModel) {
    val memories by viewModel.memories.collectAsState()
    val context = LocalContext.current

    if (memories.isEmpty()) {
        EmptyStateView(
            title = "No photos or videos",
            message = "This game doesn't have any memories yet."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(memories) { memory ->
                MemoryThumbnail(
                    memory = memory,
                    contentResolver = context.contentResolver
                )
            }
        }
    }
}

@Composable
private fun MemoryThumbnail(
    memory: MemoryItem,
    contentResolver: ContentResolver
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            LaunchedEffect(memory.contentUri) {
                val loadedBitmap = withContext(Dispatchers.Default) {
                    try {
                        val uri = Uri.parse(memory.contentUri)
                        val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
                        inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)?.let { original ->
                                Bitmap.createScaledBitmap(original, 120, 120, true)
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                loadedBitmap?.let { bitmap = it }
            }
            CircularProgressIndicator(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Video indicator
        if (memory.mimeType.startsWith("video")) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎬",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
