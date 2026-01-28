package com.playerid.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.playerid.app.ui.theme.PlayerIDTheme

class TestActivity : ComponentActivity() {
    
    companion object {
        init {
            Log.e("TestActivity", "🔥 STATIC BLOCK: TestActivity class loaded!")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("TestActivity", "🚀 TestActivity.onCreate() started")
        super.onCreate(savedInstanceState)
        Log.e("TestActivity", "📱 Setting up minimal UI content")
        
        setContent {
            PlayerIDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinimalTestScreen()
                }
            }
        }
        Log.e("TestActivity", "✅ TestActivity setup completed successfully!")
    }
}

@Composable
fun MinimalTestScreen() {
    android.util.Log.e("TestActivity", "🎯 MinimalTestScreen composable called")
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TEST ACTIVITY WORKING!\nPlayerID App Started Successfully",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MinimalTestScreenPreview() {
    PlayerIDTheme {
        MinimalTestScreen()
    }
}