package com.playerid.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPathSelectionTest {
    @Test
    fun reelVideoRelativePathsIncludeSpotrFolder() {
        assertEquals(
            listOf("Movies/PlayerID/", "Movies/Spotr/"),
            reelVideoRelativePathsForRestoredClips()
        )
    }
}
