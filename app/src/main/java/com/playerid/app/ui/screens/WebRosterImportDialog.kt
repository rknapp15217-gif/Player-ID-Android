package com.playerid.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import com.playerid.app.roster.RosterCandidate
import com.playerid.app.roster.extractRosterCandidates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONTokener
import kotlin.math.ceil
import kotlin.math.max
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebRosterImportScreen(
    teamName: String,
    onBack: () -> Unit,
    onImport: (List<RosterCandidate>) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasNavigated by remember { mutableStateOf(false) }
    var tableOptions by remember { mutableStateOf<List<TableRoster>>(emptyList()) }
    var showTablePicker by remember { mutableStateOf(false) }
    var tableSelection by remember { mutableStateOf(-1) }
    var showConfirmation by remember { mutableStateOf(false) }
    var showOverwriteWarning by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf(0) }
    var importedCandidates by remember { mutableStateOf<List<RosterCandidate>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(showConfirmation, importedCandidates) {
        if (showConfirmation && importedCandidates.isNotEmpty()) {
            delay(1400)
            onImport(importedCandidates)
            showConfirmation = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Website") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Team: $teamName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isImeVisible) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { input ->
                        urlInput = input.removePrefix("https://").removePrefix("http://")
                    },
                    label = { Text("Search or enter website") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Uri,
                        capitalization = KeyboardCapitalization.None
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val target = urlInput.trim()
                            if (target.isNotBlank()) {
                                webViewRef?.loadUrl(normalizeUrl(target))
                                hasNavigated = true
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasNavigated) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { webViewRef?.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        IconButton(onClick = { webViewRef?.goForward() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                        }
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        Button(
                            onClick = {
                                val target = urlInput.trim()
                                if (target.isNotBlank()) {
                                    webViewRef?.loadUrl(normalizeUrl(target))
                                    hasNavigated = true
                                    focusManager.clearFocus()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Go")
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Go")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val target = urlInput.trim()
                                if (target.isNotBlank()) {
                                    webViewRef?.loadUrl(normalizeUrl(target))
                                    hasNavigated = true
                                    focusManager.clearFocus()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Go")
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Go")
                        }
                    }
                }
            }

            if (hasNavigated) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.setSupportZoom(true)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        if (url != null) {
                                            urlInput = url.removePrefix("https://").removePrefix("http://")
                                        }
                                    }
                                }
                                loadUrl(normalizeUrl(urlInput))
                                webViewRef = this
                            }
                        }
                    )
                }

                if (isProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("Reading roster...")
                    }
                }

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (candidates.isNotEmpty()) {
                    Text(
                        "${candidates.size} players detected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(candidates) { candidate ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "#${candidate.number}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            candidate.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (!candidate.position.isNullOrBlank()) {
                                            Text(
                                                candidate.position,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        candidate.academicYear?.takeIf(String::isNotBlank)?.let { academicYear ->
                                            Text(
                                                academicYear,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        candidates = candidates.filterNot { it == candidate }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val webView = webViewRef
                            if (webView == null) {
                                errorMessage = "Web view not ready"
                                return@Button
                            }

                            val width = webView.width
                            val viewportHeight = webView.height
                            val contentHeightPx = (webView.contentHeight * webView.resources.displayMetrics.density).toInt()
                            val totalHeight = max(contentHeightPx, viewportHeight)
                            if (width <= 0 || viewportHeight <= 0 || totalHeight <= 0) {
                                errorMessage = "Web view not ready"
                                return@Button
                            }

                            isProcessing = true
                            errorMessage = null
                            candidates = emptyList()
                            scope.launch {
                                val domTables = withContext(Dispatchers.Main) {
                                    extractRosterTablesFromDom(webView)
                                }
                                val filteredTables = domTables.filter { it.candidates.size >= 5 }
                                if (filteredTables.size >= 2) {
                                    tableOptions = filteredTables
                                    tableSelection = -1
                                    showTablePicker = true
                                    isProcessing = false
                                    return@launch
                                } else if (filteredTables.size == 1) {
                                    candidates = filteredTables.first().candidates
                                        .distinctBy { it.number + "|" + it.name.lowercase() }
                                    isProcessing = false
                                    return@launch
                                }

                                val accumulated = mutableListOf<RosterCandidate>()
                                val pages = max(1, ceil(totalHeight / viewportHeight.toFloat()).toInt())
                                val originalScrollY = webView.scrollY

                                for (page in 0 until pages) {
                                    val scrollY = page * viewportHeight
                                    withContext(Dispatchers.Main) {
                                        webView.scrollTo(0, scrollY)
                                    }
                                    delay(80)
                                    val bitmap = withContext(Dispatchers.Main) {
                                        val capture = Bitmap.createBitmap(width, viewportHeight, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(capture)
                                        webView.draw(canvas)
                                        capture
                                    }

                                    val result = withContext(Dispatchers.Default) {
                                        extractRosterCandidates(bitmap)
                                    }
                                    accumulated.addAll(result.candidates)
                                }

                                withContext(Dispatchers.Main) {
                                    webView.scrollTo(0, originalScrollY)
                                }

                                candidates = accumulated
                                    .distinctBy { it.number + "|" + it.name.lowercase() }
                                if (candidates.isEmpty()) {
                                    errorMessage = "No players detected. Try zooming into the roster table."
                                }
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing && !showConfirmation
                    ) {
                        Text("Capture")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(
                        onClick = {
                            if (candidates.isNotEmpty()) {
                                showOverwriteWarning = true
                            }
                        },
                        enabled = candidates.isNotEmpty() && !isProcessing && !showConfirmation && !showOverwriteWarning
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }

    if (showOverwriteWarning) {
        AlertDialog(
            onDismissRequest = { showOverwriteWarning = false },
            title = { Text("Overwrite Existing Roster?") },
            text = {
                Text(
                    "Importing this roster will overwrite existing roster data for $teamName. Continue?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        importedCandidates = candidates.toList()
                        importedCount = importedCandidates.size
                        showOverwriteWarning = false
                        showConfirmation = true
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Import Complete") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✅",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        "Successfully imported $importedCount players",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "to team $teamName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Closing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    if (showTablePicker) {
        AlertDialog(
            onDismissRequest = { showTablePicker = false },
            title = { Text("Multiple rosters found") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This page looks like it has more than one roster. Choose one or combine them.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = tableSelection == -1,
                            onClick = { tableSelection = -1 }
                        )
                        Text("Combine all rosters")
                    }
                    tableOptions.forEachIndexed { index, table ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = tableSelection == index,
                                onClick = { tableSelection = index }
                            )
                            Text(table.label)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedCandidates = if (tableSelection == -1) {
                            tableOptions.flatMap { it.candidates }
                        } else {
                            tableOptions.getOrNull(tableSelection)?.candidates ?: emptyList()
                        }
                        candidates = selectedCandidates
                            .distinctBy { it.number + "|" + it.name.lowercase() }
                        showTablePicker = false
                    }
                ) {
                    Text("Use Selection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTablePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class TableRoster(
    val label: String,
    val candidates: List<RosterCandidate>
)

private suspend fun extractRosterTablesFromDom(webView: WebView): List<TableRoster> {
    val js = """
        (function() {
          function findLabelForTable(table, index) {
            const caption = table.querySelector('caption');
            if (caption && caption.innerText.trim()) return caption.innerText.trim();
            let node = table;
            while (node) {
              let prev = node.previousElementSibling;
              while (prev) {
                if (/^H[1-4]$/.test(prev.tagName) && prev.innerText.trim()) {
                  return prev.innerText.trim();
                }
                prev = prev.previousElementSibling;
              }
              node = node.parentElement;
            }
            return 'Roster Table ' + (index + 1);
          }

          const tables = Array.from(document.querySelectorAll('table'));
          const results = [];
          tables.forEach((table, index) => {
            const rows = Array.from(table.querySelectorAll('tr'));
            const items = [];
            rows.forEach(tr => {
              const cells = Array.from(tr.querySelectorAll('td,th')).map(c => c.innerText.trim());
              if (cells.length >= 2) {
                const num = cells[0].replace('#','').trim();
                const name = cells[1].trim();
                const year = cells[2] ? cells[2].trim() : '';
                const pos = cells[3] ? cells[3].trim() : '';
                if (/^\d{1,2}$/.test(num) && name) {
                  items.push({ number: num, name: name, academicYear: year, position: pos });
                }
              }
            });
            if (items.length > 0) {
              results.push({ label: findLabelForTable(table, index), items: items });
            }
          });
          return JSON.stringify(results);
        })();
    """.trimIndent()

    return suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(js) { raw ->
            if (!continuation.isActive) return@evaluateJavascript
            try {
                if (raw == null || raw == "null") {
                    continuation.resume(emptyList())
                    return@evaluateJavascript
                }
                val token = JSONTokener(raw).nextValue()
                val array = when (token) {
                    is JSONArray -> token
                    is String -> JSONArray(token)
                    else -> JSONArray()
                }
                val results = mutableListOf<TableRoster>()
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    val label = obj.optString("label").ifBlank { "Roster Table ${index + 1}" }
                    val items = obj.optJSONArray("items") ?: JSONArray()
                    val candidates = mutableListOf<RosterCandidate>()
                    for (rowIndex in 0 until items.length()) {
                        val row = items.getJSONObject(rowIndex)
                        val number = row.optString("number").trim()
                        val name = row.optString("name").trim()
                        val rawYear = row.optString("academicYear").ifBlank { row.optString("year") }
                        val rawPosition = row.optString("position").ifBlank { row.optString("pos") }
                        val academicYear = normalizeAcademicYear(rawYear)
                        if (number.matches(Regex("\\d{1,2}")) && name.length >= 2 && name.none { it.isDigit() }) {
                            candidates.add(
                                RosterCandidate(
                                    name = name,
                                    number = normalizeRosterNumber(number),
                                    position = normalizePosition(rawPosition) ?: "",
                                    graduationYear = null,
                                    academicYear = academicYear
                                )
                            )
                        }
                    }
                    if (candidates.isNotEmpty()) {
                        results.add(TableRoster(label = label, candidates = candidates))
                    }
                }
                continuation.resume(results)
                } catch (ex: Exception) {
                continuation.resume(emptyList())
                }
        }
            }
}

private fun normalizeAcademicYear(raw: String): String? {
    return when (raw.trim().lowercase()) {
        "freshman", "fr" -> "Freshman"
        "sophomore", "so" -> "Sophomore"
        "junior", "jr" -> "Junior"
        "senior", "sr" -> "Senior"
        else -> null
    }
}

private fun normalizePosition(raw: String): String? {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return null
    return when (cleaned.uppercase()) {
        "G", "GK", "GOALIE", "GOALKEEPER" -> "Goalie"
        "D", "DEF", "DEFENSE" -> "Defense"
        "M", "MID", "MIDFIELD" -> "Midfield"
        "A", "ATT", "ATTACK" -> "Attack"
        "LSM" -> "LSM"
        "FO", "FOGO" -> "FOGO"
        "M/FOGO" -> "FOGO"
        "D/LSM" -> "Defense"
        "A/M" -> "Attack"
        else -> cleaned
    }
}

private fun normalizeRosterNumber(raw: String): String {
    return raw.trim().toIntOrNull()?.toString() ?: raw.trim()
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "https://"
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else if (looksLikeUrl(trimmed)) {
        "https://$trimmed"
    } else {
        "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }
}

private fun looksLikeUrl(input: String): Boolean {
    if (input.contains(" ")) return false
    return input.contains(".") && input.any { it.isLetter() }
}
