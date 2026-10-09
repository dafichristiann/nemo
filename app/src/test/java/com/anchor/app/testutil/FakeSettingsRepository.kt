package com.anchor.app.testutil

import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsRepository] fake (docs 11-testing-strategy.md §5). Defaults to privacy-first. */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    val state = MutableStateFlow(initial)

    override fun observe(): Flow<AppSettings> = state

    override suspend fun get(): AppSettings = state.value

    override suspend fun update(settings: AppSettings) {
        state.value = settings
    }

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        state.value = state.value.copy(trackingEnabled = enabled)
    }

    override suspend fun setAiEnabled(enabled: Boolean) {
        state.value = state.value.copy(aiEnabled = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        state.value = state.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setBlockedApps(packages: Set<String>) {
        state.value = state.value.copy(blockedApps = packages)
    }

    override suspend fun setFocusBlockingEnabled(enabled: Boolean) {
        state.value = state.value.copy(focusBlockingEnabled = enabled)
    }

    override suspend fun setNickname(nickname: String) {
        state.value = state.value.copy(nickname = nickname.trim())
    }

    override suspend fun setFocusArea(area: FocusArea) {
        state.value = state.value.copy(focusArea = area)
    }

    override suspend fun setDefaultFocusDurationMin(minutes: Int) {
        state.value = state.value.copy(defaultFocusDurationMin = minutes)
    }

    override suspend fun resetAll() {
        state.value = AppSettings()
    }
}
