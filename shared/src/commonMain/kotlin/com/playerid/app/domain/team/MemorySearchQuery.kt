package com.playerid.app.domain.team

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Relative dates use the device's calendar day, never a rolling 24-hour interval. */
fun memorySearchDate(
    query: String,
    today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
): LocalDate? {
    val words = query.lowercase().split(Regex("[^a-z]+"))
    return when {
        "yesterday" in words -> today.minus(DatePeriod(days = 1))
        "today" in words -> today
        else -> null
    }
}

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
    val terms: List<String> = emptyList(),
    val captureDate: String? = null,
    val dateFrom: String? = null,
    val dateThrough: String? = null,
    val mediaType: String? = null,
    val favoritesOnly: Boolean = false,
    val resultLimit: Int = 1,
    val seasonYear: String? = null
)

fun parseMemorySearchQuery(query: String): MemorySearchQuery =
    parseMemorySearchQuery(query, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)

fun parseMemorySearchQuery(query: String, today: LocalDate): MemorySearchQuery {
    val namedSeason = Regex("\\b(?:season\\s+((?:19|20)[0-9]{2})|((?:19|20)[0-9]{2})\\s+season)\\b",
        RegexOption.IGNORE_CASE).find(query)
    val seasonYear = namedSeason?.groupValues?.drop(1)?.firstOrNull(String::isNotEmpty)
    val dateRequest = parseMemoryDateRequest(namedSeason?.let { query.replaceRange(it.range, " ") } ?: query, today)
    val limitRequest = Regex("\\b(first|earliest|last|latest|newest)\\s+([1-9][0-9]{0,2})\\b").find(dateRequest.remaining)
    val remaining = if (limitRequest == null) dateRequest.remaining else dateRequest.remaining.replaceRange(
        limitRequest.range, limitRequest.groupValues[1]
    )
    val words = remaining
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
    val opponentIndex = words.indexOfFirst { it in setOf("against", "vs", "versus") }
    val actionWords = if (opponentIndex >= 0) words.take(opponentIndex) else words
    val momentTag = detectMemoryMomentTag(actionWords)
    val mediaType = when {
        words.any { it in setOf("photo", "photos", "picture", "pictures") } -> "photo"
        words.any { it in setOf("video", "videos") } -> "video"
        words.any { it in setOf("reel", "reels") } && words.none { it in REEL_COMMAND_VERBS } -> "reel"
        else -> null
    }
    val favoritesOnly = words.any { it in setOf("favorite", "favorites", "favourite", "favourites", "starred") }
    val intentWords = buildSet {
        if (hasSeasonIntent) addAll(setOf("season", "last", "previous", "this", "current"))
        if (momentTag != null) addAll(memoryMomentTagWords(momentTag))
    }
    val selectionWords = remaining.lowercase()
        .replace(Regex("\\b(last|previous|this|current)\\s+season\\b"), " ")
        .split(Regex("[^a-z]+"))
    val resultSelection = when {
        "first" in selectionWords || "earliest" in selectionWords -> MemoryResultSelection.FIRST
        "last" in selectionWords || "latest" in selectionWords || "newest" in selectionWords -> MemoryResultSelection.LAST
        else -> null
    }
    val terms = words.filterIndexed { index, word ->
        word !in MEMORY_SEARCH_FILLER_WORDS &&
            (word !in intentWords || (opponentIndex >= 0 && index > opponentIndex))
    }

    return MemorySearchQuery(
        momentTag = momentTag,
        seasonIntent = seasonIntent,
        resultSelection = resultSelection,
        terms = terms,
        captureDate = dateRequest.start?.takeIf { it == dateRequest.end },
        dateFrom = dateRequest.start,
        dateThrough = dateRequest.end,
        mediaType = mediaType,
        favoritesOnly = favoritesOnly,
        resultLimit = limitRequest?.groupValues?.get(2)?.toInt() ?: 1,
        seasonYear = seasonYear ?: when (seasonIntent) {
            MemorySeasonIntent.CURRENT -> today.year.toString()
            MemorySeasonIntent.PREVIOUS -> (today.year - 1).toString()
            null -> null
        }
    )
}

