package com.playerid.app.domain.camera

import com.playerid.app.domain.team.testTeam
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraJerseyOptionsTest {
    @Test
    fun originalHomeAwayColorsAndSpecialTeamChoicesArePreserved() {
        val team = testTeam().copy(homeJerseyColor = "#112233", awayJerseyColor = "#445566")
        assertEquals(listOf("Home" to "#112233", "Away" to "#445566"), cameraJerseyOptions(team))
        assertEquals(listOf("Black" to "#000000", "White" to "#FFFFFF", "Gold" to "#FFB81C"),
            cameraJerseyOptions(team.copy(name = "North Allegheny Lacrosse")))
        assertEquals(listOf("Home" to "#1976D2", "Away" to "#FFFFFF"), cameraJerseyOptions(null))
    }
}
