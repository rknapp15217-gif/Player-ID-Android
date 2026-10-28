package com.playerid.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.TeamProfile
import com.playerid.app.domain.navigation.OnboardingDestination
import com.playerid.app.domain.navigation.OnboardingEvent
import com.playerid.app.domain.navigation.OnboardingState
import com.playerid.app.domain.navigation.OnboardingStep
import com.playerid.app.ui.theme.SpotrPrimaryBlue

@Composable
fun OnboardingFlow(
    availableTeams: List<TeamProfile>,
    allPlayers: List<PlayerProfile>,
    logo: @Composable () -> Unit,
    onJoinTeam: (TeamProfile, PlayerProfile?, () -> Unit) -> Unit,
    onComplete: (OnboardingDestination) -> Unit,
    backHandler: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
    teamSearchNotice: String? = null
) {
    var state by remember { mutableStateOf(OnboardingState()) }
    val selectedTeam = availableTeams.firstOrNull { it.name == state.selectedTeamName }
    val players = allPlayers.filter { it.teamName == selectedTeam?.name }
    val selectedPlayer = players.firstOrNull { it.id == state.selectedPlayerId }
    fun event(event: OnboardingEvent) { state = state.reduce(event) }
    fun join(player: PlayerProfile?) {
        val team = requireNotNull(selectedTeam) { "Select a team before joining." }
        onJoinTeam(team, player) { event(OnboardingEvent.JoinCompleted(player?.id)) }
    }
    backHandler(state.step != OnboardingStep.Welcome) { event(OnboardingEvent.Back) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state.step) {
            OnboardingStep.Welcome -> WelcomeOnboarding(
                onGetStarted = { event(OnboardingEvent.Started) },
                logo = logo
            )
            OnboardingStep.Choice -> OnboardingChoice(
                onJoin = { event(OnboardingEvent.JoinRequested) },
                onCreate = { onComplete(OnboardingDestination.CreateTeam) },
                onLater = { onComplete(OnboardingDestination.ContinueWithoutTeam) }
            )
            OnboardingStep.Teams -> TeamSearchOnboarding(
                teams = availableTeams,
                query = state.searchQuery,
                onQueryChange = { event(OnboardingEvent.SearchChanged(it)) },
                onBack = { event(OnboardingEvent.Back) },
                onTeamSelected = { event(OnboardingEvent.TeamSelected(it.name)) },
                notice = teamSearchNotice
            )
            OnboardingStep.TeamPreview -> TeamPreviewOnboarding(
                team = selectedTeam ?: return@Surface,
                players = players,
                onBack = { event(OnboardingEvent.Back) },
                onJoin = { event(OnboardingEvent.TeamConfirmed) }
            )
            OnboardingStep.Player -> PlayerSelectionOnboarding(
                team = selectedTeam ?: return@Surface,
                players = players,
                selectedPlayer = selectedPlayer,
                onPlayerSelected = { event(OnboardingEvent.PlayerSelected(it?.id)) },
                onBack = { event(OnboardingEvent.Back) },
                onContinue = { join(selectedPlayer) },
                onSkip = { join(null) }
            )
            OnboardingStep.Ready -> ReadyOnboarding(
                team = selectedTeam ?: return@Surface,
                player = selectedPlayer,
                onCapture = { onComplete(OnboardingDestination.Camera) },
                onRoster = { onComplete(OnboardingDestination.Roster(requireNotNull(selectedTeam).name)) }
            )
        }
    }
}

@Composable
private fun WelcomeOnboarding(onGetStarted: () -> Unit, logo: @Composable () -> Unit) {
    OnboardingFrame(page = 0) {
        Spacer(Modifier.weight(1f))
        logo()
        Spacer(Modifier.height(22.dp))
        Text("PlayerID", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = SpotrPrimaryBlue)
        Spacer(Modifier.height(14.dp))
        Text("Never lose the moment.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Capture, find, and relive your player's sports memories.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onGetStarted, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Get Started") }
    }
}

