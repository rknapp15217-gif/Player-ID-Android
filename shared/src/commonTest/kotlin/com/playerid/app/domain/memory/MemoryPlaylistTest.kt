package com.playerid.app.domain.memory

import com.playerid.app.domain.team.TeamWorkspaceSnapshot
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryPlaylistTest {
    private val first = MediaReference("file:///media/1792500000000-first.mov", MediaKind.VIDEO)
    private val last = MediaReference("file:///media/1792600000000-last.mov", MediaKind.VIDEO)

    @Test
    fun playlistContainsOnlyUniqueMatchingOriginalVideosInChronologicalOrder() {
        val photo = MediaReference("file:///media/photo.jpg", MediaKind.IMAGE)
        val reel = MediaReference("file:///media/reel.mov", MediaKind.VIDEO)
        val snapshot = TeamWorkspaceSnapshot(captureReels = mapOf(
            captureFileKey(reel) to MemoryReel("Saved reel", listOf(captureFileKey(first), captureFileKey(last)))))
        val matches = listOf(last, photo, reel, first, last)
        assertEquals(listOf(first, last), temporaryMemoryPlaylist(matches, snapshot))
        assertEquals(1, snapshot.captureReels.size)
        assertEquals(5, matches.size)
    }

    @Test
    fun emptyPhotoOnlyAndSingleVideoResultsNeedNoReelOrMinimumClipCount() {
        val snapshot = TeamWorkspaceSnapshot()
        assertEquals(emptyList(), temporaryMemoryPlaylist(emptyList(), snapshot))
        assertEquals(emptyList(), temporaryMemoryPlaylist(listOf(
            MediaReference("photo.jpg", MediaKind.IMAGE)), snapshot))
        assertEquals(listOf(first), temporaryMemoryPlaylist(listOf(first), snapshot))
    }
}
