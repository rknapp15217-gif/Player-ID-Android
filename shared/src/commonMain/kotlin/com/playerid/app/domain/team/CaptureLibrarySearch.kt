package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun searchCaptureLibrary(
    captures: List<MediaReference>,
    snapshot: TeamWorkspaceSnapshot,
    query: String,
    today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    interpretation: AskPlayloomQuery? = null,
    reelsOnly: Boolean? = null
): List<MediaReference> {
    if (interpretation?.issue != null) return emptyList()
    val request = interpretation?.filters ?: parseMemorySearchQuery(query, today)
    // Like Android, a moment phrase ("face off wins", "goals", "saves") matches the canonical moment tag, so
    // dictation's spelling ("face-off", "Face Off") doesn't matter. Remaining words must appear in the details.
    val momentTag = request.momentTag
    val spokenMomentWords = captureSearchTerms(query).filter { it in momentTag?.let(::memoryMomentTagWords).orEmpty() }
    val terms = request.terms
    val players = snapshot.players.associateBy { it.id }
    val seasons = snapshot.seasons.associateBy { it.id }
    val games = snapshot.games.associateBy { it.id }
    val knownOpponents = (snapshot.games.map { it.opponentName } +
        snapshot.captureOrganizations.values.mapNotNull { it.opponentName }).distinctBy(::opponentKey)
    val seasonYear = request.seasonYear?.toIntOrNull()
        ?: request.seasonIntent?.let { today.year - if (it == MemorySeasonIntent.PREVIOUS) 1 else 0 }
    val matches = captures.filter { media ->
        val date = captureDate(media, snapshot, timeZone)
        if (request.dateFrom != null && (date == null || date.toString() < request.dateFrom ||
                date.toString() > request.dateThrough.orEmpty())) return@filter false
        val key = captureFileKey(media)
        if (reelsOnly != null && (key in snapshot.captureReels) != reelsOnly) return@filter false
        if (request.mediaType == "photo" && media.kind != MediaKind.IMAGE) return@filter false
        if (request.mediaType == "video" && media.kind != MediaKind.VIDEO) return@filter false
        if (request.mediaType == "reel" && key !in snapshot.captureReels) return@filter false
        if (request.favoritesOnly && key !in snapshot.captureFavorites) return@filter false
        val reel = snapshot.captureReels[key]
        val sourceKeys = if (key in snapshot.captureOrganizations) listOf(key)
            else reel?.sourceFileKeys ?: listOf(key)
        val identityKeys = if (interpretation == null) sourceKeys else sourceKeys.filter { sourceKey ->
                val assignment = snapshot.captureOrganizations[sourceKey]
                interpretation.matchesAssignment(assignment?.playerId, players[assignment?.playerId]?.name,
                    assignment?.teamName, assignment?.let(snapshot::captureOpponentName))
            }
        val keys = if (interpretation != null && seasonYear != null) identityKeys.filter { sourceKey ->
            snapshot.captureSeasonYearForKey(sourceKey, timeZone) == seasonYear.toString()
        } else identityKeys
        if (keys.isEmpty()) return@filter false
        if (seasonYear != null && (interpretation != null || seasons.isNotEmpty()) && keys.none { sourceKey ->
                snapshot.captureSeasonYearForKey(sourceKey, timeZone) == seasonYear.toString()
            }) return@filter false
        val text = normalizeCaptureSearchText((listOfNotNull(reel?.name, reel?.let { "Reel" }, snapshot.captureMomentTags[key],
            snapshot.captureNotes[key], if (key in snapshot.captureFavorites) "Favorite" else null,
            // Year captured/created, so "show me all the reels for 2026" works.
            date?.year?.toString()) +
            keys.map { fileKey ->
                val details = snapshot.captureOrganizations[fileKey]
                val player = players[details?.playerId]
                val game = games[details?.gameId]
                listOfNotNull(
                    details?.teamName ?: "Unassigned",
                    player?.name,
                    player?.number?.let { "#$it" },
                    seasons[details?.seasonId]?.seasonLabel,
                    (game?.opponentName ?: details?.opponentName)?.let { opponent ->
                        opponentSearchName(opponent, knownOpponents)
                    },
                    game?.gameLabel,
                    snapshot.captureMomentTags[fileKey],
                    if (media.kind == MediaKind.VIDEO) "Video" else "Photo"
                ).joinToString(" ")
            }).joinToString(" "))
        val compactText = text.replace(" ", "")
        val matches: (String) -> Boolean = { term ->
            text.contains(term) || compactText.contains(term) ||
                (term.length > 3 && term.endsWith("s") && compactText.contains(term.dropLast(1)))
        }
        val momentKeys = if (interpretation == null) listOf(key) + keys else keys
        val momentMatches = momentTag == null ||
            momentKeys.any { canonicalMemoryMomentTag(snapshot.captureMomentTags[it]) == momentTag } ||
            (interpretation == null && spokenMomentWords.all(matches))
        momentMatches && terms.all(matches)
    }
    val dated = matches.mapNotNull { media ->
        captureDate(media, snapshot, timeZone)?.let { date -> Triple(media, date, captureFileKey(media)) }
    }
    if (matches.size <= 1) return matches
    return when (request.resultSelection) {
        MemoryResultSelection.FIRST -> dated.sortedWith(compareBy({ it.second }, { it.third })).take(request.resultLimit).map { it.first }
        MemoryResultSelection.LAST -> dated.sortedWith(compareByDescending<Triple<MediaReference, LocalDate, String>> { it.second }
            .thenByDescending { it.third }).take(request.resultLimit).map { it.first }
        null -> matches
    }
}

