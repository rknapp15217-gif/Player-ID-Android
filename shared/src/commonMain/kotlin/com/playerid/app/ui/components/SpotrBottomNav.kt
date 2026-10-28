package com.playerid.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role

private val CompactNavHeight = 64.dp

@Composable
fun SpotrBottomNavigationBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    compact: Boolean = false
) {
    if (items.any { it.route == "go" }) {
        Box(Modifier.fillMaxWidth().height(80.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)
            ) {}
            Row(Modifier.fillMaxSize().selectableGroup(), verticalAlignment = Alignment.Bottom) {
                val cameraIndex = items.indexOfFirst { it.route == "camera" }
                val goIndex = items.indexOfFirst { it.route == "go" }
                val rightIndices = items.indices.filter { it != cameraIndex && it != goIndex }
                @Composable
                fun MenuItem(index: Int, modifier: Modifier) {
                    val item = items[index]
                    val selected = selectedIndex == index
                    Column(
                        modifier.height(60.dp)
                            .selectable(selected, role = Role.Tab, onClick = { onItemSelected(index) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            Modifier.clip(CircleShape)
                                .background(if (selected) tint.copy(alpha = 0.16f) else Color.Transparent)
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                        ) { Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp)) }
                        Text(
                            item.label, style = MaterialTheme.typography.labelSmall, color = tint,
                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else null
                        )
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (cameraIndex >= 0) MenuItem(cameraIndex, Modifier.fillMaxWidth())
                }
                Box(Modifier.width(68.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    val selected = selectedIndex == goIndex
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = if (selected) 10.dp else 6.dp,
                        modifier = Modifier.size(68.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (selected) 3.dp else 2.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            Modifier.fillMaxSize()
                                .selectable(selected, role = Role.Tab, onClick = { onItemSelected(goIndex) }),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Playloom Go", modifier = Modifier.size(36.dp))
                        }
                    }
                }
                Row(Modifier.weight(1f)) {
                    rightIndices.forEach { index -> MenuItem(index, Modifier.weight(1f)) }
                }
            }
        }
        return
    }
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 56.dp else CompactNavHeight)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = tween(260),
                label = "scale"
            )
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                animationSpec = tween(260),
                label = "iconColor"
            )

            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier.scale(scale),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(if (compact) 32.dp else 40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        iconColor.copy(alpha = 0.16f)
                                    )
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(if (isSelected) 26.dp else 23.dp)
                        )
                    }
                },
                alwaysShowLabel = false,
                label = null,
                selected = isSelected,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = iconColor,
                    selectedTextColor = iconColor,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)