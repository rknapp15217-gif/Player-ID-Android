package com.playerid.app.memory

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.MediaIngestionState
import com.playerid.app.data.MemoryItem
import com.playerid.app.data.PlayerDatabase
import com.playerid.app.data.repositories.RoomScheduleStorageRepository
import com.playerid.app.data.repositories.toProfile
import com.playerid.app.domain.team.GameScheduleProfile
import com.playerid.app.domain.team.MemoryGameMatcher
import com.playerid.app.domain.team.MemoryReviewService
import com.playerid.app.domain.team.ScheduleImportEntry
import com.playerid.app.domain.team.ScheduleImportService
import com.playerid.app.utils.MediaPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

class MemoryIngestionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PlayerDatabase.getDatabase(application)
    private val memoryDao = database.memoryOrganizationDao()
    private val scheduleStorageRepository = RoomScheduleStorageRepository(memoryDao)
    private val memoryGameMatcher = MemoryGameMatcher()
    private val memoryReviewService = MemoryReviewService(scheduleStorageRepository)
    private val scheduleImportService = ScheduleImportService(scheduleStorageRepository)
    private val teamDao = database.teamDao()

    private val _pendingPrompt = MutableStateFlow<MemoryScanPrompt?>(null)
    val pendingPrompt: StateFlow<MemoryScanPrompt?> = _pendingPrompt.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    fun scanForNewMemoriesOnLaunch() {
        if (_isScanning.value) return

        val context = getApplication<Application>()
        if (!MediaPermissionHelper.hasMediaPermissions(context)) {
            _permissionDenied.value = true
            return
        }

        _permissionDenied.value = false
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val now = System.currentTimeMillis()
                val state = memoryDao.getIngestionState() ?: MediaIngestionState()
                val games = scheduleStorageRepository
                    .findGamesSince(now - 1000L * 60L * 60L * 24L * 90L)
                if (games.isEmpty()) {
                    memoryDao.upsertIngestionState(state.copy(lastScannedAtMs = now))
                    return@launch
                }

                val newMedia = queryNewMedia(lastScannedDateAddedMs = state.lastScannedDateAddedMs)
                if (newMedia.isEmpty()) {
                    memoryDao.upsertIngestionState(state.copy(lastScannedAtMs = now))
                    return@launch
                }

                val toInsert = mutableListOf<MemoryItem>()
                val grouped = linkedMapOf<String, MemoryPromptGroupAccumulator>()
                var maxDateAdded = state.lastScannedDateAddedMs

                for (candidate in newMedia) {
                    maxDateAdded = maxOf(maxDateAdded, candidate.dateAddedMs)
                    val existing = scheduleStorageRepository.findMemoryByMediaIdentifier(candidate.contentUri)
                    if (existing != null) continue

                    val bestMatch = memoryGameMatcher.findBestMatch(
                        dateTakenMs = candidate.dateTakenMs,
                        latitude = candidate.latitude,
                        longitude = candidate.longitude,
                        games = games
                    )

                    val isLikelySports = looksLikeSportsMedia(candidate)
                    if (bestMatch == null && !isLikelySports) continue

                    val shouldIncludePrompt = bestMatch != null && bestMatch.score >= 0.55
                    val itemId = UUID.randomUUID().toString()
                    val memory = MemoryItem(
                        id = itemId,
                        contentUri = candidate.contentUri,
                        mediaStoreId = candidate.mediaStoreId,
                        mimeType = candidate.mimeType,
                        displayName = candidate.displayName,
                        dateTakenMs = candidate.dateTakenMs,
                        dateAddedMs = candidate.dateAddedMs,
                        bucketName = candidate.bucketName,
                        width = candidate.width,
                        height = candidate.height,
                        durationMs = candidate.durationMs,
                        latitude = candidate.latitude,
                        longitude = candidate.longitude,
                        sportSeasonId = if (shouldIncludePrompt) bestMatch?.game?.sportSeasonId else null,
                        gameScheduleId = if (shouldIncludePrompt) bestMatch?.game?.id else null,
                        categorizationSource = if (shouldIncludePrompt) "auto_schedule_pending" else "unassigned",
                        autoScore = bestMatch?.score ?: 0.0,
                        needsReview = true
                    )
                    toInsert.add(memory)

                    if (shouldIncludePrompt) {
                        val key = bestMatch!!.game.id
                        val existingGroup = grouped[key]
                        if (existingGroup == null) {
                            grouped[key] = MemoryPromptGroupAccumulator(
                                gameId = bestMatch.game.id,
                                label = formatPromptLabel(bestMatch.game),
                                count = 1,
                                memoryIds = mutableListOf(itemId)
                            )
                        } else {
                            existingGroup.count += 1
                            existingGroup.memoryIds.add(itemId)
                        }
                    }
                }

                if (toInsert.isNotEmpty()) {
                    scheduleStorageRepository.saveMemories(toInsert.map { it.toProfile() })
                }

                memoryDao.upsertIngestionState(
                    MediaIngestionState(
                        key = "default",
                        lastScannedDateAddedMs = maxDateAdded,
                        lastScannedAtMs = now
                    )
                )

                if (grouped.isNotEmpty()) {
                    val groups = grouped.values
                        .sortedByDescending { it.count }
                        .map { MemoryScanPromptGroup(label = it.label, count = it.count) }
                    val ids = grouped.values.flatMap { it.memoryIds }
                    _pendingPrompt.value = MemoryScanPrompt(
                        totalCount = ids.size,
                        groups = groups,
                        pendingMemoryIds = ids
                    )
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun retryScanAfterPermission() {
        _permissionDenied.value = false
        scanForNewMemoriesOnLaunch()
    }

    fun acceptPendingMemories() {
        val prompt = _pendingPrompt.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            memoryReviewService.accept(prompt.pendingMemoryIds, System.currentTimeMillis())
            _pendingPrompt.value = null
        }
    }

    fun skipPendingMemories() {
        val prompt = _pendingPrompt.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            memoryReviewService.skip(prompt.pendingMemoryIds)
            _pendingPrompt.value = null
        }
    }

    fun importSchedule(teamName: String, entries: List<ScheduleImportEntry>, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (entries.isEmpty()) {
                withContext(Dispatchers.Main) { onComplete(0) }
                return@launch
            }

            val team = teamDao.getTeamByName(teamName)
            val firstYear = entries.minByOrNull { it.startMs }?.let {
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it.startMs),
                    ZoneId.systemDefault()
                ).year
            } ?: LocalDate.now().year

            val now = System.currentTimeMillis()
            val importedCount = scheduleImportService.import(
                teamName = teamName,
                sportName = team?.sport ?: "Unknown Sport",
                entries = entries,
                gameIds = List(entries.size) { UUID.randomUUID().toString() },
                seasonYear = firstYear,
                timestamp = now
            )
            withContext(Dispatchers.Main) { onComplete(importedCount) }
        }
    }

    private fun queryNewMedia(lastScannedDateAddedMs: Long): List<MediaCandidate> {
        val results = mutableListOf<MediaCandidate>()
        results += queryMediaCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, lastScannedDateAddedMs)
        results += queryMediaCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, lastScannedDateAddedMs)
        return results.sortedByDescending { it.dateAddedMs }
    }

    private fun queryMediaCollection(
        collectionUri: Uri,
        isVideo: Boolean,
        lastScannedDateAddedMs: Long
    ): List<MediaCandidate> {
        val resolver = getApplication<Application>().contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            "latitude",
            "longitude",
            MediaStore.Video.Media.DURATION
        )

        val lastScannedSeconds = lastScannedDateAddedMs / 1000L
        val selection = "${MediaStore.MediaColumns.DATE_ADDED} > ?"
        val selectionArgs = arrayOf(lastScannedSeconds.toString())

        val items = mutableListOf<MediaCandidate>()
        try {
            resolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val latIndex = cursor.getColumnIndex("latitude")
                val lngIndex = cursor.getColumnIndex("longitude")
                val durationIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val mediaStoreId = cursor.getLong(idIndex)
                    val dateAddedMs = cursor.getLong(dateAddedIndex) * 1000L
                    val takenValue = cursor.getLong(dateTakenIndex)
                    val dateTakenMs = if (takenValue > 0L) takenValue else dateAddedMs
                    val latitude = if (latIndex >= 0 && !cursor.isNull(latIndex)) cursor.getDouble(latIndex) else null
                    val longitude = if (lngIndex >= 0 && !cursor.isNull(lngIndex)) cursor.getDouble(lngIndex) else null
                    val durationMs = if (isVideo && durationIndex >= 0 && !cursor.isNull(durationIndex)) {
                        cursor.getLong(durationIndex)
                    } else {
                        null
                    }
                    val contentUri = Uri.withAppendedPath(collectionUri, mediaStoreId.toString())

                    items.add(
                        MediaCandidate(
                            contentUri = contentUri.toString(),
                            mediaStoreId = mediaStoreId,
                            mimeType = cursor.getString(mimeIndex) ?: "application/octet-stream",
                            displayName = cursor.getString(nameIndex) ?: "Media",
                            dateTakenMs = dateTakenMs,
                            dateAddedMs = dateAddedMs,
                            bucketName = if (bucketIndex >= 0 && !cursor.isNull(bucketIndex)) cursor.getString(bucketIndex) else null,
                            width = if (!cursor.isNull(widthIndex)) cursor.getInt(widthIndex) else null,
                            height = if (!cursor.isNull(heightIndex)) cursor.getInt(heightIndex) else null,
                            durationMs = durationMs,
                            latitude = latitude,
                            longitude = longitude
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }

        return items
    }

    private fun looksLikeSportsMedia(candidate: MediaCandidate): Boolean {
        val text = "${candidate.displayName} ${candidate.bucketName.orEmpty()}".lowercase(Locale.US)
        val sportsKeywords = listOf(
            "game", "match", "tourney", "tournament", "lacrosse", "soccer", "football",
            "basketball", "baseball", "hockey", "volleyball", "athletics", "practice"
        )
        return sportsKeywords.any { text.contains(it) }
    }

    private fun formatPromptLabel(game: GameScheduleProfile): String {
        val date = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(game.scheduledStartMs),
            ZoneId.systemDefault()
        ).toLocalDate()
        val month = date.month.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }
        return "${game.gameLabel.ifBlank { "vs ${game.opponentName}" }} on $month ${date.dayOfMonth}"
    }

}

data class MemoryScanPrompt(
    val totalCount: Int,
    val groups: List<MemoryScanPromptGroup>,
    val pendingMemoryIds: List<String>
)

data class MemoryScanPromptGroup(
    val label: String,
    val count: Int
)

private data class MediaCandidate(
    val contentUri: String,
    val mediaStoreId: Long,
    val mimeType: String,
    val displayName: String,
    val dateTakenMs: Long,
    val dateAddedMs: Long,
    val bucketName: String?,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val latitude: Double?,
    val longitude: Double?
)

private data class MemoryPromptGroupAccumulator(
    val gameId: String,
    val label: String,
    var count: Int,
    val memoryIds: MutableList<String>
)
