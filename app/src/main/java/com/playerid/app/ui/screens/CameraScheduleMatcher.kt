package com.playerid.app.ui.screens

import com.playerid.app.data.GameSchedule
import com.playerid.app.data.repositories.toEntity
import com.playerid.app.data.repositories.toProfile
import java.time.ZoneId

internal fun shouldRestoreSavedOpponent(opponent: String, updatedAtMs: Long, nowMs: Long): Boolean {
    return com.playerid.app.domain.team.shouldRestoreSavedOpponent(opponent, updatedAtMs, nowMs)
}

internal fun findCurrentScheduledGame(
    games: List<GameSchedule>,
    nowMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): GameSchedule? {
    return com.playerid.app.domain.team.findCurrentScheduledGame(
        games = games.map { it.toProfile() },
        nowMs = nowMs,
        timeZoneId = zoneId.id
    )?.toEntity()
}