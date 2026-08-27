package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryGameMatcherTest {
    private val matcher = MemoryGameMatcher()

    @Test
    fun emptyScheduleHasNoMatch() {
        assertNull(matcher.findBestMatch(1_000L, null, null, emptyList()))
    }

    @Test
    fun choosesGameClosestToCaptureTime() {
        val early = game("early", startMs = 3_600_000L, endMs = 7_200_000L)
        val late = game("late", startMs = 18_000_000L, endMs = 21_600_000L)

        val match = matcher.findBestMatch(19_800_000L, null, null, listOf(early, late))

        assertEquals("late", match?.game?.id)
        assertEquals(0.75, match?.score)
    }

    @Test
    fun nearbyLocationContributesQuarterOfScore() {
        val game = game("home", startMs = 3_600_000L, endMs = 7_200_000L).copy(
            locationLat = 40.0,
            locationLng = -80.0
        )

        val score = matcher.score(5_400_000L, 40.0, -80.0, game)

        assertEquals(1.0, score)
    }

    @Test
    fun captureOutsideTimeAndLocationWindowsScoresZero() {
        val game = game("away", startMs = 3_600_000L, endMs = 7_200_000L).copy(
            locationLat = 40.0,
            locationLng = -80.0
        )

        val score = matcher.score(86_400_000L, 41.0, -80.0, game)

        assertTrue(score == 0.0)
    }

    private fun game(id: String, startMs: Long, endMs: Long) = GameScheduleProfile(
        id = id,
        sportSeasonId = "season",
        opponentName = "Opponent",
        gameLabel = "vs Opponent",
        scheduledStartMs = startMs,
        scheduledEndMs = endMs,
        createdAt = 0L,
        updatedAt = 0L
    )
}