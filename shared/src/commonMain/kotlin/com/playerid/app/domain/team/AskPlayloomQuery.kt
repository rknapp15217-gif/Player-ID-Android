package com.playerid.app.domain.team

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** A transient interpretation, not a second memory store or a permission/ownership model. */
data class AskPlayloomQuery(
    val filters: MemorySearchQuery,
    val players: List<PlayerProfile> = emptyList(),
    val teams: List<String> = emptyList(),
    val opponents: List<String> = emptyList(),
    val issue: String? = null,
    val question: MemoryQuestion? = null
) {
    fun matchesAssignment(playerId: String?, playerName: String?, teamName: String?, opponent: String?): Boolean =
        issue == null &&
            (players.isEmpty() || players.any { player ->
                (teamName == null || player.teamName.equals(teamName, true)) &&
                    (if (playerId != null) player.id == playerId else player.name.equals(playerName, true))
            }) &&
            (teams.isEmpty() || teams.any { it.equals(teamName, true) }) &&
            (opponents.isEmpty() || opponents.any {
                opponentKey(it.replace(Regex("\\bmt\\b", RegexOption.IGNORE_CASE), "mount")) ==
                    opponentKey(opponent.orEmpty().replace(Regex("\\bmt\\b", RegexOption.IGNORE_CASE), "mount"))
            })
}

/** Legacy Android assignments sometimes store only a first name. Resolve only a unique roster match. */
fun resolveMemoryPlayerAssignment(name: String?, teamName: String?, players: List<PlayerProfile>): PlayerProfile? {
    if (name.isNullOrBlank()) return null
    val roster = players.filter { it.isActive && (teamName == null || it.teamName.equals(teamName, true)) }
    val exact = roster.filter { it.name.equals(name.trim(), true) }
    if (exact.isNotEmpty()) return exact.singleOrNull()
    return roster.filter { it.name.substringBefore(' ').equals(name.trim(), true) }.singleOrNull()
}

fun interpretAskPlayloom(
    text: String,
    players: List<PlayerProfile>,
    teams: List<String>,
    opponents: List<String>
): AskPlayloomQuery = interpretAskPlayloom(
    text, players, teams, opponents, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
)

fun interpretAskPlayloom(
    text: String,
    players: List<PlayerProfile>,
    teams: List<String>,
    opponents: List<String>,
    today: LocalDate
): AskPlayloomQuery {
    val question = parseMemoryQuestion(text)
    if (question != null) {
        return interpretAskPlayloom(question.criteria, players, teams, opponents, today).copy(question = question)
    }
    var remaining = searchWords(text).joinToString(" ")
    val unsupported = when {
        Regex("\\b(shared|receive|received|sent|ownership|owner)\\b").containsMatchIn(remaining) ->
            "Shared-with-me and ownership search isn't connected yet. No private or received memories were inferred."
        Regex("\\b(detected|detection|background)\\b").containsMatchIn(remaining) ->
            "Detected-player search isn't connected yet. Normal player search uses saved player assignments, not background detections."
        else -> null
    }
    if (unsupported != null) return AskPlayloomQuery(parseMemorySearchQuery(text, today), issue = unsupported)

    fun containsPhrase(phrase: String): Boolean =
        phrase.isNotBlank() && " $remaining ".contains(" $phrase ")
    fun removePhrase(phrase: String) {
        remaining = " $remaining ".replace(" $phrase ", " ").trim()
    }
    // Resolve named opponents before moment tags: "Ground Balls" may be an opponent, not a play.
    val opponentMatches = opponents.distinctBy(::opponentKey).filter { opponent ->
        val normal = searchWords(opponent).joinToString(" ")
        val mount = normal.replace(Regex("\\bmt\\b"), "mount")
        val mt = normal.replace(Regex("\\bmount\\b"), "mt")
        val initials = opponentInitials(opponent)
        val forms = listOf(normal, mount, mt, opponentKey(opponent)) +
            if (initials.isNotBlank() && opponents.distinctBy(::opponentKey).count { opponentInitials(it) == initials } == 1)
                listOf(initials) else emptyList()
        forms.any(::containsPhrase)
    }
    opponentMatches.forEach { opponent ->
        val normal = searchWords(opponent).joinToString(" ")
        listOf(normal, normal.replace(Regex("\\bmt\\b"), "mount"),
            normal.replace(Regex("\\bmount\\b"), "mt"), opponentKey(opponent), opponentInitials(opponent))
            .sortedByDescending(String::length).forEach { form -> if (containsPhrase(form)) removePhrase(form) }
    }
    val teamMatches = teams.distinct().sortedByDescending(String::length).filter { team ->
        val phrase = searchWords(team).joinToString(" ")
        if (containsPhrase(phrase)) { removePhrase(phrase); true } else false
    }
    val active = players.filter { it.isActive && (teamMatches.isEmpty() || it.teamName in teamMatches) }
    val resolved = mutableListOf<PlayerProfile>()
    // Longest full names win before partial names. Multiple full-name matches require a team.
    for ((name, named) in active.groupBy { searchWords(it.name).joinToString(" ") }.entries.sortedByDescending { it.key.length }) {
        if (containsPhrase(name) || containsPhrase("${name}s")) {
            if (named.size > 1) return AskPlayloomQuery(parseMemorySearchQuery(text, today),
                issue = "More than one roster entry matches ${named.first().name}. Include the team name.")
            resolved += named
            removePhrase(if (containsPhrase(name)) name else "${name}s")
        }
    }
    val ignored = setOf("show", "find", "me", "my", "all", "the", "a", "of", "from", "against", "vs", "with",
        "goal", "goals", "save", "saves", "assist", "assists", "faceoff", "faceoffs", "wins", "favorite", "favorites",
        "first", "last", "this", "season", "today", "yesterday", "video", "videos", "photo", "photos")
    for (token in remaining.split(' ').filter { it.length >= 2 && it !in ignored && it.any(Char::isLetter) }.distinct()) {
        val matches = active.filter { player ->
            val parts = searchWords(player.name)
            parts.any { it == token || "${it}s" == token || (token.length >= 2 && it.startsWith(token)) }
        }.filterNot { it in resolved }
        if (matches.size > 1) return AskPlayloomQuery(parseMemorySearchQuery(text, today),
            issue = "\"$token\" matches more than one player. Use a full name or include the team.")
        if (matches.size == 1) {
            resolved += matches
            removePhrase(token)
        }
    }
    // Parse dates from the original punctuation-preserving text after removing resolved entity phrases.
    var criteria = text
    val removedWords = (searchWords(text).toSet() - remaining.split(' ').toSet()).sortedByDescending(String::length)
    removedWords.forEach { word ->
        criteria = criteria.replace(Regex("\\b${Regex.escape(word)}(?:['’]s)?\\b", RegexOption.IGNORE_CASE), " ")
    }
    return AskPlayloomQuery(parseMemorySearchQuery(criteria, today), resolved, teamMatches, opponentMatches)
}

