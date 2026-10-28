package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class MemorySearchQueryTest {
    @Test
    fun allSavesInYearUsesCalendarDateRange() {
        val query = parseMemorySearchQuery("Show me all saves from 2026")

        assertEquals("Save", query.momentTag)
        assertEquals(emptyList(), query.terms)
        assertEquals("2026-01-01", query.dateFrom)
        assertEquals("2026-12-31", query.dateThrough)
    }

    @Test
    fun tagAgainstOpponentCombinesBothConstraints() {
        val query = parseMemorySearchQuery("Find every save against Penn Trafford")

        assertEquals("Save", query.momentTag)
        assertEquals(listOf("penn", "trafford"), query.terms)
    }

    @Test
    fun playerPossessiveAndNaturalPhrasingAreNormalized() {
        val query = parseMemorySearchQuery("Please show me Brooklyn's latest goals")

        assertEquals("Score", query.momentTag)
        assertEquals(MemoryResultSelection.LAST, query.resultSelection)
        assertEquals(listOf("brooklyn"), query.terms)
    }

    @Test
    fun previousSeasonIsAnIntentRatherThanSearchText() {
        val query = parseMemorySearchQuery("all assists from last season")

        assertEquals("Assist", query.momentTag)
        assertEquals(MemorySeasonIntent.PREVIOUS, query.seasonIntent)
        assertEquals(emptyList(), query.terms)
    }

    @Test
    fun reelCommandUnderstandsFaceoffsWithoutSayingWin() {
        val query = parseMemorySearchQuery("make a reel of all this season's faceoffs")

        assertEquals("Faceoff Win", query.momentTag)
        assertEquals(MemorySeasonIntent.CURRENT, query.seasonIntent)
        assertEquals(emptyList(), query.terms)
    }

    @Test
    fun reelCommandKeepsPlayerAndOpponentButDropsGenericPlays() {
        val query = parseMemorySearchQuery("make a reel for all Tyson's plays against Tester")

        assertEquals(null, query.momentTag)
        assertEquals(listOf("tyson", "tester"), query.terms)
    }

    @Test
    fun reelCommandCombinesPlayerAndScores() {
        val query = parseMemorySearchQuery("Make reel of Tyson's scores")

        assertEquals("Score", query.momentTag)
        assertEquals(listOf("tyson"), query.terms)
    }

    @Test
    fun speechReelHomophoneDoesNotBecomeSearchText() {
        val query = parseMemorySearchQuery("Create real of all of Tyson's saves")

        assertEquals("Save", query.momentTag)
        assertEquals(listOf("tyson"), query.terms)
    }

    @Test
    fun reelCommandsAreDetectedAndTitledLikeAndroid() {
        assertEquals(MemoryReelCommand("All scores", "all scores"), parseMemoryReelCommand("create reel of all scores"))
        assertEquals(MemoryReelCommand("Tyson's goals against Penn Trafford", "Tyson's goals against Penn Trafford"),
            parseMemoryReelCommand("Please make me a reel of Tyson's goals against Penn Trafford."))
        assertEquals("All saves", parseMemoryReelCommand("Build a real from all saves")?.title)
        assertEquals(MemoryReelCommand("Voice Reel", ""), parseMemoryReelCommand("Create a reel"))
        assertEquals(null, parseMemoryReelCommand("Show face off wins"))
        assertEquals(null, parseMemoryReelCommand("Find my reels"))
    }

    @Test
    fun reelCommandSelectsMatchingMemories() {
        val goal = com.playerid.app.platform.MediaReference("file:///m/goal.mov", com.playerid.app.platform.MediaKind.VIDEO)
        val goal2 = com.playerid.app.platform.MediaReference("file:///m/goal2.mov", com.playerid.app.platform.MediaKind.VIDEO)
        val save = com.playerid.app.platform.MediaReference("file:///m/save.mov", com.playerid.app.platform.MediaKind.VIDEO)
        val snapshot = TeamWorkspaceSnapshot(captureMomentTags = mapOf("goal.mov" to "Goal", "goal2.mov" to "Goal", "save.mov" to "Save"))
        val command = parseMemoryReelCommand("create reel of all scores")!!
        assertEquals(listOf(goal, goal2), searchCaptureLibrary(listOf(goal, save, goal2), snapshot, command.searchQuery))
    }
}
