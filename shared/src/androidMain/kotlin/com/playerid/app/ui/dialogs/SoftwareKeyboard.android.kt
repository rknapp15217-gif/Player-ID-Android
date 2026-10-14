package com.playerid.app.ui.dialogs

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberSoftwareKeyboardDismissHandler(): () -> Unit {
    val dialogView = LocalView.current
    return remember(dialogView) {
        {
            (dialogView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(dialogView.windowToken, 0)
        }
    }
}
