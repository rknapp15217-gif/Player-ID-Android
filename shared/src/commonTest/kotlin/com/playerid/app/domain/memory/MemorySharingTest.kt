package com.playerid.app.domain.memory

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemorySharingTest {
    private val photo = MediaReference("file:///media/photo.jpg", MediaKind.IMAGE)
    private val video = MediaReference("file:///media/video.mp4", MediaKind.VIDEO)
    private val other = MediaReference("file:///media/other.mov", MediaKind.VIDEO)

    @Test
    fun singleMemoryRequestNeverIncludesOtherSelections() {
        assertEquals(listOf(photo), selectedMemoriesForSharing(listOf(photo), setOf(photo.identifier, video.identifier)))
    }

    @Test
    fun batchContainsOnlySelectedVisibleMemoriesInCollectionOrder() {
        assertEquals(listOf(video, photo), selectedMemoriesForSharing(
            listOf(video, photo, other), setOf(photo.identifier, video.identifier, "missing")
        ))
    }

    @Test
    fun hiddenAndRemovedSelectionsAreNotShared() {
        assertEquals(listOf(photo), selectedMemoriesForSharing(listOf(photo), setOf(photo.identifier, other.identifier)))
        assertTrue(selectedMemoriesForSharing(emptyList(), setOf(photo.identifier)).isEmpty())
        assertTrue(selectedMemoriesForSharing(listOf(photo), emptySet()).isEmpty())
    }

    @Test
    fun duplicateFileIsOnlySharedOnce() {
        assertEquals(listOf(photo), selectedMemoriesForSharing(listOf(photo, photo), setOf(photo.identifier)))
    }
}
