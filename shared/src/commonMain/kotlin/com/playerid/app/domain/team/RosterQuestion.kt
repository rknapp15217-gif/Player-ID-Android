package com.playerid.app.domain.team

import com.playerid.app.domain.voice.VoiceCommandDecision
import com.playerid.app.domain.voice.VoiceCommandProcessor

private val rosterGroups = mapOf(
    "Goalie" to setOf("g", "gk", "goalie", "goalies", "golie", "golies", "goalkeeper", "goalkeepers"),
    "Defense" to setOf("d", "def", "defense", "defence", "defender", "defenders", "defenseman", "defensemen"),
    "Midfield" to setOf("m", "mid", "midfield", "midfielder", "midfielders", "middie", "middies"),
    "Attack" to setOf("a", "att", "attack", "attacker", "attackers", "attackman", "attackmen"),
    "LSM" to setOf("lsm", "lsms", "long stick midfielder", "long stick midfielders"),
    "FOGO" to setOf("fo", "fogo", "fogos", "faceoff specialist", "faceoff specialists", "face off specialist", "face off specialists"),
    "Freshman" to setOf("fr", "freshman", "freshmen"),
    "Sophomore" to setOf("so", "soph", "sophomore", "sophomores"),
    "Junior" to setOf("jr", "junior", "juniors"),
    "Senior" to setOf("sr", "senior", "seniors")
)

private fun rosterGroup(value: String): String? {
    val normalized = value.lowercase().trim().trimEnd('.').replace('-', ' ').replace(Regex("\\s+"), " ")
    return rosterGroups.entries.firstOrNull { normalized in it.value }?.key
}

private fun rosterGroupTarget(value: String): String =
    value.replace(Regex("^(?:all\\s+)?(?:(?:our|the|teh|my)\\s+)?", RegexOption.IGNORE_CASE), "").trim()

private fun rosterGroupFilters(value: String): List<String>? {
    val target = rosterGroupTarget(value)
    rosterGroup(target)?.let { return listOf(it) }
    val words = target.split(Regex("\\s+"))
    for (index in 1 until words.size) {
        val first = rosterGroup(words.take(index).joinToString(" ")) ?: continue
        val second = rosterGroup(words.drop(index).joinToString(" ")) ?: continue
        if ((first in PlayerFormOptions.academicYears) != (second in PlayerFormOptions.academicYears)) {
            return listOf(first, second)
        }
    }
    return null
}

private fun rosterTeamSuffix(value: String): MatchResult? =
    Regex("\\s+(?:on|for|from)\\s+(?:the\\s+)?(.+)$", RegexOption.IGNORE_CASE).find(value)

