package com.heartbeats.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.HapticPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Plays the differentiated vibration patterns, respecting the user's [HapticMode]. */
@Singleton
class HapticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }

    fun play(pattern: HapticPattern, mode: HapticMode) {
        if (!isEnabled(pattern, mode)) return
        val vibrator = vibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, NO_REPEAT))
    }

    private fun isEnabled(pattern: HapticPattern, mode: HapticMode): Boolean = when (mode) {
        HapticMode.OFF -> false
        HapticMode.BELOW_ONLY -> pattern != HapticPattern.ABOVE_ZONE && pattern != HapticPattern.BACK_IN_ZONE
        HapticMode.BOTH -> true
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}
