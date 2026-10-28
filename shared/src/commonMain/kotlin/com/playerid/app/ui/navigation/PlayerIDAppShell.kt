package com.playerid.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.playerid.app.domain.navigation.AppNavigationState
import com.playerid.app.domain.navigation.AppDestination
import com.playerid.app.ui.theme.PlayerIDLightColorScheme
import com.playerid.app.ui.components.BottomNavItem
import com.playerid.app.ui.components.SpotrBottomNavigationBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerIDAppShell(
    state: AppNavigationState,
    onTabSelected: (Int) -> Unit,
    memoriesLabel: String = "Memories",
    cameraLabel: String = "Camera",
    content: @Composable () -> Unit
) {
    // A landscape phone is too short for the tab bar, so it is hidden; rotate back to portrait to switch tabs.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val landscape = maxWidth > maxHeight
    val density = LocalDensity.current
    var tabBarHeightPx by remember { mutableStateOf(0) }
    val tabBarHeight = if (landscape) 0f else tabBarHeightPx.toFloat()
    Column(Modifier.fillMaxSize()) {
        // The tab bar already fills the bottom of the screen, so content's imePadding must only add the
        // part of the keyboard above it; otherwise sheets lift too far and leave a gap above the keyboard.
        Box(Modifier.weight(1f).consumeWindowInsets(PaddingValues(bottom = with(density) { tabBarHeight.toDp() }))) { content() }
        if (!landscape) MaterialTheme(colorScheme = if (state.destination == AppDestination.Camera) PlayerIDLightColorScheme
            else MaterialTheme.colorScheme) {
            Box(Modifier.onSizeChanged { tabBarHeightPx = it.height }) {
            SpotrBottomNavigationBar(
                items = listOf(
                    BottomNavItem(memoriesLabel, Icons.Filled.PhotoLibrary, "clips"),
                    BottomNavItem(cameraLabel, Icons.Filled.CameraAlt, "camera"),
                    BottomNavItem("Go", Icons.Filled.Search, "go")
                ),
                selectedIndex = state.selectedTabIndex,
                onItemSelected = onTabSelected,
                compact = true
            )
            }
        }
    }
    }
}
