package com.playerid.app.domain.memory

import com.playerid.app.platform.MediaReference

enum class MemoryShareResult { COMPLETED, CANCELLED }

fun selectedMemoriesForSharing(
    visibleMemories: List<MediaReference>,
    selectedIds: Set<String>
): List<MediaReference> = visibleMemories.filter { it.identifier in selectedIds }.distinctBy { it.identifier }
