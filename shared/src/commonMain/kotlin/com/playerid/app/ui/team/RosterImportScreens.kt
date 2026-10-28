package com.playerid.app.ui.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.roster.RosterCandidate
import com.playerid.app.roster.RosterOcrResult
import com.playerid.app.roster.RosterTable
import com.playerid.app.roster.displayRosterWebUrl
import com.playerid.app.roster.distinctRosterCandidates
import com.playerid.app.roster.duplicateRosterNumbers
import com.playerid.app.roster.importableRosterTables
import com.playerid.app.roster.normalizeRosterWebUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Native services the shared roster import screens need (photo picking + OCR, and an embedded browser). */
interface RosterImportAdapter {
    /** Opens the photo picker; [onPicked] runs after selection, before text recognition. Null when cancelled. */
    suspend fun recognizeRosterImages(multiple: Boolean, onPicked: () -> Unit): RosterOcrResult?

    @Composable
    fun RosterWebView(
        initialUrl: String,
        onBrowserReady: (RosterWebBrowser) -> Unit,
        onUrlChanged: (String) -> Unit,
        modifier: Modifier
    )
}

interface RosterWebBrowser {
    fun load(url: String)
    fun goBack()
    fun goForward()
    fun reload()
    suspend fun rosterTables(): List<RosterTable>
    /** Runs a page script and returns its string result (empty when the page returns nothing). */
    suspend fun evaluateText(script: String): String
    /** Scrolls through the page and reads it with text recognition (Android's fallback when no table is found). */
    suspend fun recognizePageText(): RosterOcrResult
}

private const val IMPORT_CONFIRMATION_MS = 1400L

@Composable
private fun RosterCandidateRow(candidate: RosterCandidate, duplicate: Boolean, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${candidate.number}",
                style = MaterialTheme.typography.titleMedium,
                color = if (duplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(candidate.name, style = MaterialTheme.typography.bodyMedium)
                candidate.position.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                candidate.academicYear?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
        }
    }
}

