package com.playerid.app.domain.memory

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlin.test.*

class MemoryReelsTest {
    private val first = MediaReference("file:///media/first.mov", MediaKind.VIDEO)
    private val second = MediaReference("file:///media/second.mp4", MediaKind.VIDEO)
    private val third = MediaReference("file:///media/third.mov", MediaKind.VIDEO)

    @Test
    fun newReelsOrderCaptureTimesOldestFirstAndKeepUnknownTimesLast() {
        val early = MediaReference("file:///media/1793000000000-early.mov", MediaKind.VIDEO)
        val late = MediaReference("file:///media/1793000001000-late.mov", MediaKind.VIDEO)
        val nextDay = MediaReference("file:///media/1793086400000-next.mov", MediaKind.VIDEO)
        val original = listOf(third, nextDay, late, first, early)
        assertEquals(listOf(early, late, nextDay, third, first),
            chronologicalReelSources(original, com.playerid.app.domain.team.TeamWorkspaceSnapshot()))
        assertEquals(listOf(third, nextDay, late, first, early), original)
    }

    @Test
    fun requiresNameAndAtLeastTwoDifferentVideos() {
        validateReelSelection(" Goals ", listOf(first, second))
        for ((name, sources) in listOf(
            "" to listOf(first, second), " \n " to listOf(first, second),
            "Goals" to emptyList(), "Goals" to listOf(first),
            "Goals" to listOf(first, first),
            "Goals" to listOf(first, first.copy(identifier = "file:///new-container/first.mov")),
            "Goals" to listOf(first, MediaReference("file:///media/photo.jpg", MediaKind.IMAGE))
        )) {
            assertFailsWith<IllegalArgumentException> { validateReelSelection(name, sources) }
        }
    }

    @Test
    fun movesInBothDirectionsWithoutChangingOriginalList() {
        val sources = listOf(first, second, third)
        assertEquals(listOf(second, third, first), moveReelSource(sources, 0, 2))
        assertEquals(listOf(third, first, second), moveReelSource(sources, 2, 0))
        assertEquals(listOf(second, first, third), moveReelSource(sources, 1, 0))
        assertEquals(sources, moveReelSource(sources, 1, 1))
        assertEquals(listOf(first, second, third), sources)
    }

    @Test
    fun rejectsInvalidPositionsRatherThanDroppingAClip() {
        for ((index, destination) in listOf(-1 to 0, 0 to -1, 2 to 0, 0 to 2)) {
            assertFailsWith<IllegalArgumentException> { moveReelSource(listOf(first, second), index, destination) }
        }
        assertFailsWith<IllegalArgumentException> { moveReelSource(emptyList(), 0, 0) }
    }

    @Test
    fun introTitleWrapsLikeAndroid() {
        assertEquals(listOf("Eagles Highlight", "Reel"), reelIntroTitleLines("Eagles Highlight Reel"))
        assertEquals(listOf("PlayerID Reel"), reelIntroTitleLines("   "))
        assertEquals(3, reelIntroTitleLines("one two three four five six seven eight nine ten eleven twelve").size)
        assertEquals(listOf("Supercalifragilisticexpialidocious"), reelIntroTitleLines("Supercalifragilisticexpialidocious"))
    }
}
