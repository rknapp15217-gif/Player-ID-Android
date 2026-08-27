package com.playerid.app.domain.team

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

private const val THREE_HOURS_MS = 3L * 60L * 60L * 1000L
private const val FOUR_HOURS_MS = 4L * 60L * 60L * 1000L
private const val SAVED_OPPONENT_TTL_MS = 12L * 60L * 60L * 1000L

fun shouldRestoreSavedOpponent(opponent: String, updatedAtMs: Long, nowMs: Long): Boolean =
    opponent.isNotBlank() && updatedAtMs > 0L && updatedAtMs <= nowMs &&
        nowMs - updatedAtMs <= SAVED_OPPONENT_TTL_MS

fun findCurrentScheduledGame(
    games: List<GameScheduleProfile>,
    nowMs: Long,
    timeZoneId: String = TimeZone.currentSystemDefault().id
): GameScheduleProfile? {
    val timeZone = runCatching { TimeZone.of(timeZoneId) }.getOrElse { TimeZone.UTC }
    val currentDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
    return games.asSequence()
        .filter { game ->
            Instant.fromEpochMilliseconds(game.scheduledStartMs).toLocalDateTime(timeZone).date == currentDate &&
                nowMs >= game.scheduledStartMs - THREE_HOURS_MS &&
                nowMs <= game.scheduledStartMs + FOUR_HOURS_MS
        }
        .minByOrNull { game -> abs(nowMs - game.scheduledStartMs) }
}