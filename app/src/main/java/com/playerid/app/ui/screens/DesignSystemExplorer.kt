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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playerid.app.viewmodels.DesignSystemExplorerViewModel

@Composable
fun DesignSystemExplorer() {
    val factory = remember { DesignSystemExplorerViewModel.Factory() }
    val viewModel: DesignSystemExplorerViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

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
                        value = state.requirements,
                        onValueChange = viewModel::updateRequirements,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::analyzeDesignSystem) {
                            Icon(Icons.Default.Insights, contentDescription = "Analyze")
                            Spacer(Modifier.width(6.dp))
                            Text("Analyze Design System")
                        }
                        Button(
                            enabled = !state.isGenerating,
                            onClick = viewModel::generateUi
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Generate")
                            Spacer(Modifier.width(6.dp))
                            Text("Generate UI")
                        }
                    }
                    if (state.isGenerating) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
            }

            if (state.analysisSummary.isNotBlank()) {
                SectionCard(title = "Design Snapshot", content = state.analysisSummary)
            }

            if (state.generatedCode.isNotBlank()) {
                SectionCard(title = "Generated Compose Code", content = state.generatedCode)
                Button(onClick = viewModel::validateGeneratedUi) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Validate")
                    Spacer(Modifier.width(6.dp))
                    Text("Run Design Validation")
                }
            }

            if (state.validationSummary.isNotBlank()) {
                SectionCard(title = "Validation Output", content = state.validationSummary)
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
