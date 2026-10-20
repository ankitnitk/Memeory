package com.memeory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.memeory.app.ui.home.HomeScreen
import com.memeory.app.ui.theme.MemeoryTheme
import com.memeory.core.data.work.MemeWork

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            // Look for new memes in added folders, and finish any unfinished work.
            MemeWork.onAppOpened(this)
        }
        setContent {
            MemeoryTheme {
                HomeScreen()
            }
        }
    }
}
