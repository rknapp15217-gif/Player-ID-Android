package com.playerid.app.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformContractsTest {
    @Test
    fun mediaReferencesRemainPlatformNeutral() {
        val reference = MediaReference(
            identifier = "library-item-42",
            kind = MediaKind.VIDEO,
            mimeType = "video/mp4"
        )

        assertEquals("library-item-42", reference.identifier)
        assertEquals(MediaKind.VIDEO, reference.kind)
    }
}
