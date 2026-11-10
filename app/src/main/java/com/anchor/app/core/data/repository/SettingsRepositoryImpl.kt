package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.AppSettingsDao
import com.anchor.app.core.data.entity.AppSettingsEntity
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [SettingsRepository] (docs 03 §7.1). */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingsDao,
) : SettingsRepository {

    private val mutex = Mutex()

    private suspend fun mutateEntity(transform: (AppSettingsEntity) -> AppSettingsEntity) = mutex.withLock {
        val current = dao.get() ?: AppSettingsEntity()
        dao.upsert(transform(current))
    }

    override fun observe(): Flow<AppSettings> =
        dao.observe().map { it?.toDomain() ?: AppSettings() }

    override suspend fun get(): AppSettings = dao.get()?.toDomain() ?: AppSettings()

    override suspend fun update(settings: AppSettings) = mutex.withLock {
        val current = dao.get() ?: AppSettingsEntity()
        val newEntity = settings.toEntity().copy(
            defaultFocusMode = current.defaultFocusMode,
            dailyInterventionQuota = current.dailyInterventionQuota,
            cooldownHours = current.cooldownHours,
        )
        dao.upsert(newEntity)
    }

    override suspend fun setTrackingEnabled(enabled: Boolean) = mutateEntity {
        it.copy(trackingEnabled = enabled)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) = mutateEntity {
        it.copy(notificationsEnabled = enabled)
    }

    override suspend fun setAiEnabled(enabled: Boolean) = mutateEntity {
        it.copy(aiEnabled = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) = mutateEntity {
        it.copy(onboardingCompleted = completed)
    }

    override suspend fun setBlockedApps(packages: Set<String>) = mutateEntity {
        val updatedDomain = it.toDomain().copy(blockedApps = packages)
        it.copy(blockedAppsJson = updatedDomain.toEntity().blockedAppsJson)
    }

    override suspend fun setFocusBlockingEnabled(enabled: Boolean) = mutateEntity {
        it.copy(focusBlockingEnabled = enabled)
    }

    override suspend fun setNickname(nickname: String) = mutateEntity {
        it.copy(nickname = nickname.trim().ifBlank { null })
    }

    override suspend fun setFocusArea(area: FocusArea) = mutateEntity {
        it.copy(focusArea = area.name)
    }

    override suspend fun setDefaultFocusDurationMin(minutes: Int) = mutateEntity {
        it.copy(defaultFocusDurationMin = minutes)
    }

    override suspend fun setDoomscrollAlertEnabled(enabled: Boolean) = mutateEntity {
        it.copy(doomscrollAlertEnabled = enabled)
    }

    override suspend fun setDoomscrollThresholdMinutes(minutes: Int) = mutateEntity {
        it.copy(doomscrollThresholdMinutes = minutes)
    }

    override suspend fun setManualWakeTime(epochDay: Long?, minutes: Int?) = mutateEntity {
        it.copy(manualWakeTimeEpochDay = epochDay, manualWakeTimeMinutes = minutes)
    }

    override suspend fun setLocale(locale: String) {
        val current = get()
        if (current.locale == locale) return
        update(current.copy(locale = locale))
    }

    override suspend fun resetAll() = mutex.withLock {
        dao.clear()
    }
}
