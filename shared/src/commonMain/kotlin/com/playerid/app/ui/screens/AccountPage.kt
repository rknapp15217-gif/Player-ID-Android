package com.playerid.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.auth.EmailAuth
import com.playerid.app.ui.dialogs.rememberDialogKeyboardDismissHandler
import kotlinx.coroutines.launch

@Composable
fun AccountPage(auth: EmailAuth, onBack: () -> Unit) {
    val state by auth.state.collectAsState()
    val scope = rememberCoroutineScope()
    val dismissKeyboard = rememberDialogKeyboardDismissHandler()
    var email by remember { mutableStateOf(state.pendingEmail.orEmpty()) }
    var code by remember { mutableStateOf("") }
    val runAction: (suspend () -> Unit) -> Unit = { action ->
        dismissKeyboard()
        scope.launch { action() }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Playloom account", style = MaterialTheme.typography.headlineMedium)
            Text("Sign in with an email code. Your teams, contacts and media remain on this device; cloud sync is not enabled yet.")
            val user = state.user
            if (user != null) {
                Text(user.email, style = MaterialTheme.typography.titleMedium)
            } else {
                OutlinedTextField(
                    value = email, onValueChange = { email = it; code = "" },
                    label = { Text("Email") },
                    enabled = !state.loading && state.pendingEmail == null, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = !state.loading && email.isNotBlank(),
                    onClick = { runAction { auth.requestCode(email) } }
                ) { Text(if (state.pendingEmail == null) "Send code" else "Send new code") }
                state.pendingEmail?.let { address ->
                    TextButton(enabled = !state.loading, onClick = { auth.changeEmail(); code = "" }) {
                        Text("Use another email")
                    }
                    Text("Enter the code sent to $address.")
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("Verification code") },
                        enabled = !state.loading, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(enabled = !state.loading && code.isNotBlank(),
                        onClick = { runAction { auth.verifyCode(code) } }) { Text("Verify and sign in") }
                }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let { Text(it) }
            state.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(enabled = !state.loading, onClick = { runAction { auth.restore() } }) {
                    Text("Retry saved sign-in")
                }
            }
            if (user != null || state.error != null) {
                OutlinedButton(enabled = !state.loading, onClick = {
                    runAction { auth.signOut(); code = "" }
                }) { Text("Sign out on this device") }
            }
        }
    }
}
