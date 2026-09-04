package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.player.NeiroMusicStore
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.v0.V0AppShell

class MainActivity : ComponentActivity() {
    private lateinit var musicStore: NeiroMusicStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        musicStore = NeiroMusicStore(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    V0AppShell(store = musicStore)
                }
            }
        }
    }
}


