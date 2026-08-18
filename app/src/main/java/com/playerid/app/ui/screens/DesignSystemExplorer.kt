package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playerid.app.ui.ai.DesignSystemAnalyzer
import com.playerid.app.ui.ai.DesignValidator
import com.playerid.app.ui.ai.OpenAIClient
import com.playerid.app.ui.ai.UIDesignGenerator
import kotlinx.coroutines.launch

@Composable
fun DesignSystemExplorer() {
    val analyzer = remember { DesignSystemAnalyzer() }
    val validator = remember { DesignValidator() }
    val generator = remember { UIDesignGenerator(OpenAIClient()) }
    val scope = rememberCoroutineScope()

    var requirements by remember { mutableStateOf("Create a player profile screen with stats and recent clips") }
    var generatedCode by remember { mutableStateOf("") }
    var analysisSummary by remember { mutableStateOf("") }
    var validationSummary by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Design System Explorer") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("New Screen Requirements", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = requirements,
                        onValueChange = { requirements = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val snapshot = analyzer.defaultSnapshot()
                            analysisSummary = buildString {
                                appendLine("Colors: ${snapshot.colorTokens.joinToString()}")
                                appendLine("Typography: ${snapshot.typographyTokens.joinToString()}")
                                appendLine("Components: ${snapshot.spotrComponents.joinToString()}")
                            }
                        }) {
                            Icon(Icons.Default.Insights, contentDescription = "Analyze")
                            Spacer(Modifier.width(6.dp))
                            Text("Analyze Design System")
                        }
                        Button(
                            enabled = !isGenerating,
                            onClick = {
                                isGenerating = true
                                scope.launch {
                                    val result = generator.generateComposeScreen(requirements)
                                    generatedCode = result.fold(
                                        onSuccess = { it.composeCode },
                                        onFailure = { "Generation failed: ${it.message}" }
                                    )
                                    isGenerating = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Generate")
                            Spacer(Modifier.width(6.dp))
                            Text("Generate UI")
                        }
                    }
                    if (isGenerating) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
            }

            if (analysisSummary.isNotBlank()) {
                SectionCard(title = "Design Snapshot", content = analysisSummary)
            }

            if (generatedCode.isNotBlank()) {
                SectionCard(title = "Generated Compose Code", content = generatedCode)
                Button(onClick = {
                    val result = validator.validate(generatedCode, analyzer.defaultSnapshot())
                    validationSummary = buildString {
                        appendLine("Valid: ${result.isValid}")
                        if (result.warnings.isNotEmpty()) {
                            appendLine("Warnings:")
                            result.warnings.forEach { appendLine("• $it") }
                        }
                        if (result.suggestions.isNotEmpty()) {
                            appendLine("Suggestions:")
                            result.suggestions.forEach { appendLine("• $it") }
                        }
                    }
                }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Validate")
                    Spacer(Modifier.width(6.dp))
                    Text("Run Design Validation")
                }
            }

            if (validationSummary.isNotBlank()) {
                SectionCard(title = "Validation Output", content = validationSummary)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Divider()
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
