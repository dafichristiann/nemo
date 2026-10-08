package com.anchor.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.worker.UsageWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides whether to show onboarding or the main shell on launch
 * (docs 07-screens.md §1 flow, PRD §10.10).
 *
 * Also schedules the Phase-2 usage pipeline **only while tracking is enabled** — privacy
 * default is OFF (PRD FR-S6), so no background collection runs until the user opts in.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val usageWorkScheduler: UsageWorkScheduler,
) : ViewModel() {

    /** null = not yet known (show nothing / splash); true/false once loaded. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.observe()
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Recurring tasks are core (Phase 1) and must run even without tracking (FR-T7).
        usageWorkScheduler.scheduleRecurringTasks()

        // React to opt-in/opt-out of usage tracking. Enqueue is idempotent (KEEP).
        viewModelScope.launch {
            settingsRepository.observe()
                .map { it.trackingEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) usageWorkScheduler.schedulePeriodic()
                }
        }
    }
}

