package com.playerid.app.ui.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playerid.app.domain.team.MemoryItemProfile
import com.playerid.app.memory.MemoryBrowsingViewModel
import com.playerid.app.ui.memory.MemoryBrowsingPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryBrowsingScreen(
    onBack: () -> Unit,
    viewModel: MemoryBrowsingViewModel = viewModel()
) {
    val state by viewModel.browsingState.collectAsState()
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    MemoryBrowsingPage(
        state = state,
        gameDateLabel = { game -> dateFormatter.format(Date(game.scheduledStartMs)) },
        onSelectChild = viewModel::selectChildProfile,
        onSelectSeason = viewModel::selectSeasonProfile,
        onSelectGame = viewModel::selectGameProfile,
        onBackToChildren = viewModel::goBackToChildren,
        onBackToSeasons = viewModel::goBackToSeasons,
        onBackToGames = viewModel::goBackToGames,
        onExit = onBack,
        backIcon = {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        },
        memoryThumbnail = { memory ->
            MemoryThumbnail(memory, context.contentResolver)
        }
    )
}

@Composable
private fun MemoryThumbnail(
    memory: MemoryItemProfile,
    contentResolver: ContentResolver
) {
    var bitmap by remember(memory.media.identifier) { mutableStateOf<Bitmap?>(null) }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            LaunchedEffect(memory.media.identifier) {
                bitmap = withContext(Dispatchers.Default) {
                    try {
                        contentResolver.openInputStream(Uri.parse(memory.media.identifier))?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.let { original ->
                                Bitmap.createScaledBitmap(original, 120, 120, true)
                            }
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp).align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (memory.media.mimeType?.startsWith("video") == true) {
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
                Text(text = "V", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
