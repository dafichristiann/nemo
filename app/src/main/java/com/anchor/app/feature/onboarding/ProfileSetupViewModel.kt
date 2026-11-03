package com.anchor.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the profile step of onboarding (docs 07-screens.md §2.3b): the three fields we collect
 * from a new user — nickname, main focus area, and default focus-session length.
 *
 * The values are persisted to [SettingsRepository] so they survive process death and are later
 * editable from Settings. Writes are fire-and-forget on the IO dispatcher; the flow back to the
 * UI is the single source of truth for the current selection.
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    /** Current profile settings; drives the screen's selected chips and nickname field. */
    val settings: StateFlow<AppSettings> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun onNicknameChange(nickname: String) = viewModelScope.launch(dispatchers.io) {
        // Cap the length so the greeting (and the field) stay tidy.
        settingsRepository.setNickname(nickname.take(AppSettings.MAX_NICKNAME_LENGTH))
    }

    fun onSelectFocusArea(area: FocusArea) = viewModelScope.launch(dispatchers.io) {
        settingsRepository.setFocusArea(area)
    }

    fun onSelectDuration(minutes: Int) = viewModelScope.launch(dispatchers.io) {
        settingsRepository.setDefaultFocusDurationMin(minutes)
    }

    fun onToggleDoomscroll(enabled: Boolean) = viewModelScope.launch(dispatchers.io) {
        settingsRepository.setDoomscrollAlertEnabled(enabled)
    }

    fun onSelectDoomscrollThreshold(minutes: Int) = viewModelScope.launch(dispatchers.io) {
        settingsRepository.setDoomscrollThresholdMinutes(minutes)
    }
}
