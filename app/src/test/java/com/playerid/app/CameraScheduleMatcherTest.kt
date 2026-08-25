package com.playerid.app

import com.playerid.app.data.GameSchedule
import com.playerid.app.ui.screens.findCurrentScheduledGame
import com.playerid.app.ui.screens.shouldRestoreSavedOpponent
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraScheduleMatcherTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun selectsClosestGameOnTheCurrentDate() {
        val earlyGame = game("JV", "Pine-Richland", at(2026, 8, 25, 16, 0))
        val varsityGame = game("Varsity", "North Hills", at(2026, 8, 25, 19, 10))

        val match = findCurrentScheduledGame(
            games = listOf(earlyGame, varsityGame),
            nowMs = at(2026, 8, 25, 18, 55),
            zoneId = zone
        )

        assertEquals("North Hills", match?.opponentName)
    }

    @Test
    fun ignoresGamesOutsideTheNearbyWindow() {
        val game = game("Varsity", "North Hills", at(2026, 8, 25, 19, 10))

        assertNull(findCurrentScheduledGame(listOf(game), at(2026, 8, 25, 15, 0), zone))
        assertNull(findCurrentScheduledGame(listOf(game), at(2026, 8, 26, 0, 1), zone))
    }

    @Test
    fun restoresOnlyRecentTimestampedOpponents() {
        val now = at(2026, 8, 25, 19, 0)

        assertEquals(true, shouldRestoreSavedOpponent("North Hills", now - 60_000L, now))
        assertEquals(false, shouldRestoreSavedOpponent("Wexford", 0L, now))
        assertEquals(false, shouldRestoreSavedOpponent("Wexford", now - 13L * 60L * 60L * 1000L, now))
    }

    private fun game(level: String, opponent: String, startMs: Long) = GameSchedule(
        id = "$level-$opponent",
        sportSeasonId = "season-football-2026",
        opponentName = opponent,
        gameLabel = "$level vs $opponent",
        scheduledStartMs = startMs,
        scheduledEndMs = startMs + 2L * 60L * 60L * 1000L
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}