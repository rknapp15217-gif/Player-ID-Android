package com.playerid.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.playerid.app.data.*
import java.util.UUID

class TeamViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PlayerDatabase.getDatabase(application)
    private val teamDao = database.teamDao()
    private val playerDao = database.playerDao()
    private val subscriptionDao = database.userTeamSubscriptionDao()

    private val _selectedTeam = MutableStateFlow<String?>(null)
    val selectedTeam: StateFlow<String?> = _selectedTeam.asStateFlow()

    private val _isTeamSelected = MutableStateFlow(false)
    val isTeamSelected: StateFlow<Boolean> = _isTeamSelected.asStateFlow()

    private val _learnedTeamColor = MutableStateFlow<String?>(null)
    val learnedTeamColor: StateFlow<String?> = _learnedTeamColor.asStateFlow()

    // Teams the user has subscribed to (for "My Teams" screen)
    private val _subscribedTeams = MutableStateFlow<List<Team>>(emptyList())
    val subscribedTeams: StateFlow<List<Team>> = _subscribedTeams.asStateFlow()

    private val _subscribedTeamsWithStats = MutableStateFlow<List<TeamWithPlayerCount>>(emptyList())
    val subscribedTeamsWithStats: StateFlow<List<TeamWithPlayerCount>> = _subscribedTeamsWithStats.asStateFlow()

    // All teams available for discovery (for "Browse All Teams" screen)
    private val _availableTeams = MutableStateFlow<List<Team>>(emptyList())
    val availableTeams: StateFlow<List<Team>> = _availableTeams.asStateFlow()

    // Teams with crowd-sourced statistics (for "Browse All Teams" screen)
    private val _teamsWithStats = MutableStateFlow<List<TeamWithPlayerCount>>(emptyList())
    val teamsWithStats: StateFlow<List<TeamWithPlayerCount>> = _teamsWithStats.asStateFlow()

    // Current user identifier (stable across app sessions for testing)
    private val currentUser = "TestUser_Ryan"

    // All team names for duplicate detection
    private val _allTeamNames = MutableStateFlow<List<String>>(emptyList())
    val allTeamNames: StateFlow<List<String>> = _allTeamNames.asStateFlow()

    init {
        viewModelScope.launch {
            // Initialize default teams first and wait for completion
            initializeDefaultTeamsIfNeeded()

            // Then load data
            loadTeamStatistics()
            loadAllTeamNames()
            loadSubscribedTeams()
        }
        // Load teams from database separately
        loadTeamsFromDatabase()
    }

    private fun loadTeamsFromDatabase() {
        viewModelScope.launch {
            teamDao.getAllActiveTeams().collect { teams ->
                _availableTeams.value = teams
            }
        }
    }

    private fun loadTeamStatistics() {
        viewModelScope.launch {
            try {
                val stats = teamDao.getTeamsWithPlayerCounts()
                println("TeamViewModel: Loaded ${stats.size} teams with stats")
                stats.forEach { team ->
                    println("TeamViewModel: Team: ${team.name}, active: ${team.isActive}, archived: ${team.isArchived}")
                }
                _teamsWithStats.value = stats
            } catch (e: Exception) {
                println("TeamViewModel: Error loading team statistics: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadAllTeamNames() {
        viewModelScope.launch {
            _allTeamNames.value = teamDao.getAllActiveTeamNames()
        }
    }

    private fun loadSubscribedTeams() {
        viewModelScope.launch {
            subscriptionDao.getUserSubscribedTeams(currentUser).collect { teams ->
                _subscribedTeams.value = teams
            }
        }

        viewModelScope.launch {
            subscriptionDao.getUserSubscribedTeamsWithStats(currentUser).collect { teamsWithStats ->
                _subscribedTeamsWithStats.value = teamsWithStats
            }
        }
    }

    private suspend fun initializeDefaultTeamsIfNeeded() {
        try {
            // Use direct query instead of Flow for initialization check
            val existingTeamCount = teamDao.getActiveTeamCount()
            println("TeamViewModel: Found $existingTeamCount existing teams")

            // Check if Ryan's Team exists specifically
            val ryansTeam = teamDao.getTeamByName("Ryan's Team")

            if (existingTeamCount == 0) {
                // Add realistic crowd-sourced teams with variety
                val defaultTeams = listOf(
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "North Allegheny Lacrosse",
                        description = "High school varsity lacrosse - Spring season",
                        color = "#1976D2",
                        createdBy = "Coach_Thompson"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "True Lacrosse Club",
                        description = "Elite youth lacrosse training - All ages",
                        color = "#E53E3E",
                        createdBy = "Director_Walsh"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "Pittsburgh Panthers JV",
                        description = "Junior varsity football team",
                        color = "#FFD700",
                        createdBy = "Coach_Williams"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "Western PA Elite Soccer",
                        description = "Premier soccer club - multiple age groups",
                        color = "#0EA5E9",
                        createdBy = "Club_Director"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "Thunder Volleyball",
                        description = "High school girls varsity - district champions",
                        color = "#8B5CF6",
                        createdBy = "Player_Emma23"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "Warriors JV Football",
                        description = "Junior varsity - Friday night lights",
                        color = "#059669",
                        createdBy = "Dad_CoachTom"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "Phoenix Track Club",
                        description = "Regional track and field - all ages welcome",
                        color = "#DC2626",
                        createdBy = "RunnerMom_Kim"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "Stallions Baseball",
                        description = "Little league majors - spring season",
                        color = "#7C2D12",
                        createdBy = "BaseballDad_Joe"
                    ),
                    Team(
                        id = UUID.randomUUID().toString(),
                        name = "Ryan's Team",
                        description = "Your personal test team with #10 Tyson Knapp",
                        color = "#FF6B35",
                        createdBy = "Ryan"
                    )
                )

                defaultTeams.forEach { team ->
                    teamDao.insertTeam(team)
                    println("TeamViewModel: Inserted team: ${team.name}")

                    // Auto-subscribe to Ryan's Team and a couple others for better UX
                    if (team.name == "Ryan's Team" || team.name == "North Allegheny Lacrosse" || team.name == "True Lacrosse Club") {
                        val subscription = UserTeamSubscription(
                            userId = currentUser,
                            teamName = team.name
                        )
                        subscriptionDao.subscribeToTeam(subscription)
                        println("TeamViewModel: Auto-subscribed to ${team.name}")
                    }
                }

                // Refresh after initialization
                loadTeamStatistics()
                loadAllTeamNames()
                loadSubscribedTeams()

                // Auto-select Ryan's Team for immediate testing
                selectTeam("Ryan's Team")
                println("TeamViewModel: Initialized ${defaultTeams.size} default teams and selected Ryan's Team")
            }

            // ALWAYS ensure Ryan's Team exists, regardless of other teams
            if (ryansTeam == null) {
                println("TeamViewModel: Ryan's Team not found, adding it now...")
                val ryansTeamData = Team(
                    id = UUID.randomUUID().toString(),
                    name = "Ryan's Team",
                    description = "Your personal test team with #10 Tyson Knapp",
                    color = "#FF6B35",
                    createdBy = "Ryan"
                )
                teamDao.insertTeam(ryansTeamData)

                // Auto-subscribe to Ryan's Team
                val subscription = UserTeamSubscription(
                    userId = currentUser,
                    teamName = "Ryan's Team"
                )
                subscriptionDao.subscribeToTeam(subscription)

                // Select Ryan's Team
                selectTeam("Ryan's Team")
                println("TeamViewModel: Added and selected Ryan's Team")
            } else {
                // Ryan's Team exists, just select it
                selectTeam("Ryan's Team")
                println("TeamViewModel: Found existing Ryan's Team, selected it")
            }

            // ALWAYS ensure Ryan's Team subscription exists (in case it was lost)
            try {
                val isSubscribed = subscriptionDao.isUserSubscribedToTeam(currentUser, "Ryan's Team")
                if (!isSubscribed) {
                    println("TeamViewModel: Ryan's Team subscription missing, adding it...")
                    val subscription = UserTeamSubscription(
                        userId = currentUser,
                        teamName = "Ryan's Team"
                    )
                    subscriptionDao.subscribeToTeam(subscription)
                    println("TeamViewModel: Added Ryan's Team subscription")
                } else {
                    println("TeamViewModel: Ryan's Team subscription already exists")
                }
            } catch (e: Exception) {
                println("TeamViewModel: Error checking subscription: ${e.message}")
            }

        } catch (e: Exception) {
            println("TeamViewModel: Error initializing teams: ${e.message}")
            e.printStackTrace()
        }
    }

    fun selectTeam(teamName: String) {
        _selectedTeam.value = teamName
        _isTeamSelected.value = true
    }

    fun clearTeamSelection() {
        _selectedTeam.value = null
        _isTeamSelected.value = false
        _learnedTeamColor.value = null
    }

    fun learnTeamColor(color: String, teamName: String) {
        _learnedTeamColor.value = color
        selectTeam(teamName)

        // Update team color in database
        viewModelScope.launch {
            val team = teamDao.getTeamByName(teamName)
            if (team != null) {
                val updatedTeam = team.copy(
                    color = color,
                    updatedAt = System.currentTimeMillis()
                )
                teamDao.updateTeam(updatedTeam)
            }
        }
    }

    fun addTeam(teamName: String, description: String = "") {
        viewModelScope.launch {
            val existingTeam = teamDao.getTeamByName(teamName)
            if (existingTeam == null) {
                val newTeam = Team(
                    id = UUID.randomUUID().toString(),
                    name = teamName,
                    description = description,
                    createdBy = currentUser
                )
                teamDao.insertTeam(newTeam)
                loadTeamStatistics() // Refresh stats
                loadAllTeamNames() // Refresh team names

                // Auto-subscribe user to the team they created
                subscribeToTeam(teamName)
            }
        }
    }

    fun renameTeam(oldName: String, newName: String) {
        viewModelScope.launch {
            teamDao.renameTeam(oldName, newName)

            // Update selection if renamed team was selected
            if (_selectedTeam.value == oldName) {
                _selectedTeam.value = newName
            }

            loadTeamStatistics() // Refresh stats
            loadAllTeamNames() // Refresh team names
        }
    }

    fun deleteTeam(teamName: String) {
        viewModelScope.launch {
            teamDao.deactivateTeam(teamName)

            // Clear selection if selected team was deleted
            if (_selectedTeam.value == teamName) {
                clearTeamSelection()
            }

            loadTeamStatistics() // Refresh stats
            loadAllTeamNames() // Refresh team names
        }
    }

    suspend fun getTeamContributors(teamName: String): List<String> {
        return teamDao.getTeamContributors(teamName)
    }

    fun reportTeamAsDuplicate(teamName: String) {
        viewModelScope.launch {
            teamDao.reportTeam(teamName)
            loadTeamStatistics() // Refresh to show updated counts
        }
    }

    fun verifyTeam(teamName: String) {
        viewModelScope.launch {
            teamDao.verifyTeam(teamName)
            loadTeamStatistics() // Refresh to show updated counts
        }
    }

    fun archiveInactiveTeams() {
        viewModelScope.launch {
            // Archive teams inactive for more than 6 months with negative community feedback
            val sixMonthsAgo = System.currentTimeMillis() - (6 * 30 * 24 * 60 * 60 * 1000L)
            teamDao.archiveInactiveTeams(sixMonthsAgo)
            loadTeamStatistics()
            loadAllTeamNames()
        }
    }

    fun checkForSimilarTeams(teamName: String): List<com.playerid.app.utils.TeamSimilarityUtil.SimilarTeam> {
        val existingTeams = _allTeamNames.value
        return com.playerid.app.utils.TeamSimilarityUtil.findSimilarTeams(teamName, existingTeams)
    }

    fun getCurrentUser(): String = currentUser

    // Team subscription management
    fun subscribeToTeam(teamName: String) {
        viewModelScope.launch {
            val subscription = UserTeamSubscription(
                userId = currentUser,
                teamName = teamName,
                subscribedAt = System.currentTimeMillis()
            )
            subscriptionDao.subscribeToTeam(subscription)

            // Set as selected team after subscribing
            selectTeam(teamName)
        }
    }

    fun unsubscribeFromTeam(teamName: String) {
        viewModelScope.launch {
            subscriptionDao.unsubscribeFromTeam(currentUser, teamName)

            // Clear selection if unsubscribed from selected team
            if (_selectedTeam.value == teamName) {
                clearTeamSelection()
            }
        }
    }

    suspend fun isSubscribedToTeam(teamName: String): Boolean {
        return subscriptionDao.isUserSubscribedToTeam(currentUser, teamName)
    }

    suspend fun getUserSubscriptionCount(): Int {
        return subscriptionDao.getUserSubscriptionCount(currentUser)
    }

    // Get team names for backwards compatibility
    fun getAvailableTeamNames(): List<String> {
        return _availableTeams.value.map { it.name }
    }
}