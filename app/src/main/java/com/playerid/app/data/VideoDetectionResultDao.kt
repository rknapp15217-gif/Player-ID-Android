package com.playerid.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoDetectionResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetectionResult(result: VideoDetectionResultEntity)

    @Query("SELECT * FROM video_detection_results WHERE videoUri = :videoUri LIMIT 1")
    suspend fun getDetectionResult(videoUri: String): VideoDetectionResultEntity?

    @Query("DELETE FROM video_detection_results WHERE videoUri = :videoUri")
    suspend fun deleteDetectionResult(videoUri: String)

    @Query("DELETE FROM video_detection_results")
    suspend fun deleteAllDetectionResults()
}
