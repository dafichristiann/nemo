package com.anchor.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Decides whether to show onboarding or the main shell on launch
 * (docs 07-screens.md §1 flow, PRD §10.10).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** null = not yet known (show nothing / splash); true/false once loaded. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.observe()
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
