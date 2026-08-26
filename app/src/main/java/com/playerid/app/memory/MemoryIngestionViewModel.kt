package com.playerid.app.memory

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playerid.app.data.ChildProfile
import com.playerid.app.data.GameSchedule
import com.playerid.app.data.MediaIngestionState
import com.playerid.app.data.MemoryItem
import com.playerid.app.data.PlayerDatabase
import com.playerid.app.data.SportSeason
import com.playerid.app.domain.team.ScheduleImportEntry
import com.playerid.app.domain.team.parseScheduleCsv
import com.playerid.app.utils.MediaPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEFAULT_CHILD_ID = "default-child"

class MemoryIngestionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PlayerDatabase.getDatabase(application)
    private val memoryDao = database.memoryOrganizationDao()
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
                val games = memoryDao.getGamesSince(now - 1000L * 60L * 60L * 24L * 90L)
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
                    val existing = memoryDao.findMemoryByUri(candidate.contentUri)
                    if (existing != null) continue

                    val bestMatch = findBestGameMatch(
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
                    memoryDao.upsertMemoryItems(toInsert)
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
            val existing = memoryDao.getMemoryItemsByIds(prompt.pendingMemoryIds)
            if (existing.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val updated = existing.map { item ->
                    item.copy(
                        needsReview = false,
                        categorizationSource = "auto_schedule_accepted",
                        reviewedAtMs = now,
                        updatedAt = now
                    )
                }
                memoryDao.upsertMemoryItems(updated)
            }
            _pendingPrompt.value = null
        }
    }

    fun skipPendingMemories() {
        val prompt = _pendingPrompt.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemoryItemsByIds(prompt.pendingMemoryIds)
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
            val childProfile = ChildProfile(
                id = DEFAULT_CHILD_ID,
                displayName = "My Child"
            )
            memoryDao.upsertChildProfile(childProfile)

            val firstYear = entries.minByOrNull { it.startMs }?.let {
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it.startMs),
                    ZoneId.systemDefault()
                ).year
            } ?: LocalDate.now().year

            val season = memoryDao.getActiveSeasonForTeam(teamName) ?: SportSeason(
                id = "season-${teamName.lowercase(Locale.US).replace(" ", "-")}-${firstYear}",
                childId = DEFAULT_CHILD_ID,
                sportName = team?.sport ?: "Unknown Sport",
                seasonLabel = "$firstYear Season",
                teamName = teamName
            )
            memoryDao.upsertSportSeason(season)

            val now = System.currentTimeMillis()
            val games = entries.map { entry ->
                GameSchedule(
                    id = UUID.randomUUID().toString(),
                    sportSeasonId = season.id,
                    opponentName = entry.opponent,
                    gameLabel = entry.gameLabel.ifBlank { "vs ${entry.opponent}" },
                    scheduledStartMs = entry.startMs,
                    scheduledEndMs = entry.endMs,
                    locationName = entry.locationName,
                    locationLat = entry.latitude,
                    locationLng = entry.longitude,
                    source = "uploaded",
                    createdAt = now,
                    updatedAt = now
                )
            }
            memoryDao.upsertGameSchedules(games)
            withContext(Dispatchers.Main) { onComplete(games.size) }
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

    private fun findBestGameMatch(
        dateTakenMs: Long,
        latitude: Double?,
        longitude: Double?,
        games: List<GameSchedule>
    ): GameMatchResult? {
        var best: GameMatchResult? = null
        for (game in games) {
            val score = scoreGameMatch(dateTakenMs, latitude, longitude, game)
            if (best == null || score > best.score) {
                best = GameMatchResult(game = game, score = score)
            }
        }
        return best
    }

    private fun scoreGameMatch(
        dateTakenMs: Long,
        latitude: Double?,
        longitude: Double?,
        game: GameSchedule
    ): Double {
        val preWindowMs = 1000L * 60L * 60L * 3L
        val postWindowMs = 1000L * 60L * 60L * 4L
        val inWindow = dateTakenMs in (game.scheduledStartMs - preWindowMs)..(game.scheduledEndMs + postWindowMs)

        val timeScore = if (inWindow) {
            val midpoint = (game.scheduledStartMs + game.scheduledEndMs) / 2L
            val deltaHours = kotlin.math.abs(dateTakenMs - midpoint) / (1000.0 * 60.0 * 60.0)
            (1.0 - (deltaHours / 6.0)).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val locationScore = if (
            latitude != null && longitude != null &&
            game.locationLat != null && game.locationLng != null
        ) {
            val km = haversineKm(latitude, longitude, game.locationLat, game.locationLng)
            when {
                km <= 1.0 -> 1.0
                km <= 5.0 -> 0.8
                km <= 15.0 -> 0.55
                km <= 30.0 -> 0.35
                else -> 0.0
            }
        } else {
            0.0
        }

        return (timeScore * 0.75) + (locationScore * 0.25)
    }

    private fun formatPromptLabel(game: GameSchedule): String {
        val date = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(game.scheduledStartMs),
            ZoneId.systemDefault()
        ).toLocalDate()
        val month = date.month.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }
        return "${game.gameLabel.ifBlank { "vs ${game.opponentName}" }} on $month ${date.dayOfMonth}"
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val startLat = Math.toRadians(lat1)
        val endLat = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLon / 2) * sin(dLon / 2) * cos(startLat) * cos(endLat)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
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

