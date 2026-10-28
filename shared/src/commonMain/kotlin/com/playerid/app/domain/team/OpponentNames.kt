package com.playerid.app.domain.team

/**
 * One spelling per opponent. "Pine Richland", "PINE-RICHLAND" and "pine richland" share a key, an
 * unambiguous abbreviation such as "PR" resolves to the full name, and the schedule's spelling wins
 * (shown in normal capitalization, so "PINE-RICHLAND" becomes "Pine-Richland").
 */
fun opponentKey(name: String): String = name.filter { it.isLetterOrDigit() }.lowercase()

/** Include initials only when they identify one known opponent. */
fun opponentSearchName(name: String, known: List<String>): String {
    val opponents = known.distinctBy(::opponentKey)
    val canonical = canonicalOpponentName(name, opponents)
    val initials = opponentInitials(canonical)
    val searchable = if (opponents.count { opponentInitials(it) == initials } == 1) "$canonical $initials" else canonical
    // Dictation expands "Mt." to "Mount"; keep both searchable without renaming saved teams.
    val words = opponentWords(canonical).map { it.lowercase() }
    val mountAlias = when {
        "mt" in words -> words.joinToString(" ") { if (it == "mt") "mount" else it }
        "mount" in words -> words.joinToString(" ") { if (it == "mount") "mt" else it }
        else -> null
    }
    return if (mountAlias == null) searchable else "$searchable $mountAlias"
}

private fun opponentWords(name: String): List<String> {
    val words = mutableListOf<String>()
    val current = StringBuilder()
    for (char in name) {
        if (char.isLetterOrDigit()) current.append(char)
        else if (current.isNotEmpty()) { words += current.toString(); current.clear() }
    }
    if (current.isNotEmpty()) words += current.toString()
    return words
}

internal fun opponentInitials(name: String): String =
    opponentWords(name).joinToString("") { it.first().lowercase() }

private fun isOpponentAbbreviation(name: String): Boolean {
    val key = opponentKey(name)
    return opponentWords(name).size == 1 && key.length in 2..5 && key.all { it.isLetter() }
}

/** Collapses spacing and converts ALL-CAPS names to normal capitalization. Short one-word acronyms stay as typed. */
fun displayOpponentName(name: String): String {
    val trimmed = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
    val letters = trimmed.filter { it.isLetter() }
    if (letters.isEmpty() || letters.any { it.isLowerCase() }) return trimmed
    if (opponentWords(trimmed).size == 1 && letters.length <= 4) return trimmed
    val result = StringBuilder()
    var startOfWord = true
    for (char in trimmed) {
        if (char.isLetter()) {
            result.append(if (startOfWord) char.uppercaseChar() else char.lowercaseChar())
            startOfWord = false
        } else {
            result.append(char)
            startOfWord = !(char.isDigit() || char == '\'' || char == '\u2019')
        }
    }
    return result.toString()
}

/**
 * The spelling to store for [name]: a [known] opponent with the same key, else the single known opponent
 * whose initials match an abbreviation, else [name] in display form.
 */
fun canonicalOpponentName(name: String, known: List<String>): String {
    val cleaned = displayOpponentName(name)
    val key = opponentKey(cleaned)
    if (key.isEmpty()) return cleaned
    known.firstOrNull { opponentKey(it) == key }?.let { return it }
    if (isOpponentAbbreviation(cleaned)) {
        val matches = known.filter { opponentWords(it).size >= 2 && opponentInitials(it) == key }.distinctBy(::opponentKey)
        if (matches.size == 1) return matches.single()
    }
    return cleaned
}

/** Canonical opponents for each team: scheduled spellings first, then saved names (most-used spelling first). */
internal fun TeamWorkspaceSnapshot.knownOpponentsByTeam(): Map<String, List<String>> {
    val teamOfSeason = seasons.associate { it.id to it.teamName }
    val organizations = captureOrganizations.values + listOfNotNull(cameraOrganization)
    return teams.associate { team ->
        val known = mutableListOf<String>()
        fun add(name: String) {
            val canonical = canonicalOpponentName(name, known)
            if (canonical.isNotEmpty() && known.none { opponentKey(it) == opponentKey(canonical) }) known += canonical
        }
        games.filter { teamOfSeason[it.sportSeasonId] == team.name }.sortedBy { it.scheduledStartMs }
            .forEach { add(it.opponentName) }
        organizations.filter { it.teamName == team.name }.mapNotNull { it.opponentName?.let(::displayOpponentName) }
            .groupingBy { it }.eachCount().entries
            // Full names before abbreviations so "PR" can resolve to "Pine Richland" when there's no schedule.
            .sortedWith(compareBy<Map.Entry<String, Int>> { isOpponentAbbreviation(it.key) }
                .thenByDescending { it.value }.thenBy { it.key })
            .forEach { add(it.key) }
        team.name to known.toList()
    }
}

/** Rewrites every stored opponent to its canonical spelling. Idempotent; applied on load and on every save. */
fun TeamWorkspaceSnapshot.withCanonicalOpponents(): TeamWorkspaceSnapshot {
    val displayGames = games.map { game ->
        val display = displayOpponentName(game.opponentName)
        if (display == game.opponentName) game else game.copy(opponentName = display)
    }
    val staged = copy(games = displayGames)
    val known = staged.knownOpponentsByTeam()
    fun CaptureOrganization.canonical(): CaptureOrganization {
        val opponent = opponentName ?: return this
        val canonical = canonicalOpponentName(opponent, known[teamName].orEmpty())
        return if (canonical == opponent) this else copy(opponentName = canonical)
    }
    return staged.copy(
        captureOrganizations = captureOrganizations.mapValues { it.value.canonical() },
        cameraOrganization = cameraOrganization?.canonical()
    )
}
