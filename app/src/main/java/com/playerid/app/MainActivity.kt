package com.playerid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.playerid.app.ui.theme.PlayerIDTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        init {
            android.util.Log.e("MainActivity", "🔥 STATIC BLOCK: MainActivity class loaded!")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.i("MainActivity", "🚀 MainActivity.onCreate() started")
        super.onCreate(savedInstanceState)
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
}