package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** Settings repository (docs 02-architecture.md §3.1, docs 03 §7). */
interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun get(): AppSettings
    suspend fun update(settings: AppSettings)
    suspend fun setTrackingEnabled(enabled: Boolean)
    suspend fun setAiEnabled(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    /** Persists the blocked-apps set (FR-S4). */
    suspend fun setBlockedApps(packages: Set<String>)
    /** Opt-in toggle for Strict/Custom blocking (FR-F4/F8, PRD v0.4 §10.5). */
    suspend fun setFocusBlockingEnabled(enabled: Boolean)
    /** Erase all user data (PRD §10.10 "delete all data"). */
    suspend fun resetAll()
}
