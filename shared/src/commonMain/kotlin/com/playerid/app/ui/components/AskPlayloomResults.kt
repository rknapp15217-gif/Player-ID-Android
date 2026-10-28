package com.playerid.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.AskPlayloomQuery
import com.playerid.app.domain.team.chips
import com.playerid.app.domain.team.withoutChip
import com.playerid.app.domain.team.MemoryQuestionAnswer

@Composable
fun AskPlayloomAnswer(title: String, detail: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(
            Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskPlayloomResults(
    interpretation: AskPlayloomQuery,
    count: Int,
    reels: Boolean,
    onChange: (AskPlayloomQuery) -> Unit,
    onClear: () -> Unit,
    answer: MemoryQuestionAnswer? = null,
    onPlayAll: (() -> Unit)? = null,
    playableCount: Int = 0,
    onCreateReel: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    if (!reels && interpretation.issue == null && answer == null && count > 0) return
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        val issue = interpretation.issue
        val noun = if (reels) { if (count == 1) "reel" else "reels" }
            else { if (count == 1) "memory" else "memories" }
        if (issue == null && answer != null) {
            AskPlayloomAnswer(answer.title, answer.detail)
        }
        if (reels || issue != null) Text(
            issue ?: "$count $noun found",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (issue != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        if (issue == null) {
            if (reels) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                if (onPlayAll != null && playableCount > 0) TextButton(onClick = onPlayAll) {
                    Text("Play all ($playableCount)")
                }
                if (onCreateReel != null) TextButton(enabled = playableCount >= 2, onClick = onCreateReel) {
                    Text("Create reel")
                }
                if (onShare != null && count > 0) TextButton(onClick = onShare) {
                    Text("Share ($count)")
                }
            }
            if (reels) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                interpretation.chips().forEach { chip ->
                    InputChip(
                        selected = true,
                        onClick = { onChange(interpretation.withoutChip(chip.id)) },
                        label = { Text(chip.label) },
                        trailingIcon = { Icon(Icons.Default.Close, "Remove ${chip.label}", Modifier.size(16.dp)) }
                    )
                }
                TextButton(onClick = onClear) { Text("Clear") }
            }
            if (count == 0 && answer == null) Text(
                if (reels) "No matching saved reels. Change the chips or existing filters."
                else "No matching saved memories. Change your search or filters.",
                style = MaterialTheme.typography.bodySmall)
        } else if (reels) TextButton(onClick = onClear) { Text("Clear search") }
    }
}
