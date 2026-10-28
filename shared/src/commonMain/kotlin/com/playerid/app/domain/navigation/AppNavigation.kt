package com.playerid.app.domain.navigation

enum class AppDestination {
    Memories, Camera, Go, More, Teams, Settings
}

data class AppNavigationState(
    val destination: AppDestination = AppDestination.Camera,
    val previousMoreDestination: AppDestination = AppDestination.More,
    val goOpenGeneration: Int = 0
) {
    val selectedTabIndex: Int
        get() = when (destination) {
            AppDestination.Memories -> 0
            AppDestination.Camera -> 1
            AppDestination.Go -> 2
            else -> -1
        }

    fun navigate(destination: AppDestination): AppNavigationState = copy(
        destination = destination,
        previousMoreDestination = if (destination in moreDestinations) destination else previousMoreDestination
    )

    fun selectTab(index: Int): AppNavigationState = when (index) {
        0 -> navigate(AppDestination.Memories)
        1 -> navigate(AppDestination.Camera)
        2 -> navigate(AppDestination.Go).copy(goOpenGeneration = goOpenGeneration + 1)
        else -> throw IllegalArgumentException("Unknown navigation tab: $index")
    }

    fun back(): AppNavigationState = navigate(when (destination) {
        AppDestination.Teams, AppDestination.Settings -> AppDestination.More
        AppDestination.More -> AppDestination.Memories
        else -> AppDestination.Camera
    })

    companion object {
        private val moreDestinations = setOf(AppDestination.More, AppDestination.Teams, AppDestination.Settings)
    }
}

enum class OnboardingStep { Welcome, Choice, Teams, TeamPreview, Player, Ready }

sealed interface OnboardingDestination {
    data object Camera : OnboardingDestination
    data object ContinueWithoutTeam : OnboardingDestination
    data object CreateTeam : OnboardingDestination
    data class Roster(val teamName: String) : OnboardingDestination
}

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val selectedTeamName: String? = null,
    val selectedPlayerId: String? = null,
    val searchQuery: String = ""
) {
    fun reduce(event: OnboardingEvent): OnboardingState = when (event) {
        OnboardingEvent.Started -> copy(step = OnboardingStep.Choice)
        OnboardingEvent.JoinRequested -> copy(step = OnboardingStep.Teams)
        is OnboardingEvent.SearchChanged -> copy(searchQuery = event.query)
        is OnboardingEvent.TeamSelected -> copy(
            step = OnboardingStep.TeamPreview, selectedTeamName = event.teamName, selectedPlayerId = null
        )
        OnboardingEvent.TeamConfirmed -> copy(step = OnboardingStep.Player)
        is OnboardingEvent.PlayerSelected -> copy(selectedPlayerId = event.playerId)
        is OnboardingEvent.JoinCompleted -> copy(step = OnboardingStep.Ready, selectedPlayerId = event.playerId)
        OnboardingEvent.Back -> copy(step = when (step) {
            OnboardingStep.Welcome, OnboardingStep.Choice -> OnboardingStep.Welcome
            OnboardingStep.Teams -> OnboardingStep.Choice
            OnboardingStep.TeamPreview -> OnboardingStep.Teams
            OnboardingStep.Player -> OnboardingStep.TeamPreview
            OnboardingStep.Ready -> OnboardingStep.Player
        })
    }
}

sealed interface OnboardingEvent {
    data object Started : OnboardingEvent
    data object JoinRequested : OnboardingEvent
    data class SearchChanged(val query: String) : OnboardingEvent
    data class TeamSelected(val teamName: String) : OnboardingEvent
    data object TeamConfirmed : OnboardingEvent
    data class PlayerSelected(val playerId: String?) : OnboardingEvent
    data class JoinCompleted(val playerId: String?) : OnboardingEvent
    data object Back : OnboardingEvent
}
