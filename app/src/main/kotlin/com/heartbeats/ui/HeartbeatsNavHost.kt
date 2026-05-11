package com.heartbeats.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.heartbeats.ui.main.MainScreen
import com.heartbeats.ui.settings.SettingsScreen
import com.heartbeats.ui.setup.SetupScreen

object Routes {
    const val SETUP = "setup"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun HeartbeatsNavHost(startDestination: String) {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SETUP) {
            SetupScreen(
                onDone = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
