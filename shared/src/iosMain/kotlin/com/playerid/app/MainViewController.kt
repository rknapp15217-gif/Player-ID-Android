package com.playerid.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.playerid.app.ui.theme.SpotrLightBackground
import com.playerid.app.ui.theme.SpotrPrimaryBlue
import com.playerid.app.ui.theme.SpotrText
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = SpotrPrimaryBlue,
            background = SpotrLightBackground,
            surface = SpotrLightBackground,
            onBackground = SpotrText,
            onSurface = SpotrText
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PlayerID",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Shared application framework",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}