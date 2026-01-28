package com.playerid.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * Completely empty test activity to isolate startup crashes.
 * This will help determine if the problem is:
 * 1. Dependencies/build configuration (if this crashes)
 * 2. MainActivity code (if this works but MainActivity crashes)
 */
class CrashTestActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "CrashTestActivity"
        
        init {
            Log.d(TAG, "Static block executing - class loading successful")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() called - entry point reached")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() completed - activity initialized successfully")
        
        // Absolutely minimal content - just a blank screen
        // No Compose, no camera, no ML, no custom views
        setContentView(android.R.layout.activity_list_item)
        
        Log.d(TAG, "setContentView completed - CrashTestActivity fully loaded")
    }
}