fun parseRosterQuestion(text: String): String? {
    val query = text.trim().trimEnd('?', '.', '!')
        .replace(Regex("^(?:(?:can|could|would) you\\s+)?(?:please\\s+)?(?:tell me\\s+)?", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^who['’]s\\b", RegexOption.IGNORE_CASE), "who is")
    val count = Regex("^(?:how many|count|what\\s+is\\s+the\\s+number\\s+of|what['’]s\\s+the\\s+number\\s+of)\\s+(.+)$",
        RegexOption.IGNORE_CASE).matchEntire(query)?.groupValues?.get(1)
    if (count != null) {
        val criteria = count.replace(Regex("\\s+are on our roster$", RegexOption.IGNORE_CASE), "")
        val suffix = rosterTeamSuffix(criteria)
        val target = rosterGroupTarget((suffix?.let { criteria.substring(0, it.range.first) } ?: criteria)
            .replace(Regex("\\s+(?:do we have|we have|are there|are on our roster)$", RegexOption.IGNORE_CASE), ""))
        if (rosterGroupFilters(target) != null) return target + (suffix?.value ?: "")
    }
    val group = Regex("^(?:who\\s+(?:are|is)|which\\s+players\\s+are|show(?:\\s+me)?|list)\\s+(.+)$",
        RegexOption.IGNORE_CASE).matchEntire(query)?.groupValues?.get(1)
    if (group != null) {
        val suffix = rosterTeamSuffix(group)
        val target = rosterGroupTarget(suffix?.let { group.substring(0, it.range.first) } ?: group)
        if (rosterGroupFilters(target) != null) return target + (suffix?.value ?: "")
    }
    val possessive = Regex("^what\\s+is\\s+(.+?)['’]s\\s+(?:jersey\\s+)?number$",
        RegexOption.IGNORE_CASE).matchEntire(query)
    if (possessive != null) return possessive.groupValues[1]
    val match = Regex(
        "^(?:who\\s+(?:is(?:\\s+wearing)?|wears)|which\\s+player\\s+is|what\\s+is\\s+(?:the\\s+)?player|identify(?:\\s+player)?|" +
            "what\\s+(?:jersey\\s+)?number\\s+(?:is|does))\\s+(.+)$",
        RegexOption.IGNORE_CASE
    ).matchEntire(query) ?: return null
    val target = match.groupValues[1].replace(Regex("\\s+wear$", RegexOption.IGNORE_CASE), "").trim()
    // Keep sports-event and schedule questions in their existing interpreters.
    if (Regex("\\b(next|opponent|opponents|game|games|schedule|goal|goals|scored|score|saves|assists|wins|first|last|yesterday|today)\\b",
            RegexOption.IGNORE_CASE).containsMatchIn(target)) return null
    return target.takeIf(String::isNotBlank)
}

fun answerRosterQuestion(
    request: LibraryActionRequest.Roster,
    players: List<PlayerProfile>,
    selectedTeam: String?,
    compact: Boolean = false,
    includeCount: Boolean = false,
    summarizeLargeList: Boolean = false
): String {
    val teamSuffix = rosterTeamSuffix(request.query)
    val namedTeam = teamSuffix?.groupValues?.get(1)
    val target = teamSuffix?.let { request.query.substring(0, it.range.first) } ?: request.query
    val roster = players.filter { it.isActive && (selectedTeam == null || it.teamName == selectedTeam) &&
        (namedTeam == null || it.teamName.equals(namedTeam, true)) }
    val scope = selectedTeam ?: "your teams"
    if (roster.isEmpty()) return "No active roster is saved for $scope. Add or import players in Teams."
    val groups = rosterGroupFilters(target)
    val groupLabel = groups?.joinToString(" + ")
    val matches = if (groups != null) {
        val matching = roster.filter { player ->
            groups.all { group ->
                if (group in PlayerFormOptions.academicYears) rosterGroup(player.academicYear) == group else
                    (listOf(player.position) + player.position.split('/', ',', '&', ';')).any { rosterGroup(it) == group }
            }
        }
        if (matching.isEmpty()) return "No active players with saved roster details matching \"$groupLabel\" " +
            "matched in ${namedTeam ?: scope}. Missing details aren't inferred; update the roster in Teams."
        matching
    } else {
        val decision = VoiceCommandProcessor().lookupRoster(listOf(target), roster, allowApproximateNames = false)
        if (decision !is VoiceCommandDecision.Match) {
            return "No saved roster match for \"${request.query}\" in $scope. Try the player's full name or jersey number."
        }
        decision.players
    }.sortedWith(compareBy({ it.teamName.lowercase() }, { it.name.lowercase() }, { it.number }))
    if (summarizeLargeList && matches.size > 2) {
        return "${matches.size} players. See the roster for the list."
    }
    val heading = when {
        compact && includeCount -> "${matches.size} ${if (matches.size == 1) "player" else "players"}"
        compact -> ""
        groups != null -> "${matches.size} saved roster ${if (matches.size == 1) "match" else "matches"} for $groupLabel."
        matches.size == 1 -> "Saved roster match"
        else -> "${matches.size} saved roster matches. Use the full name or select a team to narrow them down."
    }
    val displayedMatches = if (compact) matches else matches.take(10)
    return (if (heading.isEmpty()) "" else "$heading\n\n") + displayedMatches.joinToString("\n\n") { player ->
        listOfNotNull(
            "${player.name} #${player.number}",
            player.teamName.takeUnless { compact },
            player.position.takeIf { it.isNotBlank() && !it.equals("unknown", true) },
            player.academicYear.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
        ).joinToString(" · ")
    } + if (!compact && matches.size > 10) "\n\n${matches.size - 10} more matches. Select a team or look up a player by full name." else ""
}
