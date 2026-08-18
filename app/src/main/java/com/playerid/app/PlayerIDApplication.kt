package com.playerid.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

class PlayerIDApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.i("PlayerIDApplication", "Initializing WorkManager for app startup")
        WorkManager.initialize(this, workManagerConfiguration)
        Log.i("PlayerIDApplication", "WorkManager initialized successfully")
    }
}
