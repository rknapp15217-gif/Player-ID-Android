package com.playerid.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.playerid.app.data.teamsnap.TeamSnapAuthCallback
import com.playerid.app.ui.theme.PlayerIDTheme
import com.playerid.app.PlayerIDApp

class MainActivity : ComponentActivity() {
    
    companion object {
        init {
            android.util.Log.e("MainActivity", "🔥 STATIC BLOCK: MainActivity class loaded!")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.i("MainActivity", "🚀 MainActivity.onCreate() started")
        super.onCreate(savedInstanceState)
        
        // Handle OAuth redirect
        intent?.data?.let { uri ->
            if (uri.scheme == "playerid-teamsnap" && uri.host == "oauth") {
                android.util.Log.i("MainActivity", "🔑 TeamSnap OAuth redirect detected: $uri")
                TeamSnapAuthCallback.setRedirect(uri)
            }
        }
        
        // Handle navigation from external sources (e.g., ScreenCaptureService)
        intent?.getStringExtra(AppNavigationCallback.EXTRA_NAV_ROUTE)?.let { route ->
            android.util.Log.i("MainActivity", "🧭 Navigation route from intent: $route")
            AppNavigationCallback.setRoute(route)
        }
        
        android.util.Log.i("MainActivity", "📱 Setting up UI content")
        setContent {
            PlayerIDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    android.util.Log.i("MainActivity", "🎯 Calling PlayerIDApp() composable")

                    PlayerIDApp()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Handle OAuth redirect when app is already running
        intent.data?.let { uri ->
            if (uri.scheme == "playerid-teamsnap" && uri.host == "oauth") {
                android.util.Log.i("MainActivity", "🔑 TeamSnap OAuth redirect (existing activity): $uri")
                TeamSnapAuthCallback.setRedirect(uri)
            }
        }
        
        // Handle navigation from external sources
        intent.getStringExtra(AppNavigationCallback.EXTRA_NAV_ROUTE)?.let { route ->
            android.util.Log.i("MainActivity", "🧭 Navigation route from new intent: $route")
            AppNavigationCallback.setRoute(route)
        }
    }
}