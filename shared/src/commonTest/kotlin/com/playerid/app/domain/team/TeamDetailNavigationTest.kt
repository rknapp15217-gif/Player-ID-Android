package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamDetailNavigationTest {
    @Test
    fun initialPageHonorsRosterDeepLink() {
        assertEquals(TeamDetailPage.Overview, initialTeamDetailPage(openRosterInitially = false))
        assertEquals(TeamDetailPage.Roster, initialTeamDetailPage(openRosterInitially = true))
    }

    @Test
    fun navigationEventsSelectEachDetailPage() {
        assertEquals(
            TeamDetailPage.Roster,
            TeamDetailPage.Overview.reduce(TeamDetailNavigationEvent.RosterSelected)
        )
        assertEquals(
            TeamDetailPage.Schedule,
            TeamDetailPage.Roster.reduce(TeamDetailNavigationEvent.ScheduleSelected)
        )
        assertEquals(
            TeamDetailPage.Overview,
            TeamDetailPage.Schedule.reduce(TeamDetailNavigationEvent.OverviewSelected)
        )
    }
}