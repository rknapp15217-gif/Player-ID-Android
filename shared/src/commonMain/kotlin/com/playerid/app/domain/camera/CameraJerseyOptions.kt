package com.playerid.app.domain.camera

import com.playerid.app.domain.team.TeamProfile

fun cameraJerseyOptions(team: TeamProfile?): List<Pair<String, String>> =
    if (team?.name == "North Allegheny Lacrosse") {
        listOf("Black" to "#000000", "White" to "#FFFFFF", "Gold" to "#FFB81C")
    } else {
        listOf(
            "Home" to (team?.homeJerseyColor ?: team?.color ?: "#1976D2"),
            "Away" to (team?.awayJerseyColor ?: team?.awayColor ?: "#FFFFFF")
        )
    }
