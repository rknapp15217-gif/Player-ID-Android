package com.playerid.app.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.SCHEDULE_EXTRACTION_SCRIPT
import com.playerid.app.domain.team.SCHEDULE_IMPORT_START_URL
import com.playerid.app.domain.team.ScheduleImportEntry
import com.playerid.app.domain.team.TeamImportSource
import com.playerid.app.domain.team.formatScheduleImportStart
import com.playerid.app.domain.team.isImportable
import com.playerid.app.domain.team.parseScheduleText
import com.playerid.app.roster.normalizeRosterWebUrl
import kotlinx.coroutines.launch

/** Android's "Import Schedule" screen: read games from a screenshot, the team's app, or a website, then pick which to import. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleImportPage(
    teamName: String,
    source: TeamImportSource,
    adapter: RosterImportAdapter,
    onBack: () -> Unit,
    onImport: (List<ScheduleImportEntry>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var parsedEntries by remember { mutableStateOf<List<ScheduleImportEntry>>(emptyList()) }
    var selectedEntryIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isReading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var websiteUrl by remember { mutableStateOf(SCHEDULE_IMPORT_START_URL) }
    var browser by remember { mutableStateOf<RosterWebBrowser?>(null) }

    fun acceptText(text: String) {
        parsedEntries = parseScheduleText(text)
        selectedEntryIndices = parsedEntries.indices.filter { parsedEntries[it].isImportable() }.toSet()
        errorMessage = if (parsedEntries.isEmpty()) {
            "No games detected. Include a date, time, and opponent for each game."
        } else null
    }
    fun readScreenshots(multiple: Boolean) {
        scope.launch {
            errorMessage = null
            try {
                val result = adapter.recognizeRosterImages(multiple) { isReading = true }
                if (result != null) acceptText(result.rawLines.joinToString("\n"))
            } catch (error: Exception) {
                errorMessage = error.message ?: "Couldn't read the screenshot."
            } finally {
                isReading = false
            }
        }
    }
    fun openWebsite() {
        browser?.load(normalizeRosterWebUrl(websiteUrl))
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Team: $teamName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            when (source) {
                TeamImportSource.Screenshot -> OutlinedButton(
                    onClick = { readScreenshots(multiple = false) },
                    enabled = !isReading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Schedule Screenshot")
                }
                // Android captures the sports app's screen; iPhone apps can't, so the user screenshots it instead.
                TeamImportSource.App -> {
                    if (parsedEntries.isEmpty()) Text(
                        "Open your team app (TeamSnap, GameChanger, SportsEngine, and others), take screenshots of the schedule, then choose them here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { readScreenshots(multiple = true) },
                        enabled = !isReading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose Schedule Screenshots")
                    }
                }
                TeamImportSource.Website -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = websiteUrl,
                            onValueChange = { websiteUrl = it },
                            label = { Text("Schedule website") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go,
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(onGo = { openWebsite() }),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { openWebsite() }) {
                            Icon(Icons.Default.Language, contentDescription = "Open website")
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        adapter.RosterWebView(
                            initialUrl = SCHEDULE_IMPORT_START_URL,
                            onBrowserReady = { browser = it },
                            onUrlChanged = { websiteUrl = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Button(
                        onClick = {
                            val activeBrowser = browser ?: return@Button
                            isReading = true
                            scope.launch {
                                try {
                                    acceptText(activeBrowser.evaluateText(SCHEDULE_EXTRACTION_SCRIPT))
                                } catch (error: Exception) {
                                    errorMessage = error.message ?: "Couldn't read this page."
                                } finally {
                                    isReading = false
                                }
                            }
                        },
                        enabled = browser != null && !isReading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isReading) "Reading Schedule..." else "Capture Schedule") }
                }
            }

            if (isReading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (parsedEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Selected ${selectedEntryIndices.size} of ${parsedEntries.size} games",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val importable = parsedEntries.indices.filter { parsedEntries[it].isImportable() }.toSet()
                    val allSelected = selectedEntryIndices.size == importable.size
                    TextButton(onClick = { selectedEntryIndices = if (allSelected) emptySet() else importable }) {
                        Text(if (allSelected) "Clear all" else "Select all")
                    }
                }
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(parsedEntries) { index, entry ->
                        val importable = entry.isImportable()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = index in selectedEntryIndices,
                                enabled = importable,
                                onCheckedChange = { checked ->
                                    selectedEntryIndices = if (checked) selectedEntryIndices + index else selectedEntryIndices - index
                                }
                            )
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(entry.gameLabel.ifBlank { "vs ${entry.opponent}" }, fontWeight = FontWeight.Medium)
                                    Text(
                                        formatScheduleImportStart(entry.startMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    entry.locationName?.takeIf(String::isNotBlank)?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!importable) Text(
                                        "End time must be after start time.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { onImport(parsedEntries.filterIndexed { index, _ -> index in selectedEntryIndices }) },
                    enabled = selectedEntryIndices.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import ${selectedEntryIndices.size} Games") }
            }
        }
    }
}
