package com.heartbeats.health

import android.content.Context
import androidx.concurrent.futures.await
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.heartbeats.domain.model.ZoneRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the heart-rate sensor currently has a usable signal. */
enum class HrAvailability { ACQUIRING, AVAILABLE, UNAVAILABLE }

/** Messages surfaced from an active Health Services exercise session. */
sealed interface HealthUpdate {
    data class Heartbeat(val bpm: Int) : HealthUpdate
    data class AvailabilityChanged(val availability: HrAvailability) : HealthUpdate
    data object ExerciseEnded : HealthUpdate
    data class Error(val cause: Throwable) : HealthUpdate
}

/**
 * Thin wrapper around Health Services' [androidx.health.services.client.ExerciseClient].
 * Heartbeats uses an exercise session (not [androidx.health.services.client.MeasureClient])
 * for better battery behavior and goal support, with platform auto-pause disabled so we
 * own the pause logic.
 */
@Singleton
class HealthServicesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val exerciseClient = HealthServices.getClient(context).exerciseClient

    /** Returns true if this device can stream heart rate for our exercise type. */
    suspend fun supportsHeartRate(): Boolean = runCatching {
        val capabilities = exerciseClient.getCapabilitiesAsync().await()
        val typeCapabilities = capabilities.getExerciseTypeCapabilities(EXERCISE_TYPE)
        DataType.HEART_RATE_BPM in typeCapabilities.supportedDataTypes
    }.getOrDefault(false)

    /**
     * Cold flow that starts an exercise session while collected and ends it (clearing the
     * update callback) when collection stops. Heart rate, sensor availability and exercise
     * lifecycle changes are emitted as [HealthUpdate]s.
     */
    fun updates(): Flow<HealthUpdate> = callbackFlow {
        val callback = object : ExerciseUpdateCallback {
            override fun onRegistered() = Unit

            override fun onRegistrationFailed(throwable: Throwable) {
                trySend(HealthUpdate.Error(throwable))
            }

            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                update.latestMetrics.getData(DataType.HEART_RATE_BPM).forEach { sample ->
                    trySend(HealthUpdate.Heartbeat(sample.value.toInt()))
                }
                if (update.exerciseStateInfo.state.isEnded) {
                    trySend(HealthUpdate.ExerciseEnded)
                }
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

            override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
                if (dataType != DataType.HEART_RATE_BPM) return
                val mapped = when (availability) {
                    DataTypeAvailability.AVAILABLE -> HrAvailability.AVAILABLE
                    DataTypeAvailability.ACQUIRING, DataTypeAvailability.UNKNOWN -> HrAvailability.ACQUIRING
                    else -> HrAvailability.UNAVAILABLE
                }
                trySend(HealthUpdate.AvailabilityChanged(mapped))
            }
        }

        exerciseClient.setUpdateCallback(callback)
        try {
            val config = ExerciseConfig(
                exerciseType = EXERCISE_TYPE,
                dataTypes = setOf(DataType.HEART_RATE_BPM),
                isAutoPauseAndResumeEnabled = false,
                isGpsEnabled = false,
            )
            exerciseClient.startExerciseAsync(config).await()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            trySend(HealthUpdate.Error(failure))
        }

        awaitClose {
            // Best effort: the session/callback may already be gone.
            runCatching { exerciseClient.clearUpdateCallbackAsync(callback) }
            runCatching { exerciseClient.endExerciseAsync() }
        }
    }

    /** Registers the below-zone exercise goal. Call once warmup has ended. */
    suspend fun registerZoneGoal(zone: ZoneRange) {
        runCatching {
            exerciseClient.addGoalToActiveExerciseAsync(ExerciseGoalBuilder.belowZoneGoal(zone.lowBpm)).await()
        }
    }

    companion object {
        /** Generic-but-supported exercise type; HEART_RATE_BPM is available for it on Wear devices. */
        val EXERCISE_TYPE: ExerciseType = ExerciseType.RUNNING
    }
}
