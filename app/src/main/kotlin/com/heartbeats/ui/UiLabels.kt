package com.heartbeats.ui

import androidx.annotation.StringRes
import com.heartbeats.R
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.WarmupDuration
import com.heartbeats.domain.model.ZonePreset

@StringRes
fun ZonePreset.labelRes(): Int = when (this) {
    ZonePreset.FAT_BURN -> R.string.zone_fat_burn
    ZonePreset.CARDIO -> R.string.zone_cardio
    ZonePreset.PEAK -> R.string.zone_peak
}

@StringRes
fun WarmupDuration.labelRes(): Int = when (this) {
    WarmupDuration.ONE_MIN -> R.string.warmup_1_min
    WarmupDuration.THREE_MIN -> R.string.warmup_3_min
    WarmupDuration.FIVE_MIN -> R.string.warmup_5_min
    WarmupDuration.TEN_MIN -> R.string.warmup_10_min
}

@StringRes
fun HapticMode.labelRes(): Int = when (this) {
    HapticMode.OFF -> R.string.haptics_off
    HapticMode.BELOW_ONLY -> R.string.haptics_below_only
    HapticMode.BOTH -> R.string.haptics_both
}
