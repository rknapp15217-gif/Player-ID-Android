package com.playerid.app.viewmodels

import android.app.Application
import android.graphics.PointF
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.*
import com.playerid.app.roster.RosterCandidate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class PlayerViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    
    private val database = PlayerDatabase.getDatabase(application)
    private val playerDao = database.playerDao()
    private val teamDao = database.teamDao()
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    companion object {
        private const val TAG = "PlayerViewModel"
        private val NUMBER_WORDS = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "double zero" to "00", "zero zero" to "00"
        )
        // Aggressive fuzzy wake word variations
        private val WAKE_WORDS_REGEX = Regex("(?i)^(spotter|spottr|spotr|sport|spot|potter|hey spotter|hey spotr|hey spottr)\\b")
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
                player.name.contains(query, ignoreCase = true) ||
                player.number.contains(query) ||
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
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            // Stadium announcer effect: lower pitch, slower rate
            tts?.setPitch(0.8f)
            tts?.setSpeechRate(0.85f)
            // Try to select a male US voice if available
            val voices = tts?.voices
            val announcerVoice = voices?.firstOrNull {
                it.locale == Locale.US && it.name.contains("male", ignoreCase = true)
            }
            if (announcerVoice != null) {
                tts?.voice = announcerVoice
            }
            isTtsReady = true
        }
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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
        _selectedTeam.value = team
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
        viewModelScope.launch {
            _isListening.value = false
            val originalText = spokenText.lowercase().trim()
            Log.d(TAG, "Voice assistant processing: $originalText")
            
            // 1. PRIORITY ACTION: STOP/CAPTURE (Works even without wake word)
            if (originalText.contains("capture") || originalText.contains("stop") || 
                originalText.contains("finish") || originalText.contains("done")) {
                
                _voiceActions.emit(VoiceAction.StopRecording)
                val msg = "Moment captured!"
                _voiceResult.value = VoiceAssistantResult.Success(msg)
                speak(msg)
                // Session ends after capture
                _isVoiceSessionActive.value = false
                return@launch
            }

            // 2. FUZZY WAKE WORD CLEANING
            var cleanText = originalText.replace(WAKE_WORDS_REGEX, "").trim()

            // 3. Handle Team Switching
            if (cleanText.contains("team") || cleanText.contains("switch") || cleanText.contains("select")) {
                val teams = teamDao.getAllActiveTeams().first()
                val targetTeam = teams.find { cleanText.contains(it.name.lowercase()) }
                if (targetTeam != null) {
                    setSelectedTeam(targetTeam.name)
                    val msg = "Switched to ${targetTeam.name}"
                    _voiceResult.value = VoiceAssistantResult.Success(msg)
                    speak(msg)
                    _isVoiceSessionActive.value = false
                    return@launch
                }
            }

            val team = _selectedTeam.value
            if (team == null) {
                val msg = "Please select a team first"
                _voiceResult.value = VoiceAssistantResult.Error(msg)
                speak(msg)
                _isVoiceSessionActive.value = false
                return@launch
            }

            val roster = allPlayers.first().filter { it.team == team }

            // 4. Clean up filler words
            val fillerWords = listOf("number", "jersey", "player", "who is", "what is", "is", "the", "find", "identify")
            var processedText = cleanText
            fillerWords.forEach { word ->
                processedText = processedText.replace("\\b$word\\b".toRegex(), "").trim()
            }

            NUMBER_WORDS.forEach { (word, digit) ->
                if (processedText == word) processedText = digit
            }

            // 5. Try number match
            val numericOnly = processedText.filter { it.isDigit() }
            if (numericOnly.isNotEmpty()) {
                val player = roster.find { it.number == numericOnly }
                if (player != null) {
                    val msg = "#${numericOnly} ${player.name}"
                    _voiceResult.value = VoiceAssistantResult.Success(msg, player)
                    speak(player.name)
                    _isVoiceSessionActive.value = false
                    return@launch
                }
            }

            // 6. Try name match
            val matches = roster.filter { 
                processedText.isNotEmpty() && (it.name.lowercase().contains(processedText) || processedText.contains(it.name.lowercase()))
            }

            when {
                matches.size == 1 -> {
                    val p = matches.first()
                    val msg = "Number #${p.number} ${p.name}"
                    _voiceResult.value = VoiceAssistantResult.Success(msg, p)
                    speak("Number ${p.number}")
                }
                matches.size > 1 -> {
                    val names = matches.joinToString(" and ") { "${it.name} #${it.number}" }
                    val msg = "I found ${matches.size} players: $names"
                    _voiceResult.value = VoiceAssistantResult.Error("Multiple players found. Be more specific.")
                    speak("I found multiple players. Please specify.")
                }
                else -> {
                    val msg = "I couldn't find a player matching '$spokenText'"
                    _voiceResult.value = VoiceAssistantResult.Error(msg)
                    speak("Player not found")
                }
            }
            _isVoiceSessionActive.value = false
        }
    }

    fun clearVoiceResult() {
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
            for (candidate in candidates) {
                val existing = playerDao.getPlayerByNumber(candidate.number, teamName)
                val candidateYear = candidate.academicYear?.takeIf { it.isNotBlank() }
                val candidatePosition = candidate.position?.takeIf { it.isNotBlank() }
                if (existing != null) {
                    val updated = existing.copy(
                        name = candidate.name,
                        position = candidatePosition ?: existing.position,
                        academicYear = candidateYear ?: existing.academicYear,
                        updatedAt = now,
                        addedBy = addedBy
                    )
                    playerDao.updatePlayer(updated)
                } else {
                    val player = Player(
                        id = UUID.randomUUID().toString(),
                        number = candidate.number,
                        name = candidate.name,
                        position = candidatePosition ?: "",
                        team = teamName,
                        academicYear = candidateYear ?: "Unknown",
                        addedBy = addedBy,
                        createdAt = now,
                        updatedAt = now
                    )
                    playerDao.insertPlayer(player)
                }
            }
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
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

sealed class VoiceAssistantResult {
    data class Success(val message: String, val player: Player? = null) : VoiceAssistantResult()
    data class Error(val message: String) : VoiceAssistantResult()
}

sealed class VoiceAction {
    object StopRecording : VoiceAction()
    object StopRecordingSilent : VoiceAction()
}