private fun searchWords(text: String): List<String> = text.lowercase()
    .replace("’s", "").replace("'s", "")
    .map { if (it.isLetterOrDigit() || it == '#') it else ' ' }.joinToString("")
    .split(' ').filter(String::isNotBlank)

data class AskPlayloomChip(val id: String, val label: String)

fun AskPlayloomQuery.withSelectedSeasonYear(year: String?): AskPlayloomQuery =
    if (filters.seasonIntent == MemorySeasonIntent.CURRENT && year != null)
        copy(filters = filters.copy(seasonYear = year)) else this

fun AskPlayloomQuery.chips(): List<AskPlayloomChip> = buildList {
    players.forEach { add(AskPlayloomChip("player:${it.id}", "${it.name} #${it.number}")) }
    teams.forEach { add(AskPlayloomChip("team:$it", it)) }
    opponents.forEach { add(AskPlayloomChip("opponent:$it", "vs $it")) }
    filters.momentTag?.let { add(AskPlayloomChip("moment", it)) }
    if (filters.favoritesOnly) add(AskPlayloomChip("favorite", "Favorites"))
    if (filters.seasonIntent != null) {
        val label = if (filters.seasonIntent == MemorySeasonIntent.CURRENT) "This season" else "Last season"
        add(AskPlayloomChip("season", "$label${filters.seasonYear?.let { " ($it)" }.orEmpty()}"))
    } else filters.seasonYear?.let { add(AskPlayloomChip("season", "$it season")) }
    filters.dateFrom?.let { start ->
        add(AskPlayloomChip("date", if (start == filters.dateThrough) start else "$start to ${filters.dateThrough}"))
    }
    filters.mediaType?.let { add(AskPlayloomChip("media", it.replaceFirstChar(Char::uppercaseChar))) }
    filters.resultSelection?.let {
        add(AskPlayloomChip("sort", "${if (it == MemoryResultSelection.FIRST) "First" else "Latest"} ${filters.resultLimit}"))
    }
    filters.terms.forEach { add(AskPlayloomChip("term:$it", "Text: $it")) }
}

fun AskPlayloomQuery.withoutChip(id: String): AskPlayloomQuery = when {
    id.startsWith("player:") -> copy(players = players.filterNot { it.id == id.removePrefix("player:") })
    id.startsWith("team:") -> copy(teams = teams - id.removePrefix("team:"))
    id.startsWith("opponent:") -> copy(opponents = opponents - id.removePrefix("opponent:"))
    id.startsWith("term:") -> copy(filters = filters.copy(terms = filters.terms - id.removePrefix("term:")))
    id == "moment" -> copy(filters = filters.copy(momentTag = null))
    id == "favorite" -> copy(filters = filters.copy(favoritesOnly = false))
    id == "season" -> copy(filters = filters.copy(seasonIntent = null, seasonYear = null))
    id == "date" -> copy(filters = filters.copy(captureDate = null, dateFrom = null, dateThrough = null))
    id == "media" -> copy(filters = filters.copy(mediaType = null))
    id == "sort" -> copy(filters = filters.copy(resultSelection = null, resultLimit = 1))
    else -> this
}
