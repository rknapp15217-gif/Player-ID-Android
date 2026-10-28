package com.playerid.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

internal class MemoryFilterScrollGestures(
    private val revealDistance: Float,
    private val collapseDistance: Float
) {
    private var distance = 0f

    fun drag(delta: Float): Boolean? {
        if (delta == 0f) return null
        if (delta > 0f && distance < 0f || delta < 0f && distance > 0f) distance = 0f
        distance += delta
        return when {
            distance <= -collapseDistance -> false
            distance >= revealDistance -> true
            else -> null
        }
    }

    fun endGesture() {
        distance = 0f
    }
}

@Composable
fun rememberMemoryFilterScroll(
    gridState: LazyGridState,
    onExpandedChange: (Boolean) -> Unit
): NestedScrollConnection {
    val onChange = rememberUpdatedState(onExpandedChange)
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        }.collect { atTop ->
            if (atTop) onChange.value(true)
        }
    }
    val density = LocalDensity.current
    return remember(density) {
        val gestures = MemoryFilterScrollGestures(
            with(density) { 140.dp.toPx() }, with(density) { 8.dp.toPx() }
        )
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag) {
                    gestures.drag(available.y)?.let { onChange.value(it) }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                gestures.endGesture()
                return Velocity.Zero
            }
        }
    }
}
