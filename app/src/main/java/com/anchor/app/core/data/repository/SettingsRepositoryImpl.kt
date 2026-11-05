package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.AppSettingsDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [SettingsRepository] (docs 03 §7.1). */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingsDao,
) : SettingsRepository {

    override fun observe(): Flow<AppSettings> =
        dao.observe().map { it?.toDomain() ?: AppSettings() }

    override suspend fun get(): AppSettings = dao.get()?.toDomain() ?: AppSettings()

    override suspend fun update(settings: AppSettings) = dao.upsert(settings.toEntity())

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(trackingEnabled = enabled).toEntity())
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(notificationsEnabled = enabled).toEntity())
    }

    override suspend fun setAiEnabled(enabled: Boolean) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(aiEnabled = enabled).toEntity())
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(onboardingCompleted = completed).toEntity())
    }

    override suspend fun setBlockedApps(packages: Set<String>) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(blockedApps = packages).toEntity())
    }

    override suspend fun setFocusBlockingEnabled(enabled: Boolean) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(focusBlockingEnabled = enabled).toEntity())
    }

    override suspend fun setNickname(nickname: String) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(nickname = nickname.trim()).toEntity())
    }

    override suspend fun setFocusArea(area: FocusArea) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(focusArea = area).toEntity())
    }

    override suspend fun setDefaultFocusDurationMin(minutes: Int) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(defaultFocusDurationMin = minutes).toEntity())
    }

    override suspend fun setDoomscrollAlertEnabled(enabled: Boolean) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(doomscrollAlertEnabled = enabled).toEntity())
    }

    override suspend fun setDoomscrollThresholdMinutes(minutes: Int) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(doomscrollThresholdMinutes = minutes).toEntity())
    }

    override suspend fun setManualWakeTime(epochDay: Long?, minutes: Int?) {
        val current = dao.get()?.toDomain() ?: AppSettings()
        dao.upsert(current.copy(manualWakeTimeEpochDay = epochDay, manualWakeTimeMinutes = minutes).toEntity())
    }

    override suspend fun setLocale(locale: String) {
        val current = get()
        if (current.locale == locale) return
        update(current.copy(locale = locale))
    }

    override suspend fun resetAll() {
        dao.clear()
    }
}
