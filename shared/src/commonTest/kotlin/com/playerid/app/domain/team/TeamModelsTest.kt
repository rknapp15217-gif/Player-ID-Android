package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamModelsTest {
    @Test
    fun playerUsesPlatformNeutralTeamIdentity() {
        val player = PlayerProfile(
            number = "12",
            name = "Alex Morgan",
            position = "Attack",
            teamName = "Tigers",
            academicYear = "Junior",
            createdAt = 100,
            updatedAt = 100
        )

        assertEquals("Tigers", player.teamName)
    }
}