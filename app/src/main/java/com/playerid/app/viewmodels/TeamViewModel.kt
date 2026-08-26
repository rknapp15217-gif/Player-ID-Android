package com.playerid.app.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.playerid.app.data.*
import com.playerid.app.data.repositories.RoomTeamRosterRepository
import com.playerid.app.data.repositories.RoomTeamSubscriptionRepository
import com.playerid.app.data.repositories.RoomScheduleStorageRepository
import com.playerid.app.data.repositories.toEntity
import com.playerid.app.domain.team.TeamSubscription
import com.playerid.app.domain.team.TeamSubscriptionService
import java.util.UUID

class TeamViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PlayerDatabase.getDatabase(application)
    private val teamDao = database.teamDao()
    private val playerDao = database.playerDao()
    private val subscriptionDao = database.userTeamSubscriptionDao()
    private val teamRepository = RoomTeamRosterRepository(teamDao, playerDao)
    private val teamSubscriptionRepository = RoomTeamSubscriptionRepository(subscriptionDao)
    private val teamSubscriptionService = TeamSubscriptionService(teamSubscriptionRepository)
    private val memoryOrganizationDao = database.memoryOrganizationDao()
    private val scheduleStorageRepository = RoomScheduleStorageRepository(memoryOrganizationDao)
    private val prefs = application.getSharedPreferences("team_selection", Context.MODE_PRIVATE)

    private val _kidOptions = MutableStateFlow(listOf("Tyson", "Brooklyn"))
    val kidOptions: StateFlow<List<String>> = _kidOptions.asStateFlow()

    private val _selectedKid = MutableStateFlow(DEFAULT_KID_NAME)
    val selectedKid: StateFlow<String> = _selectedKid.asStateFlow()

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

    fun getGamesForTeam(teamName: String): Flow<List<GameSchedule>> =
        scheduleStorageRepository.observeGamesForTeam(teamName).map { games ->
            games.map { it.toEntity() }
        }

    init {
        viewModelScope.launch {
            // Initialize default teams first and wait for completion
            initializeDefaultTeamsIfNeeded()

            restoreLastSelectedTeamIfAvailable()

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
            teamRepository.observeActiveTeams().collect { teams ->
                _availableTeams.value = teams.map { it.toEntity() }
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

    private suspend fun restoreLastSelectedTeamIfAvailable() {
        if (_selectedTeam.value != null) return
        val lastTeam = prefs.getString(KEY_LAST_SELECTED_TEAM, null) ?: "North Allegheny Lacrosse"
        val team = teamRepository.findTeam(lastTeam)
        if (team != null) {
            selectTeam(lastTeam)
        }
    }

    private fun loadSubscribedTeams() {
        viewModelScope.launch {
            teamSubscriptionRepository.observeSubscribedTeams(currentUser).collect { teams ->
                _subscribedTeams.value = teams.map { it.toEntity() }
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
                        color = "#000000",
                        awayColor = "#FFB81C",
                        homeJerseyColor = "#000000",
                        awayJerseyColor = "#FFFFFF",
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
                }

                // Refresh after initialization
                loadTeamStatistics()
                loadAllTeamNames()
                loadSubscribedTeams()

                println("TeamViewModel: Initialized ${defaultTeams.size} default teams")
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

                println("TeamViewModel: Added Ryan's Team")
            } else {
                println("TeamViewModel: Found existing Ryan's Team")
            }

            // Keep North Allegheny colors aligned with team branding (black + gold).
            val northAllegheny = teamDao.getTeamByName("North Allegheny Lacrosse")
            if (northAllegheny != null) {
                val brandedNorthAllegheny = northAllegheny.copy(
                    color = "#000000",
                    awayColor = "#FFB81C",
                    homeJerseyColor = "#000000",
                    awayJerseyColor = "#FFFFFF",
                    updatedAt = System.currentTimeMillis()
                )
                if (northAllegheny != brandedNorthAllegheny) {
                    teamDao.updateTeam(brandedNorthAllegheny)
                    println("TeamViewModel: Updated North Allegheny colors to black/gold branding")
                }
            }

        } catch (e: Exception) {
            println("TeamViewModel: Error initializing teams: ${e.message}")
            e.printStackTrace()
        }
    }

    fun selectTeam(teamName: String) {
        _selectedTeam.value = teamName
        _isTeamSelected.value = true
        _selectedKid.value = getSelectedKidForTeam(teamName)
        prefs.edit().putString(KEY_LAST_SELECTED_TEAM, teamName).apply()
    }

    fun clearTeamSelection() {
        _selectedTeam.value = null
        _isTeamSelected.value = false
        _selectedKid.value = DEFAULT_KID_NAME
        _learnedTeamColor.value = null
        prefs.edit().remove(KEY_LAST_SELECTED_TEAM).apply()
    }

    fun getAssignedKidForTeam(teamName: String?): String? {
        val normalizedTeam = teamName?.trim().orEmpty()
        if (normalizedTeam.isEmpty()) return null
        val kid = prefs.getString(KEY_ASSIGNED_KID_PREFIX + normalizedTeam.lowercase(), null)
        return kid?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun assignKidToTeam(teamName: String, kidName: String) {
        val normalizedTeam = teamName.trim()
        if (normalizedTeam.isEmpty()) return
        val normalizedKid = normalizeKidName(kidName)
        prefs.edit()
            .putString(KEY_ASSIGNED_KID_PREFIX + normalizedTeam.lowercase(), normalizedKid)
            .apply()
        if (_selectedTeam.value == normalizedTeam) {
            _selectedKid.value = normalizedKid
        }
    }

    fun assignPlayerToTeam(teamName: String, playerName: String) {
        val normalizedTeam = teamName.trim()
        val normalizedPlayer = playerName.trim()
        if (normalizedTeam.isEmpty() || normalizedPlayer.isEmpty()) return
        prefs.edit()
            .putString(KEY_ASSIGNED_KID_PREFIX + normalizedTeam.lowercase(), normalizedPlayer)
            .putString(KEY_SELECTED_KID_PREFIX + normalizedTeam.lowercase(), normalizedPlayer)
            .apply()
        if (_kidOptions.value.none { it.equals(normalizedPlayer, ignoreCase = true) }) {
            _kidOptions.value = _kidOptions.value + normalizedPlayer
        }
        if (_selectedTeam.value == normalizedTeam) {
            _selectedKid.value = normalizedPlayer
        }
    }

    fun getSelectedKidForTeam(teamName: String?): String {
        val normalizedTeam = teamName?.trim().orEmpty()
        if (normalizedTeam.isEmpty()) return DEFAULT_KID_NAME
        val teamKey = normalizedTeam.lowercase()
        val explicitlySelected = prefs.getString(KEY_SELECTED_KID_PREFIX + teamKey, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (explicitlySelected != null) {
            return normalizeKidName(explicitlySelected)
        }
        val assigned = prefs.getString(KEY_ASSIGNED_KID_PREFIX + teamKey, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return normalizeKidName(assigned)
    }

    fun selectKidForTeam(teamName: String?, kidName: String) {
        val normalizedKid = normalizeKidName(kidName)
        val normalizedTeam = teamName?.trim().orEmpty()
        if (normalizedTeam.isNotEmpty()) {
            prefs.edit()
                .putString(KEY_SELECTED_KID_PREFIX + normalizedTeam.lowercase(), normalizedKid)
                .apply()
        }
        _selectedKid.value = normalizedKid
    }

    private fun normalizeKidName(kidName: String?): String {
        val requested = kidName?.trim().orEmpty()
        val canonical = _kidOptions.value.firstOrNull { it.equals(requested, ignoreCase = true) }
        return canonical ?: requested.ifEmpty { DEFAULT_KID_NAME }
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

    fun addTeam(
        teamName: String,
        sport: String = "Soccer",
        description: String = "",
        color: String = "#1976D2",
        awayColor: String = "#FFFFFF",
        homeJerseyColor: String = "#1976D2",
        awayJerseyColor: String = "#FFFFFF"
    ) {
        viewModelScope.launch {
            val existingTeam = teamDao.getTeamByName(teamName)
            if (existingTeam == null) {
                val newTeam = Team(
                    id = UUID.randomUUID().toString(),
                    name = teamName,
                    sport = sport,
                    color = color,
                    awayColor = awayColor,
                    homeJerseyColor = homeJerseyColor,
                    awayJerseyColor = awayJerseyColor,
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

    fun updateTeamColors(
        teamName: String,
        color: String,
        awayColor: String,
        homeJerseyColor: String,
        awayJerseyColor: String
    ) {
        viewModelScope.launch {
            val existingTeam = teamDao.getTeamByName(teamName)
            if (existingTeam != null) {
                teamDao.updateTeam(
                    existingTeam.copy(
                        color = color,
                        awayColor = awayColor,
                        homeJerseyColor = homeJerseyColor,
                        awayJerseyColor = awayJerseyColor,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                loadTeamStatistics()
                loadAllTeamNames()
            }
        }
    }

    fun updateTeamSettings(
        currentName: String,
        newName: String,
        color: String,
        awayColor: String,
        homeJerseyColor: String,
        awayJerseyColor: String
    ) {
        viewModelScope.launch {
            val existingTeam = teamDao.getTeamByName(currentName) ?: return@launch
            teamDao.updateTeam(
                existingTeam.copy(
                    color = color,
                    awayColor = awayColor,
                    homeJerseyColor = homeJerseyColor,
                    awayJerseyColor = awayJerseyColor,
                    updatedAt = System.currentTimeMillis()
                )
            )

            val normalizedName = newName.trim()
            if (normalizedName != currentName) {
                teamDao.renameTeam(currentName, normalizedName)
                if (_selectedTeam.value == currentName) {
                    _selectedTeam.value = normalizedName
                    prefs.edit().putString(KEY_LAST_SELECTED_TEAM, normalizedName).apply()
                }
            }

            loadTeamStatistics()
            loadAllTeamNames()
        }
    }

    fun renameTeam(oldName: String, newName: String) {
        viewModelScope.launch {
            teamDao.renameTeam(oldName, newName)

            // Update selection if renamed team was selected
            if (_selectedTeam.value == oldName) {
                _selectedTeam.value = newName
                prefs.edit().putString(KEY_LAST_SELECTED_TEAM, newName).apply()
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
            teamSubscriptionRepository.subscribe(
                TeamSubscription(
                    userId = currentUser,
                    teamName = teamName,
                    subscribedAt = System.currentTimeMillis()
                )
            )

            // Set as selected team after subscribing
            selectTeam(teamName)
        }
    }

    fun replaceSubscriptionsWithTeam(teamName: String) {
        val normalizedTeam = teamName.trim()
        if (normalizedTeam.isEmpty()) return
        selectTeam(normalizedTeam)
        viewModelScope.launch {
            teamSubscriptionService.replaceWithTeam(
                userId = currentUser,
                teamName = normalizedTeam,
                subscribedAt = System.currentTimeMillis()
            )
        }
    }

    fun unsubscribeFromTeam(teamName: String) {
        viewModelScope.launch {
            teamSubscriptionRepository.unsubscribe(currentUser, teamName)

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

    companion object {
        private const val KEY_LAST_SELECTED_TEAM = "last_selected_team"
        private const val KEY_ASSIGNED_KID_PREFIX = "assigned_kid_"
        private const val KEY_SELECTED_KID_PREFIX = "selected_kid_"
        private const val DEFAULT_KID_NAME = "Tyson"
    }
}