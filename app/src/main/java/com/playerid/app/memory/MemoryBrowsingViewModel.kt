package com.playerid.app.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.ChildProfile
import com.playerid.app.data.GameSchedule
import com.playerid.app.data.MemoryItem
import com.playerid.app.data.PlayerDatabase
import com.playerid.app.data.SportSeason
import com.playerid.app.data.repositories.RoomScheduleStorageRepository
import com.playerid.app.data.repositories.toEntity
import com.playerid.app.data.repositories.toProfile
import com.playerid.app.domain.team.ChildProfileRecord
import com.playerid.app.domain.team.GameScheduleProfile
import com.playerid.app.domain.team.MemoryBrowsingStateHolder
import com.playerid.app.domain.team.SportSeasonProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemoryBrowsingViewModel(application: Application) : AndroidViewModel(application) {

    private val memoryDao = PlayerDatabase.getDatabase(application).memoryOrganizationDao()
    private val scheduleStorageRepository = RoomScheduleStorageRepository(memoryDao)
    private val stateHolder = MemoryBrowsingStateHolder(scheduleStorageRepository, viewModelScope)
    val browsingState = stateHolder.state

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
        viewModelScope.launch {
            stateHolder.state.collect { state ->
                _children.value = state.children.map { it.toEntity() }
                _seasons.value = state.seasons.map { it.toEntity() }
                _games.value = state.games.map { it.toEntity() }
                _memories.value = state.memories.map { it.toEntity() }
                _selectedChild.value = state.selectedChild?.toEntity()
                _selectedSeason.value = state.selectedSeason?.toEntity()
                _selectedGame.value = state.selectedGame?.toEntity()
            }
        }
    }

    fun selectChild(child: ChildProfile) {
        stateHolder.selectChild(child.toProfile())
    }

    fun selectChildProfile(child: ChildProfileRecord) = stateHolder.selectChild(child)

    fun selectSeason(season: SportSeason) {
        stateHolder.selectSeason(season.toProfile())
    }

    fun selectSeasonProfile(season: SportSeasonProfile) = stateHolder.selectSeason(season)

    fun selectGame(game: GameSchedule) {
        stateHolder.selectGame(game.toProfile())
    }

    fun selectGameProfile(game: GameScheduleProfile) = stateHolder.selectGame(game)

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
        stateHolder.goBackToChildren()
    }

    fun goBackToSeasons() {
        stateHolder.goBackToSeasons()
    }

    fun goBackToGames() {
        stateHolder.goBackToGames()
    }
}
