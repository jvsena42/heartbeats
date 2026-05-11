package com.heartbeats.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.WarmupDuration
import com.heartbeats.domain.model.ZonePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the small set of user settings backed by Preferences DataStore.
 * First launch is detected by the absence of the [KEY_AGE] entry.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val age: Flow<Int?> = dataStore.data.map { it[KEY_AGE] }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { KEY_AGE !in it }

    val zonePreset: Flow<ZonePreset> = dataStore.data.map { prefs ->
        prefs[KEY_ZONE]?.let { name -> runCatching { ZonePreset.valueOf(name) }.getOrNull() } ?: ZonePreset.DEFAULT
    }

    val warmupDuration: Flow<WarmupDuration> = dataStore.data.map { prefs ->
        prefs[KEY_WARMUP]?.let { name -> runCatching { WarmupDuration.valueOf(name) }.getOrNull() }
            ?: WarmupDuration.DEFAULT
    }

    val hapticMode: Flow<HapticMode> = dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC]?.let { name -> runCatching { HapticMode.valueOf(name) }.getOrNull() } ?: HapticMode.DEFAULT
    }

    suspend fun setAge(age: Int) {
        dataStore.edit { it[KEY_AGE] = age }
    }

    suspend fun setZonePreset(preset: ZonePreset) {
        dataStore.edit { it[KEY_ZONE] = preset.name }
    }

    suspend fun setWarmupDuration(duration: WarmupDuration) {
        dataStore.edit { it[KEY_WARMUP] = duration.name }
    }

    suspend fun setHapticMode(mode: HapticMode) {
        dataStore.edit { it[KEY_HAPTIC] = mode.name }
    }

    /** Persists the full first-run configuration in a single transaction. */
    suspend fun completeSetup(age: Int, preset: ZonePreset, warmup: WarmupDuration) {
        dataStore.edit {
            it[KEY_AGE] = age
            it[KEY_ZONE] = preset.name
            it[KEY_WARMUP] = warmup.name
        }
    }

    companion object {
        const val DATASTORE_NAME = "heartbeats_settings"

        private val KEY_AGE = intPreferencesKey("age")
        private val KEY_ZONE = stringPreferencesKey("zone_preset")
        private val KEY_WARMUP = stringPreferencesKey("warmup_duration")
        private val KEY_HAPTIC = stringPreferencesKey("haptic_mode")
    }
}
