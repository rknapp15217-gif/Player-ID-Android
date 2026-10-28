package com.playerid.app.domain.team

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed class LibraryActionRequest {
    data class Count(val query: String) : LibraryActionRequest()
    data class Schedule(val query: String) : LibraryActionRequest()
    data class Roster(val query: String) : LibraryActionRequest()
    data class Reel(
        val command: MemoryReelCommand,
        val share: Boolean = false,
        val recipient: String? = null
    ) : LibraryActionRequest()
    data class Unsupported(val message: String) : LibraryActionRequest()
    data class Share(val query: String, val recipient: String) : LibraryActionRequest()
}

/** Ordinary searches return null and remain scoped to their current library tab. */
fun parseLibraryActionRequest(text: String): LibraryActionRequest? {
    val query = text.trim().trimEnd('?', '.', '!')
        .replace(Regex("^(?:(?:can|could|would) you\\s+)?(?:please\\s+)?(?:tell me\\s+)?", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^(who|what|when|where)['’]s\\b", RegexOption.IGNORE_CASE), "$1 is")
    parseRosterQuestion(text)?.let { return LibraryActionRequest.Roster(it) }
    val count = Regex("^(?:please\\s+)?(?:how many|count|what is the number of|what's the number of)\\s+(.+)$",
        RegexOption.IGNORE_CASE).matchEntire(query)
    if (count != null) {
        val criteria = count.groupValues[1]
            .replace(Regex("\\b(did|does|has|have|had|do|there|are|is|were|was|record|recorded)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ").trim()
        if (criteria.isNotEmpty()) return LibraryActionRequest.Count(criteria)
    }
    val lower = query.lowercase()
    if (isScheduleFollowUp(query) ||
        Regex("^(?:(?:can|could) you (?:tell me )?|please )?(?:who|when|where|what time|what|which|do|does)\\b").containsMatchIn(lower) &&
        Regex("\\b(opponent|opponents|game|games|match|matches|play|played|playing|schedule)\\b").containsMatchIn(lower)) {
        return LibraryActionRequest.Schedule(query)
    }
    val sharing = Regex("\\b(share|send)\\b", RegexOption.IGNORE_CASE).containsMatchIn(query)
    if (sharing && Regex("\\b(reel|real)\\b", RegexOption.IGNORE_CASE).containsMatchIn(query)) {
        val recipientMatch = Regex("\\s+(?:with|to)\\s+", RegexOption.IGNORE_CASE).findAll(query).lastOrNull()
        val rawRecipient = recipientMatch?.let { query.substring(it.range.last + 1).trim().takeIf(String::isNotEmpty) }
        val recipient = rawRecipient?.let(::normalizeRecipientText)?.takeIf(String::isNotBlank)
        val source = recipientMatch?.let { query.substring(0, it.range.first) } ?: query
        val creation = source.replace(Regex("\\b(share|send)\\b", RegexOption.IGNORE_CASE), "create")
        val command = parseMemoryReelCommand(creation)
        if (command != null && command.searchQuery.isNotBlank()) {
            return LibraryActionRequest.Reel(command, share = true, recipient = recipient)
        }
        return LibraryActionRequest.Unsupported("Describe the memories to include, for example: Share a reel of Tyson's faceoff wins with Jenna.")
    }
    parseMemoryReelCommand(query)?.let { return LibraryActionRequest.Reel(it) }
    if (sharing) {
        val recipientMatch = Regex("\\s+(?:with|to)\\s+", RegexOption.IGNORE_CASE).findAll(query).lastOrNull()
        val recipient = recipientMatch?.let { normalizeRecipientText(query.substring(it.range.last + 1)) }
        if (!recipient.isNullOrBlank()) {
            val criteria = query.substring(0, recipientMatch.range.first)
                .replace(Regex("^\\s*(?:share|send)\\s+(?:me\\s+)?(?:all\\s+)?(?:of\\s+)?(?:the\\s+|my\\s+)?(?:all\\s+)?",
                    RegexOption.IGNORE_CASE), "")
                .replace(Regex("^(?:memories|memory|videos?|clips?|photos?|pictures?)\\b\\s*(?:of|from|with)?\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            if (criteria.isNotBlank()) return LibraryActionRequest.Share(criteria, recipient)
            return LibraryActionRequest.Unsupported("Describe the memories to include, for example: Send all memories against Mount Lebanon to Jenna.")
        }
    }
    if (Regex("^(how|who|when|where|why|what|which)\\b", RegexOption.IGNORE_CASE).containsMatchIn(query)) {
        return LibraryActionRequest.Unsupported(
            "I can identify saved roster players by name or number, count saved memories, look up scheduled opponents, dates and locations, and prepare reels for sharing. " +
                "Try: How many faceoff wins did Tyson have yesterday? I can't infer unrecorded game statistics."
        )
    }
    return null
}

data class ActionMemoryQuery(val query: String, val error: String? = null)

/** Dictation often omits the apostrophe in a player's possessive name. Never guess between namesakes. */
fun resolveActionMemoryQuery(query: String, playerNames: List<String>): ActionMemoryQuery {
    var resolved = query
    val names = playerNames.distinctBy { it.lowercase() }
    val tokens = query.lowercase().replace("’", "'").replace("'s", "")
        .split(Regex("[^a-z0-9]+")).filter(String::isNotEmpty)
    for ((first, matches) in names.groupBy { it.substringBefore(' ').lowercase() }) {
        if (first.isBlank() || tokens.none { it == first || it == "${first}s" }) continue
        val explicit = matches.filter { name ->
            val parts = name.lowercase().split(' ').filter(String::isNotBlank)
            parts.size > 1 && parts.all { part -> part in tokens || "${part}s" in tokens }
        }
        if (matches.size > 1 && explicit.size != 1) {
            return ActionMemoryQuery(query, "More than one player is named ${matches.first().substringBefore(' ')}. Use their full name and team.")
        }
        val chosen = explicit.singleOrNull() ?: matches.singleOrNull()
        chosen?.lowercase()?.split(' ')?.filter(String::isNotBlank)?.forEach { part ->
            resolved = resolved.replace(Regex("\\b${Regex.escape(part)}s\\b", RegexOption.IGNORE_CASE), part)
        }
    }
    return ActionMemoryQuery(resolved)
}

private fun normalizeRecipientText(text: String): String {
    val cleaned = text.trim()
        .trimEnd('.', '!', '?')
        .replace(Regex("^(?:to|with|for|at|on)\\s+", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^(?:a|an|the)\\s+", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\b(?:contact|contacts|person|people|friend|friends|in|my|phone|cell|mobile)\\b", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return cleaned.takeIf(String::isNotBlank).orEmpty()
}

fun memoryCountAnswer(count: Int, criteria: String, reels: Boolean): String =
    "Found $count matching saved ${if (reels) "reel${if (count == 1) "" else "s"}" else "memor${if (count == 1) "y" else "ies"}"} " +
        "for \"$criteria\".\n\n" +
        if (reels) "This counts saved reels, not individual plays."
        else "This counts recorded memories and their saved tags, not complete game statistics. Missing recordings or tags aren't included."

data class ActionScheduleGame(
    val id: String,
    val teamName: String,
    val opponent: String,
    val startMs: Long,
    val location: String? = null
)

fun actionScheduleGames(snapshot: TeamWorkspaceSnapshot): List<ActionScheduleGame> {
    val teams = snapshot.seasons.associate { it.id to it.teamName }
    return snapshot.games.mapNotNull { game ->
        teams[game.sportSeasonId]?.let { team ->
            ActionScheduleGame(game.id, team, game.opponentName, game.scheduledStartMs, game.locationName)
        }
    }
}

fun answerScheduleQuestion(
    request: LibraryActionRequest.Schedule,
    games: List<ActionScheduleGame>,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    timeZoneId: String = TimeZone.currentSystemDefault().id
): String = scheduleQuestionAnswer(request, games, nowMs, timeZoneId).text

data class ScheduleQuestionAnswer(val text: String, val games: List<ActionScheduleGame> = emptyList())

private fun isScheduleFollowUp(query: String): Boolean =
    query.lowercase().trim().trimEnd('?', '.', '!').replace("’", "'") in setOf(
        "where is it", "where's it", "when is it", "when's it", "what time is it",
        "where is that game", "when is that game", "what time is that game"
    )

fun scheduleQuestionAnswer(
    request: LibraryActionRequest.Schedule,
    games: List<ActionScheduleGame>,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    timeZoneId: String = TimeZone.currentSystemDefault().id,
    previousAnswer: ScheduleQuestionAnswer? = null
): ScheduleQuestionAnswer {
    val zone = TimeZone.of(timeZoneId)
    if (isScheduleFollowUp(request.query)) {
        val previous = previousAnswer?.games.orEmpty()
        if (previous.size != 1) return ScheduleQuestionAnswer(
            "Specify a team or opponent.")
        val current = games.singleOrNull { it == previous.single() }
            ?: return ScheduleQuestionAnswer("That game has changed. Ask for the next game again.")
        return ScheduleQuestionAnswer(formatScheduleGames(listOf(current), zone), listOf(current))
    }
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
    val dateRequest = parseMemoryDateRequest(request.query, today)
    val dateStart = dateRequest.start
    val dateEnd = dateRequest.end ?: dateStart
    val words = dateRequest.remaining.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    val past = Regex("\\b(played|last|previous|past)\\b").containsMatchIn(words) ||
        (dateRequest.end?.let { it < today.toString() } == true)
    val nextMeeting = words.split(' ').any { it == "next" || it == "again" }
    // Remove only question grammar. Unknown opponent/team words must not silently become "next game".
    val filler = setOf("who", "when", "where", "what", "which", "time", "is", "are", "our", "the", "a",
        "an", "we", "do", "does", "will", "play", "playing", "next", "game", "games", "match", "matches",
        "opponent", "opponents", "against", "vs", "versus", "with", "at", "on", "schedule", "scheduled",
        "can", "could", "would", "you", "tell", "me", "please", "for", "have", "again",
        "s", "in", "during", "show", "list", "was", "were", "did", "played", "last", "previous", "past")
    val terms = words.split(' ').filter { it !in filler }
    val opponents = games.map { it.opponent }.distinct()
    val matching = games.filter { game ->
        val searchableWords = (game.teamName + " " + opponentSearchName(game.opponent, opponents))
            .lowercase().split(Regex("[^a-z0-9]+")).filter(String::isNotBlank)
        terms.all { it in searchableWords || it == opponentKey(game.opponent) || it == opponentKey(game.teamName) }
    }.filter {
        if (dateStart == null || dateEnd == null) {
            if (past) it.startMs < nowMs else it.startMs >= nowMs
        } else {
            val date = Instant.fromEpochMilliseconds(it.startMs).toLocalDateTime(zone).date.toString()
            date >= dateStart && date <= dateEnd && (!nextMeeting || it.startMs >= nowMs)
        }
    }.distinctBy { listOf(it.teamName, it.opponent, it.startMs.toString()) }
        .let { if (past) it.sortedByDescending { game -> game.startMs } else it.sortedBy { game -> game.startMs } }
    if (matching.isEmpty()) {
        val subject = request.query.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.lowercase() in terms }.joinToString(" ")
        val teamOnly = games.any { opponentKey(it.teamName) == opponentKey(subject) }
        val target = if (subject.isEmpty()) "" else " ${if (teamOnly) "for" else "against"} $subject"
        val dates = when {
            dateStart == null -> ""
            dateStart == dateEnd -> " on $dateStart"
            else -> " from $dateStart to $dateEnd"
        }
        return ScheduleQuestionAnswer("No ${if (dateStart == null) { if (past) "past " else "upcoming " } else ""}scheduled game$target$dates.")
    }
    val upcoming = if (nextMeeting || (past && dateStart == null) ||
        (dateRequest.start == null && terms.isEmpty())) {
        // Without a team context, show each team's next game rather than guessing which team "our" means.
        matching.distinctBy { it.teamName }
    } else matching
    return ScheduleQuestionAnswer(formatScheduleGames(upcoming, zone), upcoming)
}

private fun formatScheduleGames(games: List<ActionScheduleGame>, zone: TimeZone): String {
    val lines = games.take(5).map { game ->
        val local = Instant.fromEpochMilliseconds(game.startMs).toLocalDateTime(zone)
        val minute = local.minute.toString().padStart(2, '0')
        val hour = (local.hour % 12).takeIf { it != 0 } ?: 12
        "${game.teamName} vs ${game.opponent}\n${local.date} at $hour:$minute ${if (local.hour < 12) "AM" else "PM"}" +
            "\n${game.location?.takeIf(String::isNotBlank) ?: "Location not saved"}"
    }
    return lines.joinToString("\n\n") +
        if (games.size > 5) "\n\n${games.size - 5} more games match. Add a team or opponent to narrow the question." else ""
}

/** Match whole name words, not substrings such as Ann in Jenna. Ambiguous matches require selection. */
fun actionRecipientMatches(requested: String, displayName: String): Boolean {
    fun words(text: String) = text.lowercase().split(Regex("[^a-z0-9]+")).filter(String::isNotEmpty)
    val requestedWords = words(requested)
    return requestedWords.isNotEmpty() && words(displayName).containsAll(requestedWords)
}
