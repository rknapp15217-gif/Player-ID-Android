package com.playerid.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.playerid.app.data.repositories.toProfile
import com.playerid.app.domain.navigation.OnboardingDestination
import com.playerid.app.ui.components.PlayerIDLogo
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel

@Composable
fun OnboardingScreen(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    onComplete: (String) -> Unit
) {
    val teams by teamViewModel.availableTeams.collectAsState()
    val players by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    OnboardingFlow(
        availableTeams = teams.map { it.toProfile() },
        allPlayers = players.map { it.toProfile() },
        logo = { PlayerIDLogo() },
        onJoinTeam = { team, player, onJoined ->
            teamViewModel.replaceSubscriptionsWithTeam(team.name)
            playerViewModel.setSelectedTeam(team.name)
            player?.let { teamViewModel.assignPlayerToTeam(team.name, it.name) }
            onJoined()
        },
        onComplete = { destination ->
            when (destination) {
                OnboardingDestination.CreateTeam, OnboardingDestination.ContinueWithoutTeam -> {
                    teamViewModel.clearTeamSelection()
                    playerViewModel.setSelectedTeam(null)
                }
                OnboardingDestination.Camera, is OnboardingDestination.Roster -> Unit
            }
            onComplete(when (destination) {
                OnboardingDestination.Camera, OnboardingDestination.ContinueWithoutTeam -> "camera"
                OnboardingDestination.CreateTeam -> "teams/create"
                is OnboardingDestination.Roster -> "teams/roster/${Uri.encode(destination.teamName)}"
            })
        },
        backHandler = { enabled, onBack -> BackHandler(enabled, onBack) }
    )
}
