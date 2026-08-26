package com.playerid.app.domain.team

import com.playerid.app.utils.TeamSimilarityUtil

object TeamCreationOptions {
    val sports = listOf(
        "Soccer",
        "Basketball",
        "Hockey",
        "Baseball",
        "Football",
        "Lacrosse",
        "Volleyball",
        "Other"
    )

    val presetColors = listOf(
        "Navy" to "#0B3D91",
        "Royal" to "#1976D2",
        "Red" to "#E53E3E",
        "Maroon" to "#7A0019",
        "Green" to "#059669",
        "Black" to "#111827",
        "White" to "#FFFFFF",
        "Gray" to "#9CA3AF",
        "Gold" to "#D4AF37",
        "Orange" to "#EA580C",
        "Purple" to "#7C3AED",
        "Teal" to "#0D9488"
    )
}

data class TeamCreationFormState(
    val teamName: String = "",
    val sport: String = "Soccer",
    val homeJerseyColor: String = "#1976D2",
    val awayJerseyColor: String = "#FFFFFF"
) {
    val canSubmit: Boolean
        get() = teamName.isNotBlank()

    fun similarTeams(existingTeams: List<String>): List<TeamSimilarityUtil.SimilarTeam> {
        if (!canSubmit || existingTeams.isEmpty()) return emptyList()
        return TeamSimilarityUtil.findSimilarTeams(teamName, existingTeams)
    }

    fun submission(): TeamCreationSubmission? {
        if (!canSubmit) return null
        return TeamCreationSubmission(
            teamName = teamName.trim(),
            sport = sport,
            homeJerseyColor = homeJerseyColor,
            awayJerseyColor = awayJerseyColor
        )
    }

    fun reduce(event: TeamCreationEvent): TeamCreationFormState = when (event) {
        is TeamCreationEvent.TeamNameChanged -> copy(teamName = event.teamName)
        is TeamCreationEvent.SportSelected -> copy(sport = event.sport)
        is TeamCreationEvent.HomeJerseyColorSelected -> copy(homeJerseyColor = event.colorHex)
        is TeamCreationEvent.AwayJerseyColorSelected -> copy(awayJerseyColor = event.colorHex)
    }
}

sealed interface TeamCreationEvent {
    data class TeamNameChanged(val teamName: String) : TeamCreationEvent
    data class SportSelected(val sport: String) : TeamCreationEvent
    data class HomeJerseyColorSelected(val colorHex: String) : TeamCreationEvent
    data class AwayJerseyColorSelected(val colorHex: String) : TeamCreationEvent
}

data class TeamCreationSubmission(
    val teamName: String,
    val sport: String,
    val homeJerseyColor: String,
    val awayJerseyColor: String
)