package com.playerid.app.video

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.playerid.app.data.DetectionResultSerializer
import com.playerid.app.data.PlayerDatabase
import com.playerid.app.data.VideoDetectionResultEntity

class DeferredDeepScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val videoUriString = inputData.getString(DeferredDeepScanScheduler.KEY_VIDEO_URI)
        val teamName = inputData.getString(DeferredDeepScanScheduler.KEY_TEAM_NAME)?.trim().orEmpty()
        val jerseyColorHex = inputData.getString(DeferredDeepScanScheduler.KEY_JERSEY_COLOR_HEX)

        if (videoUriString.isNullOrBlank() || teamName.isBlank()) return Result.failure()

        return try {
            val videoUri = Uri.parse(videoUriString)
            val database = PlayerDatabase.getDatabase(applicationContext)
            val detectionDao = database.videoDetectionResultDao()
            val playerDao = database.playerDao()

            val cached = VideoSharePreparationCache.get(videoUri)
            if (cached?.mode == VideoProcessingManager.DetectionMode.ACCURATE) return Result.success()
            val existing = detectionDao.getDetectionResult(videoUriString)
            if (existing?.detectionMode == "ACCURATE") return Result.success()

            val roster = playerDao.getPlayersByTeamSnapshot(teamName)
            if (roster.isEmpty()) return Result.success()

            val manager = VideoProcessingManager(applicationContext)
            val result = try {
                manager.autoDetectPlayersWithTracksInVideo(
                    videoUri = videoUri,
                    roster = roster,
                    mode = VideoProcessingManager.DetectionMode.ACCURATE,
                    jerseyColorHex = jerseyColorHex,
                    maxScanDurationMs = 20_000L,
                    stopAfterUniqueDetections = 4
                )
            } finally {
                manager.release()
            }

            detectionDao.insertDetectionResult(
                VideoDetectionResultEntity(
                    videoUri = videoUriString,
                    detectionMode = "ACCURATE",
                    detectionJson = DetectionResultSerializer.serialize(result),
                    detectionTimestampMs = System.currentTimeMillis()
                )
            )
            VideoSharePreparationCache.set(
                videoUri,
                PreparedShareResult(
                    analysisResult = result,
                    preparedAtMs = System.currentTimeMillis(),
                    mode = VideoProcessingManager.DetectionMode.ACCURATE
                )
            )

            Result.success()
        } catch (e: Exception) {
            Log.d("DeferredDeepScan", "Worker retry for $videoUriString: ${e.message}")
            Result.retry()
        }
    }
}