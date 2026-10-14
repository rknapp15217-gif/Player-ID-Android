package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class MemorySearchQueryTest {
    @Test
    fun allSavesInYearKeepsOnlyYearAsTextTerm() {
        val query = parseMemorySearchQuery("Show me all saves from 2026")

        assertEquals("Save", query.momentTag)
        assertEquals(listOf("2026"), query.terms)
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
}