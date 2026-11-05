package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import kotlinx.coroutines.flow.Flow

/** Settings repository (docs 02-architecture.md §3.1, docs 03 §7). */
interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun get(): AppSettings
    suspend fun update(settings: AppSettings)
    suspend fun setTrackingEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setAiEnabled(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    /** Persists the blocked-apps set (FR-S4). */
    suspend fun setBlockedApps(packages: Set<String>)
    /** Opt-in toggle for Strict/Custom blocking (FR-F4/F8, PRD v0.4 §10.5). */
    suspend fun setFocusBlockingEnabled(enabled: Boolean)
    /** Persists the mascot's preferred name for the user (blank clears it). */
    suspend fun setNickname(nickname: String)
    /** Persists the user's main focus area (docs 07-screens.md §2.3b). */
    suspend fun setFocusArea(area: FocusArea)
    /** Persists the default focus-session length in minutes (25/45/60). */
    suspend fun setDefaultFocusDurationMin(minutes: Int)
    /** Toggle the continuous doomscroll alert feature. */
    suspend fun setDoomscrollAlertEnabled(enabled: Boolean) {}
    /** Update continuous scrolling alert threshold in minutes (10/15/20/30/45). */
    suspend fun setDoomscrollThresholdMinutes(minutes: Int) {}
    /** Set manual wake time override for a given date. */
    suspend fun setManualWakeTime(epochDay: Long?, minutes: Int?) {}
    suspend fun setLocale(locale: String)
    /** Erase all user data (PRD §10.10 "delete all data"). */
    suspend fun resetAll()
}
