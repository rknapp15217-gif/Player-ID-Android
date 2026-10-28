package com.playerid.app.domain.memory

import com.playerid.app.domain.team.TeamWorkspaceSnapshot
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference

/** A snapshot of matching original videos, never a persisted reel or an exported file. */
fun temporaryMemoryPlaylist(
    matches: List<MediaReference>,
    snapshot: TeamWorkspaceSnapshot
): List<MediaReference> = chronologicalReelSources(
    matches.filter { it.kind == MediaKind.VIDEO && captureFileKey(it) !in snapshot.captureReels }
        .distinctBy(::captureFileKey),
    snapshot
)
