package com.playerid.app

import com.playerid.app.data.GameSchedule
import com.playerid.app.data.repositories.toEntity
import com.playerid.app.data.repositories.toProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleDomainMapperTest {
    @Test
    fun entityProfileRoundTripPreservesAllFields() {
        val schedule = GameSchedule(
            id = "game-1",
            sportSeasonId = "season-1",
            opponentName = "North Hills",
            gameLabel = "Varsity vs North Hills",
            scheduledStartMs = 1_000L,
            scheduledEndMs = 2_000L,
            locationName = "Newman Stadium",
            locationLat = 40.6,
            locationLng = -80.0,
            source = "website",
            createdAt = 3_000L,
            updatedAt = 4_000L
        )

        assertEquals(schedule, schedule.toProfile().toEntity())
    }
}