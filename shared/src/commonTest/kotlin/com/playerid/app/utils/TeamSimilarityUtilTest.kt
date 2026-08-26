package com.playerid.app.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamSimilarityUtilTest {
    @Test
    fun normalizesCommonSchoolAndSportAbbreviations() {
        assertTrue(TeamSimilarityUtil.areTeamsHighlySimilar("North HS LAX", "North High School Lacrosse"))
    }

    @Test
    fun keepsDistinctTeamsSeparate() {
        assertFalse(TeamSimilarityUtil.areTeamsSimilar("North Allegheny Football", "Seneca Valley Lacrosse"))
    }

    @Test
    fun returnsMatchesInSimilarityOrder() {
        val matches = TeamSimilarityUtil.findSimilarTeams(
            "North Allegheny HS Football",
            listOf("North Allegheny High School Soccer", "North Hills High School Soccer")
        )

        assertEquals("North Allegheny High School Soccer", matches.first().name)
    }
}
