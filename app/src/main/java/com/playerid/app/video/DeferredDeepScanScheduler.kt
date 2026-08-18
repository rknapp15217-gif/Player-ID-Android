package com.playerid.app.video

import android.content.Context
import android.net.Uri
import android.util.Log
import com.playerid.app.data.Player
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DeferredDeepScanScheduler {
    private const val DEFAULT_DELAY_MS = 90_000L
    private const val WORK_NAME_PREFIX = "deferred_deep_scan_"
    internal const val KEY_VIDEO_URI = "video_uri"
    internal const val KEY_TEAM_NAME = "team_name"
    internal const val KEY_JERSEY_COLOR_HEX = "jersey_color_hex"

    fun schedule(
        context: Context,
        videoUri: Uri,
        roster: List<Player>,
        jerseyColorHex: String? = null,
        delayMs: Long = DEFAULT_DELAY_MS
    ) {
        Log.d("DeferredDeepScanScheduler", "schedule() called for uri=$videoUri rosterSize=${roster.size} delayMs=$delayMs")
        if (roster.isEmpty()) return

        val teamName = roster.firstOrNull()?.team?.trim().orEmpty()
        if (teamName.isBlank()) return

        val inputData = Data.Builder()
            .putString(KEY_VIDEO_URI, videoUri.toString())
            .putString(KEY_TEAM_NAME, teamName)
            .putString(KEY_JERSEY_COLOR_HEX, jerseyColorHex)
            .build()

        // Keep this low-priority and opportunistic so users don't feel active UI slowdown.
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = OneTimeWorkRequestBuilder<DeferredDeepScanWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val uniqueWorkName = "$WORK_NAME_PREFIX${videoUri}"
        try {
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.KEEP, request)
        } catch (e: IllegalStateException) {
            Log.w(
                "DeferredDeepScanScheduler",
                "WorkManager not initialized; skipping deferred deep scan for $videoUri",
                e
            )
        }
    }
}