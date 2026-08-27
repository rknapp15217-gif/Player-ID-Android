package com.playerid.app.viewmodels

import android.app.Application
import android.graphics.PointF
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.*
import com.playerid.app.data.repositories.RoomTeamRosterRepository
import com.playerid.app.data.repositories.toEntity
import com.playerid.app.data.repositories.toProfile
import com.playerid.app.domain.team.TeamRosterService
import com.playerid.app.domain.voice.VoiceCommandDecision
import com.playerid.app.domain.voice.VoiceCommandProcessor
import com.playerid.app.roster.RosterCandidate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class PlayerViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
            fun dismissVoiceResult() {
                _voiceResult.value = null
            }
        private val prefs = application.getSharedPreferences("playerid_selected_team", android.content.Context.MODE_PRIVATE)
        private val KEY_SELECTED_TEAM = "selected_team"
    
    private val database = PlayerDatabase.getDatabase(application)
    private val playerDao = database.playerDao()
    private val teamDao = database.teamDao()
    private val teamRosterService = TeamRosterService(
        RoomTeamRosterRepository(teamDao, playerDao)
    )
    private val voiceCommandProcessor = VoiceCommandProcessor()
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    companion object {
        private const val TAG = "PlayerViewModel"
    }
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedTeam = MutableStateFlow<String?>(null)
    val selectedTeam: StateFlow<String?> = _selectedTeam.asStateFlow()
    
    private val _trackedPlayers = MutableStateFlow<List<TrackedPlayer>>(emptyList())
    val trackedPlayers: StateFlow<List<TrackedPlayer>> = _trackedPlayers.asStateFlow()

    private val _capturePastMode = MutableStateFlow(true)
    val capturePastMode: StateFlow<Boolean> = _capturePastMode.asStateFlow()

    // Global Voice Assistant State
    private val _voiceResult = MutableStateFlow<VoiceAssistantResult?>(null)
    val voiceResult: StateFlow<VoiceAssistantResult?> = _voiceResult.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Flag to prevent recorder from auto-starting during a voice session
    private val _isVoiceSessionActive = MutableStateFlow(false)
    val isVoiceSessionActive: StateFlow<Boolean> = _isVoiceSessionActive.asStateFlow()

    // Action flow to trigger UI/Camera events from voice
    private val _voiceActions = MutableSharedFlow<VoiceAction>()
    val voiceActions = _voiceActions.asSharedFlow()

    val detectedPlayersWithInfo = combine(_trackedPlayers, _selectedTeam) { tracked, team ->
        tracked.map { 
            val player = team?.let { t -> 
                playerDao.getPlayerByNumber(it.jerseyNumber, t)
            } ?: findPlayerByNumber(it.jerseyNumber)
            Pair(it, player)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlayers = playerDao.getAllActivePlayers()

    fun observeTeamRoster(teamName: String): Flow<List<Player>> =
        teamRosterService.observeRoster(teamName).map { players ->
            players.map { it.toEntity() }
        }
    
    val filteredPlayers = combine(
        allPlayers,
        _selectedTeam,
        searchQuery
    ) { players, team, query ->
        val teamPlayers = if (team != null) {
            players.filter { it.team == team }
        } else {
            emptyList()
        }
        if (query.isEmpty()) {
            teamPlayers
        } else {
            teamPlayers.filter { player ->
                player.name.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        // Restore selected team from SharedPreferences
        val persistedTeam = prefs.getString(KEY_SELECTED_TEAM, null)
        if (persistedTeam != null) {
            _selectedTeam.value = persistedTeam
        }
        initializeSampleData()
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            // Keep prosody near neutral to avoid the synthetic "robot" effect.
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.94f)

            val preferredVoice = selectMostNaturalVoice(tts?.voices)
            if (preferredVoice != null) {
                tts?.voice = preferredVoice
                Log.d(TAG, "Using TTS voice: ${preferredVoice.name}")
            } else {
                Log.d(TAG, "No preferred TTS voice found; using engine default")
            }
            isTtsReady = true
        }
    }

    private fun selectMostNaturalVoice(voices: Set<Voice>?): Voice? {
        if (voices.isNullOrEmpty()) return null

        val usVoices = voices.filter {
            it.locale == Locale.US &&
                !it.isNetworkConnectionRequired &&
                !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }

        val candidates = if (usVoices.isNotEmpty()) usVoices else voices.toList()

        val preferredNameHints = listOf("neural", "wavenet", "studio", "natural", "enhanced")

        return candidates
            .sortedWith(
                compareByDescending<Voice> { voice ->
                    preferredNameHints.any { hint -> voice.name.contains(hint, ignoreCase = true) }
                }
                    .thenByDescending { it.quality }
                    .thenBy { it.latency }
            )
            .firstOrNull()
    }

    private fun speak(text: String) {
        Log.d(TAG, "speak() called: isTtsReady=$isTtsReady, text='$text'")
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w(TAG, "TTS not ready, skipping speak: '$text'")
        }
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
        if (listening) {
            _isVoiceSessionActive.value = true
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSelectedTeam(team: String?) {
        android.util.Log.d("PlayerViewModel", "setSelectedTeam called with: $team")
        _selectedTeam.value = team
        // Persist to SharedPreferences
        prefs.edit().putString(KEY_SELECTED_TEAM, team).apply()
        android.util.Log.d("PlayerViewModel", "selectedTeam now: ${_selectedTeam.value}")
    }
    
    fun updateTrackedPlayers(tracked: List<TrackedPlayer>) {
        _trackedPlayers.value = tracked
    }

    fun setCapturePastMode(enabled: Boolean) {
        _capturePastMode.value = enabled
    }
    
    private suspend fun findPlayerByNumber(number: String): Player? {
        return allPlayers.first().find { it.number == number }
    }

    /**
     * Core logic for Voice Assistant with Robust Wake Word Handling
     */
    fun processVoiceCommand(spokenText: String) {
        processVoiceCommandHypotheses(listOf(spokenText))
    }

    fun processVoiceCommandHypotheses(
        spokenTexts: List<String>,
        selectedTeamOverride: String? = null,
        onTeamSwitched: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isListening.value = false
            _voiceResult.value = null
            val playersById = allPlayers.first().associateBy(Player::id)
            val decision = voiceCommandProcessor.process(
                spokenTexts = spokenTexts,
                teamNames = teamDao.getAllActiveTeams().first().map { it.name },
                selectedTeam = selectedTeamOverride ?: _selectedTeam.value,
                players = playersById.values.map { it.toProfile() }
            )
            when (decision) {
                is VoiceCommandDecision.Capture -> {
                    _voiceActions.emit(VoiceAction.StopRecording)
                    _voiceResult.value = VoiceAssistantResult.Success(decision.message)
                    speak(decision.message)
                }
                is VoiceCommandDecision.SwitchTeam -> {
                    if (selectedTeamOverride != null || onTeamSwitched != null) {
                        onTeamSwitched?.invoke(decision.teamName)
                    } else {
                        setSelectedTeam(decision.teamName)
                    }
                    _voiceResult.value = VoiceAssistantResult.Success(decision.message)
                    speak(decision.message)
                }
                is VoiceCommandDecision.Match -> {
                    val matches = decision.players.mapNotNull { playersById[it.id] }
                    _voiceResult.value = VoiceAssistantResult.Success(
                        message = decision.message,
                        player = matches.singleOrNull(),
                        players = if (matches.size > 1) matches else emptyList()
                    )
                    speak(decision.speech)
                }
                is VoiceCommandDecision.Error -> {
                    _voiceResult.value = VoiceAssistantResult.Error(decision.message)
                    speak(decision.message)
                    _isListening.value = false
                }
            }
            _isVoiceSessionActive.value = false
        }
    }

    fun clearVoiceResult() {
        Log.d(TAG, "clearVoiceResult() called")
        _voiceResult.value = null
        _isVoiceSessionActive.value = false
    }

    fun stopRecordingForVoice() {
        _isVoiceSessionActive.value = true
        viewModelScope.launch {
            _voiceActions.emit(VoiceAction.StopRecordingSilent)
        }
    }

    fun reportVoiceError(message: String) {
        Log.d(TAG, "Setting voiceResult: Error '$message' (reportVoiceError)")
        _voiceResult.value = VoiceAssistantResult.Error(message)
        speak(message)
        _isVoiceSessionActive.value = false
    }

    fun importRosterCandidates(
        teamName: String,
        candidates: List<RosterCandidate>,
        addedBy: String = "ocr_import"
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            teamRosterService.importRoster(
                teamName = teamName,
                candidates = candidates,
                addedBy = addedBy,
                newPlayerIds = candidates.map { UUID.randomUUID().toString() },
                timestamp = now
            )
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
    
    fun addPlayer(player: Player, addedBy: String = "Unknown") {
        viewModelScope.launch {
            teamRosterService.addPlayer(
                player = player.toProfile(),
                playerId = UUID.randomUUID().toString(),
                addedBy = addedBy,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            teamRosterService.updatePlayer(player.toProfile(), System.currentTimeMillis())
        }
    }
    
    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            teamRosterService.deletePlayer(player.id)
        }
    }

    fun exportDatabase() {
        viewModelScope.launch {
            val players = allPlayers.first()
            Log.d(TAG, "Exporting ${players.size} players to log console")
            players.forEach { Log.d(TAG, "EXPORT: $it") }
        }
    }

    fun importDatabase() {
        initializeSampleData()
    }
    fun clearCache() {
        viewModelScope.launch {
            database.clearAllTables()
        }
    }
    
    private fun initializeSampleData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existingTyson = allPlayers.first().firstOrNull { it.name == "Tyson Knapp" }
            if (existingTyson != null && (existingTyson.addedBy != "443-878-0344" || existingTyson.team != "North Allegheny Lacrosse")) {
                playerDao.updatePlayer(
                    existingTyson.copy(
                        team = "North Allegheny Lacrosse",
                        addedBy = "443-878-0344",
                        updatedAt = now
                    )
                )
            }

            val playerCount = playerDao.getPlayerCount()
            if (playerCount < 20) {
                val samplePlayers = listOf(
                    Player(id = UUID.randomUUID().toString(), number = "00", name = "Benny Zero", position = "Forward", team = "Ryan's Team", academicYear = "Senior", addedBy = "Ryan"),
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
                    Player(id = UUID.randomUUID().toString(), number = "10", name = "Tyson Knapp", position = "Forward", team = "North Allegheny Lacrosse", academicYear = "Junior", addedBy = "443-878-0344"),
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

// Sealed classes should be outside PlayerViewModel
sealed class VoiceAssistantResult {
    data class Success(
        val message: String,
        val player: Player? = null,
        val players: List<Player> = emptyList()
    ) : VoiceAssistantResult()
    data class Error(val message: String) : VoiceAssistantResult()
}

sealed class VoiceAction {
    object StopRecording : VoiceAction()
    object StopRecordingSilent : VoiceAction()
}

