package com.playerid.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.playerid.app.ui.ai.ChatMessage
import com.playerid.app.ui.ai.UIDesignGenerator
import kotlinx.coroutines.launch

/**
 * Chat-style screen that lets developers ask ChatGPT to generate Jetpack Compose
 * UI components following the Spotr design system.
 *
 * Navigate to this screen via the "design_explorer" route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemExplorerScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val generator = remember { UIDesignGenerator(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(!generator.openAIClient.hasApiKey()) }
    var apiKeyInput by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Chat history: pairs of (role, content)
    val chatHistory = remember { mutableStateListOf<ChatMessage>() }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // Scroll to bottom when new message arrives
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { if (generator.openAIClient.hasApiKey()) showApiKeyDialog = false },
            icon = { Icon(Icons.Default.Key, contentDescription = null) },
            title = { Text("OpenAI API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter your OpenAI API key. It will be stored securely on device " +
                                "and never committed to source control.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) {
                            generator.openAIClient.saveApiKey(apiKeyInput.trim())
                            showApiKeyDialog = false
                            apiKeyInput = ""
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                if (generator.openAIClient.hasApiKey()) {
                    TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI UI Designer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Delete, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Key, contentDescription = "Set API key")
                    }
                    if (chatHistory.isNotEmpty()) {
                        IconButton(onClick = { chatHistory.clear() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear history")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Describe the UI component…") },
                        maxLines = 4,
                        enabled = !isLoading
                    )
                    FilledIconButton(
                        onClick = {
                            val requirement = inputText.trim()
                            if (requirement.isBlank() || isLoading) return@FilledIconButton
                            val userMsg = ChatMessage(ChatMessage.Role.USER, requirement)
                            chatHistory.add(userMsg)
                            inputText = ""
                            isLoading = true
                            scope.launch {
                                val result = generator.generate(requirement, chatHistory.dropLast(1))
                                result.fold(
                                    onSuccess = { reply ->
                                        chatHistory.add(ChatMessage(ChatMessage.Role.ASSISTANT, reply))
                                    },
                                    onFailure = { error ->
                                        snackbarMessage = error.message ?: "Request failed"
                                        chatHistory.removeLastOrNull()
                                    }
                                )
                                isLoading = false
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (chatHistory.isEmpty() && !isLoading) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chatHistory) { message ->
                        ChatBubble(
                            message = message,
                            onCopy = { content ->
                                copyToClipboard(context, content)
                                snackbarMessage = "Copied to clipboard"
                            }
                        )
                    }
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generating…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onCopy: (String) -> Unit
) {
    val isUser = message.role == ChatMessage.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (isUser)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val hasCodeBlock = message.content.contains("```")
            if (hasCodeBlock) {
                // Render code blocks with monospace font
                val parts = message.content.split("```")
                parts.forEachIndexed { index, part ->
                    if (index % 2 == 0) {
                        if (part.isNotBlank()) {
                            Text(
                                text = part.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Code block content
                        val codeContent = part.removePrefix("kotlin").trim()
                        SelectionContainer {
                            Text(
                                text = codeContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onCopy(codeContent) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Copy code", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🎨", style = MaterialTheme.typography.displayLarge)
        Text(
            "AI UI Designer",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Describe a component or screen and ChatGPT will generate\n" +
                    "Jetpack Compose code following the Spotr design system.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Examples:",
            style = MaterialTheme.typography.labelLarge
        )
        listOf(
            "Create a player stats card with jersey number and position",
            "Build a team roster list screen with search",
            "Design a score banner overlay for the camera screen"
        ).forEach { example ->
            SuggestionChip(
                onClick = {},
                label = { Text(example, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Compose Code", text))
}
