package com.memeory.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.memeory.app.ui.detail.MemeDetailScreen
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
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(onOpenMeme = { id -> nav.navigate("meme/$id") })
                    }
                    composable(
                        route = "meme/{memeId}",
                        arguments = listOf(navArgument("memeId") { type = NavType.LongType }),
                    ) {
                        MemeDetailScreen(onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
