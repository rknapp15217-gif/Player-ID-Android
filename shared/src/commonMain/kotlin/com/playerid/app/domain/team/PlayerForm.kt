package com.playerid.app.domain.team

object PlayerFormOptions {
    val academicYears = listOf("Freshman", "Sophomore", "Junior", "Senior")
}

data class PlayerFormState(
    val name: String = "",
    val number: String = "",
    val position: String = "",
    val teamName: String = "",
    val academicYear: String = PlayerFormOptions.academicYears.first()
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && number.isNotBlank() && position.isNotBlank()

    fun submission(): PlayerFormSubmission? {
        if (!canSubmit) return null
        return PlayerFormSubmission(
            name = name.trim(),
            number = number.trim(),
            position = position.trim(),
            teamName = teamName,
            academicYear = academicYear
        )
    }

    fun reduce(event: PlayerFormEvent): PlayerFormState = when (event) {
        is PlayerFormEvent.NameChanged -> copy(name = event.value)
        is PlayerFormEvent.NumberChanged -> copy(number = event.value)
        is PlayerFormEvent.PositionChanged -> copy(position = event.value)
        is PlayerFormEvent.TeamSelected -> copy(teamName = event.value)
        is PlayerFormEvent.AcademicYearSelected -> copy(academicYear = event.value)
    }

    companion object {
        fun forNewPlayer(teamName: String) = PlayerFormState(teamName = teamName)

        fun forEditing(player: PlayerProfile) = PlayerFormState(
            name = player.name,
            number = player.number,
            position = player.position,
            teamName = player.teamName,
            academicYear = player.academicYear
        )
    }
}

sealed interface PlayerFormEvent {
    data class NameChanged(val value: String) : PlayerFormEvent
    data class NumberChanged(val value: String) : PlayerFormEvent
    data class PositionChanged(val value: String) : PlayerFormEvent
    data class TeamSelected(val value: String) : PlayerFormEvent
    data class AcademicYearSelected(val value: String) : PlayerFormEvent
}

data class PlayerFormSubmission(
    val name: String,
    val number: String,
    val position: String,
    val teamName: String,
    val academicYear: String
)