private data class GameMatchResult(
    val game: GameSchedule,
    val score: Double
)

private data class MemoryPromptGroupAccumulator(
    val gameId: String,
    val label: String,
    var count: Int,
    val memoryIds: MutableList<String>
)

private fun parseScheduleTextLegacy(rawText: String): List<ScheduleImportEntry> {
    val csvEntries = parseScheduleCsv(rawText)
    if (csvEntries.isNotEmpty()) return csvEntries

    val lines = rawText.lineSequence()
        .map { it.replace("\u00A0", " ").replace(Regex("\\s+"), " ").trim() }
        .filter(String::isNotEmpty)
        .toList()
    val datePattern = Regex(
        "(?i)\\b(?:\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?|(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)[\\s\\n]+\\d{1,2}(?:,?[\\s\\n]+\\d{4})?)\\b"
    )
    val timePattern = Regex("(?i)\\b(?:1[0-2]|0?[1-9])(?::[0-5]\\d)?\\s*(?:AM|PM)\\b|\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
    val opponentPattern = Regex("(?im)(?:^|\\n)\\s*(?:vs\\.?|@)\\s*([^|•,\\n]+)")
    val venueRowPattern = Regex("(?im)^\\s*([^\\n|•]+?)\\s+at\\s+([^\\n|•»]+)")
    val zone = ZoneId.systemDefault()
    val joined = lines.joinToString("\n")
    val dateMatches = datePattern.findAll(joined).toList()

    fun scheduleLevelAt(position: Int): String? {
        return Regex("(?im)^SCHEDULE LEVEL:\\s*(.+)$")
            .findAll(joined.substring(0, position))
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    fun buildEntry(
        dateText: String,
        details: String,
        fallbackOpponent: String? = null,
        fallbackTime: String? = null,
        requireExplicitRowEvidence: Boolean = true
    ): ScheduleImportEntry? {
        val date = parseFlexibleDate(dateText.replace(Regex("\\s+"), " ")) ?: return null
        val detectedTime = timePattern.find(details)?.value ?: fallbackTime
        val startTime = detectedTime?.let(::parseFlexibleTime) ?: return null
        val explicitOpponent = opponentPattern.find(details)?.groupValues?.getOrNull(1)
            ?.replace(timePattern, "")
            ?.replace(Regex("(?i)\\s+(?:home|away)\\s*$"), "")
            ?.trim(' ', '-', ':')
            ?.takeIf(String::isNotEmpty)
        if (requireExplicitRowEvidence && explicitOpponent == null) return null
        val opponent = explicitOpponent ?: fallbackOpponent?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (opponent.length > 60 || opponent.contains(Regex("(?i)\\b(menu|login|sign in|calendar|schedule|roster|navigation)\\b"))) {
            return null
        }
        val isAway = details.contains(Regex("(?i)(?:^|\\s)@\\s*${Regex.escape(opponent)}"))
        val startMs = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        return ScheduleImportEntry(
            gameLabel = if (isAway) "@ $opponent" else "vs $opponent",
            opponent = opponent,
            startMs = startMs,
            endMs = date.atTime(startTime.plusHours(2)).atZone(zone).toInstant().toEpochMilli(),
            locationName = null,
            latitude = null,
            longitude = null
        )
    }

    val segmented = dateMatches.mapIndexedNotNull { index, dateMatch ->
        val detailsStart = dateMatch.range.last + 1
        val detailsEnd = dateMatches.getOrNull(index + 1)?.range?.first ?: joined.length
        val nearbyDetails = joined.substring(detailsStart, detailsEnd)
            .lineSequence()
            .take(5)
            .joinToString("\n")
        buildEntry(dateMatch.value, nearbyDetails)
    }.distinctBy { it.startMs to it.opponent.lowercase(Locale.US) }

    val tableRows = dateMatches.mapIndexedNotNull { index, dateMatch ->
        if (!dateMatch.value.contains(Regex("[/-]\\d{2,4}"))) return@mapIndexedNotNull null
        val detailsStart = dateMatch.range.last + 1
        val detailsEnd = dateMatches.getOrNull(index + 1)?.range?.first ?: joined.length
        val details = joined.substring(detailsStart, detailsEnd)
            .lineSequence()
            .take(6)
            .joinToString("\n")
        val time = timePattern.find(details)?.value ?: return@mapIndexedNotNull null
        val matchupLine = details.lineSequence().firstOrNull { venueRowPattern.containsMatchIn(it) }
            ?: return@mapIndexedNotNull null
        val venueMatch = venueRowPattern.find(matchupLine) ?: return@mapIndexedNotNull null
        val rawOpponent = venueMatch.groupValues[1]
            .replace(Regex("\\s*».*$"), "")
            .replace(Regex("\\s*#\\s*$"), "")
            .replace(Regex("(?i)\\s*\\(scrimmage\\)\\s*$"), "")
            .trim()
        val isAway = rawOpponent.startsWith("@")
        val opponentWithoutMarker = rawOpponent.removePrefix("@").trim()
        val opponent = if (isAway) {
            opponentWithoutMarker
        } else {
            extractUppercaseOpponent(opponentWithoutMarker)
        }
        if (opponent.isBlank()) return@mapIndexedNotNull null
        val date = parseFlexibleDate(dateMatch.value.replace(Regex("(?i)^(?:mon|tue|wed|thu|fri|sat|sun),?\\s*"), ""))
            ?: return@mapIndexedNotNull null
        val startTime = parseFlexibleTime(time) ?: return@mapIndexedNotNull null
        val startMs = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
        val scheduleLevel = scheduleLevelAt(dateMatch.range.first)
        val matchupLabel = if (isAway) "@ $opponent" else "vs $opponent"
        ScheduleImportEntry(
            gameLabel = scheduleLevel?.let { "$it $matchupLabel" } ?: matchupLabel,
            opponent = opponent,
            startMs = startMs,
            endMs = date.atTime(startTime.plusHours(2)).atZone(zone).toInstant().toEpochMilli(),
            locationName = venueMatch.groupValues[2].trim(),
            latitude = null,
            longitude = null
        )
    }

    if (tableRows.isNotEmpty()) {
        return tableRows
            .distinctBy { Triple(it.startMs, it.opponent.lowercase(Locale.US), it.gameLabel.lowercase(Locale.US)) }
            .sortedBy(ScheduleImportEntry::startMs)
    }

    if (segmented.size == dateMatches.size || dateMatches.size <= 1) return segmented

    fun sectionLines(header: String, followingHeaders: Set<String>): List<String> {
        val start = lines.indexOfFirst { it.equals(header, ignoreCase = true) }
        if (start < 0) return emptyList()
        val end = ((start + 1) until lines.size).firstOrNull { index ->
            followingHeaders.any { lines[index].equals(it, ignoreCase = true) }
        } ?: lines.size
        return lines.subList(start + 1, end)
    }

    val dateSection = sectionLines("DATES", setOf("OPPONENTS", "TIMES"))
    val opponentSection = sectionLines("OPPONENTS", setOf("DATES", "TIMES"))
    val timeSection = sectionLines("TIMES", setOf("DATES", "OPPONENTS"))
    if (dateSection.isEmpty() || opponentSection.isEmpty() || timeSection.isEmpty()) return segmented

    val columnDates = datePattern.findAll(dateSection.joinToString("\n")).toList()
    val opponents = opponentPattern.findAll(opponentSection.joinToString("\n")).mapNotNull { match ->
        match.groupValues.getOrNull(1)
            ?.replace(timePattern, "")
            ?.replace(Regex("(?i)\\s+(?:home|away)\\s*$"), "")
            ?.trim(' ', '-', ':')
            ?.takeIf(String::isNotEmpty)
    }.toList()
    val times = timePattern.findAll(timeSection.joinToString("\n")).map { it.value }.toList()
    val columnEntries = columnDates.mapIndexedNotNull { index, dateMatch ->
        buildEntry(
            dateText = dateMatch.value,
            details = "",
            fallbackOpponent = opponents.getOrNull(index),
            fallbackTime = times.getOrNull(index),
            requireExplicitRowEvidence = false
        )
    }
    val resolvedEntries = if (columnEntries.size == columnDates.size) columnEntries else segmented + columnEntries
    return resolvedEntries
        .distinctBy { it.startMs to it.opponent.lowercase(Locale.US) }
        .sortedBy(ScheduleImportEntry::startMs)
}

private fun extractUppercaseOpponent(raw: String): String {
    val cleaned = raw.replace(Regex("\\s*#\\s*$"), "").trim()
    val uppercaseRuns = Regex("(?:[A-Z][A-Z.'-]*)(?:\\s+(?:[A-Z][A-Z.'-]*|W/)){0,5}")
        .findAll(cleaned)
        .map { it.value.trim() }
        .filter { it.length >= 3 }
        .toList()
    return uppercaseRuns.lastOrNull() ?: cleaned
}

private fun parseFlexibleDate(raw: String): LocalDate? {
    val normalized = raw.trim().replace(Regex("\\s+"), " ")
    val currentYear = LocalDate.now().year
    val formatters = listOf(
        "M/d/uuuu", "M-d-uuuu", "M/d/uu", "M-d-uu",
        "MMM d, uuuu", "MMMM d, uuuu", "MMM d uuuu", "MMMM d uuuu"
    ).map { pattern ->
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.US)
    }
    formatters.forEach { formatter ->
        runCatching { LocalDate.parse(normalized, formatter) }.getOrNull()?.let { return it }
    }
    val withoutYear = listOf("M/d", "M-d", "MMM d", "MMMM d")
        .map { pattern ->
            DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.YEAR, currentYear.toLong())
                .toFormatter(Locale.US)
        }
    withoutYear.forEach { formatter ->
        runCatching { LocalDate.parse(normalized, formatter) }
            .getOrNull()?.let { return it }
    }
    return null
}

private fun parseFlexibleTime(raw: String): LocalTime? {
    val normalized = raw.trim().uppercase(Locale.US)
    return runCatching { LocalTime.parse(normalized) }.getOrNull()
        ?: runCatching {
            val amPm = when {
                normalized.endsWith("AM") -> "AM"
                normalized.endsWith("PM") -> "PM"
                else -> null
            }
            if (amPm != null) {
                val stripped = normalized.removeSuffix(amPm).trim()
                val chunks = stripped.split(":")
                val hourBase = chunks.getOrNull(0)?.toIntOrNull() ?: return@runCatching null
                val minute = chunks.getOrNull(1)?.toIntOrNull() ?: 0
                var hour = hourBase % 12
                if (amPm == "PM") hour += 12
                LocalTime.of(hour, minute)
            } else {
                null
            }
        }.getOrNull()
}
