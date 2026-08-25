package com.playerid.app.ui.screens

import com.playerid.app.data.GameSchedule
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

private const val THREE_HOURS_MS = 3L * 60L * 60L * 1000L
private const val FOUR_HOURS_MS = 4L * 60L * 60L * 1000L
private const val SAVED_OPPONENT_TTL_MS = 12L * 60L * 60L * 1000L

internal fun shouldRestoreSavedOpponent(opponent: String, updatedAtMs: Long, nowMs: Long): Boolean {
    return opponent.isNotBlank() &&
        updatedAtMs > 0L &&
        updatedAtMs <= nowMs &&
        nowMs - updatedAtMs <= SAVED_OPPONENT_TTL_MS
}

internal fun findCurrentScheduledGame(
    games: List<GameSchedule>,
    nowMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): GameSchedule? {
    val currentDate = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()
    return games
        .asSequence()
        .filter { game ->
            Instant.ofEpochMilli(game.scheduledStartMs).atZone(zoneId).toLocalDate() == currentDate &&
                nowMs >= game.scheduledStartMs - THREE_HOURS_MS &&
                nowMs <= game.scheduledStartMs + FOUR_HOURS_MS
        }
        .minByOrNull { game -> abs(nowMs - game.scheduledStartMs) }
}