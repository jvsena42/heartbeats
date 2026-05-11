package com.heartbeats.ui.setup

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.heartbeats.domain.model.WarmupDuration
import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.ui.labelRes

@Composable
fun SetupScreen(
    onDone: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

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
            item { ListHeader { Text(stringResource(R.string.app_name)) } }

            item {
                Text(
                    text = stringResource(R.string.setup_age_subtitle),
                    style = MaterialTheme.typography.caption2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
            }
            item { AgeStepper(age = state.age, onChange = viewModel::changeAge) }

            item { ListHeader { Text(stringResource(R.string.setup_zone_title)) } }
            items(ZonePreset.entries.size) { index ->
                val preset = ZonePreset.entries[index]
                ZonePresetChip(
                    preset = preset,
                    range = state.ranges[preset],
                    selected = preset == state.preset,
                    onClick = { viewModel.selectPreset(preset) },
                )
            }

            item { ListHeader { Text(stringResource(R.string.setup_warmup_title)) } }
            items(WarmupDuration.entries.size) { index ->
                val duration = WarmupDuration.entries[index]
                SelectableChip(
                    label = stringResource(duration.labelRes()),
                    selected = duration == state.warmup,
                    onClick = { viewModel.selectWarmup(duration) },
                )
            }

            item {
                Chip(
                    onClick = { viewModel.finish(onDone) },
                    label = { Text(stringResource(R.string.setup_done)) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AgeStepper(age: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactButton(onClick = { onChange(-1) }) { Text("−") }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = age.toString(), style = MaterialTheme.typography.display2)
            Text(text = stringResource(R.string.setup_age_title), style = MaterialTheme.typography.caption3)
        }
        Spacer(Modifier.width(12.dp))
        CompactButton(onClick = { onChange(1) }) { Text("+") }
    }
}

@Composable
private fun ZonePresetChip(
    preset: ZonePreset,
    range: ZoneRange?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Chip(
        onClick = onClick,
        label = { Text(stringResource(preset.labelRes())) },
        secondaryLabel = if (range != null) {
            { Text(stringResource(R.string.bpm_range, range.lowBpm, range.highBpm)) }
        } else {
            null
        },
        colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text(label) },
        colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
