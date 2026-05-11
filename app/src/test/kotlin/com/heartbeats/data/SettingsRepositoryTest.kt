package com.heartbeats.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.heartbeats.data.repository.SettingsRepository
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.WarmupDuration
import com.heartbeats.domain.model.ZonePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testScope = TestScope()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val file = File(tmpFolder.newFolder(), "settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) { file }
        repository = SettingsRepository(dataStore)
    }

    @Test
    fun `defaults are returned on first launch`() = testScope.runTest {
        assertNull(repository.age.first())
        assertTrue(repository.isFirstLaunch.first())
        assertEquals(ZonePreset.FAT_BURN, repository.zonePreset.first())
        assertEquals(WarmupDuration.THREE_MIN, repository.warmupDuration.first())
        assertEquals(HapticMode.BOTH, repository.hapticMode.first())
    }

    @Test
    fun `setters persist and are retrievable`() = testScope.runTest {
        repository.setAge(42)
        repository.setZonePreset(ZonePreset.PEAK)
        repository.setWarmupDuration(WarmupDuration.TEN_MIN)
        repository.setHapticMode(HapticMode.BELOW_ONLY)

        assertEquals(42, repository.age.first())
        assertFalse(repository.isFirstLaunch.first())
        assertEquals(ZonePreset.PEAK, repository.zonePreset.first())
        assertEquals(WarmupDuration.TEN_MIN, repository.warmupDuration.first())
        assertEquals(HapticMode.BELOW_ONLY, repository.hapticMode.first())
    }

    @Test
    fun `completeSetup writes age, preset and warmup atomically`() = testScope.runTest {
        repository.completeSetup(age = 30, preset = ZonePreset.CARDIO, warmup = WarmupDuration.FIVE_MIN)

        assertEquals(30, repository.age.first())
        assertEquals(ZonePreset.CARDIO, repository.zonePreset.first())
        assertEquals(WarmupDuration.FIVE_MIN, repository.warmupDuration.first())
        assertFalse(repository.isFirstLaunch.first())
    }

    @Test
    fun `unrecognized stored enum values fall back to defaults`() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("zone_preset")] = "NONSENSE" }
        assertEquals(ZonePreset.DEFAULT, repository.zonePreset.first())
    }

    @Test
    fun `age flow emits updates`() = testScope.runTest {
        repository.age.test {
            assertNull(awaitItem())
            repository.setAge(25)
            assertEquals(25, awaitItem())
            repository.setAge(26)
            assertEquals(26, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
