package com.heartbeats.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.heartbeats.MainActivity
import com.heartbeats.R
import com.heartbeats.data.repository.SettingsRepository
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.HapticPattern
import com.heartbeats.domain.model.WorkoutState
import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.domain.model.ZoneStatus
import com.heartbeats.domain.usecase.ComputeZoneRangeUseCase
import com.heartbeats.domain.usecase.ShouldPauseMusicUseCase
import com.heartbeats.domain.usecase.WarmupEvent
import com.heartbeats.domain.usecase.WarmupTimerUseCase
import com.heartbeats.domain.usecase.ZoneSample
import com.heartbeats.domain.usecase.ZoneStatusClassifier
import com.heartbeats.haptics.HapticsManager
import com.heartbeats.health.HealthServicesManager
import com.heartbeats.health.HealthUpdate
import com.heartbeats.health.HrAvailability
import com.heartbeats.media.MediaControllerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service (type `health`) that owns a Heartbeats workout: it streams heart
 * rate via Health Services, runs the warmup, then enforces the target zone — pausing and
 * resuming the user's music via [MediaControllerManager] and buzzing via [HapticsManager].
 * State is published through [WorkoutController].
 */
@AndroidEntryPoint
class WorkoutService : LifecycleService() {

    @Inject lateinit var health: HealthServicesManager
    @Inject lateinit var media: MediaControllerManager
    @Inject lateinit var haptics: HapticsManager
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var computeZoneRange: ComputeZoneRangeUseCase
    @Inject lateinit var classifier: ZoneStatusClassifier
    @Inject lateinit var warmupTimer: WarmupTimerUseCase
    @Inject lateinit var pauseDecider: ShouldPauseMusicUseCase
    @Inject lateinit var controller: WorkoutController

    private var sessionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> beginSession()
            ACTION_STOP -> {
                val job = sessionJob
                if (job != null) job.cancel() else finishAndStop()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        sessionJob?.cancel()
        runCatching { media.resumePausedMedia() }
        controller.publish(WorkoutState.Idle)
        super.onDestroy()
    }

    private fun beginSession() {
        if (sessionJob?.isActive == true) return
        promoteToForeground()
        sessionJob = lifecycleScope.launch {
            try {
                runSession()
            } finally {
                finishAndStop()
            }
        }
    }

    private fun finishAndStop() {
        runCatching { media.resumePausedMedia() }
        media.clearTracking()
        pauseDecider.reset()
        controller.publish(WorkoutState.Idle)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun runSession() = coroutineScope {
        val sessionScope: CoroutineScope = this
        val age = settings.age.first()
        val preset = settings.zonePreset.first()
        val warmupDuration = settings.warmupDuration.first()
        val zone: ZoneRange? = age?.let { computeZoneRange(it, preset) }
        if (age == null || zone == null || !health.supportsHeartRate()) return@coroutineScope

        pauseDecider.reset()

        val bpmState = MutableStateFlow<Int?>(null)
        val availabilityState = MutableStateFlow(HrAvailability.ACQUIRING)
        val hapticModeState = MutableStateFlow(HapticMode.DEFAULT)

        launch { settings.hapticMode.collect { hapticModeState.value = it } }

        launch {
            health.updates().collect { update ->
                when (update) {
                    is HealthUpdate.Heartbeat -> bpmState.value = update.bpm
                    is HealthUpdate.AvailabilityChanged -> {
                        availabilityState.value = update.availability
                        if (update.availability != HrAvailability.AVAILABLE) bpmState.value = null
                    }
                    HealthUpdate.ExerciseEnded -> sessionScope.cancel()
                    is HealthUpdate.Error -> Unit
                }
            }
        }

        fun availableBpm(): Int? = bpmState.value.takeIf { availabilityState.value == HrAvailability.AVAILABLE }

        // --- Warmup: stream HR, no zone enforcement ---
        warmupTimer.run(warmupDuration, controller.skipWarmupSignal).collect { event ->
            when (event) {
                is WarmupEvent.Tick -> {
                    val available = availabilityState.value == HrAvailability.AVAILABLE
                    val bpm = availableBpm()
                    val inZone = bpm != null && classifier.classify(bpm, zone) == ZoneStatus.IN_ZONE
                    controller.publish(WorkoutState.Warmup(event.remainingSeconds, bpm, inZone, available))
                }
                WarmupEvent.ThirtySecondsRemaining ->
                    haptics.play(HapticPattern.WARMUP_THIRTY_SECONDS, hapticModeState.value)
                WarmupEvent.TenSecondsRemaining ->
                    haptics.play(HapticPattern.WARMUP_TEN_SECONDS, hapticModeState.value)
                WarmupEvent.Completed ->
                    haptics.play(HapticPattern.WARMUP_COMPLETE, hapticModeState.value)
            }
        }

        // --- Active: enforce the zone ---
        health.registerZoneGoal(zone)
        pauseDecider.reset()

        var lastStatus: ZoneStatus? = null
        var lastKnownStatus = ZoneStatus.IN_ZONE
        var musicPausedByUs = false
        var lastBelowBuzzAt = 0L

        while (isActive) {
            val now = SystemClock.elapsedRealtime()
            val available = availabilityState.value == HrAvailability.AVAILABLE
            val bpm = availableBpm()
            val status: ZoneStatus? = bpm?.let { classifier.classify(it, zone) }
            if (status != null) lastKnownStatus = status
            val shouldPause = pauseDecider.shouldPauseMusic(ZoneSample(bpm, status, isWarmup = false), now)

            if (shouldPause && !musicPausedByUs) {
                media.pauseActiveMedia()
                musicPausedByUs = true
            } else if (!shouldPause && musicPausedByUs) {
                media.resumePausedMedia()
                musicPausedByUs = false
            }

            when (status) {
                ZoneStatus.BELOW -> {
                    val firstBelow = lastStatus != ZoneStatus.BELOW
                    if (firstBelow || (!musicPausedByUs && now - lastBelowBuzzAt >= BELOW_REBUZZ_MS)) {
                        haptics.play(HapticPattern.BELOW_ZONE, hapticModeState.value)
                        lastBelowBuzzAt = now
                    }
                }
                ZoneStatus.ABOVE -> if (lastStatus != ZoneStatus.ABOVE) {
                    haptics.play(HapticPattern.ABOVE_ZONE, hapticModeState.value)
                }
                ZoneStatus.IN_ZONE -> if (lastStatus == ZoneStatus.BELOW) {
                    haptics.play(HapticPattern.BACK_IN_ZONE, hapticModeState.value)
                }
                null -> Unit
            }
            lastStatus = status

            controller.publish(
                WorkoutState.Active(
                    currentBpm = bpm,
                    zoneStatus = lastKnownStatus,
                    hrAvailable = available,
                    musicPaused = musicPausedByUs,
                ),
            )
            delay(TICK_MS)
        }
    }

    private fun promoteToForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_workout_title))
            .setContentText(getString(R.string.notif_workout_text_active))
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_workout),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )
    }

    companion object {
        const val ACTION_START = "com.heartbeats.action.START_WORKOUT"
        const val ACTION_STOP = "com.heartbeats.action.STOP_WORKOUT"

        private const val CHANNEL_ID = "heartbeats_workout"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_MS = 1_000L
        private const val BELOW_REBUZZ_MS = 5_000L
    }
}
