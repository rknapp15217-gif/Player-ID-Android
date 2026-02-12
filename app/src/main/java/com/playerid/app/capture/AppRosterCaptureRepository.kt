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
