package com.playerid.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.memory.ScheduleImportEntry
import com.playerid.app.memory.parseScheduleText
import com.playerid.app.capture.AppRosterCaptureRepository
import com.playerid.app.capture.CaptureContent
import com.playerid.app.capture.RosterApp
import com.playerid.app.capture.RosterAppDetector
import com.playerid.app.capture.ScreenCaptureService
import com.playerid.app.roster.extractRosterCandidates
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ScheduleImportScreen(
    teamName: String,
    source: String,
    onBack: () -> Unit,
    onImport: (List<ScheduleImportEntry>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var parsedEntries by remember { mutableStateOf<List<ScheduleImportEntry>>(emptyList()) }
    var selectedEntryIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isReading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var installedApps by remember { mutableStateOf<List<RosterApp>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<RosterApp?>(null) }
    var isWaitingForApp by remember { mutableStateOf(false) }
    var websiteUrl by remember { mutableStateOf("https://www.google.com") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebsiteReady by remember { mutableStateOf(false) }
    val scheduleLines by AppRosterCaptureRepository.scheduleLines.collectAsState()

    fun acceptText(text: String) {
        parsedEntries = parseScheduleText(text)
        selectedEntryIndices = parsedEntries.indices.toSet()
        errorMessage = if (parsedEntries.isEmpty()) {
            "No games detected. Include a date, time, and opponent for each game."
        } else null
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isReading = true
            val entries = withContext(Dispatchers.IO) {
                runCatching {
                    val input = context.contentResolver.openInputStream(uri) ?: return@runCatching emptyList()
                    input.use { stream ->
                        stream.bufferedReader().use { reader ->
                            parseScheduleText(reader.readText())
                        }
                    }
                }.getOrElse { emptyList() }
            }
            parsedEntries = entries
            selectedEntryIndices = entries.indices.toSet()
            isReading = false

            if (entries.isEmpty()) {
                Toast.makeText(
                    context,
                    "No schedule rows found. Use CSV: date,startTime,endTime,opponent,location,latitude,longitude,gameLabel",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val screenshotPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isReading = true
            val text = withContext(Dispatchers.Default) {
                runCatching { extractRosterCandidates(context, uri).rawLines.joinToString("\n") }.getOrDefault("")
            }
            acceptText(text)
            isReading = false
        }
    }

    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            AppRosterCaptureRepository.cacheProjection(result.resultCode, data)
            startScheduleCaptureService(context, result.resultCode, data)
            isWaitingForApp = true
            selectedApp?.let { app ->
                if (!RosterAppDetector.launchApp(context, app.packageName)) errorMessage = "Could not open ${app.name}"
            }
            selectedApp = null
        } else {
            errorMessage = "Screen capture permission denied."
            selectedApp = null
        }
    }

    LaunchedEffect(teamName, source) {
        if (source == "app") {
            AppRosterCaptureRepository.setActiveTeamName(teamName)
            AppRosterCaptureRepository.setCaptureContent(CaptureContent.SCHEDULE)
            installedApps = withContext(Dispatchers.IO) { RosterAppDetector.getInstalledRosterApps(context) }
        }
    }
    LaunchedEffect(scheduleLines) {
        if (source == "app" && scheduleLines.isNotEmpty()) acceptText(scheduleLines.joinToString("\n"))
    }
    BackHandler(source == "app") {
        stopScheduleCaptureService(context)
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            Text(
                text = "Team: $teamName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            when (source) {
                "screenshot" -> OutlinedButton(
                    onClick = { screenshotPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Schedule Screenshot")
                }
                "app" -> {
                    if (installedApps.isEmpty() && !isWaitingForApp) {
                        Text("No supported sports apps found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    installedApps.forEach { app ->
                        OutlinedButton(
                            onClick = {
                                if (!Settings.canDrawOverlays(context)) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                    errorMessage = "Enable Display over other apps, then select the app again."
                                } else {
                                    selectedApp = app
                                    val cached = AppRosterCaptureRepository.getCachedProjection()
                                    if (cached != null) {
                                        startScheduleCaptureService(context, cached.first, cached.second)
                                        isWaitingForApp = true
                                        RosterAppDetector.launchApp(context, app.packageName)
                                        selectedApp = null
                                    } else {
                                        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                        projectionLauncher.launch(manager.createScreenCaptureIntent())
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val bitmap = remember(app.icon) { app.icon?.toBitmap(48, 48)?.asImageBitmap() }
                            if (bitmap != null) Image(bitmap, null, modifier = Modifier.width(22.dp).height(22.dp))
                            else Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(app.name)
                        }
                    }
                    if (isWaitingForApp && parsedEntries.isEmpty()) {
                        Text("Capture schedule screens in the selected app, then tap Done.")
                    }
                }
                "website" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = websiteUrl,
                            onValueChange = { websiteUrl = it },
                            label = { Text("Schedule website") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { webViewRef?.loadUrl(normalizeScheduleUrl(websiteUrl)) }) {
                            Icon(Icons.Default.Language, contentDescription = "Open website")
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isWebsiteReady = true
                                        websiteUrl = url ?: websiteUrl
                                    }
                                }
                                loadUrl(normalizeScheduleUrl(websiteUrl))
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    Button(
                        onClick = {
                            isReading = true
                            val extractionScript = """
                                (function() {
                                    const datePattern = /\b\d{1,2}[\/-]\d{1,2}[\/-]\d{2,4}\b/;
                                    const timePattern = /\b(?:1[0-2]|0?[1-9])(?::[0-5]\d)?\s*(?:am|pm)\b/i;
                                    const headings = Array.from(document.querySelectorAll('#schedule-results h2, h2'))
                                        .filter(heading => /Football Schedule$/i.test((heading.innerText || '').trim()));
                                    const sections = headings.map(heading => {
                                        let table = null;
                                        let node = heading.nextElementSibling;
                                        while (node && !table && !/^H[1-6]$/.test(node.tagName)) {
                                            if (node.matches && node.matches('table.standard-table')) table = node;
                                            node = node.nextElementSibling;
                                        }
                                        const headingText = (heading.innerText || '').trim();
                                        const levelText = headingText
                                            .replace(/^\d{4}-\d{4}\s+/i, '')
                                            .replace(/\s*Football Schedule$/i, '')
                                            .trim();
                                        return { level: levelText || 'Varsity', table: table };
                                    }).filter(section => section.table);
                                    const sourceSections = sections.length > 0
                                        ? sections
                                        : [{ level: 'Varsity', table: document.querySelector('#schedule-results table.standard-table, table.standard-table') }];
                                    const rows = sourceSections.flatMap(section => Array.from(section.table?.querySelectorAll('tbody tr') || []).map(row => {
                                        const date = (row.querySelector('.game-date')?.innerText || '').trim();
                                        const time = (row.querySelector('.game-time')?.innerText || '').trim();
                                        const opponent = (row.querySelector('.opp')?.innerText || '').trim();
                                        const venue = (row.querySelector('.venue')?.innerText || '').replace('»', '').trim();
                                        if (date && time && opponent) {
                                            return 'SCHEDULE LEVEL: ' + section.level + '\n' + date + '\n' + time + '\n' + opponent + (venue ? ' at ' + venue : '');
                                        }
                                        return Array.from(row.querySelectorAll('th,td'))
                                            .map(cell => (cell.innerText || '').trim())
                                            .filter(Boolean)
                                            .join('\n');
                                    })).filter(row => datePattern.test(row) && timePattern.test(row));
                                    return rows.length > 0 ? rows.join('\n') : (document.body.innerText || '');
                                })();
                            """.trimIndent()
                            webViewRef?.evaluateJavascript(extractionScript) { raw ->
                                val text = runCatching { JSONArray("[$raw]").getString(0) }.getOrDefault("")
                                acceptText(text)
                                isReading = false
                            }
                        },
                        enabled = isWebsiteReady && !isReading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isReading) "Reading Schedule..." else "Capture Schedule") }
                }
                else -> Button(
                    onClick = { filePicker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Schedule File")
                }
            }

            if (isReading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (parsedEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Selected ${selectedEntryIndices.size} of ${parsedEntries.size} games",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            selectedEntryIndices = if (selectedEntryIndices.size == parsedEntries.size) {
                                emptySet()
                            } else {
                                parsedEntries.indices.toSet()
                            }
                        }
                    ) {
                        Text(if (selectedEntryIndices.size == parsedEntries.size) "Clear all" else "Select all")
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(parsedEntries) { index, entry ->
                        val start = Instant.ofEpochMilli(entry.startMs)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = index in selectedEntryIndices,
                                onCheckedChange = { checked ->
                                    selectedEntryIndices = if (checked) {
                                        selectedEntryIndices + index
                                    } else {
                                        selectedEntryIndices - index
                                    }
                                }
                            )
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = entry.gameLabel.ifBlank { "vs ${entry.opponent}" },
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = start,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!entry.locationName.isNullOrBlank()) {
                                        Text(
                                            text = entry.locationName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onImport(parsedEntries.filterIndexed { index, _ -> index in selectedEntryIndices })
                    },
                    enabled = selectedEntryIndices.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import ${selectedEntryIndices.size} Games")
                }
            }
        }
    }
}

private fun normalizeScheduleUrl(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
}

private fun startScheduleCaptureService(context: Context, resultCode: Int, data: Intent) {
    AppRosterCaptureRepository.setCaptureContent(CaptureContent.SCHEDULE)
    val intent = Intent(context, ScreenCaptureService::class.java).apply {
        action = ScreenCaptureService.ACTION_START
        putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
        putExtra(ScreenCaptureService.EXTRA_DATA, data)
        putExtra(ScreenCaptureService.EXTRA_AUTO_REMIND, false)
        putExtra(ScreenCaptureService.EXTRA_CAPTURE_CONTENT, CaptureContent.SCHEDULE.name)
    }
    context.startForegroundService(intent)
}

private fun stopScheduleCaptureService(context: Context) {
    context.startService(Intent(context, ScreenCaptureService::class.java).apply { action = ScreenCaptureService.ACTION_STOP })
}
