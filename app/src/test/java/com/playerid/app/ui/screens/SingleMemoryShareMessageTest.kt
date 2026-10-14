package com.playerid.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SingleMemoryShareMessageTest {
    @Test
    fun includesOpponentAndBrandFooter() {
        assertEquals(
            "Hi! I caught this memory from the game against North Allegheny and thought your family might want it.\n\n" +
                "Shared via Playloom - Never lose the moment",
            buildSingleMemoryShareMessage(" North Allegheny ")
        )
    }

    @Test
    fun usesNaturalFallbackWhenOpponentIsMissing() {
        assertEquals(
            "Hi! I caught this memory from the game against the other team and thought your family might want it.\n\n" +
                "Shared via Playloom - Never lose the moment",
            buildSingleMemoryShareMessage(null)
        )
    }

    @Test
    fun usesPluralWordingForMultipleMemories() {
        assertEquals(
            "Hi! I caught these memories from the game against Penn Trafford and thought your family might want them.\n\n" +
                "Shared via Playloom - Never lose the moment",
            buildMultipleMemoriesShareMessage("Penn Trafford")
        )
    }
}
