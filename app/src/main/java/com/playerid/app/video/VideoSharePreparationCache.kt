package com.playerid.app.video

import android.content.Context
import android.net.Uri
import com.playerid.app.data.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches background player-ID analysis per clip so share flow can open faster.
 */
object VideoSharePreparationCache {
    private val cache = ConcurrentHashMap<String, PreparedShareResult>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    suspend fun prepare(
        context: Context,
        videoUri: Uri,
        roster: List<Player>,
        mode: VideoProcessingManager.DetectionMode = VideoProcessingManager.DetectionMode.FAST
    ) {
        if (roster.isEmpty()) return
        val key = videoUri.toString()
        if (cache.containsKey(key)) return
        if (!inFlight.add(key)) return

        val manager = VideoProcessingManager(context)
        try {
            val result = manager.autoDetectPlayersWithTracksInVideo(
                videoUri = videoUri,
                roster = roster,
                mode = mode,
                onProgress = {}
            )
            cache[key] = PreparedShareResult(
                analysisResult = result,
                preparedAtMs = System.currentTimeMillis(),
                mode = mode
            )
        } catch (_: Exception) {
            // Background preparation should never interrupt the capture flow.
        } finally {
            manager.release()
            inFlight.remove(key)
        }
    }

    fun get(videoUri: Uri): PreparedShareResult? = cache[videoUri.toString()]

    fun set(videoUri: Uri, result: PreparedShareResult) {
        cache[videoUri.toString()] = result
    }

    fun clear(videoUri: Uri) {
        cache.remove(videoUri.toString())
    }
}

data class PreparedShareResult(
    val analysisResult: VideoPlayerDetectionResult,
    val preparedAtMs: Long,
    val mode: VideoProcessingManager.DetectionMode
)
