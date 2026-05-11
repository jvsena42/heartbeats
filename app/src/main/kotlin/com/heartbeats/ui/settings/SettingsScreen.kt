package com.heartbeats.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.heartbeats.R
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.WarmupDuration
import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.ui.labelRes
import com.heartbeats.ui.setup.SelectableChip

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationAccess()
        onPauseOrDispose { }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { ListHeader { Text(stringResource(R.string.settings_title)) } }

            item { ListHeader { Text(stringResource(R.string.settings_age)) } }
            item { AgeRow(age = state.age, onChange = viewModel::changeAge) }

            item { ListHeader { Text(stringResource(R.string.settings_zone)) } }
            items(ZonePreset.entries.size) { index ->
                val preset = ZonePreset.entries[index]
                val range = state.ranges[preset]
                val secondary = if (range != null) {
                    stringResource(R.string.bpm_range, range.lowBpm, range.highBpm)
                } else {
                    null
                }
                Chip(
                    onClick = { viewModel.selectPreset(preset) },
                    label = { Text(stringResource(preset.labelRes())) },
                    secondaryLabel = if (secondary != null) {
                        { Text(secondary) }
                    } else {
                        null
                    },
                    colors = if (preset == state.preset) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { ListHeader { Text(stringResource(R.string.settings_warmup)) } }
            items(WarmupDuration.entries.size) { index ->
                val duration = WarmupDuration.entries[index]
                SelectableChip(
                    label = stringResource(duration.labelRes()),
                    selected = duration == state.warmup,
                    onClick = { viewModel.selectWarmup(duration) },
                )
            }

            item { ListHeader { Text(stringResource(R.string.settings_haptics)) } }
            items(HapticMode.entries.size) { index ->
                val mode = HapticMode.entries[index]
                SelectableChip(
                    label = stringResource(mode.labelRes()),
                    selected = mode == state.hapticMode,
                    onClick = { viewModel.selectHapticMode(mode) },
                )
            }

            item {
                Chip(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    label = { Text(stringResource(R.string.settings_notification_access)) },
                    secondaryLabel = {
                        Text(
                            stringResource(
                                if (state.notificationAccessGranted) {
                                    R.string.settings_notification_access_granted
                                } else {
                                    R.string.settings_notification_access_missing
                                },
                            ),
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_medication_note),
                    style = MaterialTheme.typography.caption3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AgeRow(age: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactButton(onClick = { onChange(-1) }) { Text("−") }
        Spacer(Modifier.width(16.dp))
        Text(text = age.toString(), style = MaterialTheme.typography.title1)
        Spacer(Modifier.width(16.dp))
        CompactButton(onClick = { onChange(1) }) { Text("+") }
    }
}
