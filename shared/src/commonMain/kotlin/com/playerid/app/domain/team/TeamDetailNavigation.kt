package com.playerid.app.domain.team

enum class TeamDetailPage {
    Overview,
    Roster,
    Schedule
}

fun initialTeamDetailPage(openRosterInitially: Boolean): TeamDetailPage {
    return if (openRosterInitially) TeamDetailPage.Roster else TeamDetailPage.Overview
}

fun TeamDetailPage.reduce(event: TeamDetailNavigationEvent): TeamDetailPage = when (event) {
    TeamDetailNavigationEvent.OverviewSelected -> TeamDetailPage.Overview
    TeamDetailNavigationEvent.RosterSelected -> TeamDetailPage.Roster
    TeamDetailNavigationEvent.ScheduleSelected -> TeamDetailPage.Schedule
}

enum class TeamDetailNavigationEvent {
    OverviewSelected,
    RosterSelected,
    ScheduleSelected
}