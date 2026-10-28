package com.playerid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal fun playloomGoExampleRequests(hints: List<String>): List<String> = listOf(
    hints.firstOrNull { it.startsWith("Find", true) } ?: "Find goals this season",
    "Create a reel of last weekend",
    "Show my favorite faceoff wins",
    "Share a reel of goals",
    "When is our next game?",
    "How many goals did I record?",
    "Open our roster",
    "Open teams"
)

@Composable
fun PlayloomGoPage(
    hints: List<String>,
    recentRequests: List<String>,
    onSubmit: (String) -> Unit,
    onClearRecent: () -> Unit,
    searchBar: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val examples = playloomGoExampleRequests(hints).zip(listOf(
        Icons.Default.Search, Icons.Default.Movie, Icons.Default.StarOutline, Icons.Default.Send,
        Icons.Default.CalendarToday, Icons.Default.BarChart, Icons.Default.Groups, Icons.Default.Shield
    ))
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.primaryContainer.copy(alpha = 0.35f), colors.surface)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AutoAwesome, contentDescription = null,
            tint = Color(0xFF2475FF), modifier = Modifier.padding(top = 12.dp, bottom = 12.dp).size(48.dp)
        )
        Text("Playloom Go", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Your sports memory assistant", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        searchBar()
        Text(
            "Try these", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp)
        )
        examples.forEach { (request, icon) ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.16f))
            ) {
                Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { onSubmit(request) }
                    .padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, contentDescription = null,
                        tint = if (icon == Icons.Default.StarOutline) Color(0xFF2475FF) else colors.primary,
                        modifier = Modifier.size(22.dp))
                    Text(request, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (recentRequests.isNotEmpty()) TextButton(onClick = onClearRecent) { Text("Clear") }
        }
        if (recentRequests.isEmpty()) Text(
            "Your recent requests will appear here.",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        recentRequests.forEach { request ->
            TextButton(onClick = { onSubmit(request) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(request, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    textAlign = TextAlign.Start, style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
