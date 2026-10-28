package com.playerid.app.domain.team

fun matchesScheduleOpponent(
    games: List<ActionScheduleGame>,
    teamName: String?,
    opponent: String?,
    knownOpponents: List<String>
): Boolean {
    if (teamName.isNullOrBlank() || opponent.isNullOrBlank()) return false
    fun key(name: String): String = opponentKey(
        canonicalOpponentName(name, knownOpponents)
            .replace(Regex("\\bmt\\.?\\s", RegexOption.IGNORE_CASE), "Mount ")
    )
    return games.any {
        it.teamName.equals(teamName, ignoreCase = true) && key(it.opponent) == key(opponent)
    }
}