internal fun detectMemoryMomentTag(words: List<String>): String? = when {
    words.any { it in setOf("goal", "goals", "score", "scores", "scored", "scoring") } -> "Score"
    words.any { it in setOf("assist", "assists", "assisted") } -> "Assist"
    words.any { it in setOf("save", "saves", "saved") } -> "Save"
    words.containsAll(listOf("defensive", "stop")) || words.containsAll(listOf("defensive", "stops")) -> "Defensive Stop"
    words.any { it in setOf("faceoff", "faceoffs") } || saysFaceOff(words) -> "Faceoff Win"
    words.containsAll(listOf("ground", "ball")) || words.containsAll(listOf("ground", "balls")) -> "Ground Ball"
    else -> null
}

internal fun memoryMomentTagWords(tag: String): Set<String> = when (tag) {
    "Score" -> setOf("goal", "goals", "score", "scores", "scored", "scoring")
    "Assist" -> setOf("assist", "assists", "assisted")
    "Save" -> setOf("save", "saves", "saved")
    "Defensive Stop" -> setOf("defensive", "stop", "stops")
    "Faceoff Win" -> setOf("faceoff", "faceoffs", "face", "off", "offs", "win", "wins", "won")
    "Ground Ball" -> setOf("ground", "ball", "balls")
    else -> emptySet()
}

// Dictation often writes "face off" or "face-off" (split into "face", "off") instead of "faceoff".
private fun saysFaceOff(words: List<String>): Boolean =
    words.zipWithNext().any { (first, second) -> first == "face" && (second == "off" || second == "offs") }

/** Maps saved moment tags to the same canonical tags spoken/typed searches detect (Android's canonicalMomentTag). */
fun canonicalMemoryMomentTag(tag: String?): String? = when (tag?.trim()?.lowercase()) {
    "score", "goal", "winning goal" -> "Score"
    "assist" -> "Assist"
    "save" -> "Save"
    "defensive stop", "big play" -> "Defensive Stop"
    "faceoff win", "face off win", "face-off win", "win" -> "Faceoff Win"
    "ground ball" -> "Ground Ball"
    else -> null
}

private val MEMORY_SEARCH_FILLER_WORDS = setOf(
    "a", "all", "an", "and", "any", "against", "at", "by", "clip", "clips",
    "build", "create", "generate", "compile", "did", "during", "every", "find", "for", "from", "get", "give", "in", "made", "make", "me",
    "memory", "memories", "moment", "moments", "my", "of", "on", "play", "plays", "please", "recorded",
    "real", "reel", "reels", "show", "that", "the", "to", "versus", "video", "videos", "vs", "was", "were",
    "where", "with", "first", "last", "earliest", "latest", "newest", "recent",
    "can", "could", "would", "you", "i", "want", "like", "put", "together",
    "taken", "only", "just", "photo", "photos", "picture", "pictures",
    "favorite", "favorites", "favourite", "favourites", "starred"
)
/** A spoken or typed "make/create/build a reel of …" request from the Memories action bar. */
data class MemoryReelCommand(val title: String, val searchQuery: String)

private val REEL_COMMAND_VERBS = setOf("make", "create", "build", "generate", "compile")
private val REEL_COMMAND_NOUNS = setOf("reel", "reels", "real")
private val REEL_COMMAND_LEAD_WORDS = setOf("please", "me", "a", "an", "new", "of", "from", "for", "with") +
    REEL_COMMAND_VERBS + REEL_COMMAND_NOUNS

/**
 * Detects Android's reel command (a make/create/build word plus reel/reels/real). The words after the command
 * ("all scores against Penn Trafford") pick the memories and become the reel title, as on Android.
 */
fun parseMemoryReelCommand(text: String): MemoryReelCommand? {
    val tokens = text.trim().split(' ', '\t', '\n').filter(String::isNotBlank)
    fun bare(token: String) = token.lowercase().filter { it.isLetterOrDigit() }
    val words = tokens.map(::bare)
    if (words.none { it in REEL_COMMAND_VERBS } || words.none { it in REEL_COMMAND_NOUNS }) return null
    val nounIndex = words.indexOfFirst { it in REEL_COMMAND_NOUNS }
    val rest = tokens.drop(nounIndex + 1).dropWhile { bare(it) in REEL_COMMAND_LEAD_WORDS }
        .joinToString(" ").trim { !it.isLetterOrDigit() && it != '#' && it != '\'' && it != '’' }
    return MemoryReelCommand(
        title = rest.replaceFirstChar { it.uppercaseChar() }.ifBlank { "Voice Reel" },
        searchQuery = rest
    )
}
