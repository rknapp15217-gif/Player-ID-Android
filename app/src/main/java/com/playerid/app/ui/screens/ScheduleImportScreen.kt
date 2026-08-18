package com.playerid.app.ui.screens

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.playerid.app.memory.ScheduleImportEntry
import com.playerid.app.memory.parseScheduleCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleImportScreen(
    teamName: String,
    onBack: () -> Unit,
    onImport: (List<ScheduleImportEntry>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var parsedEntries by remember { mutableStateOf<List<ScheduleImportEntry>>(emptyList()) }
    var isReading by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isReading = true
            val entries = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        parseScheduleCsv(reader.readText())
                    } ?: emptyList()
                }.getOrElse { emptyList() }
            }
            parsedEntries = entries
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Schedule") },
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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Upload a schedule CSV like roster import.")
                    Text(
                        text = "Header: date,startTime,endTime,opponent,location,latitude,longitude,gameLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Example date: 2026-07-04, time: 6:30 PM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    filePicker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Schedule File")
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

            if (parsedEntries.isNotEmpty()) {
                Text(
                    text = "Detected ${parsedEntries.size} games",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(parsedEntries) { entry ->
                        val start = Instant.ofEpochMilli(entry.startMs)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
                        Card(
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

                Button(
                    onClick = { onImport(parsedEntries) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Schedule")
                }
            }
        }
    }
}
