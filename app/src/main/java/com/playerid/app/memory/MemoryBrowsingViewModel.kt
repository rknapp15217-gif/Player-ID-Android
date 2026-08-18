package com.playerid.app.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.ChildProfile
import com.playerid.app.data.GameSchedule
import com.playerid.app.data.MemoryItem
import com.playerid.app.data.PlayerDatabase
import com.playerid.app.data.SportSeason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemoryBrowsingViewModel(application: Application) : AndroidViewModel(application) {

    private val memoryDao = PlayerDatabase.getDatabase(application).memoryOrganizationDao()

    private val _children = MutableStateFlow<List<ChildProfile>>(emptyList())
    val children: StateFlow<List<ChildProfile>> = _children.asStateFlow()

    private val _seasons = MutableStateFlow<List<SportSeason>>(emptyList())
    val seasons: StateFlow<List<SportSeason>> = _seasons.asStateFlow()

    private val _games = MutableStateFlow<List<GameSchedule>>(emptyList())
    val games: StateFlow<List<GameSchedule>> = _games.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedChild = MutableStateFlow<ChildProfile?>(null)
    val selectedChild: StateFlow<ChildProfile?> = _selectedChild.asStateFlow()

    private val _selectedSeason = MutableStateFlow<SportSeason?>(null)
    val selectedSeason: StateFlow<SportSeason?> = _selectedSeason.asStateFlow()

    private val _selectedGame = MutableStateFlow<GameSchedule?>(null)
    val selectedGame: StateFlow<GameSchedule?> = _selectedGame.asStateFlow()

    init {
        loadChildren()
    }

    private fun loadChildren() {
        viewModelScope.launch {
            memoryDao.getActiveChildren().collect { children ->
                _children.value = children
            }
        }
    }

    fun selectChild(child: ChildProfile) {
        _selectedChild.value = child
        _selectedSeason.value = null
        _selectedGame.value = null
        loadSeasonsForChild(child.id)
    }

    private fun loadSeasonsForChild(childId: String) {
        viewModelScope.launch {
            memoryDao.getSeasonsForChild(childId).collect { seasons ->
                _seasons.value = seasons
            }
        }
    }

    fun selectSeason(season: SportSeason) {
        _selectedSeason.value = season
        _selectedGame.value = null
        loadGamesForSeason(season.id)
    }

    private fun loadGamesForSeason(seasonId: String) {
        viewModelScope.launch {
            memoryDao.getGamesForSeason(seasonId).collect { games ->
                _games.value = games
            }
        }
    }

    fun selectGame(game: GameSchedule) {
        _selectedGame.value = game
        loadMemoriesForGame(game.id)
    }

    private fun loadMemoriesForGame(gameId: String) {
        viewModelScope.launch {
            memoryDao.getMemoryForGame(gameId).collect { memories ->
                _memories.value = memories
            }
        }
    }

    fun getSeasonTitle(season: SportSeason): String {
        return buildString {
            append(season.sportName)
            if (season.seasonLabel.isNotBlank()) {
                append(" • ")
                append(season.seasonLabel)
            }
            if (season.teamName.isNotBlank()) {
                append(" • ")
                append(season.teamName)
            }
        }
    }

    fun getGameTitle(game: GameSchedule): String {
        return buildString {
            append("vs ")
            append(game.opponentName)
            if (game.gameLabel.isNotBlank()) {
                append(" • ")
                append(game.gameLabel)
            }
        }
    }

    fun getGameDate(game: GameSchedule): String {
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
        return sdf.format(java.util.Date(game.scheduledStartMs))
    }

    fun goBackToChildren() {
        _selectedChild.value = null
        _selectedSeason.value = null
        _selectedGame.value = null
        _seasons.value = emptyList()
        _games.value = emptyList()
        _memories.value = emptyList()
    }

    fun goBackToSeasons() {
        _selectedSeason.value = null
        _selectedGame.value = null
        _games.value = emptyList()
        _memories.value = emptyList()
    }

    fun goBackToGames() {
        _selectedGame.value = null
        _memories.value = emptyList()
    }
}
