package com.playerid.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.playerid.app.AppNavigationCallback
import com.playerid.app.MainActivity
import com.playerid.app.capture.AppRosterCaptureRepository
import com.playerid.app.capture.CaptureContent
import com.playerid.app.capture.RosterApp
import com.playerid.app.capture.RosterAppDetector
import com.playerid.app.capture.ScreenCaptureService
import com.playerid.app.roster.extractRosterCandidates
import com.playerid.app.viewmodels.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactImportScreen(
    teamName: String,
    source: String,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val capturedLines by AppRosterCaptureRepository.contactLines.collectAsState()
    val capturePlayerNames by AppRosterCaptureRepository.contactPlayerNames.collectAsState()
    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    val teamPlayers = remember(allPlayers, teamName) {
        allPlayers.filter { it.team == teamName }.sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.name }))
    }
    var contacts by remember { mutableStateOf<List<SelectedContact>>(emptyList()) }
    var selectedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var installedApps by remember { mutableStateOf<List<RosterApp>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<RosterApp?>(null) }
    var isWaitingForApp by remember { mutableStateOf(false) }
    var isReading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun acceptLines(lines: List<String>) {
        contacts = parseTeamContacts(
            lines = lines,
            playerIdsByName = teamPlayers.associate { it.name.lowercase() to it.id }
        )
        selectedIndices = contacts.indices.toSet()
        errorMessage = if (contacts.isEmpty()) {
            "No contacts detected. Include a name with an email address or phone number."
        } else null
    }

    val screenshotPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isReading = true
            val lines = withContext(Dispatchers.Default) {
                runCatching { extractRosterCandidates(context, uri).rawLines }.getOrDefault(emptyList())
            }
            acceptLines(lines)
            isReading = false
        }
    }
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            AppRosterCaptureRepository.cacheProjection(result.resultCode, data)
            startContactCaptureService(context, result.resultCode, data)
            isWaitingForApp = true
            selectedApp?.let { app ->
                if (!RosterAppDetector.launchApp(context, app.packageName)) {
                    errorMessage = "Could not open ${app.name}"
                }
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
            AppRosterCaptureRepository.setCaptureContent(CaptureContent.CONTACTS)
            installedApps = withContext(Dispatchers.IO) { RosterAppDetector.getInstalledRosterApps(context) }
        }
    }
    LaunchedEffect(capturedLines) {
        if (source == "app" && capturedLines.isNotEmpty()) acceptLines(capturedLines)
    }
    BackHandler(source == "app") {
        context.stopService(Intent(context, ScreenCaptureService::class.java))
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Team: $teamName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (source == "screenshot") {
                OutlinedButton(onClick = { screenshotPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Contacts Screenshot")
                }
            } else if (source == "app" && capturePlayerNames.isEmpty() && contacts.isEmpty()) {
                Text("Select players", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose each player whose TeamSnap contact page you want to capture.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (teamPlayers.isEmpty()) {
                    Text("Import the roster before collecting player contacts.", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(teamPlayers) { _, player ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = player.id in selectedPlayerIds,
                                    onCheckedChange = { checked ->
                                        selectedPlayerIds = if (checked) selectedPlayerIds + player.id else selectedPlayerIds - player.id
                                    }
                                )
                                Column {
                                    Text(player.name, fontWeight = FontWeight.Medium)
                                    Text("#${player.number}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            AppRosterCaptureRepository.startContactCapture(
                                teamPlayers.filter { it.id in selectedPlayerIds }.map { it.name }
                            )
                        },
                        enabled = selectedPlayerIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue with ${selectedPlayerIds.size} Players") }
                }
            } else if (source == "app" && !isWaitingForApp && contacts.isEmpty()) {
                Text(
                    "Selected ${capturePlayerNames.size} players. Choose the team app that contains their contact pages.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (installedApps.isEmpty()) {
                    Text("No supported team apps found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                installedApps.forEach { app ->
                    OutlinedButton(
                        onClick = {
                            if (!Settings.canDrawOverlays(context)) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                errorMessage = "Enable Display over other apps, then select the app again."
                            } else {
                                if (capturePlayerNames.isEmpty()) return@OutlinedButton
                                selectedApp = app
                                val cached = AppRosterCaptureRepository.getCachedProjection()
                                if (cached != null) {
                                    startContactCaptureService(context, cached.first, cached.second)
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
                        Spacer(Modifier.width(8.dp))
                        Text(app.name)
                    }
                }
            }
            if (isWaitingForApp && contacts.isEmpty()) {
                Text("Capture the contacts screen in the selected app, then tap Done.")
            }
            if (isReading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (contacts.isNotEmpty()) {
                Text("Selected ${selectedIndices.size} of ${contacts.size} contacts", fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(contacts) { index, contact ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = index in selectedIndices,
                                onCheckedChange = { checked ->
                                    selectedIndices = if (checked) selectedIndices + index else selectedIndices - index
                                }
                            )
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(contact.displayName, fontWeight = FontWeight.Medium)
                                    contact.phoneNumber?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                    contact.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        saveTeamContacts(context, teamName, contacts.filterIndexed { index, _ -> index in selectedIndices })
                        AppRosterCaptureRepository.clearContacts()
                        onBack()
                    },
                    enabled = selectedIndices.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import ${selectedIndices.size} Contacts") }
            }
        }
    }
}

internal fun parseTeamContacts(
    lines: List<String>,
    playerIdsByName: Map<String, String> = emptyMap()
): List<SelectedContact> {
    val normalized = lines.map { it.replace(Regex("\\s+"), " ").trim() }.filter(String::isNotBlank)
    val emailPattern = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    val phonePattern = Regex("(?:\\+?1[ .-]?)?(?:\\(?\\d{3}\\)?[ .-]?)\\d{3}[ .-]?\\d{4}")
    val contacts = mutableListOf<SelectedContact>()
    var currentName: String? = null
    var currentPlayerId: String? = null

    normalized.forEach { line ->
        if (line.startsWith("__PLAYER__:")) {
            currentName = line.substringAfter("__PLAYER__:").trim().ifBlank { null }
            currentPlayerId = currentName?.lowercase()?.let(playerIdsByName::get)
            return@forEach
        }
        val email = emailPattern.find(line)?.value
        val phone = phonePattern.find(line)?.value
        if (email == null && phone == null) {
            if (currentName == null && line.length >= 2 && line.any(Char::isLetter) && line.length <= 80) {
                currentName = line
            }
            return@forEach
        }
        val inlineName = line
            .replace(email.orEmpty(), "")
            .replace(phone.orEmpty(), "")
            .replace(Regex("(?i)\\b(email|phone|mobile|cell|contact)\\b[: -]*"), "")
            .trim(' ', '-', '|', ',', ':')
        val name = inlineName.takeIf { candidate -> candidate.length >= 2 && candidate.any(Char::isLetter) }
            ?: currentName
            ?: "Team contact"
        currentName = name
        val existingIndex = contacts.indexOfLast { existing ->
            existing.displayName.equals(name, ignoreCase = true) ||
                (email != null && existing.email.equals(email, ignoreCase = true)) ||
                (phone != null && existing.phoneNumber == phone)
        }
        if (existingIndex >= 0) {
            val existing = contacts[existingIndex]
            contacts[existingIndex] = existing.copy(
                phoneNumber = existing.phoneNumber ?: phone,
                email = existing.email ?: email,
                linkedPlayerIds = existing.linkedPlayerIds + listOfNotNull(currentPlayerId)
            )
        } else {
            contacts += SelectedContact(name, phone, email, setOfNotNull(currentPlayerId))
        }
    }
    return contacts.distinctBy {
        listOf(it.displayName, it.phoneNumber.orEmpty(), it.email.orEmpty()).joinToString("|").lowercase()
    }
}

private fun startContactCaptureService(context: Context, resultCode: Int, data: Intent) {
    AppRosterCaptureRepository.setCaptureContent(CaptureContent.CONTACTS)
    val intent = Intent(context, ScreenCaptureService::class.java).apply {
        action = ScreenCaptureService.ACTION_START
        putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
        putExtra(ScreenCaptureService.EXTRA_DATA, data)
        putExtra(ScreenCaptureService.EXTRA_AUTO_REMIND, false)
        putExtra(ScreenCaptureService.EXTRA_CAPTURE_CONTENT, CaptureContent.CONTACTS.name)
    }
    context.startForegroundService(intent)
}