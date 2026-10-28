package com.playerid.app.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.endEditing

@Composable
@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
internal actual fun rememberSoftwareKeyboardDismissHandler(): () -> Unit {
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(keyboardController) {
        {
            keyboardController?.hide()
            UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .flatMap { it.windows.filterIsInstance<UIWindow>() }
                .forEach { it.endEditing(true) }
        }
    }
}
