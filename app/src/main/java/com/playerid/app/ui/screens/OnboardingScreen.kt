package com.playerid.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.playerid.app.R
import com.playerid.app.data.Player
import com.playerid.app.data.Team
import com.playerid.app.ui.theme.SpotrPrimaryBlue
import com.playerid.app.viewmodels.PlayerViewModel
import com.playerid.app.viewmodels.TeamViewModel

private enum class OnboardingStep { Welcome, Choice, Teams, TeamPreview, Player, Ready }

@Composable
fun OnboardingScreen(
    teamViewModel: TeamViewModel,
    playerViewModel: PlayerViewModel,
    onComplete: (String) -> Unit
) {
    val availableTeams by teamViewModel.availableTeams.collectAsState()
    val allPlayers by playerViewModel.allPlayers.collectAsState(initial = emptyList())
    var step by remember { mutableStateOf(OnboardingStep.Welcome) }
    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    fun joinSelectedTeam(player: Player?) {
        val team = selectedTeam ?: return
        teamViewModel.replaceSubscriptionsWithTeam(team.name)
        playerViewModel.setSelectedTeam(team.name)
        player?.let {
            teamViewModel.assignPlayerToTeam(team.name, it.name)
        }
    }

    BackHandler(step != OnboardingStep.Welcome) {
        step = when (step) {
            OnboardingStep.Choice -> OnboardingStep.Welcome
            OnboardingStep.Teams -> OnboardingStep.Choice
            OnboardingStep.TeamPreview -> OnboardingStep.Teams
            OnboardingStep.Player -> OnboardingStep.TeamPreview
            OnboardingStep.Ready -> OnboardingStep.Player
            OnboardingStep.Welcome -> OnboardingStep.Welcome
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (step) {
            OnboardingStep.Welcome -> WelcomeOnboarding(
                onGetStarted = { step = OnboardingStep.Choice }
            )
            OnboardingStep.Choice -> OnboardingChoice(
                onJoin = { step = OnboardingStep.Teams },
                onCreate = {
                    teamViewModel.clearTeamSelection()
                    playerViewModel.setSelectedTeam(null)
                    onComplete("teams/create")
                },
                onLater = {
                    teamViewModel.clearTeamSelection()
                    playerViewModel.setSelectedTeam(null)
                    onComplete("camera")
                }
            )
            OnboardingStep.Teams -> TeamSearchOnboarding(
                teams = availableTeams,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onBack = { step = OnboardingStep.Choice },
                onTeamSelected = { team ->
                    selectedTeam = team
                    selectedPlayer = null
                    step = OnboardingStep.TeamPreview
                }
            )
            OnboardingStep.TeamPreview -> TeamPreviewOnboarding(
                team = selectedTeam ?: return@Surface,
                players = allPlayers.filter { it.team == selectedTeam?.name },
                onBack = { step = OnboardingStep.Teams },
                onJoin = { step = OnboardingStep.Player }
            )
            OnboardingStep.Player -> PlayerSelectionOnboarding(
                team = selectedTeam ?: return@Surface,
                players = allPlayers.filter { it.team == selectedTeam?.name },
                selectedPlayer = selectedPlayer,
                onPlayerSelected = { selectedPlayer = it },
                onBack = { step = OnboardingStep.TeamPreview },
                onContinue = {
                    joinSelectedTeam(selectedPlayer)
                    step = OnboardingStep.Ready
                },
                onSkip = {
                    selectedPlayer = null
                    joinSelectedTeam(null)
                    step = OnboardingStep.Ready
                }
            )
            OnboardingStep.Ready -> ReadyOnboarding(
                team = selectedTeam ?: return@Surface,
                player = selectedPlayer,
                onCapture = { onComplete("camera") },
                onRoster = { onComplete("teams/roster/${Uri.encode(selectedTeam?.name.orEmpty())}") }
            )
        }
    }
}

@Composable
private fun WelcomeOnboarding(onGetStarted: () -> Unit) {
    OnboardingFrame(page = 0) {
        Spacer(Modifier.weight(1f))
        androidx.compose.foundation.Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(26.dp))
        )
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
    teams: List<Team>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onTeamSelected: (Team) -> Unit
) {
    val filtered = remember(teams, query) {
        teams.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    OnboardingFrame(page = 2, onBack = onBack) {
        Text("Find your team", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Search by team name", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                items(filtered, key = { team: Team -> team.id.ifBlank { team.name } }) { team: Team ->
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
private fun TeamPreviewOnboarding(team: Team, players: List<Player>, onBack: () -> Unit, onJoin: () -> Unit) {
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
    team: Team,
    players: List<Player>,
    selectedPlayer: Player?,
    onPlayerSelected: (Player?) -> Unit,
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
            items(players, key = { player: Player -> player.id }) { player: Player ->
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
private fun ReadyOnboarding(team: Team, player: Player?, onCapture: () -> Unit, onRoster: () -> Unit) {
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
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
private fun TeamMonogram(team: Team, size: Int = 50) {
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