package com.anchor.app.testutil

import com.anchor.app.core.domain.model.AppSettings
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

    override suspend fun resetAll() {
        state.value = AppSettings()
    }
}
