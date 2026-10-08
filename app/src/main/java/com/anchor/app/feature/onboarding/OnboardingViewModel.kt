package com.anchor.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding flow state (docs 07-screens.md §2.3–2.4).
 *
 * Onboarding never forces permissions (PRD §10.10) — it only records that it was
 * completed so it does not show again.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    fun complete() = viewModelScope.launch(dispatchers.io) {
        settingsRepository.setOnboardingCompleted(true)
    }
}
