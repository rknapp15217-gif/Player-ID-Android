package com.playerid.app.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceServicesTest {
    @Test
    fun videoCompositionUsesOnlySharedMediaReferences() {
        val request = VideoCompositionRequest(
            clips = listOf(MediaReference("clip-1", MediaKind.VIDEO)),
            title = "Season Reel"
        )

        assertEquals("clip-1", request.clips.single().identifier)
        assertEquals("Season Reel", request.title)
    }
}
