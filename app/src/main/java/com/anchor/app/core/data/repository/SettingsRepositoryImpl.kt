package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.AppSettingsDao
import com.anchor.app.core.data.entity.AppSettingsEntity
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
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

    override suspend fun resetAll() {
        dao.clear()
    }
}

private fun AppSettingsEntity.toDomain(): AppSettings = AppSettings(
    trackingEnabled = trackingEnabled,
    aiEnabled = aiEnabled,
    notificationsEnabled = notificationsEnabled,
    usageRetentionDays = usageRetentionDays,
    onboardingCompleted = onboardingCompleted,
    locale = locale,
    blockedApps = decodeBlockedApps(blockedAppsJson),
    focusBlockingEnabled = focusBlockingEnabled,
)

private fun AppSettings.toEntity(): AppSettingsEntity = AppSettingsEntity(
    trackingEnabled = trackingEnabled,
    aiEnabled = aiEnabled,
    notificationsEnabled = notificationsEnabled,
    usageRetentionDays = usageRetentionDays,
    onboardingCompleted = onboardingCompleted,
    locale = locale,
    blockedAppsJson = encodeBlockedApps(blockedApps),
    focusBlockingEnabled = focusBlockingEnabled,
)

private fun encodeBlockedApps(apps: Set<String>): String? {
    if (apps.isEmpty()) return null
    val array = JSONArray()
    apps.forEach { array.put(it) }
    return array.toString()
}

private fun decodeBlockedApps(json: String?): Set<String> {
    if (json.isNullOrBlank()) return emptySet()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { array.getString(it) }.toSet()
    }.getOrDefault(emptySet())
}
