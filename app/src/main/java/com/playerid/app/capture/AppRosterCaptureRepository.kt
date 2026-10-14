package com.playerid.app.capture

import android.content.Intent
import com.playerid.app.roster.RosterCandidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppRosterCaptureRepository {
    private val _candidates = MutableStateFlow<List<RosterCandidate>>(emptyList())
    val candidates: StateFlow<List<RosterCandidate>> = _candidates.asStateFlow()
    private val _activeTeamName = MutableStateFlow<String?>(null)
    val activeTeamName: StateFlow<String?> = _activeTeamName.asStateFlow()
    private val _scheduleLines = MutableStateFlow<List<String>>(emptyList())
    val scheduleLines: StateFlow<List<String>> = _scheduleLines.asStateFlow()
    private val _contactLines = MutableStateFlow<List<String>>(emptyList())
    val contactLines: StateFlow<List<String>> = _contactLines.asStateFlow()
    private val _contactPlayerNames = MutableStateFlow<List<String>>(emptyList())
    val contactPlayerNames: StateFlow<List<String>> = _contactPlayerNames.asStateFlow()
    private val _contactPlayerIndex = MutableStateFlow(0)
    val contactPlayerIndex: StateFlow<Int> = _contactPlayerIndex.asStateFlow()
    private val _captureContent = MutableStateFlow(CaptureContent.ROSTER)
    val captureContent: StateFlow<CaptureContent> = _captureContent.asStateFlow()
    private var cachedProjectionResultCode: Int? = null
    private var cachedProjectionData: Intent? = null

    fun setCandidates(candidates: List<RosterCandidate>) {
        _candidates.value = candidates
    }

    fun addCandidates(newCandidates: List<RosterCandidate>) {
        if (newCandidates.isEmpty()) return
        _candidates.value = (_candidates.value + newCandidates)
            .distinctBy { it.number + "|" + it.name.lowercase() }
    }

    fun clear() {
        _candidates.value = emptyList()
    }

    fun addScheduleLines(lines: List<String>) {
        _scheduleLines.value = (_scheduleLines.value + lines.map(String::trim).filter(String::isNotEmpty)).distinct()
    }

    fun clearSchedule() {
        _scheduleLines.value = emptyList()
    }

    fun addContactLines(lines: List<String>) {
        _contactLines.value = (_contactLines.value + lines.map(String::trim).filter(String::isNotEmpty)).distinct()
    }

    fun clearContacts() {
        _contactLines.value = emptyList()
        _contactPlayerNames.value = emptyList()
        _contactPlayerIndex.value = 0
    }

    fun startContactCapture(playerNames: List<String>) {
        _contactLines.value = emptyList()
        _contactPlayerNames.value = playerNames.distinct()
        _contactPlayerIndex.value = 0
    }

    fun addCurrentPlayerContactLines(lines: List<String>) {
        val playerName = _contactPlayerNames.value.getOrNull(_contactPlayerIndex.value) ?: return
        addContactLines(listOf("__PLAYER__:$playerName") + lines)
        if (_contactPlayerIndex.value < _contactPlayerNames.value.lastIndex) {
            _contactPlayerIndex.value += 1
        }
    }

    fun setCaptureContent(content: CaptureContent) {
        _captureContent.value = content
    }

    fun setActiveTeamName(teamName: String) {
        _activeTeamName.value = teamName
    }

    fun cacheProjection(resultCode: Int, data: Intent) {
        cachedProjectionResultCode = resultCode
        cachedProjectionData = data
    }

    fun getCachedProjection(): Pair<Int, Intent>? {
        val resultCode = cachedProjectionResultCode
        val data = cachedProjectionData
        return if (resultCode != null && data != null) {
            resultCode to data
        } else {
            null
        }
    }

    fun clearCachedProjection() {
        cachedProjectionResultCode = null
        cachedProjectionData = null
    }
}

enum class CaptureContent { ROSTER, SCHEDULE, CONTACTS }
