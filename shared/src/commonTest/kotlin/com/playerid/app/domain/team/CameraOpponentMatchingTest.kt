package com.playerid.app.domain.team

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CameraOpponentMatchingTest {
    private val timeZoneId = "America/New_York"

    @Test
    fun selectsClosestGameOnCurrentLocalDate() {
        val early = game("early", "Pine-Richland", at(2026, 8, 25, 16, 0))
        val varsity = game("varsity", "North Hills", at(2026, 8, 25, 19, 10))

        val match = findCurrentScheduledGame(
            listOf(early, varsity),
            at(2026, 8, 25, 18, 55),
            timeZoneId
        )

        assertEquals("North Hills", match?.opponentName)
    }

    @Test
    fun ignoresGamesOutsideWindowOrLocalDate() {
        val game = game("varsity", "North Hills", at(2026, 8, 25, 19, 10))

        assertNull(findCurrentScheduledGame(listOf(game), at(2026, 8, 25, 15, 0), timeZoneId))
        assertNull(findCurrentScheduledGame(listOf(game), at(2026, 8, 26, 0, 1), timeZoneId))
    }

    @Test
    fun restoresOnlyRecentTimestampedOpponent() {
        val now = at(2026, 8, 25, 19, 0)

        assertEquals(true, shouldRestoreSavedOpponent("North Hills", now - 60_000L, now))
        assertEquals(false, shouldRestoreSavedOpponent("Wexford", 0L, now))
        assertEquals(false, shouldRestoreSavedOpponent("Wexford", now - 13L * 60L * 60L * 1000L, now))
    }

    private fun game(id: String, opponent: String, startMs: Long) = GameScheduleProfile(
        id = id,
        sportSeasonId = "season",
        opponentName = opponent,
        gameLabel = "vs $opponent",
        scheduledStartMs = startMs,
        scheduledEndMs = startMs + 2L * 60L * 60L * 1000L,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute)
            .toInstant(TimeZone.of(timeZoneId))
            .toEpochMilliseconds()
}