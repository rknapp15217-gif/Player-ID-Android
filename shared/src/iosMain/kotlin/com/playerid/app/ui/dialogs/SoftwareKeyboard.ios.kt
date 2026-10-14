package com.playerid.app.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberSoftwareKeyboardDismissHandler(): () -> Unit = remember { {} }
