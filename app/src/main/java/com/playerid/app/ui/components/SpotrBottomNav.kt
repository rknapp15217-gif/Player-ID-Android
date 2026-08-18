package com.playerid.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val CompactNavHeight = 64.dp

@Composable
fun SpotrBottomNavigationBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(CompactNavHeight)
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
                                    .size(40.dp)
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