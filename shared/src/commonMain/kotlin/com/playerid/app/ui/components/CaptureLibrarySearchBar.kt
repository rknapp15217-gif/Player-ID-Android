package com.playerid.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler

/**
 * Shares the same entry point for typed and spoken requests without introducing a chat page.
 */
@Composable
fun CaptureLibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onVoiceSearch: (() -> Unit)? = null,
    listening: Boolean = false,
    onSubmit: (String) -> Unit = {},
    voiceError: String? = null,
    onDismissVoiceError: () -> Unit = {}
) {
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    val placeholder = when {
        listening -> "Listening…"
        else -> "Ask, find, create…"
    }
    Column(Modifier.fillMaxWidth()) {
        Surface(
            Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                Modifier.padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicTextField(
                    value = query, onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .semantics { contentDescription = "Ask, find, create" },
                    singleLine = true,
                    enabled = !listening,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { dismissKeyboard(); onSubmit(query) },
                        onDone = { dismissKeyboard(); onSubmit(query) }
                    ),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text(
                                placeholder, style = MaterialTheme.typography.bodyLarge,
                                color = if (listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            innerTextField()
                        }
                    }
                )
                if (query.isNotEmpty()) IconButton(
                    onClick = { onQueryChange("") }, enabled = !listening, modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Clear, "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
                if (onVoiceSearch != null) FilledIconButton(
                    onClick = { dismissKeyboard(); onVoiceSearch() }, modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Mic, if (listening) "Cancel voice search" else "Voice search",
                        modifier = Modifier.size(24.dp))
                }
            }
        }
        if (voiceError != null) {
            Text(voiceError, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            Row {
                if (onVoiceSearch != null) TextButton(onClick = onVoiceSearch) { Text("Retry voice") }
                TextButton(onClick = onDismissVoiceError) { Text("Dismiss") }
            }
        }
    }
}
