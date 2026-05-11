package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.WarmupDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformWhile
import javax.inject.Inject

/** Events emitted by the warmup countdown. */
sealed interface WarmupEvent {
    data class Tick(val remainingSeconds: Int) : WarmupEvent
    data object ThirtySecondsRemaining : WarmupEvent
    data object TenSecondsRemaining : WarmupEvent
    data object Completed : WarmupEvent
}

/**
 * Drives the warmup countdown. Emits a [WarmupEvent.Tick] every second (starting
 * at the full duration, ending at 0), plus marker events at 30s and 10s remaining,
 * and finally [WarmupEvent.Completed]. Collecting [skipSignal] ends the countdown
 * early with [WarmupEvent.Completed]. Cancelling the collector stops it cleanly.
 */
class WarmupTimerUseCase @Inject constructor() {

    fun run(
        duration: WarmupDuration,
        skipSignal: Flow<Unit> = emptyFlow(),
    ): Flow<WarmupEvent> {
        val ticks: Flow<WarmupEvent> = flow {
            var remaining = duration.seconds
            emit(WarmupEvent.Tick(remaining))
            while (remaining > 0) {
                delay(SECOND_MS)
                remaining -= 1
                emit(WarmupEvent.Tick(remaining))
                when (remaining) {
                    THIRTY_SECONDS -> emit(WarmupEvent.ThirtySecondsRemaining)
                    TEN_SECONDS -> emit(WarmupEvent.TenSecondsRemaining)
                }
            }
            emit(WarmupEvent.Completed)
        }

        val skipMarker: Flow<WarmupEvent> = skipSignal.map { WarmupEvent.Completed }.take(1)

        return merge(ticks, skipMarker).transformWhile { event ->
            emit(event)
            event !is WarmupEvent.Completed
        }
    }

    companion object {
        private const val SECOND_MS = 1_000L
        private const val THIRTY_SECONDS = 30
        private const val TEN_SECONDS = 10
    }
}
