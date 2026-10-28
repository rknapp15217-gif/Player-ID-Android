package com.playerid.app.domain.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AppNavigationTest {
    @Test
    fun appStartsOnCameraWithAndroidTabOrder() {
        val state = AppNavigationState()
        assertEquals(AppDestination.Camera, state.destination)
        assertEquals(1, state.selectedTabIndex)
        assertEquals(AppDestination.Memories, state.selectTab(0).destination)
        assertEquals(AppDestination.Camera, state.selectTab(1).destination)
        assertEquals(AppDestination.Go, state.selectTab(2).destination)
        assertEquals(2, state.selectTab(2).selectedTabIndex)
    }

    @Test
    fun settingsMenuAndNestedDestinationsDoNotSelectBottomTabs() {
        listOf(AppDestination.More, AppDestination.Teams, AppDestination.Settings).forEach {
            assertEquals(-1, AppNavigationState().navigate(it).selectedTabIndex)
        }
    }

    @Test
    fun settingsMenuRemainsReachableAfterSwitchingTabs() {
        val teams = AppNavigationState().navigate(AppDestination.Teams)
        assertEquals(AppDestination.More, teams.selectTab(2).navigate(AppDestination.More).destination)
        val settings = teams.navigate(AppDestination.Settings)
        assertEquals(AppDestination.More, settings.selectTab(0).navigate(AppDestination.More).destination)
    }

    @Test
    fun backFromTeamsAndSettingsReturnsToMore() {
        listOf(AppDestination.Teams, AppDestination.Settings).forEach {
            assertEquals(AppDestination.More, AppNavigationState().navigate(it).back().destination)
        }
        assertEquals(AppDestination.Memories, AppNavigationState().navigate(AppDestination.More).back().destination)
    }

    @Test
    fun returningFromTeamsOpensMoreMenu() {
        assertEquals(
            AppDestination.More,
            AppNavigationState().navigate(AppDestination.Teams).back().destination
        )
    }

    @Test
    fun goHasItsOwnTabAndBackReturnsToCamera() {
        val state = AppNavigationState().navigate(AppDestination.Settings).selectTab(2)
        assertEquals(AppDestination.Go, state.destination)
        assertEquals(AppDestination.Settings, state.previousMoreDestination)
        assertEquals(AppDestination.Go, state.selectTab(2).destination)
        assertEquals(AppDestination.Camera, state.back().destination)
        assertEquals(AppDestination.More, state.back().navigate(AppDestination.More).destination)
    }

    @Test
    fun invalidTabIsNotSilentlyIgnored() {
        assertFailsWith<IllegalArgumentException> { AppNavigationState().selectTab(-1) }
        assertFailsWith<IllegalArgumentException> { AppNavigationState().selectTab(3) }
        assertFailsWith<IllegalArgumentException> { AppNavigationState().selectTab(4) }
    }

    @Test
    fun everyGoTapCreatesFreshLandingIncludingReselectionAndReturn() {
        val first = AppNavigationState().selectTab(2)
        val again = first.selectTab(2)
        val returned = again.selectTab(0).selectTab(2)
        assertEquals(1, first.goOpenGeneration)
        assertEquals(2, again.goOpenGeneration)
        assertEquals(3, returned.goOpenGeneration)
        assertEquals(AppDestination.Go, returned.destination)
        assertEquals(returned.goOpenGeneration, returned.navigate(AppDestination.More).goOpenGeneration)
    }

    @Test
    fun onboardingPreservesAndroidStepOrder() {
        var state = OnboardingState()
        assertEquals(OnboardingStep.Welcome, state.step)
        state = state.reduce(OnboardingEvent.Started)
        assertEquals(OnboardingStep.Choice, state.step)
        state = state.reduce(OnboardingEvent.JoinRequested)
        assertEquals(OnboardingStep.Teams, state.step)
        state = state.reduce(OnboardingEvent.SearchChanged("Tigers"))
        assertEquals("Tigers", state.searchQuery)
        state = state.reduce(OnboardingEvent.TeamSelected("Tigers"))
        assertEquals(OnboardingStep.TeamPreview, state.step)
        state = state.reduce(OnboardingEvent.TeamConfirmed)
        assertEquals(OnboardingStep.Player, state.step)
        state = state.reduce(OnboardingEvent.PlayerSelected("player-1"))
        assertEquals(OnboardingStep.Player, state.step)
        state = state.reduce(OnboardingEvent.JoinCompleted("player-1"))
        assertEquals(OnboardingStep.Ready, state.step)
        assertEquals("Tigers", state.selectedTeamName)
        assertEquals("player-1", state.selectedPlayerId)
    }

    @Test
    fun choosingAnotherTeamClearsPreviousPlayer() {
        val state = OnboardingState(
            step = OnboardingStep.Teams, selectedTeamName = "Tigers", selectedPlayerId = "player-1"
        ).reduce(OnboardingEvent.TeamSelected("Lions"))
        assertEquals("Lions", state.selectedTeamName)
        assertNull(state.selectedPlayerId)
    }

    @Test
    fun skipClearsPreviouslyChosenPlayer() {
        val state = OnboardingState(step = OnboardingStep.Player, selectedPlayerId = "player-1")
            .reduce(OnboardingEvent.JoinCompleted(null))
        assertEquals(OnboardingStep.Ready, state.step)
        assertNull(state.selectedPlayerId)
    }

    @Test
    fun onboardingBackStepsPreserveSelection() {
        val expected = mapOf(
            OnboardingStep.Ready to OnboardingStep.Player,
            OnboardingStep.Player to OnboardingStep.TeamPreview,
            OnboardingStep.TeamPreview to OnboardingStep.Teams,
            OnboardingStep.Teams to OnboardingStep.Choice,
            OnboardingStep.Choice to OnboardingStep.Welcome,
            OnboardingStep.Welcome to OnboardingStep.Welcome
        )
        expected.forEach { (from, to) ->
            val state = OnboardingState(from, "Tigers", "player-1").reduce(OnboardingEvent.Back)
            assertEquals(to, state.step)
            assertEquals("Tigers", state.selectedTeamName)
            assertEquals("player-1", state.selectedPlayerId)
        }
    }
}