@Composable
private fun OnboardingChoice(onJoin: () -> Unit, onCreate: () -> Unit, onLater: () -> Unit) {
    OnboardingFrame(page = 1) {
        Spacer(Modifier.weight(1f))
        Text("What would you like to do?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        ChoiceCard(
            icon = { Icon(Icons.Default.PersonSearch, contentDescription = null, tint = SpotrPrimaryBlue) },
            title = "Find or join my team",
            subtitle = "Get the roster",
            borderColor = SpotrPrimaryBlue,
            onClick = onJoin
        )
        Spacer(Modifier.height(12.dp))
        ChoiceCard(
            icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF238636)) },
            title = "Create a team",
            subtitle = "Set up a roster",
            borderColor = Color(0xFF238636),
            onClick = onCreate
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onLater) { Text("I'll do this later") }
    }
}

@Composable
private fun ChoiceCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = BorderStroke(1.5.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TeamSearchOnboarding(
    teams: List<TeamProfile>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onTeamSelected: (TeamProfile) -> Unit,
    notice: String? = null
) {
    val filtered = remember(teams, query) {
        teams.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    OnboardingFrame(page = 2, onBack = onBack) {
        Text("Find your team", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Search by team name", color = MaterialTheme.colorScheme.onSurfaceVariant)
        notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search teams") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No teams found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { team -> team.id.ifBlank { team.name } }) { team ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onTeamSelected(team) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            TeamMonogram(team)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(team.name, fontWeight = FontWeight.SemiBold)
                                Text(team.sport, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamPreviewOnboarding(team: TeamProfile, players: List<PlayerProfile>, onBack: () -> Unit, onJoin: () -> Unit) {
    OnboardingFrame(page = 2, onBack = onBack) {
        Spacer(Modifier.weight(0.35f))
        TeamMonogram(team, size = 82)
        Spacer(Modifier.height(16.dp))
        Text(team.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(team.sport, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        Text("${players.size} players", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Button(onClick = onJoin, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Join Team") }
        TextButton(onClick = onBack) { Text("Choose another team") }
    }
}

@Composable
private fun PlayerSelectionOnboarding(
    team: TeamProfile,
    players: List<PlayerProfile>,
    selectedPlayer: PlayerProfile?,
    onPlayerSelected: (PlayerProfile?) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    OnboardingFrame(page = 3, onBack = onBack) {
        Text("Is one of these players yours?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(team.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(players, key = { player -> player.id }) { player ->
                val selected = player.id == selectedPlayer?.id
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPlayerSelected(player) },
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) SpotrPrimaryBlue else MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = if (selected) SpotrPrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerCircle(40)
                        Spacer(Modifier.width(12.dp))
                        Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = SpotrPrimaryBlue)
                    }
                }
            }
        }
        Button(onClick = onContinue, enabled = selectedPlayer != null, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Continue")
        }
        TextButton(onClick = onSkip) { Text("None / Skip") }
    }
}

@Composable
private fun ReadyOnboarding(team: TeamProfile, player: PlayerProfile?, onCapture: () -> Unit, onRoster: () -> Unit) {
    OnboardingFrame(page = 4) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(82.dp).background(SpotrPrimaryBlue.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SpotrPrimaryBlue, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("You're ready!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        player?.let {
            PlayerCircle(64)
            Spacer(Modifier.height(10.dp))
            Text(it.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(team.name, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Button(onClick = onCapture, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Capture a Memory")
        }
        TextButton(onClick = onRoster) {
            Icon(Icons.Default.Groups, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("View Roster")
        }
    }
}

@Composable
private fun OnboardingFrame(
    page: Int,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.graphicsLayer {
                            scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                        }
                    )
                }
            }
        }
        content()
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(5) { index ->
                Box(
                    Modifier.size(if (index == page) 8.dp else 6.dp)
                        .background(if (index == page) SpotrPrimaryBlue else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TeamMonogram(team: TeamProfile, size: Int = 50) {
    val initials = team.name.split(" ").filter(String::isNotBlank).take(2).joinToString("") { it.take(1) }.uppercase()
    Box(
        Modifier.size(size.dp).background(SpotrPrimaryBlue.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = SpotrPrimaryBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PlayerCircle(size: Int) {
    Box(
        Modifier.size(size.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size((size * 0.58f).dp)
        )
    }
}