// Plain character checks rather than \p{L} regex classes, which Kotlin/Native's regex engine rejects.
private fun captureSearchWords(text: String): List<String> {
    val source = text.lowercase()
    val chars = source.toCharArray()
    for (i in chars.indices) {
        val possessiveS = source[i] == 's' && i > 0 && (source[i - 1] == '\'' || source[i - 1] == '’') &&
            (i + 1 == source.length || !source[i + 1].isLetterOrDigit())
        if (possessiveS) chars[i] = ' '
        if (!chars[i].isLetterOrDigit() && chars[i] != '#') chars[i] = ' '
    }
    return chars.concatToString().split(' ').filter(String::isNotEmpty)
}

private fun normalizeCaptureSearchText(text: String): String = captureSearchWords(text).joinToString(" ")

// Spoken and suggested searches read like sentences; keep only the words that describe memories.
private val captureSearchFillerWords = setOf(
    "find", "show", "me", "my", "all", "the", "a", "an", "of", "against", "vs", "vs.", "versus",
    "this", "season", "from", "with", "and", "for", "in", "plays", "play", "memories", "memory",
    "clips", "clip", "videos", "photos", "every", "any", "last", "latest", "recent",
    "please", "on", "at", "during", "recorded", "taken", "that", "was", "were", "today", "yesterday"
)

/** Lowercase words of a typed or spoken search: punctuation/hyphens split words and possessive 's is dropped. */
internal fun captureSearchTerms(query: String): List<String> =
    captureSearchWords(query).filter { it !in captureSearchFillerWords }

/**
 * Example searches the Memories action bar cycles through, using the user's own opponents and only
 * the players chosen as "My Player" in Teams. Player examples are omitted until a My Player is set.
 */
fun captureLibrarySearchIdeas(snapshot: TeamWorkspaceSnapshot): List<String> {
    val myPlayers = captureFilterPlayers(snapshot, null)
        .mapNotNull { it.name.trim().takeIf(String::isNotBlank) }
        .distinct()
    val opponent = (snapshot.games.map { it.opponentName } +
        snapshot.captureOrganizations.values.mapNotNull { it.opponentName })
        .firstOrNull { !it.isNullOrBlank() }
    val playerIdeas = myPlayers.flatMap { player ->
        listOf(
            "Find $player's goals this season",
            "How many faceoff wins did $player have yesterday?",
            "Find $player's favorite plays",
            "Make a reel of $player's saves"
        )
    }
    val rosterIdea = snapshot.players.firstOrNull { it.isActive && it.number.isNotBlank() }
        ?.let { listOf("Who is number ${it.number}?") }.orEmpty()
    val generalIdeas = rosterIdea + listOf(
        "Who is our next opponent?",
        opponent?.let { "When do we play $it?" } ?: "When is our next game?",
        "Create a reel of all videos from today",
        "Find my favorite plays this month",
        "Create a reel of the latest 3 goals",
        opponent?.let { "Find all goals against $it" } ?: "Find all goals",
        "Create a reel of all goals",
        opponent?.let { "Find the last save against $it" } ?: "Find the last save",
        "Find all defensive stops this season",
        opponent?.let { "Find assists against $it" } ?: "Find assists"
    )
    // Interleave so player examples stay spread through the rotation.
    val ideas = mutableListOf<String>()
    for (index in 0 until maxOf(generalIdeas.size, playerIdeas.size)) {
        generalIdeas.getOrNull(index)?.let(ideas::add)
        playerIdeas.getOrNull(index)?.let(ideas::add)
    }
    return ideas.distinct()
}
