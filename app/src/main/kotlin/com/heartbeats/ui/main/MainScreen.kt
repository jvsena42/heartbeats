package com.heartbeats.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.heartbeats.R
import com.heartbeats.domain.model.WorkoutState
import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.domain.model.ZoneStatus
import com.heartbeats.ui.labelRes
import com.heartbeats.ui.theme.zoneStatusColor

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationAccess()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[hrPermission()] == true) viewModel.startWorkout()
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val workout = state.workout) {
                WorkoutState.Idle -> IdleContent(
                    preset = state.preset,
                    zoneRange = state.zoneRange,
                    notificationAccessGranted = state.notificationAccessGranted,
                    onStart = {
                        val missing = missingPermissions(context)
                        if (missing.isEmpty()) viewModel.startWorkout() else permissionLauncher.launch(missing.toTypedArray())
                    },
                    onOpenSettings = onOpenSettings,
                    onGrantMusicAccess = { context.startActivity(notificationListenerSettingsIntent()) },
                )

                is WorkoutState.Warmup -> WarmupContent(
                    state = workout,
                    onSkip = viewModel::skipWarmup,
                    onStop = viewModel::stopWorkout,
                )

                is WorkoutState.Active -> ActiveContent(
                    state = workout,
                    onStop = viewModel::stopWorkout,
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    preset: ZonePreset,
    zoneRange: ZoneRange?,
    notificationAccessGranted: Boolean,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onGrantMusicAccess: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.main_idle_title), style = MaterialTheme.typography.title3)
        Text(stringResource(preset.labelRes()), style = MaterialTheme.typography.caption1)
        if (zoneRange != null) {
            Text(
                text = stringResource(R.string.bpm_range, zoneRange.lowBpm, zoneRange.highBpm),
                style = MaterialTheme.typography.caption2,
            )
        }
        Spacer8()
        Chip(
            onClick = onStart,
            label = { Text(stringResource(R.string.action_start)) },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!notificationAccessGranted) {
            Spacer8()
            CompactChip(
                onClick = onGrantMusicAccess,
                label = { Text(stringResource(R.string.settings_notification_access_missing)) },
            )
        }
        Spacer8()
        CompactChip(onClick = onOpenSettings, label = { Text(stringResource(R.string.settings_title)) })
    }
}

@Composable
private fun WarmupContent(
    state: WorkoutState.Warmup,
    onSkip: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.warmup_title), style = MaterialTheme.typography.caption1)
        Text(
            text = stringResource(R.string.warmup_remaining, state.remainingSeconds / 60, state.remainingSeconds % 60),
            style = MaterialTheme.typography.display1,
        )
        if (state.hrAvailable && state.currentBpm != null) {
            Text(
                text = "${state.currentBpm} " + stringResource(R.string.bpm_unit),
                style = MaterialTheme.typography.body2,
            )
        } else {
            Text(stringResource(R.string.waiting_for_hr), style = MaterialTheme.typography.caption2)
        }
        Spacer8()
        if (state.inZone) {
            Chip(
                onClick = onSkip,
                label = { Text(stringResource(R.string.action_skip_warmup)) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer8()
        }
        CompactChip(onClick = onStop, label = { Text(stringResource(R.string.action_stop)) })
    }
}

@Composable
private fun ActiveContent(
    state: WorkoutState.Active,
    onStop: () -> Unit,
) {
    val statusColor = if (state.hrAvailable) zoneStatusColor(state.zoneStatus) else MaterialTheme.colors.onBackground
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.currentBpm?.toString() ?: "––",
            color = statusColor,
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(R.string.bpm_unit), style = MaterialTheme.typography.caption2)
        Spacer8()
        Text(
            text = stringResource(activeStatusRes(state)),
            color = statusColor,
            style = MaterialTheme.typography.caption1,
            textAlign = TextAlign.Center,
        )
        if (state.musicPaused) {
            Text(
                text = stringResource(R.string.music_paused_hint),
                style = MaterialTheme.typography.caption3,
                textAlign = TextAlign.Center,
            )
        }
        Spacer8()
        CompactChip(onClick = onStop, label = { Text(stringResource(R.string.action_stop)) })
    }
}

@Composable
private fun Spacer8() {
    Spacer(Modifier.height(8.dp))
}

private fun activeStatusRes(state: WorkoutState.Active): Int = when {
    !state.hrAvailable -> R.string.hr_unavailable
    state.zoneStatus == ZoneStatus.BELOW -> R.string.status_below
    state.zoneStatus == ZoneStatus.ABOVE -> R.string.status_above
    else -> R.string.status_in_zone
}

private fun hrPermission(): String =
    if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS

private fun requiredPermissions(): List<String> = buildList {
    add(hrPermission())
    add(Manifest.permission.ACTIVITY_RECOGNITION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

private fun missingPermissions(context: Context): List<String> = requiredPermissions().filter {
    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
}

private fun notificationListenerSettingsIntent(): Intent =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
