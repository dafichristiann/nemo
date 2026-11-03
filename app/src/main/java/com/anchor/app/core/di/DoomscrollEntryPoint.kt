package com.anchor.app.core.di

import com.anchor.app.core.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for [com.anchor.app.platform.accessibility.NemoAccessibilityService]
 * to access [SettingsRepository] for continuous doomscroll alerts.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DoomscrollEntryPoint {
    fun settingsRepository(): SettingsRepository
}
