package com.playerid.app.domain.memory

import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.domain.team.TeamWorkspaceSnapshot
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.serialization.Serializable

@Serializable
data class MemoryReel(val name: String, val sourceFileKeys: List<String>)

/** Unknown capture times go last, retaining their original order. */
fun chronologicalReelSources(
    sources: List<MediaReference>,
    snapshot: TeamWorkspaceSnapshot
): List<MediaReference> = sources.sortedBy { media ->
    val key = captureFileKey(media)
    key.substringBefore('-').takeIf { it.length == 13 && it.all(Char::isDigit) }?.toLongOrNull()
        ?: snapshot.captureOrganizations[key]?.gameId?.let { id ->
            snapshot.games.firstOrNull { it.id == id }?.scheduledStartMs
        } ?: Long.MAX_VALUE
}

fun validateReelSelection(name: String, sources: List<MediaReference>) {
    require(name.trim().isNotEmpty()) { "Enter a reel name." }
    require(sources.size >= 2) { "Choose at least two videos for a reel." }
    require(sources.all { it.kind == MediaKind.VIDEO }) { "Reels can contain videos only." }
    require(sources.map(::captureFileKey).distinct().size == sources.size) {
        "A video can appear only once in this reel."
    }
}

fun moveReelSource(sources: List<MediaReference>, index: Int, destination: Int): List<MediaReference> {
    require(index in sources.indices && destination in sources.indices) { "This reel position is invalid." }
    return sources.toMutableList().apply { add(destination, removeAt(index)) }.toList()
}

/** Length of the title card that opens every reel, matching Android's 1.6 s intro. */
const val REEL_INTRO_MILLIS = 1_600L

/** Wraps a reel title for its intro card like Android: words packed into lines of at most 18 characters, 3 lines max. */
fun reelIntroTitleLines(title: String, maxCharsPerLine: Int = 18, maxLines: Int = 3): List<String> {
    val lines = mutableListOf<String>()
    var current = ""
    title.trim().ifBlank { "PlayerID Reel" }.split(Regex("\\s+")).forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (candidate.length <= maxCharsPerLine) current = candidate else {
            if (current.isNotEmpty()) lines += current
            current = word
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines.take(maxLines)
}
