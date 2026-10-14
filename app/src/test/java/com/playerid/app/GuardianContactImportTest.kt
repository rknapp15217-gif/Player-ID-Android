package com.playerid.app

import com.playerid.app.ui.screens.parseTeamContacts
import org.junit.Assert.assertEquals
import org.junit.Test

class GuardianContactImportTest {
    @Test
    fun mergesOneGuardianAcrossMultiplePlayers() {
        val contacts = parseTeamContacts(
            lines = listOf(
                "__PLAYER__:Alex Smith",
                "Jamie Smith (412) 555-0100",
                "__PLAYER__:Taylor Smith",
                "Jamie Smith (412) 555-0100"
            ),
            playerIdsByName = mapOf(
                "alex smith" to "player-alex",
                "taylor smith" to "player-taylor"
            )
        )

        assertEquals(1, contacts.size)
        assertEquals(setOf("player-alex", "player-taylor"), contacts.single().linkedPlayerIds)
    }
}