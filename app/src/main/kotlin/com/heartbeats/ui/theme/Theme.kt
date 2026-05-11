package com.heartbeats.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val HeartbeatsColors = Colors(
    primary = HeartRed,
    secondary = InZoneGreen,
    background = ScreenBackground,
    surface = SurfaceDark,
    error = AboveRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = OnDark,
    onSurface = OnDark,
)

@Composable
fun HeartbeatsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = HeartbeatsColors, content = content)
}
