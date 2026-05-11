package com.heartbeats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.ambient.AmbientLifecycleObserver
import com.heartbeats.ui.HeartbeatsApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Registering the observer keeps the app visible (system-dimmed) in ambient mode;
    // heart-rate streaming continues regardless because it runs in the foreground service.
    private val ambientObserver: AmbientLifecycleObserver by lazy {
        AmbientLifecycleObserver(
            this,
            object : AmbientLifecycleObserver.AmbientLifecycleCallback {
                override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) = Unit
                override fun onExitAmbient() = Unit
                override fun onUpdateAmbient() = Unit
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        setContent { HeartbeatsApp() }
    }
}
