package com.playerid.app.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [
        Player::class,
        Team::class,
        UserTeamSubscription::class,
        VideoDetectionResultEntity::class,
        ChildProfile::class,
        SportSeason::class,
        GameSchedule::class,
        MemoryItem::class,
        MediaIngestionState::class
    ],
    version = 11,
    exportSchema = false
)
abstract class PlayerDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun teamDao(): TeamDao
    abstract fun userTeamSubscriptionDao(): UserTeamSubscriptionDao
    abstract fun videoDetectionResultDao(): VideoDetectionResultDao
    abstract fun memoryOrganizationDao(): MemoryOrganizationDao
    
    companion object {
        @Volatile
        private var INSTANCE: PlayerDatabase? = null
        
        fun getDatabase(context: Context): PlayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlayerDatabase::class.java,
                    "player_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}