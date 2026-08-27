package com.playerid.app.domain.team

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MemoryBrowsingState(
    val children: List<ChildProfileRecord> = emptyList(),
    val seasons: List<SportSeasonProfile> = emptyList(),
    val games: List<GameScheduleProfile> = emptyList(),
    val memories: List<MemoryItemProfile> = emptyList(),
    val selectedChild: ChildProfileRecord? = null,
    val selectedSeason: SportSeasonProfile? = null,
    val selectedGame: GameScheduleProfile? = null
)

class MemoryBrowsingStateHolder(
    private val repository: ScheduleStorageRepository,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(MemoryBrowsingState())
    val state: StateFlow<MemoryBrowsingState> = mutableState.asStateFlow()

    private var seasonsJob: Job? = null
    private var gamesJob: Job? = null
    private var memoriesJob: Job? = null

    init {
        scope.launch {
            repository.observeActiveChildren().collect { children ->
                mutableState.value = mutableState.value.copy(children = children)
            }
        }
    }

    fun selectChild(child: ChildProfileRecord) {
        seasonsJob?.cancel()
        gamesJob?.cancel()
        memoriesJob?.cancel()
        mutableState.value = mutableState.value.copy(
            selectedChild = child,
            selectedSeason = null,
            selectedGame = null,
            seasons = emptyList(),
            games = emptyList(),
            memories = emptyList()
        )
        seasonsJob = scope.launch {
            repository.observeSeasonsForChild(child.id).collect { seasons ->
                mutableState.value = mutableState.value.copy(seasons = seasons)
            }
        }
    }

    fun selectSeason(season: SportSeasonProfile) {
        gamesJob?.cancel()
        memoriesJob?.cancel()
        mutableState.value = mutableState.value.copy(
            selectedSeason = season,
            selectedGame = null,
            games = emptyList(),
            memories = emptyList()
        )
        gamesJob = scope.launch {
            repository.observeGamesForSeason(season.id).collect { games ->
                mutableState.value = mutableState.value.copy(games = games)
            }
        }
    }

    fun selectGame(game: GameScheduleProfile) {
        memoriesJob?.cancel()
        mutableState.value = mutableState.value.copy(
            selectedGame = game,
            memories = emptyList()
        )
        memoriesJob = scope.launch {
            repository.observeMemoriesForGame(game.id).collect { memories ->
                mutableState.value = mutableState.value.copy(memories = memories)
            }
        }
    }

    fun goBackToChildren() {
        seasonsJob?.cancel()
        gamesJob?.cancel()
        memoriesJob?.cancel()
        mutableState.value = mutableState.value.copy(
            selectedChild = null,
            selectedSeason = null,
            selectedGame = null,
            seasons = emptyList(),
            games = emptyList(),
            memories = emptyList()
        )
    }

    fun goBackToSeasons() {
        gamesJob?.cancel()
        memoriesJob?.cancel()
        mutableState.value = mutableState.value.copy(
            selectedSeason = null,
            selectedGame = null,
            games = emptyList(),
            memories = emptyList()
        )
    }

    fun goBackToGames() {
        memoriesJob?.cancel()
        mutableState.value = mutableState.value.copy(
            selectedGame = null,
            memories = emptyList()
        )
    }
}