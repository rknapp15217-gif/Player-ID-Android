package com.playerid.app.domain.team

enum class MemorySeasonIntent {
    CURRENT,
    PREVIOUS
}

enum class MemoryResultSelection {
    FIRST,
    LAST
}

data class MemorySearchQuery(
    val momentTag: String? = null,
    val seasonIntent: MemorySeasonIntent? = null,
    val resultSelection: MemoryResultSelection? = null,
    val terms: List<String> = emptyList()
)

fun parseMemorySearchQuery(query: String): MemorySearchQuery {
    val words = query
        .lowercase()
        .replace("’s", " ")
        .replace("'s", " ")
        .replace(Regex("[^a-z0-9#]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)

    val hasSeasonIntent = "season" in words
    val seasonIntent = when {
        !hasSeasonIntent -> null
        "last" in words || "previous" in words -> MemorySeasonIntent.PREVIOUS
        "this" in words || "current" in words -> MemorySeasonIntent.CURRENT
        else -> null
    }
    val momentTag = detectMemoryMomentTag(words)
    val intentWords = buildSet {
        if (hasSeasonIntent) addAll(setOf("season", "last", "previous", "this", "current"))
        if (momentTag != null) addAll(memoryMomentTagWords(momentTag))
    }
    val resultSelection = when {
        hasSeasonIntent -> null
        "first" in words || "earliest" in words -> MemoryResultSelection.FIRST
        "last" in words || "latest" in words || "newest" in words -> MemoryResultSelection.LAST
        else -> null
    }
    val terms = words.filter { it !in MEMORY_SEARCH_FILLER_WORDS && it !in intentWords }

    return MemorySearchQuery(
        momentTag = momentTag,
        seasonIntent = seasonIntent,
        resultSelection = resultSelection,
        terms = terms
    )
}

private fun detectMemoryMomentTag(words: List<String>): String? = when {
    words.any { it in setOf("goal", "goals", "score", "scores", "scored", "scoring") } -> "Score"
    words.any { it in setOf("assist", "assists", "assisted") } -> "Assist"
    words.any { it in setOf("save", "saves", "saved") } -> "Save"
    words.containsAll(listOf("defensive", "stop")) || words.containsAll(listOf("defensive", "stops")) -> "Defensive Stop"
    words.any { it in setOf("faceoff", "faceoffs") } -> "Faceoff Win"
    words.containsAll(listOf("ground", "ball")) || words.containsAll(listOf("ground", "balls")) -> "Ground Ball"
    else -> null
}

private fun memoryMomentTagWords(tag: String): Set<String> = when (tag) {
    "Score" -> setOf("goal", "goals", "score", "scores", "scored", "scoring")
    "Assist" -> setOf("assist", "assists", "assisted")
    "Save" -> setOf("save", "saves", "saved")
    "Defensive Stop" -> setOf("defensive", "stop", "stops")
    "Faceoff Win" -> setOf("faceoff", "faceoffs", "win", "wins", "won")
    "Ground Ball" -> setOf("ground", "ball", "balls")
    else -> emptySet()
}

private val MEMORY_SEARCH_FILLER_WORDS = setOf(
    "a", "all", "an", "and", "any", "against", "at", "by", "clip", "clips",
    "build", "create", "did", "during", "every", "find", "for", "from", "get", "give", "in", "made", "make", "me",
    "memory", "memories", "moment", "moments", "my", "of", "on", "play", "plays", "please", "recorded",
    "real", "reel", "reels", "show", "that", "the", "to", "versus", "video", "videos", "vs", "was", "were",
    "where", "with", "first", "last", "earliest", "latest", "newest"
)