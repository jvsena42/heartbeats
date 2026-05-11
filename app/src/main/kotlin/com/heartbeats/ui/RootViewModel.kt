package com.heartbeats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeats.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Resolves the navigation start destination: `null` while loading, then setup vs. main. */
@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val firstLaunch: StateFlow<Boolean?> = settingsRepository.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
