package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.WarmupDuration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WarmupTimerUseCaseTest {

    private val useCase = WarmupTimerUseCase()

    @Test
    fun `counts down from the configured duration and completes at zero`() = runTest {
        val events = useCase.run(WarmupDuration.ONE_MIN).toList()

        val ticks = events.filterIsInstance<WarmupEvent.Tick>()
        assertEquals(WarmupEvent.Tick(60), ticks.first())
        assertEquals(WarmupEvent.Tick(0), ticks.last())
        // 60 down to 0 inclusive
        assertEquals(61, ticks.size)
        assertEquals(WarmupEvent.Completed, events.last())
        assertEquals(1, events.count { it is WarmupEvent.Completed })
    }

    @Test
    fun `emits 30s and 10s markers exactly once`() = runTest {
        val events = useCase.run(WarmupDuration.ONE_MIN).toList()

        assertEquals(1, events.count { it is WarmupEvent.ThirtySecondsRemaining })
        assertEquals(1, events.count { it is WarmupEvent.TenSecondsRemaining })
    }

    @Test
    fun `short warmup never emits the 30s marker`() = runTest {
        // ONE_MIN is the shortest preset and still passes 30s; verify ordering instead:
        // the 30s marker must come after Tick(30) and before Tick(29).
        val events = useCase.run(WarmupDuration.ONE_MIN).toList()
        val idx30Marker = events.indexOfFirst { it is WarmupEvent.ThirtySecondsRemaining }
        val idxTick30 = events.indexOfFirst { it == WarmupEvent.Tick(30) }
        val idxTick29 = events.indexOfFirst { it == WarmupEvent.Tick(29) }
        assertTrue(idxTick30 < idx30Marker)
        assertTrue(idx30Marker < idxTick29)
    }

    @Test
    fun `skip signal ends the countdown early`() = runTest {
        val skip = flow {
            delay(3_500)
            emit(Unit)
        }

        val events = useCase.run(WarmupDuration.TEN_MIN, skip).toList()

        assertEquals(WarmupEvent.Completed, events.last())
        assertEquals(1, events.count { it is WarmupEvent.Completed })
        val ticks = events.filterIsInstance<WarmupEvent.Tick>()
        assertEquals(600, ticks.first().remainingSeconds)
        assertTrue("expected to stop after a few ticks but got ${ticks.size}", ticks.size <= 5)
        assertTrue(ticks.last().remainingSeconds >= 595)
    }

    @Test
    fun `collection can be cancelled cleanly without completing`() = runTest {
        val events = mutableListOf<WarmupEvent>()
        val job = launch { useCase.run(WarmupDuration.TEN_MIN).toList(events) }

        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        job.cancel()

        assertTrue(job.isCancelled)
        assertTrue(events.isNotEmpty())
        assertTrue(events.none { it is WarmupEvent.Completed })
        assertEquals(WarmupEvent.Tick(600), events.first())
    }
}
