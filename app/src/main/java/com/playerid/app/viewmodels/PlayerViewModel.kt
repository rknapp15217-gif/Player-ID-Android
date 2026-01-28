package com.playerid.app.viewmodels

import android.app.Application
import android.graphics.PointF
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = PlayerDatabase.getDatabase(application)
    private val playerDao = database.playerDao()
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedTeam = MutableStateFlow<String?>(null)
    val selectedTeam: StateFlow<String?> = _selectedTeam.asStateFlow()
    
    // Updated to hold TrackedPlayer objects
    private val _trackedPlayers = MutableStateFlow<List<TrackedPlayer>>(emptyList())
    val trackedPlayers: StateFlow<List<TrackedPlayer>> = _trackedPlayers.asStateFlow()

    // This will be enhanced to hold full player profiles
    val detectedPlayersWithInfo = _trackedPlayers.map { tracked ->
        tracked.map { 
            val player = selectedTeam.value?.let {
                 team -> playerDao.getPlayerByNumber(it.jerseyNumber, team)
            } ?: findPlayerByNumber(it.jerseyNumber)
            Pair(it, player)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlayers = playerDao.getAllActivePlayers()
    
    val filteredPlayers = combine(
        allPlayers,
        searchQuery
    ) { players, query ->
        if (query.isEmpty()) {
            players
        } else {
            players.filter { player ->
                player.name.contains(query, ignoreCase = true) ||
                player.number.contains(query) ||
                player.team.contains(query, ignoreCase = true) ||
                player.position.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        initializeSampleData()
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSelectedTeam(team: String?) {
        _selectedTeam.value = team
    }
    
    // Renamed and updated to accept TrackedPlayer
    fun updateTrackedPlayers(tracked: List<TrackedPlayer>) {
        Log.d(TAG, "🧠 Tracking ${tracked.size} players.")
        _trackedPlayers.value = tracked
    }
    
    /**
     * Add manual bubble for manual detection mode
     */
    fun addManualBubble(playerName: String, playerNumber: String, position: PointF) {
        Log.d(TAG, "➕ Adding manual bubble: $playerName #$playerNumber at (${position.x}, ${position.y})")
        // TODO: Implement manual bubble creation
    }
    
    private suspend fun findPlayerByNumber(number: String): Player? {
        return allPlayers.first().find { it.number == number }
    }
    
    fun addPlayer(player: Player, addedBy: String = "Unknown") {
        viewModelScope.launch {
            val newPlayer = player.copy(
                id = UUID.randomUUID().toString(),
                addedBy = addedBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            playerDao.insertPlayer(newPlayer)
        }
    }
    
    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            val updatedPlayer = player.copy(updatedAt = System.currentTimeMillis())
            playerDao.updatePlayer(updatedPlayer)
        }
    }
    
    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            playerDao.deletePlayer(player)
        }
    }
    
    suspend fun getAllTeams(): List<String> {
        return playerDao.getAllTeams()
    }

    fun exportDatabase() {
        viewModelScope.launch {
            val players = allPlayers.first()
            println("Exporting ${players.size} players:")
            players.forEach { println(it) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            database.clearAllTables()
        }
    }

    fun importDatabase() {
        initializeSampleData()
    }
    
    private fun initializeSampleData() {
        viewModelScope.launch {
            val playerCount = playerDao.getPlayerCount()
            if (playerCount == 0) {
                val samplePlayers = listOf(
                    Player(id = UUID.randomUUID().toString(), number = "10", name = "Sofia Martinez", position = "Forward", team = "Eagles High School", academicYear = "Senior", addedBy = "Coach_Martinez"),
                    Player(id = UUID.randomUUID().toString(), number = "7", name = "Diego Santos", position = "Midfielder", team = "Eagles High School", academicYear = "Junior", addedBy = "Parent_Maria"),
                    Player(id = UUID.randomUUID().toString(), number = "1", name = "Tyler Johnson", position = "Goalkeeper", team = "Eagles High School", academicYear = "Sophomore", addedBy = "Coach_Martinez"),
                    Player(id = UUID.randomUUID().toString(), number = "23", name = "Maya Patel", position = "Defender", team = "Lightning FC U16", academicYear = "Sophomore", addedBy = "Parent_Sarah"),
                    Player(id = UUID.randomUUID().toString(), number = "15", name = "Connor Walsh", position = "Midfielder", team = "Lightning FC U16", academicYear = "Freshman", addedBy = "Parent_Dave"),
                    Player(id = UUID.randomUUID().toString(), number = "9", name = "Zoe Kim", position = "Forward", team = "Lightning FC U16", academicYear = "Sophomore", addedBy = "Parent_Sarah"),
                    Player(id = UUID.randomUUID().toString(), number = "32", name = "Marcus Williams", position = "Center", team = "Riverside Rockets", academicYear = "Junior", addedBy = "Assistant_Coach_Mike"),
                    Player(id = UUID.randomUUID().toString(), number = "14", name = "Aiden Brown", position = "Point Guard", team = "Riverside Rockets", academicYear = "Sophomore", addedBy = "Parent_Jennifer"),
                    Player(id = UUID.randomUUID().toString(), number = "21", name = "Isabella Garcia", position = "Shooting Guard", team = "Riverside Rockets", academicYear = "Freshman", addedBy = "Assistant_Coach_Mike"),
                    Player(id = UUID.randomUUID().toString(), number = "12", name = "Emma Thompson", position = "Setter", team = "Thunder Volleyball", academicYear = "Senior", addedBy = "Player_Emma23"),
                    Player(id = UUID.randomUUID().toString(), number = "8", name = "Olivia Davis", position = "Outside Hitter", team = "Thunder Volleyball", academicYear = "Junior", addedBy = "Parent_Lisa_D"),
                    Player(id = UUID.randomUUID().toString(), number = "5", name = "Ava Wilson", position = "Libero", team = "Thunder Volleyball", academicYear = "Sophomore", addedBy = "Player_Emma23"),
                    Player(id = UUID.randomUUID().toString(), number = "44", name = "Jayden Miller", position = "Running Back", team = "Warriors JV Football", academicYear = "Sophomore", addedBy = "Dad_CoachTom"),
                    Player(id = UUID.randomUUID().toString(), number = "12", name = "Ethan Rodriguez", position = "Quarterback", team = "Warriors JV Football", academicYear = "Junior", addedBy = "Parent_Carlos"),
                    Player(id = UUID.randomUUID().toString(), number = "10", name = "Tyson Knapp", position = "Forward", team = "Ryan's Team", academicYear = "Junior", addedBy = "Ryan"),
                    Player(id = UUID.randomUUID().toString(), number = "7", name = "Jake Wilson", position = "Midfielder", team = "Ryan's Team", academicYear = "Senior", addedBy = "Ryan"),
                    Player(id = UUID.randomUUID().toString(), number = "23", name = "Alex Rodriguez", position = "Defender", team = "Ryan's Team", academicYear = "Sophomore", addedBy = "Ryan"),
                    Player(id = UUID.randomUUID().toString(), number = "17", name = "Lucas Anderson", position = "Pitcher", team = "Stallions Baseball", academicYear = "Freshman", addedBy = "BaseballDad_Joe"),
                    Player(id = UUID.randomUUID().toString(), number = "3", name = "Noah Taylor", position = "Catcher", team = "Stallions Baseball", academicYear = "Sophomore", addedBy = "Mom_Rachel")
                )
                playerDao.insertPlayers(samplePlayers)
            }
        }
    }
}
