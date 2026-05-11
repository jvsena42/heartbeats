package com.heartbeats.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.heartbeats.ui.theme.HeartbeatsTheme

@Composable
fun HeartbeatsApp(rootViewModel: RootViewModel = hiltViewModel()) {
    val firstLaunch by rootViewModel.firstLaunch.collectAsStateWithLifecycle()
    HeartbeatsTheme {
        when (firstLaunch) {
            null -> LoadingScreen()
            true -> HeartbeatsNavHost(startDestination = Routes.SETUP)
            false -> HeartbeatsNavHost(startDestination = Routes.MAIN)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold(timeText = { TimeText() }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