@Composable
private fun DuplicateNumbersWarning(duplicates: Set<String>) {
    if (duplicates.isNotEmpty()) Text(
        "More than one player has #${duplicates.sorted().joinToString(", #")}. Remove the extra players before importing.",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun RosterOverwriteWarningDialog(teamName: String, onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overwrite Existing Roster?") },
        text = {
            Text(
                "Importing this roster will overwrite existing roster data for $teamName. Continue?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = { Button(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RosterImportCompleteContent(count: Int, teamName: String) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✅", style = MaterialTheme.typography.displaySmall)
        Text(
            "Successfully imported $count players",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text("to team $teamName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Closing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Android's "Import from Screenshot" dialog: review recognized players, confirm overwrite, then auto-close. */
@Composable
fun RosterImportReviewDialog(
    title: String,
    teamName: String,
    isProcessing: Boolean,
    errorMessage: String?,
    candidates: List<RosterCandidate>,
    emptyMessage: String,
    onRemove: (RosterCandidate) -> Unit,
    onDismiss: () -> Unit,
    onImport: (List<RosterCandidate>) -> Unit
) {
    var showOverwriteWarning by remember { mutableStateOf(false) }
    var imported by remember { mutableStateOf<List<RosterCandidate>?>(null) }
    val duplicates = duplicateRosterNumbers(candidates)
    LaunchedEffect(imported) {
        imported?.let {
            delay(IMPORT_CONFIRMATION_MS)
            onImport(it)
        }
    }
    val confirming = imported != null
    AlertDialog(
        onDismissRequest = { if (!confirming) onDismiss() },
        title = {
            Column {
                Text(if (confirming) "Import Complete" else title, style = MaterialTheme.typography.titleLarge)
                if (!confirming) Text(
                    "Team: $teamName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    confirming -> RosterImportCompleteContent(imported!!.size, teamName)
                    isProcessing -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.size(12.dp))
                        Text("Reading roster...")
                    }
                    errorMessage != null -> Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    candidates.isEmpty() -> Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
                    else -> {
                        Text("${candidates.size} players detected", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        DuplicateNumbersWarning(duplicates)
                        LazyColumn(Modifier.fillMaxWidth().height(240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(candidates) { candidate ->
                                RosterCandidateRow(candidate, candidate.number in duplicates) { onRemove(candidate) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!confirming) Button(
                onClick = { showOverwriteWarning = true },
                enabled = candidates.isNotEmpty() && !isProcessing && duplicates.isEmpty()
            ) { Text("Import Players") }
        },
        dismissButton = {
            if (!confirming) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
    if (showOverwriteWarning) RosterOverwriteWarningDialog(
        teamName,
        onContinue = { showOverwriteWarning = false; imported = candidates.toList() },
        onDismiss = { showOverwriteWarning = false }
    )
}

/** Android's "Import from Website" screen: browse to the roster, Capture, review, Import. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebRosterImportPage(
    teamName: String,
    adapter: RosterImportAdapter,
    onBack: () -> Unit,
    onImport: (List<RosterCandidate>) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var browser by remember { mutableStateOf<RosterWebBrowser?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var tableOptions by remember { mutableStateOf<List<RosterTable>>(emptyList()) }
    var tableSelection by remember { mutableStateOf(-1) }
    var showOverwriteWarning by remember { mutableStateOf(false) }
    var imported by remember { mutableStateOf<List<RosterCandidate>?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val duplicates = duplicateRosterNumbers(candidates)

    LaunchedEffect(imported) {
        imported?.let {
            delay(IMPORT_CONFIRMATION_MS)
            onImport(it)
        }
    }
    fun go() {
        val target = urlInput.trim()
        if (target.isBlank()) return
        val url = normalizeRosterWebUrl(target)
        if (loadedUrl == null) loadedUrl = url else browser?.load(url)
        focusManager.clearFocus()
    }
    fun capture() {
        val activeBrowser = browser ?: run { errorMessage = "Web view not ready"; return }
        isProcessing = true
        errorMessage = null
        candidates = emptyList()
        scope.launch {
            try {
                val tables = importableRosterTables(activeBrowser.rosterTables())
                when {
                    tables.size >= 2 -> { tableOptions = tables; tableSelection = -1 }
                    tables.size == 1 -> candidates = distinctRosterCandidates(tables.first().candidates)
                    else -> {
                        candidates = distinctRosterCandidates(activeBrowser.recognizePageText().candidates)
                        if (candidates.isEmpty()) errorMessage = "No players detected. Try zooming into the roster table."
                    }
                }
            } catch (error: Exception) {
                errorMessage = error.message ?: "Couldn't read this page."
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Website") },
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
            Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Team: $teamName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = displayRosterWebUrl(it) },
                label = { Text("Search or enter website") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                    // Regular keyboard (with space bar) because the field accepts searches as well as addresses.
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false
                ),
                keyboardActions = KeyboardActions(onGo = { go() }),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (loadedUrl != null) Arrangement.spacedBy(8.dp) else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loadedUrl != null) {
                    IconButton(onClick = { browser?.goBack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    IconButton(onClick = { browser?.goForward() }) { Icon(Icons.Default.ArrowForward, contentDescription = "Forward") }
                    IconButton(onClick = { browser?.reload() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                }
                Button(onClick = { go() }) {
                    Icon(Icons.Default.Search, contentDescription = "Go")
                    Spacer(Modifier.size(8.dp))
                    Text("Go")
                }
            }

            loadedUrl?.let { url ->
                Card(
                    Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    adapter.RosterWebView(
                        initialUrl = url,
                        onBrowserReady = { browser = it },
                        onUrlChanged = { urlInput = displayRosterWebUrl(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isProcessing) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.size(12.dp))
                    Text("Reading roster...")
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                if (candidates.isNotEmpty()) {
                    Text("${candidates.size} players detected", style = MaterialTheme.typography.bodyMedium)
                    DuplicateNumbersWarning(duplicates)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(candidates) { candidate ->
                            RosterCandidateRow(candidate, candidate.number in duplicates) {
                                candidates = candidates.filterNot { it == candidate }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { capture() }, enabled = !isProcessing && browser != null) { Text("Capture") }
                    TextButton(
                        onClick = { showOverwriteWarning = true },
                        enabled = candidates.isNotEmpty() && duplicates.isEmpty() && !isProcessing && imported == null
                    ) { Text("Import") }
                }
            }
        }
    }

    if (showOverwriteWarning) RosterOverwriteWarningDialog(
        teamName,
        onContinue = { showOverwriteWarning = false; imported = candidates.toList() },
        onDismiss = { showOverwriteWarning = false }
    )
    imported?.let { players ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Import Complete") },
            text = { RosterImportCompleteContent(players.size, teamName) },
            confirmButton = {}
        )
    }
    if (tableOptions.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { tableOptions = emptyList() },
            title = { Text("Multiple rosters found") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This page looks like it has more than one roster. Choose one or combine them.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tableSelection == -1, onClick = { tableSelection = -1 })
                        Text("Combine all rosters")
                    }
                    tableOptions.forEachIndexed { index, table ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = tableSelection == index, onClick = { tableSelection = index })
                            Text(table.label)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val selected = if (tableSelection == -1) tableOptions.flatMap { it.candidates }
                    else tableOptions.getOrNull(tableSelection)?.candidates.orEmpty()
                    candidates = distinctRosterCandidates(selected)
                    tableOptions = emptyList()
                }) { Text("Use Selection") }
            },
            dismissButton = { TextButton(onClick = { tableOptions = emptyList() }) { Text("Cancel") } }
        )
    }
}

/**
 * iPhone counterpart of Android's "Import from App". Android records the roster app's screen; iOS doesn't allow
 * reading other apps, so the user takes screenshots in their roster app and PlayerID reads them together.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRosterImportPage(
    teamName: String,
    adapter: RosterImportAdapter,
    onBack: () -> Unit,
    onImport: (List<RosterCandidate>) -> Unit
) {
    var reviewing by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from App") },
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
            Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Team: $teamName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Import from your roster app", style = MaterialTheme.typography.titleMedium)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Open your team app (TeamSnap, GameChanger, SportsEngine, and others) and go to the roster.",
                        "Take a screenshot (side button + volume up). Scroll and take more until every player is captured.",
                        "Come back to PlayerID and choose those screenshots below."
                    ).forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Text(
                "iPhone doesn't let apps read other apps' screens, so PlayerID reads your screenshots instead. They stay on this iPhone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    scope.launch {
                        errorMessage = null
                        candidates = emptyList()
                        try {
                            val result = adapter.recognizeRosterImages(multiple = true) {
                                reviewing = true
                                isProcessing = true
                            }
                            if (result != null) candidates = distinctRosterCandidates(result.candidates)
                        } catch (error: Exception) {
                            reviewing = true
                            errorMessage = error.message ?: "Failed to read roster"
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Choose Screenshots")
            }
        }
    }

    if (reviewing) RosterImportReviewDialog(
        title = "Import from App",
        teamName = teamName,
        isProcessing = isProcessing,
        errorMessage = errorMessage,
        candidates = candidates,
        emptyMessage = "No players detected. Try screenshots that show the roster list clearly.",
        onRemove = { removed -> candidates = candidates.filterNot { it == removed } },
        onDismiss = { reviewing = false },
        onImport = onImport
    )
}
