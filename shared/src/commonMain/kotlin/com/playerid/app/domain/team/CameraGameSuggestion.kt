package com.playerid.app.domain.team

import kotlinx.datetime.TimeZone

fun TeamWorkspaceSnapshot.suggestCameraGame(
    teamName: String?,
    nowMs: Long,
    timeZoneId: String = TimeZone.currentSystemDefault().id
): GameScheduleProfile? {
    if (teamName == null) return null
    val seasonIds = seasons.filter { it.teamName == teamName && it.isActive }.map { it.id }.toSet()
    return findCurrentScheduledGame(games.filter { it.sportSeasonId in seasonIds }, nowMs, timeZoneId)
}

/**
 * Like Android, the camera follows the schedule: when the selected team has a game happening now, the camera setup
 * switches to it (opponent, season and game; team, player and jersey are kept). Returns null when nothing should
 * change, including games in [alreadyApplied] so a user who changes the matchup isn't overridden again.
 */
fun TeamWorkspaceSnapshot.cameraOrganizationForCurrentGame(
    nowMs: Long,
    alreadyApplied: Set<String> = emptySet(),
    timeZoneId: String = TimeZone.currentSystemDefault().id
): CaptureOrganization? {
    val current = cameraOrganization ?: return null
    val game = suggestCameraGame(current.teamName, nowMs, timeZoneId) ?: return null
    if (game.id == current.gameId || game.id in alreadyApplied) return null
    return current.copy(seasonId = game.sportSeasonId, gameId = game.id, opponentName = game.opponentName)
}
