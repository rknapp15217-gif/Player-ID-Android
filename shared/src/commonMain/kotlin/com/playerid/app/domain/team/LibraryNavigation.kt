package com.playerid.app.domain.team

enum class LibraryDestination { ROSTER, SCHEDULE, TEAMS, MEMORIES, REELS, CAMERA }

data class LibraryNavigationRequest(val destination: LibraryDestination, val teamName: String? = null)

fun parseLibraryNavigation(text: String): LibraryNavigationRequest? {
    val query = text.trim().trimEnd('?', '.', '!')
        .replace(Regex("^(?:please\\s+|(?:can|could) you\\s+)", RegexOption.IGNORE_CASE), "")
    val match = Regex("^(?:open|show(?: me)?|go to|take me to)\\s+(?:(?:the|our|my)\\s+)?(.+)$",
        RegexOption.IGNORE_CASE).matchEntire(query) ?: return null
    val target = match.groupValues[1]
    val destinations = mapOf("roster" to LibraryDestination.ROSTER, "schedule" to LibraryDestination.SCHEDULE,
        "teams" to LibraryDestination.TEAMS, "memories" to LibraryDestination.MEMORIES,
        "reels" to LibraryDestination.REELS, "camera" to LibraryDestination.CAMERA)
    destinations[target.lowercase()]?.let { return LibraryNavigationRequest(it) }
    val teamMatch = Regex("^(.+?)(?:['’]s?)?\\s+(roster|schedule)$|^(roster|schedule)\\s+(?:for|of)\\s+(.+)$",
        RegexOption.IGNORE_CASE).matchEntire(target) ?: return null
    val noun = teamMatch.groupValues[2].ifBlank { teamMatch.groupValues[3] }.lowercase()
    val team = teamMatch.groupValues[1].ifBlank { teamMatch.groupValues[4] }
    return LibraryNavigationRequest(destinations.getValue(noun), team)
}

data class LibraryNavigationResolution(val request: LibraryNavigationRequest? = null, val error: String? = null)

fun resolveLibraryNavigation(
    request: LibraryNavigationRequest,
    teams: List<String>,
    selectedTeam: String?
): LibraryNavigationResolution {
    if (request.destination !in setOf(LibraryDestination.ROSTER, LibraryDestination.SCHEDULE)) {
        return LibraryNavigationResolution(request)
    }
    val available = teams.distinct()
    val name = request.teamName ?: selectedTeam ?: available.singleOrNull()
    if (name == null) return LibraryNavigationResolution(error =
        if (available.isEmpty()) "No teams available. Add or join a team in Teams." else "Which team? Say its name, for example: Open ${available.first()}'s roster.")
    val matches = available.filter { it.equals(name, true) }
    if (matches.size != 1) return LibraryNavigationResolution(error = "No unique team named $name. Use a saved team's full name.")
    return LibraryNavigationResolution(request.copy(teamName = matches.single()))
}
