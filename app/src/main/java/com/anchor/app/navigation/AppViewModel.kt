package com.anchor.app.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.usecase.usage.CategorizeAppsUseCase
import com.anchor.app.platform.accessibility.AccessibilityStatus
import com.anchor.app.platform.apps.InstalledAppsSource
import com.anchor.app.platform.notification.NotificationPermissions
import com.anchor.app.platform.usagestats.UsageStatsSource
import com.anchor.app.worker.UsageWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val settingsRepository: SettingsRepository,
    private val usageWorkScheduler: UsageWorkScheduler,
    private val usageStats: UsageStatsSource,
    private val notificationPermissions: NotificationPermissions,
    @ApplicationContext private val context: Context,
    private val accessibilityStatus: AccessibilityStatus,
    private val installedAppsSource: InstalledAppsSource,
    private val categorizeApps: CategorizeAppsUseCase,
) : ViewModel() {

    private val _onboardingCompletedOverride = MutableStateFlow<Boolean?>(null)

    /** null = not yet known (show nothing / splash); true/false once loaded. */
    val onboardingCompleted: StateFlow<Boolean?> = combine(
        settingsRepository.observe().map { it.onboardingCompleted },
        _onboardingCompletedOverride,
    ) { repoValue, overrideValue ->
        overrideValue ?: repoValue
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Recurring tasks are core (Phase 1) and must run even without tracking (FR-T7).
        usageWorkScheduler.scheduleRecurringTasks()

        // Auto-kategorisasi seluruh aplikasi terinstall ke database
        viewModelScope.launch(Dispatchers.IO) {
            val apps = installedAppsSource.launchableApps().map { it.packageName }
            categorizeApps(apps)
        }

        // React to opt-in/opt-out of usage tracking. Enqueue is idempotent (KEEP); opting out
        // cancels the pipeline so no background collection runs without consent (FR-S6).
        viewModelScope.launch {
            settingsRepository.observe()
                .map { it.trackingEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) usageWorkScheduler.schedulePeriodic()
                    else usageWorkScheduler.cancelPeriodic()
                }
        }
    }

    /**
     * True when all required permissions (Usage Access and Accessibility) are granted.
     * Notifications are optional since in-app overlays act as a fallback.
     */
    fun allRequiredPermissionsGranted(): Boolean =
        usageStats.hasPermission() &&
            accessibilityStatus.isEnabled(context)

    /**
     * Backward-compatible alias delegating to [allRequiredPermissionsGranted].
     */
    fun optionalPermissionsGranted(): Boolean = allRequiredPermissionsGranted()

    /**
     * Persists that the first-run flow is finished, so the splash/permission step never shows
     * again (docs 07-screens.md §2.3: "tidak memaksa izin"). Called once the user leaves
     * onboarding in either direction (Get started/Skip, Continue/Skip for now).
     */
    fun completeOnboarding() {
        _onboardingCompletedOverride.value = true
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }
}
